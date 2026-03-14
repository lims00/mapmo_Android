package com.a6w.memo.common.factory

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.a6w.memo.data.worker.MapmoNotificationWorker
import com.a6w.memo.domain.repository.LabelRepository
import com.a6w.memo.domain.repository.MapmoRepository
import com.a6w.memo.domain.service.MapmoNotificationService
import javax.inject.Inject
import kotlin.jvm.java

/**
 * Mapmo Notification Worker Factory
 * - Generates instance of [MapmoNotificationWorker] with Android Context
 */
class MapmoNotificationWorkerFactory @Inject constructor(
    private val labelRepository: LabelRepository,
    private val mapmoRepository: MapmoRepository,
    private val notificationService: MapmoNotificationService
): WorkerFactory() {
    // Create Mapmo Notification Worker
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        // Generate Mapmo Notification Worker
        // - If class name is other, nothing is generated
        return when(workerClassName) {
            MapmoNotificationWorker::class.java.name -> {
                MapmoNotificationWorker(
                    context = appContext,
                    params = workerParameters,
                    labelRepository = labelRepository,
                    mapmoRepository = mapmoRepository,
                    notificationService = notificationService,
                )
            }
            else -> null
        }
    }
}