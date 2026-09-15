package heitezy.ledstripcontroller.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

data class FrequencyData(
    val bass: Float,        // 0–1  (60–250 Hz)   → red
    val mid: Float,         // 0–1  (250–4000 Hz)  → green
    val high: Float,        // 0–1  (4000–16000 Hz) → blue
    val isBeat: Boolean,
)

object AudioAnalyzer {

    private const val SAMPLE_RATE = 44100
    private const val FFT_SIZE = 4096

    private val BASS_LOW_BIN  = hzToBin(60)
    private val BASS_HIGH_BIN = hzToBin(250)
    private val MID_LOW_BIN   = hzToBin(250)
    private val MID_HIGH_BIN  = hzToBin(4000)
    private val HIGH_LOW_BIN  = hzToBin(4000)
    private val HIGH_HIGH_BIN = hzToBin(16000)

    private fun hzToBin(hz: Int) =
        (hz.toLong() * FFT_SIZE / SAMPLE_RATE).toInt().coerceIn(1, FFT_SIZE / 2 - 1)

    // ── Mic stream ────────────────────────────────────────────────────────────

    fun streamMic(): Flow<FrequencyData> = flow {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuf <= 0) return@flow
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, FFT_SIZE * 2),
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            return@flow  // no microphone available on this device
        }
        processAudioRecord(audioRecord, stereo = false) { emit(it) }
    }.flowOn(Dispatchers.Default)

    // ── Internal audio / playback capture (Android 10+) ──────────────────────

    @RequiresApi(Build.VERSION_CODES.Q)
    fun streamPlayback(mediaProjection: MediaProjection): Flow<FrequencyData> = flow {
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)   // playback capture is always stereo
            .build()

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT,
        )
        val audioRecord = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(config)
            .setAudioFormat(format)
            .setBufferSizeInBytes(maxOf(minBuf, FFT_SIZE * 4))
            .build()

        processAudioRecord(audioRecord, stereo = true) { emit(it) }
    }.flowOn(Dispatchers.Default)

    // ── Shared processing core ────────────────────────────────────────────────

    private suspend fun processAudioRecord(
        audioRecord: AudioRecord,
        stereo: Boolean,
        emit: suspend (FrequencyData) -> Unit,
    ) {
        val channelCount = if (stereo) 2 else 1
        val pcm = ShortArray(FFT_SIZE * channelCount)
        val mono = ShortArray(FFT_SIZE)
        val real = FloatArray(FFT_SIZE)
        val imag = FloatArray(FFT_SIZE)
        val energyHistory = ArrayDeque<Float>(50)
        // [peakBass, peakMid, peakHigh] — dynamic normalization per band
        val peaks = floatArrayOf(0.001f, 0.001f, 0.001f)

        try {
            audioRecord.startRecording()
            while (currentCoroutineContext().isActive) {
                val read = audioRecord.read(pcm, 0, FFT_SIZE * channelCount)
                if (read <= 0) continue

                val frames = if (stereo) read / 2 else read

                if (stereo) {
                    for (i in 0 until frames) {
                        mono[i] = ((pcm[i * 2].toInt() + pcm[i * 2 + 1].toInt()) / 2).toShort()
                    }
                } else {
                    pcm.copyInto(mono, endIndex = frames)
                }

                emit(processMonoBuffer(mono, frames, real, imag, energyHistory, peaks))
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }
    }

    private fun processMonoBuffer(
        buffer: ShortArray,
        size: Int,
        real: FloatArray,
        imag: FloatArray,
        energyHistory: ArrayDeque<Float>,
        peaks: FloatArray,
    ): FrequencyData {
        // Hann window + normalise to [-1, 1]
        for (i in 0 until size) {
            val w = 0.5f * (1f - cos(2f * PI.toFloat() * i / (size - 1)))
            real[i] = buffer[i] * w / 32768f
        }
        for (i in size until FFT_SIZE) real[i] = 0f
        imag.fill(0f)

        fft(real, imag)

        val mag = FloatArray(FFT_SIZE / 2) { i -> sqrt(real[i] * real[i] + imag[i] * imag[i]) }

        val bassRaw = bandAvg(mag, BASS_LOW_BIN, BASS_HIGH_BIN)
        val midRaw  = bandAvg(mag, MID_LOW_BIN,  MID_HIGH_BIN)
        val highRaw = bandAvg(mag, HIGH_LOW_BIN,  HIGH_HIGH_BIN)

        // Beat: bass-weighted energy vs rolling 4-second average
        val beatEnergy = bassRaw * 3f + midRaw * 0.5f
        energyHistory.addLast(beatEnergy)
        if (energyHistory.size > 43) energyHistory.removeFirst()
        val avg = energyHistory.average().toFloat()
        val isBeat = beatEnergy > avg * 1.4f && beatEnergy > 0.005f

        // Slow-decay peak per band → keeps bars full-range regardless of volume
        val decay = 0.995f
        peaks[0] = max(peaks[0] * decay, bassRaw)
        peaks[1] = max(peaks[1] * decay, midRaw)
        peaks[2] = max(peaks[2] * decay, highRaw)

        return FrequencyData(
            bass = (bassRaw / peaks[0]).coerceIn(0f, 1f),
            mid  = (midRaw  / peaks[1]).coerceIn(0f, 1f),
            high = (highRaw / peaks[2]).coerceIn(0f, 1f),
            isBeat = isBeat,
        )
    }

    private fun bandAvg(mag: FloatArray, lo: Int, hi: Int): Float {
        var sum = 0f
        for (i in lo..hi.coerceAtMost(mag.lastIndex)) sum += mag[i]
        return sum / (hi - lo + 1)
    }

    // ── Cooley-Tukey in-place FFT (radix-2) ──────────────────────────────────

    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        var j = 0
        for (i in 1 until n) {
            var bit = n ushr 1
            while (j and bit != 0) { j = j xor bit; bit = bit ushr 1 }
            j = j xor bit
            if (i < j) {
                var t = real[i]; real[i] = real[j]; real[j] = t
                t = imag[i]; imag[i] = imag[j]; imag[j] = t
            }
        }
        var len = 2
        while (len <= n) {
            val half = len ushr 1
            val ang = -2.0 * PI / len
            val wR = cos(ang).toFloat()
            val wI = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var curR = 1f; var curI = 0f
                for (k in 0 until half) {
                    val eR = real[i + k];          val eI = imag[i + k]
                    val oR = real[i+k+half]*curR - imag[i+k+half]*curI
                    val oI = real[i+k+half]*curI + imag[i+k+half]*curR
                    real[i+k] = eR+oR;             imag[i+k] = eI+oI
                    real[i+k+half] = eR-oR;        imag[i+k+half] = eI-oI
                    val nr = curR*wR - curI*wI;    curI = curR*wI + curI*wR;  curR = nr
                }
                i += len
            }
            len = len shl 1
        }
    }
}
