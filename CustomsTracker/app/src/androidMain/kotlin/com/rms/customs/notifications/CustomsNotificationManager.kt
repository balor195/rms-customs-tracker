package com.rms.customs.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.rms.customs.MainActivity
import com.rms.customs.domain.model.AppNotification
import com.rms.customs.domain.model.NotificationType

class CustomsNotificationManager(
    private val context: Context,
) {
    companion object {
        const val CHANNEL_SLA_BREACH        = "customs_sla_breach"
        const val CHANNEL_SLA_ESCALATED     = "customs_sla_escalated"
        const val CHANNEL_TRANSACTION_CLOSED = "customs_transaction_closed"
    }

    private val notifManager: NotificationManager by lazy {
        context.getSystemService(NotificationManager::class.java)
    }

    fun createChannels() {
        notifManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SLA_BREACH,
                "تنبيهات تجاوز المدة",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "تنبيهات تجاوز أهداف الوقت الزمنية للمراحل" }
        )
        notifManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SLA_ESCALATED,
                "تصعيد — تجاوز حرج",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "تنبيهات تصعيد للمعاملات المتأخرة بشكل حرج" }
        )
        notifManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TRANSACTION_CLOSED,
                "إغلاق المعاملات",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "تنبيه عند نقل شحنة معاملة إلى المستودعات وإغلاقها" }
        )
    }

    fun postSlaNotification(notification: AppNotification, stableId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("transaction_id", notification.transactionId.toString())
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            stableId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val isEscalated = notification.type == NotificationType.SLA_ESCALATED
        val channel  = if (isEscalated) CHANNEL_SLA_ESCALATED else CHANNEL_SLA_BREACH
        val priority = if (isEscalated) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT

        val built = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(notification.titleAr)
            .setContentText(notification.messageAr)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.messageAr))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notifManager.notify(stableId, built)
    }

    fun postTransactionClosedNotification(notification: AppNotification, stableId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("transaction_id", notification.transactionId.toString())
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            stableId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val built = NotificationCompat.Builder(context, CHANNEL_TRANSACTION_CLOSED)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notification.titleAr)
            .setContentText(notification.messageAr)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.messageAr))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notifManager.notify(stableId, built)
    }
}
