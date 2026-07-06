package com.rms.customs.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rms.customs.domain.repository.SyncRepository

class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val syncRepository: SyncRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return syncRepository.sync().fold(
            onSuccess = { Result.success() },
            onFailure = { e ->
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            },
        )
    }

    companion object {
        const val WORK_NAME = "rms_sync_periodic"
    }
}
