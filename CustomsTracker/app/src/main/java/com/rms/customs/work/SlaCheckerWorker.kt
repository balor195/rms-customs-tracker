package com.rms.customs.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rms.customs.domain.model.AppNotification
import com.rms.customs.domain.model.NotificationType
import com.rms.customs.domain.repository.NotificationRepository
import com.rms.customs.domain.repository.SlaRepository
import com.rms.customs.domain.repository.TransactionRepository
import com.rms.customs.notifications.CustomsNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID
import java.util.concurrent.TimeUnit

@HiltWorker
class SlaCheckerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val slaRepository: SlaRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationManager: CustomsNotificationManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now      = System.currentTimeMillis()
        val since24h = now - TimeUnit.HOURS.toMillis(24)

        val activePhases = transactionRepository.getActivePhasesForSlaCheck()

        for (phase in activePhases) {
            val startedAt = phase.startedAt ?: continue
            val sla = slaRepository.getForSubPhase(phase.phaseNumber, phase.subPhase) ?: continue
            if (!sla.isActive) continue

            val daysSinceStart = TimeUnit.MILLISECONDS.toDays(now - startedAt).toInt()

            val type = when {
                daysSinceStart >= sla.escalationAfterDays -> NotificationType.SLA_ESCALATED
                daysSinceStart >= sla.targetDays          -> NotificationType.SLA_BREACH
                else                                      -> continue
            }

            // Deduplicate: at most one notification per (transaction + type) per 24 hours
            if (notificationRepository.countRecentForTx(phase.transactionId, type, since24h) > 0) continue

            val overdueDays  = daysSinceStart - sla.targetDays
            val phaseLabel   = subPhaseLabel(phase.phaseNumber, phase.subPhase)
            val isEscalated  = type == NotificationType.SLA_ESCALATED

            val notification = AppNotification(
                id            = UUID.randomUUID(),
                transactionId = phase.transactionId,
                type          = type,
                titleAr       = if (isEscalated) "تصعيد: $phaseLabel" else "تجاوز المدة: $phaseLabel",
                titleEn       = if (isEscalated) "Escalated: $phaseLabel" else "SLA Breach: $phaseLabel",
                messageAr     = "تجاوزت المعاملة الهدف الزمني بـ $overdueDays يوم — $phaseLabel",
                messageEn     = "Transaction exceeded SLA target by $overdueDays day(s) — $phaseLabel",
                isRead        = false,
                createdAt     = now,
            )

            notificationRepository.create(notification)
            // Stable Android notification ID = phaseRecord UUID hash → same phase updates same notification
            notificationManager.postSlaNotification(notification, phase.id.hashCode())
        }

        return Result.success()
    }

    private fun subPhaseLabel(phaseNumber: Int, subPhase: String): String = when (subPhase) {
        "4.1"       -> "القيادة العامة للقوات المسلحة"
        "4.2"       -> "الجمارك الأردنية"
        "4.3"       -> "هيئة الغذاء والدواء الأردنية"
        else        -> "المرحلة $phaseNumber"
    }

    companion object {
        const val WORK_NAME = "sla_checker_periodic"
    }
}
