package com.rms.customs.notifications

import com.rms.customs.data.local.PlatformContext
import com.rms.customs.domain.model.AppNotification
import com.rms.customs.domain.model.enums.NotificationType
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationInterruptionLevelActive
import platform.UserNotifications.UNNotificationInterruptionLevelTimeSensitive
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

// No Info.plist/entitlements changes needed for local (non-remote) notifications, unlike Keychain
// or BGTaskScheduler. A bare Kotlin/Native test binary isn't a real, foreground-registered app
// though (no bundle identity for the notification system to authorize against) - the same class of
// gap that made Phase 4b's Keychain round-trip untestable there - so this sub-phase's CI
// verification is compilation only, same as Phase 4c; no iosTest behavioral test is written.
actual class CustomsNotificationManager actual constructor(context: PlatformContext) {

    actual fun createChannels() {
        // iOS has no channel concept - the closest equivalent to "prepare the notification system
        // once at startup" is requesting authorization.
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(options) { _, _ -> }
    }

    actual fun postSlaNotification(notification: AppNotification, stableId: Int) {
        val isEscalated = notification.type == NotificationType.SLA_ESCALATED
        post(
            titleAr = notification.titleAr,
            messageAr = notification.messageAr,
            transactionId = notification.transactionId,
            stableId = stableId,
            timeSensitive = isEscalated,
        )
    }

    actual fun postTransactionClosedNotification(notification: AppNotification, stableId: Int) {
        post(
            titleAr = notification.titleAr,
            messageAr = notification.messageAr,
            transactionId = notification.transactionId,
            stableId = stableId,
            timeSensitive = false,
        )
    }

    // transaction_id is stashed in userInfo for Phase 5's eventual tap-to-navigate handling - no
    // PendingIntent equivalent is wired here, same as this project's other iOS actuals not being
    // hooked into live UI/DI yet.
    private fun post(titleAr: String, messageAr: String, transactionId: String, stableId: Int, timeSensitive: Boolean) {
        val content = UNMutableNotificationContent().apply {
            setTitle(titleAr)
            setBody(messageAr)
            setUserInfo(mapOf("transaction_id" to transactionId))
            setInterruptionLevel(
                if (timeSensitive) UNNotificationInterruptionLevelTimeSensitive else UNNotificationInterruptionLevelActive
            )
        }
        val request = UNNotificationRequest(
            identifier = stableId.toString(),
            content = content,
            trigger = null,
        )
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, null)
    }
}
