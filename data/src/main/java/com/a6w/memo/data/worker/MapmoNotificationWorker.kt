package com.a6w.memo.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.a6w.memo.domain.repository.MapmoRepository
import com.a6w.memo.domain.service.MapmoNotificationService

/**
 * Mapmo Notification Worker
 * - Show notification of mapmo with Coroutine Worker
 */
class MapmoNotificationWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: MapmoRepository,
    private val notificationService: MapmoNotificationService
) : CoroutineWorker(context, params) {
    companion object {
        private const val KEY_WORKER_INPUT_MEMO_ID = "MEMO_ID"
        private const val KEY_WORKER_INPUT_USER_ID = "USER_ID"
    }

    override suspend fun doWork(): Result {
        // Get Mapmo ID / User ID Data
        val mapmoID = inputData.getString(KEY_WORKER_INPUT_MEMO_ID) ?: return Result.failure()
        val userID = inputData.getString(KEY_WORKER_INPUT_USER_ID) ?: return Result.failure()

        // Get target mapmo instance
        val targetMapmo = repository.getMapmo(mapmoID, userID) ?: return Result.failure()

        // Show notification
        notificationService.showNotification(targetMapmo.title, targetMapmo.content)

        return Result.success()
    }
}