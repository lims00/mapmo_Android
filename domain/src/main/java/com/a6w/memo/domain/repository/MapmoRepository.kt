package com.a6w.memo.domain.repository

import com.a6w.memo.domain.model.Mapmo


/**
 * MapmoRepository interface
 */
interface MapmoRepository {
    // Add a new Mapmo
    suspend fun addMapmo(mapmoContent: Mapmo, userID: String): Boolean
    // Update Mapmo information
    suspend fun updateMapmo(mapmoContent: Mapmo, userID: String): Boolean
    // Retrieve Mapmo detailed information
    suspend fun getMapmo(mapmoID: String, userID: String): Mapmo?
    // Delete Mapmo by id
    suspend fun deleteMapmo(mapmoID: String, userID: String): Boolean
    // Toggle notification state of a Mapmo (true <-> false)
    suspend fun toggleNotification(mapmoID: String): Mapmo?
}