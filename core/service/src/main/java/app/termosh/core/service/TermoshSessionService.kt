package app.termosh.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TermoshSessionService : Service() {

    companion object {
        private const val CHANNEL_ID = "termosh_session"
        private const val NOTIF_ID = 1001

        const val ACTION_START = "app.termosh.action.START"
        const val ACTION_STOP = "app.termosh.action.STOP"
        const val ACTION_DISCONNECT = "app.termosh.action.DISCONNECT"
        const val ACTION_UPDATE = "app.termosh.action.UPDATE"

        const val EXTRA_TITLE = "title"
        const val EXTRA_STATUS = "status"

        fun start(context: Context, title: String) {
            val i = Intent(context, TermoshSessionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            val i = Intent(context, TermoshSessionService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(i)
        }

        fun updateStatus(context: Context, status: String) {
            val i = Intent(context, TermoshSessionService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_STATUS, status)
            }
            context.startService(i)
        }
    }

    private var currentTitle: String = "Termosh session"
    private var currentStatus: String = "Терминальная сессия активна"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_DISCONNECT -> {
                SessionControl.requestDisconnectAll()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                intent.getStringExtra(EXTRA_STATUS)?.let { currentStatus = it }
                updateNotification()
                return START_STICKY
            }
            else -> {
                intent?.getStringExtra(EXTRA_TITLE)?.let { currentTitle = it }
                startForeground(NOTIF_ID, buildNotification())
            }
        }
        return START_STICKY
    }

    private fun updateNotification() {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(NOTIF_ID, buildNotification())
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Termosh sessions",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Активные SSH/mosh сессии"
                setShowBadge(false)
            }
            mgr.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent().apply {
            setClassName(packageName, "app.termosh.MainActivity")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val disconnectIntent = Intent(this, TermoshSessionService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPi = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText(currentStatus)
            .setSmallIcon(R.drawable.ic_stat_termosh)
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отключить", disconnectPi)
            .build()
    }
}
