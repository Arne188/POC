package com.example.hookedadassistant
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
object Notifier {
    private const val CHANNEL_ID="ad_action_channel"
    fun notifyActionNeeded(context: Context, message: String) {
        val nm=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID,"Ad-Aktionen",NotificationManager.IMPORTANCE_HIGH))
        val n=NotificationCompat.Builder(context,CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Hooked Ad Assistant")
            .setContentText(message).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
        nm.notify(1001,n)
        if(Prefs.vibrate(context)) {
            val v=context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createOneShot(300,VibrationEffect.DEFAULT_AMPLITUDE))
        }
        StatsRepository(context).addAlert()
    }
}
