package app.termosh.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val CHANNEL_ID = "termosh_command_finish"
        private const val NOTIF_ID_BASE = 2000
    }

    init {
        createChannel()
    }

    fun notifyFinished(serverName: String, command: String) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        val openIntent = Intent().apply {
            setClassName(context.packageName, "app.termosh.MainActivity")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val preview = command.lineSequence().firstOrNull()?.take(60) ?: ""
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Команда завершена — $serverName")
            .setContentText(preview)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        mgr.notify(NOTIF_ID_BASE + serverName.hashCode(), notif)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Завершение команд",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Уведомления о завершении длинных команд"
            }
            mgr.createNotificationChannel(ch)
        }
    }
}
