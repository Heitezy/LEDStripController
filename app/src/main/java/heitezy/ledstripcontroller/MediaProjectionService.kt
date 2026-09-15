package heitezy.ledstripcontroller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Shared foreground service backing anything that consumes the app's
 * [android.media.projection.MediaProjection] token: Phone-Audio Music Sync
 * and Screen Sync. Both need an active foreground service of type
 * `mediaProjection` while capturing, so rather than run two nearly-identical
 * services, this one is reused and simply told (via [EXTRA_REASON]) which
 * feature is currently using it, so the notification text stays accurate.
 */
class MediaProjectionService : Service() {

    enum class Reason { PHONE_AUDIO, SCREEN }

    override fun onBind(intent: Intent): IBinder = Binder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        val reason = intent?.getStringExtra(EXTRA_REASON)?.let {
            runCatching { Reason.valueOf(it) }.getOrNull()
        }
        val text = when (reason) {
            Reason.SCREEN -> getString(R.string.notif_screen_capture)
            Reason.PHONE_AUDIO, null -> getString(R.string.notif_phone_audio_capture)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setSilent(true)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    private fun ensureChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_media_capture_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_media_capture_desc)
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "music_sync_capture"
        const val NOTIFICATION_ID = 1001
        private const val EXTRA_REASON = "reason"

        /** Builds the start/update intent, carrying which feature is using the projection. */
        fun intent(context: Context, reason: Reason): Intent =
            Intent(context, MediaProjectionService::class.java)
                .putExtra(EXTRA_REASON, reason.name)
    }
}
