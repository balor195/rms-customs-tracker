package com.rms.customs.work

import com.rms.customs.data.local.PlatformContext
import com.rms.customs.domain.repository.SyncRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate

private const val SYNC_TASK_IDENTIFIER = "com.rms.customs.sync"
private const val SYNC_INTERVAL_SECONDS = 15.0 * 60.0 // 15 minutes, matches the Android actual

// BGTaskScheduler.register() requires BGTaskSchedulerPermittedIdentifiers to be declared in the
// app's real Info.plist to do anything useful - a bare Kotlin/Native test binary (run via
// ./gradlew iosSimulatorArm64Test) has no Info.plist at all, the same class of gap that made Phase
// 4b's Keychain round-trip untestable there. Per that lesson, no behavioral test is written here;
// compilation is the verifiable bar for this sub-phase. Real behavior can only be confirmed once
// wired into the live app on a real device (Phase 5+).
//
// BGTask requests are one-shot, not auto-repeating like Android's PeriodicWorkRequest, so a fresh
// request is submitted every time a task starts, scheduling the next run before this one finishes.
@OptIn(ExperimentalForeignApi::class)
actual class BackgroundSyncScheduler actual constructor(
    context: PlatformContext,
    private val syncRepository: SyncRepository,
) {
    private var registered = false
    private val scope = CoroutineScope(Dispatchers.Default)

    actual fun scheduleSync() {
        registerIfNeeded()
        submitRequest()
    }

    private fun registerIfNeeded() {
        if (registered) return
        registered = true
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            SYNC_TASK_IDENTIFIER,
            null,
        ) { task -> handleTask(task as BGTask) }
    }

    private fun handleTask(task: BGTask) {
        submitRequest()
        scope.launch {
            val success = syncRepository.sync().isSuccess
            task.setTaskCompletedWithSuccess(success)
        }
    }

    private fun submitRequest() {
        val request = BGAppRefreshTaskRequest(SYNC_TASK_IDENTIFIER)
        // Uses NSDate's designated initializer (timeIntervalSince1970) rather than a convenience
        // class/instance method - Kotlin/Native cinterop always maps ObjC -init... methods to a
        // Kotlin constructor reliably; two different convenience-method name guesses
        // (dateWithTimeIntervalSinceNow, dateByAddingTimeInterval) both failed to resolve.
        val nowSeconds = Clock.System.now().toEpochMilliseconds() / 1000.0
        request.earliestBeginDate = NSDate(timeIntervalSince1970 = nowSeconds + SYNC_INTERVAL_SECONDS)
        try {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
        } catch (_: Throwable) {
            // Submission can legitimately fail (e.g. too many pending requests) - not fatal.
        }
    }
}
