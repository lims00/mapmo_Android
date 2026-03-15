package com.a6w.memo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.a6w.memo.common.util.FirebaseLogUtil
import com.a6w.memo.data.worker.MapmoNotificationWorker
import com.a6w.memo.data.worker.WorkerDefs
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint

/**
 * Geofencing Receiver
 * - Receives Action for Android GMS Geofencing Service
 */
@AndroidEntryPoint
class GeofencingReceiver: BroadcastReceiver() {
    companion object {
        // TODO: User ID must be managed with User Info
        private const val TEST_USER_ID = "test_user_1"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Get geofencing event from Intent
        // - If intent is not geofencing event, just return
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        // Check if geofencing event has error
        if(geofencingEvent.hasError()) {
            // Get error message and log as Firebase Crashlytics
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            FirebaseLogUtil.logException(Exception(errorMessage), this::class.java.name)
            return
        }

        // Check if geofencing event type
        when(geofencingEvent.geofenceTransition) {
            // Enter transition
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                // Get triggered geofences
                val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

                // Run notification worker for each triggered geofence
                triggeringGeofences.forEach { geofence ->
                    val mapmoID = geofence.requestId
                    enqueueNotificationWork(context, mapmoID)
                }
            }

            // Do nothing if exit, dwell event
            Geofence.GEOFENCE_TRANSITION_EXIT,
            Geofence.GEOFENCE_TRANSITION_DWELL -> {}
        }
    }

    private fun enqueueNotificationWork(context: Context, mapmoID: String) {
        val workRequest = OneTimeWorkRequestBuilder<MapmoNotificationWorker>()
            .setInputData(
                workDataOf(
                    WorkerDefs.KEY_WORKER_INPUT_MEMO_ID to mapmoID,
                    WorkerDefs.KEY_WORKER_INPUT_USER_ID to TEST_USER_ID,
                )
            )
            // Network connection is required
            .setConstraints(
                Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}