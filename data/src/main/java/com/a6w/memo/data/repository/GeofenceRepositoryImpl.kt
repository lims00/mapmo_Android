package com.a6w.memo.data.repository

import android.app.PendingIntent
import com.a6w.memo.domain.model.Location
import com.a6w.memo.domain.repository.GeofenceRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Geofence Repository Implementation
 * - Manage geofence operations by GMS Location Client
 * - Add / Remove geofencing to Android GMS
 */
class GeofenceRepositoryImpl @Inject constructor(
    private val geofencingClient: GeofencingClient,
    private val geofencePendingIntent: PendingIntent,
): GeofenceRepository {
    companion object {
        private const val DEFAULT_GEOFENCE_RADIUS = 500f
    }

    override suspend fun registerGeofence(
        mapmoID: String,
        location: Location,
    ): Result<Unit> = runCatching {
        // Location Info
        val lat = location.lat
        val lng = location.lng

        // Generate Geofence
        val geofence = Geofence.Builder()
            // Request ID is same as Mapmo ID
            .setRequestId(mapmoID)
            // Set location info
            .setCircularRegion(lat, lng, DEFAULT_GEOFENCE_RADIUS)
            // Set as do not expire geofence request
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            // Callback only when enter
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        // Generate Geofence Request
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        // Add Geofence Request to Client
        // - Requires ACCESS_FINE_LOCATION Permission
        geofencingClient.addGeofences(request, geofencePendingIntent).await()
    }

    override fun unregisterGeofence(
        mapmoID: String,
    ): Result<Unit> {
        TODO("Not yet implemented")
    }
}