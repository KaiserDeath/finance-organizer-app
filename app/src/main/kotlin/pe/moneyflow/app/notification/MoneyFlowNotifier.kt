package pe.moneyflow.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the reminders notification channel and posts the "upcoming payments" summary.
 * Posting is a no-op when the user has not granted POST_NOTIFICATIONS (Android 13+).
 */
@Singleton
class MoneyFlowNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Recordatorios de pagos",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisos de pagos próximos y vencidos"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    /** Posts (or updates) the single reminders summary notification. */
    fun showUpcomingReminder(title: String, body: String) {
        if (!hasPermission()) return
        ensureChannel()

        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(ID_UPCOMING, notification)
    }

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        private const val ID_UPCOMING = 1001
    }
}
