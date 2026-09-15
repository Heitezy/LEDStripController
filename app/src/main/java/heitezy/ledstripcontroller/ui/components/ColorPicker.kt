package heitezy.ledstripcontroller.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import heitezy.ledstripcontroller.R
import heitezy.ledstripcontroller.ui.hsvToRgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    hue: Float,
    saturation: Float,
    colorValue: Float,
    onHueSaturationChanged: (Float, Float) -> Unit,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(stringResource(R.string.label_color), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        ColorWheel(
            hue = hue,
            saturation = saturation,
            onColorSelected = onHueSaturationChanged,
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(16.dp))

        // Brightness/value slider
        val previewColor = Color(
            hsvToRgb(hue, saturation, colorValue).let { (r, g, b) ->
                android.graphics.Color.rgb(r, g, b)
            }
        )
        Text(
            stringResource(R.string.label_brightness, (colorValue * 100).toInt()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = colorValue,
            onValueChange = onValueChanged,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = previewColor,
                activeTrackColor = previewColor.copy(alpha = 0.8f),
            ),
        )

        Spacer(Modifier.height(8.dp))

        // Color preview swatch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(previewColor),
        )

        Spacer(Modifier.height(12.dp))

        // Preset quick-select colors
        val presets = listOf(
            0f to 1f,   // Red
            30f to 1f,  // Orange
            60f to 1f,  // Yellow
            120f to 1f, // Green
            180f to 1f, // Cyan
            210f to 1f, // Sky blue
            240f to 1f, // Blue
            270f to 1f, // Violet
            300f to 1f, // Magenta
            0f to 0f,   // White
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { (h, s) ->
                val c = Color(
                    hsvToRgb(h, s, 1f).let { (r, g, b) -> android.graphics.Color.rgb(r, g, b) }
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(
                            width = if (h == hue && s == saturation) 2.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape,
                        )
                        .pointerInput(h, s) {
                            awaitEachGesture {
                                awaitFirstDown()
                                onHueSaturationChanged(h, s)
                            }
                        },
                )
            }
        }
    }
}

// ── Color Wheel ───────────────────────────────────────────────────────────────

@Composable
private fun ColorWheel(
    hue: Float,
    saturation: Float,
    onColorSelected: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var componentSize by remember { mutableStateOf(Size.Zero) }
    var wheelBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(componentSize) {
        if (componentSize.width > 0f) {
            wheelBitmap = withContext(Dispatchers.Default) {
                buildColorWheelBitmap(componentSize.width.toInt(), componentSize.height.toInt())
                    .asImageBitmap()
            }
        }
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { sz ->
                componentSize = Size(sz.width.toFloat(), sz.height.toFloat())
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pickColor(down.position, componentSize, onColorSelected)
                    drag(down.id) { change ->
                        pickColor(change.position, componentSize, onColorSelected)
                    }
                }
            },
    ) {
        wheelBitmap?.let { bmp ->
            drawImage(bmp)
        }

        if (componentSize.width > 0f) {
            val cx = componentSize.width / 2f
            val cy = componentSize.height / 2f
            val radius = min(componentSize.width, componentSize.height) / 2f
            val angleRad = hue * PI.toFloat() / 180f
            val ix = cx + cos(angleRad) * saturation * radius
            val iy = cy + sin(angleRad) * saturation * radius

            drawCircle(Color.White, radius = 14f, center = Offset(ix, iy))
            drawCircle(
                Color.Black, radius = 14f, center = Offset(ix, iy),
                style = Stroke(width = 3f),
            )
        }
    }
}

private fun pickColor(pos: Offset, size: Size, onColorSelected: (Float, Float) -> Unit) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = min(size.width, size.height) / 2f
    val dx = pos.x - cx
    val dy = pos.y - cy
    val dist = sqrt(dx * dx + dy * dy)
    val sat = (dist / radius).coerceIn(0f, 1f)
    val hue = (atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f
    onColorSelected(hue, sat)
}

private fun buildColorWheelBitmap(width: Int, height: Int): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val cx = width / 2f
    val cy = height / 2f
    val radius = min(width, height) / 2f
    val hsv = FloatArray(3); hsv[2] = 1f

    for (y in 0 until height) {
        for (x in 0 until width) {
            val dx = x - cx
            val dy = y - cy
            val dist = sqrt(dx * dx + dy * dy)
            if (dist <= radius) {
                hsv[0] = (atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f
                hsv[1] = (dist / radius).coerceIn(0f, 1f)
                bmp.setPixel(x, y, android.graphics.Color.HSVToColor(hsv))
            }
        }
    }
    return bmp
}
