package heitezy.ledstripcontroller.screen

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.pow

data class ScreenColor(val r: Int, val g: Int, val b: Int)

object ScreenAnalyzer {

    private const val CAPTURE_WIDTH = 160
    private const val FRAME_INTERVAL_MS = 50L   // 20 FPS cap
    private const val SAMPLE_STRIDE = 4

    // Dominant-hue histogram. Narrow bins (15° each) keep in-bin colors close
    // enough that averaging them together doesn't muddy the result the way
    // averaging the whole frame's raw RGB does (e.g. red + cyan pixels
    // averaging to gray even though neither color is actually on screen much).
    private const val HUE_BINS = 24
    private const val MIN_SATURATION = 0.15f        // below this, a pixel is "gray" for hue purposes
    private const val SATURATION_BOOST = 1.25f      // a diffuse strip reads less saturated than an emissive screen
    private const val BRIGHTNESS_BLEND = 0.5f       // how strongly overall scene brightness pulls the result's value
    private const val MIN_COLOR_SHARE = 0.02       // saturated-weight fraction needed to trust a hue over "gray"

    // --- sRGB <-> linear-light helpers -------------------------------------------------
    // Screen pixels are gamma-encoded (sRGB). Summing/averaging those encoded byte
    // values directly (as the previous implementation did) is not how light actually
    // combines — it skews results toward mid-gray and desaturates the outcome.
    // Converting to linear light before combining, then back to sRGB afterward,
    // gives a perceptually correct blend.
    private val SRGB_TO_LINEAR = FloatArray(256) { i ->
        val c = i / 255f
        if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }

    private fun linearToSrgb(c: Float): Int {
        val clamped = c.coerceIn(0f, 1f)
        val srgb = if (clamped <= 0.0031308f) clamped * 12.92f
                   else 1.055f * clamped.pow(1f / 2.4f) - 0.055f
        return (srgb * 255f).toInt().coerceIn(0, 255)
    }

    @RequiresApi(29)
    fun stream(mediaProjection: MediaProjection, context: Context): Flow<ScreenColor> = callbackFlow {
        val metrics = context.resources.displayMetrics
        val captureHeight = (CAPTURE_WIDTH * metrics.heightPixels.toFloat() / metrics.widthPixels).toInt()

        val handlerThread = HandlerThread("elk-screen-capture").also { it.start() }
        val handler = Handler(handlerThread.looper)

        val reader = ImageReader.newInstance(CAPTURE_WIDTH, captureHeight, PixelFormat.RGBA_8888, 2)

        var lastFrameMs = 0L

        // Reusable scratch buffers so nothing is allocated per-frame.
        val binWeight = DoubleArray(HUE_BINS)
        val binLinR = DoubleArray(HUE_BINS)
        val binLinG = DoubleArray(HUE_BINS)
        val binLinB = DoubleArray(HUE_BINS)
        val hsv = FloatArray(3)

        reader.setOnImageAvailableListener({ r ->
            val now = System.currentTimeMillis()
            if (now - lastFrameMs < FRAME_INTERVAL_MS) {
                r.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }
            lastFrameMs = now

            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            image.use { image ->
                val plane = image.planes[0]
                val buffer = plane.buffer
                val rowStride = plane.rowStride
                val pixelStride = plane.pixelStride
                val w = image.width
                val h = image.height
                val limit = buffer.limit()

                java.util.Arrays.fill(binWeight, 0.0)
                java.util.Arrays.fill(binLinR, 0.0)
                java.util.Arrays.fill(binLinG, 0.0)
                java.util.Arrays.fill(binLinB, 0.0)

                var overallWeight = 0.0
                var overallLinR = 0.0
                var overallLinG = 0.0
                var overallLinB = 0.0
                var overallValueSum = 0.0
                var overallValueWeight = 0.0

                var py = 0
                while (py < h) {
                    var px = 0
                    while (px < w) {
                        val off = py * rowStride + px * pixelStride
                        if (off + 3 >= limit) { px += SAMPLE_STRIDE; continue }
                        val rv = buffer[off].toInt() and 0xFF
                        val gv = buffer[off + 1].toInt() and 0xFF
                        val bv = buffer[off + 2].toInt() and 0xFF

                        Color.RGBToHSV(rv, gv, bv, hsv)
                        val hue = hsv[0]   // 0..360
                        val sat = hsv[1]   // 0..1
                        val v = hsv[2]     // 0..1

                        val linR = SRGB_TO_LINEAR[rv]
                        val linG = SRGB_TO_LINEAR[gv]
                        val linB = SRGB_TO_LINEAR[bv]

                        // Overall scene brightness: bright pixels dominate perception
                        // regardless of how colorful they are, so weight by v^2.
                        val brightnessWeight = (v * v).toDouble() + 0.01
                        overallValueSum += v * brightnessWeight
                        overallValueWeight += brightnessWeight
                        overallLinR += linR * brightnessWeight
                        overallLinG += linG * brightnessWeight
                        overallLinB += linB * brightnessWeight
                        overallWeight += brightnessWeight

                        if (sat >= MIN_SATURATION) {
                            val weight = (sat * v).toDouble()
                            val bin = ((hue / 360f) * HUE_BINS).toInt().coerceIn(0, HUE_BINS - 1)
                            binWeight[bin] += weight
                            binLinR[bin] += linR * weight
                            binLinG[bin] += linG * weight
                            binLinB[bin] += linB * weight
                        }

                        px += SAMPLE_STRIDE
                    }
                    py += SAMPLE_STRIDE
                }

                if (overallWeight > 0) {
                    // Smooth the histogram across neighboring bins (circular) so a
                    // color that straddles a bin edge isn't lost to noise.
                    var bestBin = -1
                    var bestScore = 0.0
                    for (i in 0 until HUE_BINS) {
                        val prev = binWeight[(i - 1 + HUE_BINS) % HUE_BINS]
                        val next = binWeight[(i + 1) % HUE_BINS]
                        val score = prev * 0.25 + binWeight[i] + next * 0.25
                        if (score > bestScore) {
                            bestScore = score
                            bestBin = i
                        }
                    }

                    val saturatedWeightTotal = binWeight.sum()
                    val overallAvgValue = overallValueSum / overallValueWeight

                    val (fr, fg, fb) = if (bestBin == -1 || saturatedWeightTotal < overallWeight * MIN_COLOR_SHARE) {
                        // The frame is essentially gray/washed out (a text-heavy or
                        // white UI, a black loading screen, etc.) — no single hue
                        // dominates enough to trust. Fall back to a neutral tone at
                        // the scene's actual brightness instead of amplifying
                        // whatever faint color cast happened to sample highest.
                        Triple(
                            linearToSrgb((overallLinR / overallWeight).toFloat()),
                            linearToSrgb((overallLinG / overallWeight).toFloat()),
                            linearToSrgb((overallLinB / overallWeight).toFloat()),
                        )
                    } else {
                        val prevBin = (bestBin - 1 + HUE_BINS) % HUE_BINS
                        val nextBin = (bestBin + 1) % HUE_BINS
                        val w = binWeight[bestBin] + binWeight[prevBin] * 0.25 + binWeight[nextBin] * 0.25
                        val lr = (binLinR[bestBin] + binLinR[prevBin] * 0.25 + binLinR[nextBin] * 0.25) / w
                        val lg = (binLinG[bestBin] + binLinG[prevBin] * 0.25 + binLinG[nextBin] * 0.25) / w
                        val lb = (binLinB[bestBin] + binLinB[prevBin] * 0.25 + binLinB[nextBin] * 0.25) / w

                        val dr = linearToSrgb(lr.toFloat())
                        val dg = linearToSrgb(lg.toFloat())
                        val db = linearToSrgb(lb.toFloat())

                        // Re-grade in HSV: keep the dominant hue, pull value toward the
                        // scene's overall brightness (so a dim scene stays dim even if the
                        // winning hue came from a small bright accent), and boost
                        // saturation slightly since a diffuse LED strip reads less vivid
                        // than the same color on an emissive screen.
                        Color.RGBToHSV(dr, dg, db, hsv)
                        hsv[1] = (hsv[1] * SATURATION_BOOST).coerceIn(0f, 1f)
                        hsv[2] = (hsv[2] * (1 - BRIGHTNESS_BLEND) + overallAvgValue.toFloat() * BRIGHTNESS_BLEND)
                            .coerceIn(0f, 1f)
                        val packed = Color.HSVToColor(hsv)
                        Triple(Color.red(packed), Color.green(packed), Color.blue(packed))
                    }

                    trySend(ScreenColor(
                        r = fr.coerceIn(0, 255),
                        g = fg.coerceIn(0, 255),
                        b = fb.coerceIn(0, 255),
                    ))
                }
            }
        }, handler)

        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                channel.close()
            }
        }
        mediaProjection.registerCallback(callback, handler)

        val display = mediaProjection.createVirtualDisplay(
            "elk-screen",
            CAPTURE_WIDTH,
            captureHeight,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler,
        )

        awaitClose {
            mediaProjection.unregisterCallback(callback)
            display.release()
            reader.close()
            handlerThread.quit()
        }
    }
}
