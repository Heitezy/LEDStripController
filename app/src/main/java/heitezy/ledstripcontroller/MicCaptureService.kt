package heitezy.ledstripcontroller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Dedicated foreground service for **microphone-mode** Music Sync.
 *
 * Android restricts microphone access for apps that are not in the foreground
 * (from Android 9 onward) and is free to kill/deprioritize a background
 * process's memory once the Activity is no longer visible. Running this
 * foreground service while mic-based Music Sync is active does two things:
 *
 *  1. Keeps the app process alive/high-priority while minimized, so the
 *     coroutine reading from [heitezy.ledstripcontroller.audio.AudioAnalyzer]
 *     keeps running and keeps writing colors to the LED strip over BLE.
 *  2. Keeps microphone access granted while the app has no visible UI.
 *
 * This is intentionally separate from [MediaProjectionService], which is
 * only used for Phone-Audio-mode capture and Screen Sync (both require a
 * `MediaProjection` token). Microphone capture needs no such token, so this
 * service can be started/stopped directly with no consent dialog.
 */
class MicCaptureService : Service() {

    override fun onBind(intent: Intent?): IBinder = Binder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_mic_listening))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setSilent(true)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        // Not restarted by the system if killed — the ViewModel decides when
        // capture should be running and will re-start us explicitly.
        return START_NOT_STICKY
    }

    private fun ensureChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_music_sync_mic_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_music_sync_mic_desc)
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "music_sync_mic_capture"
        const val NOTIFICATION_ID = 1002
    }
}
