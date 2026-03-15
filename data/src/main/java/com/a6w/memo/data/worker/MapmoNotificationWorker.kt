package com.a6w.memo.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.a6w.memo.domain.repository.LabelRepository
import com.a6w.memo.domain.repository.MapmoRepository
import com.a6w.memo.domain.service.MapmoNotificationService

/**
 * Mapmo Notification Worker
 * - Show notification of mapmo with Coroutine Worker
 */
class MapmoNotificationWorker(
    context: Context,
    params: WorkerParameters,
    private val labelRepository: LabelRepository,
    private val mapmoRepository: MapmoRepository,
    private val notificationService: MapmoNotificationService
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // Get Mapmo ID / User ID Data
        val mapmoID = inputData.getString(WorkerDefs.KEY_WORKER_INPUT_MEMO_ID) ?: return Result.failure()
        val userID = inputData.getString(WorkerDefs.KEY_WORKER_INPUT_USER_ID) ?: return Result.failure()

        // Get target mapmo, label instance
        val targetMapmo = mapmoRepository.getMapmo(mapmoID, userID) ?: return Result.failure()
        val targetLabel = labelRepository.getLabel(targetMapmo.labelID ?: "", userID) ?: return Result.failure()

        val notificationTitle = "${targetLabel.name}에 도착하셨나요?"
        val notificationContent = targetMapmo.title

        // Show notification
        notificationService.showNotification(notificationTitle, notificationContent)

        return Result.success()
    }
}