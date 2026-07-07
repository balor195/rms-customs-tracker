package com.rms.customs.notifications

import com.rms.customs.data.local.PlatformContext
import com.rms.customs.domain.model.AppNotification

expect class CustomsNotificationManager(context: PlatformContext) {
    fun createChannels()
    fun postSlaNotification(notification: AppNotification, stableId: Int)
    fun postTransactionClosedNotification(notification: AppNotification, stableId: Int)
}
