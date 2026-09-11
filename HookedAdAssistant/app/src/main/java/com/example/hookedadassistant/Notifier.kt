package com.example.hookedadassistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

object Notifier {
    private const val CHANNEL_ID = "ad_action_silent_v2"

    fun notifyActionNeeded(context: Context, message: String) {
        if (!Prefs.assistantOn(context)) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "Ad-Hinweise (lautlos)", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
            description = "Lautlose Hinweise des Hooked Ad Assistant"
        }
        nm.createNotificationChannel(channel)

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Hooked Ad Assistant")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        nm.notify(1001, n)

        if (Prefs.vibrate(context)) {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        StatsRepository(context).addAlert()
    }
}
