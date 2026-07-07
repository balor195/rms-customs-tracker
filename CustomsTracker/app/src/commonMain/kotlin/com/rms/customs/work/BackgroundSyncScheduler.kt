package com.rms.customs.work

import com.rms.customs.data.local.PlatformContext
import com.rms.customs.domain.repository.SyncRepository

expect class BackgroundSyncScheduler(context: PlatformContext, syncRepository: SyncRepository) {
    fun scheduleSync()
}
