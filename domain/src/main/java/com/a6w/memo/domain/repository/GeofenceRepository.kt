package com.a6w.memo.domain.repository

import com.a6w.memo.domain.model.Location

/**
 * Geofence Repository
 * - Manage geofence operations
 * - Add / Remove geofencing to Android GMS
 */
interface GeofenceRepository {
    // Register geofencing
    suspend fun registerGeofence(mapmoID: String, location: Location): Result<Unit>
    // Unregister geofencing
    fun unregisterGeofence(mapmoID: String): Result<Unit>
}