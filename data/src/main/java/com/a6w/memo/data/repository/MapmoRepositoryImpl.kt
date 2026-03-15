package com.a6w.memo.data.repository

import com.a6w.memo.data.firebase.FirestoreKey
import com.a6w.memo.domain.model.Location
import com.a6w.memo.domain.model.Mapmo
import com.a6w.memo.domain.repository.LabelRepository
import com.a6w.memo.domain.repository.MapmoListRepository
import com.a6w.memo.domain.repository.MapmoRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await

/**
 * MapmoRepositoryImpl
 *
 * - Repository implementation for managing Mapmo data in Firestore
 * - Provides operations for retrieving, creating, and updating Mapmo documents
 *
 * Responsibilities:
 * - Fetch a single Mapmo by ID
 * - Add a new Mapmo
 * - Update an existing Mapmo
 * - Cache the mapmo Data
 */
class MapmoRepositoryImpl: MapmoRepository {
    private val firestoreDB = FirebaseFirestore.getInstance()
    private val mapmoCollection by lazy { firestoreDB.collection(FirestoreKey.COLLECTION_KEY_MAPMO) }

    // Individual Mapmo cache keyed by mapmoID
    private val mapmoCache = mutableMapOf<String, Mapmo>()
    private val mapmoListRepositoryImpl: MapmoListRepository = MapmoListRepositoryImpl()
    override suspend fun getMapmo(
        mapmoID: String,
        userID: String,
    ): Mapmo? {
        try {
            // Return cached mapmo if available
            mapmoCache[mapmoID]?.let { return it }

            // Fetch mapmo by ID
            val document =
                mapmoCollection.document(mapmoID).get()
                    .await()
            // Check if the document exists
            if (document.exists().not()) return null

            val mapmoID = document.id
            val content = document.getString(FirestoreKey.DOCUMENT_KEY_CONTENT) ?: ""
            val isNotifyEnabled =
                document.getBoolean(FirestoreKey.DOCUMENT_KEY_IS_NOTIFY_ENABLED) ?: false
            val labelID = document.getString(FirestoreKey.DOCUMENT_KEY_LABEL_ID) // nullable
            val timeStampUpdatedAt =
                document.get(FirestoreKey.DOCUMENT_KEY_UPDATED_AT) as? Timestamp
            val updatedAt = timeStampUpdatedAt?.seconds ?: -1

            // Result Mapmo Data
            val mapmoResult = Mapmo(
                mapmoID = mapmoID,
                content = content,
                isNotifyEnabled = isNotifyEnabled,
                labelID = labelID,
                updatedAt = updatedAt,
            )

            // Store in cache
            mapmoCache[mapmoID] = mapmoResult

            return mapmoResult
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

    }

    override suspend fun addMapmo(
        mapmoContent: Mapmo,
        userID: String,
    ): Boolean {
        try {
             val updatedAt = Timestamp.now()

            // Create map data to upload to Firestore
            val mapmoData = hashMapOf(
                FirestoreKey.DOCUMENT_KEY_CONTENT to mapmoContent.content,
                FirestoreKey.DOCUMENT_KEY_IS_NOTIFY_ENABLED to mapmoContent.isNotifyEnabled,
                FirestoreKey.DOCUMENT_KEY_LABEL_ID to mapmoContent.labelID,
                FirestoreKey.DOCUMENT_KEY_UPDATED_AT to updatedAt,
                FirestoreKey.DOCUMENT_KEY_USER_ID to userID,
            )

            // Add a new document to the mapmo collection
            val addedMapmoRef = mapmoCollection
                .add(mapmoData)
                .await()

            // Update cache with newly added Mapmo
            val addedMapmoID = addedMapmoRef.id
            val addedMapmo = mapmoContent.copy(mapmoID = addedMapmoID)
            mapmoCache[addedMapmoID] = addedMapmo
            // Remove cached MapmoList Data
            mapmoListRepositoryImpl.removeCachedMapmoList(userID)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }

    override suspend fun updateMapmo(
        mapmoContent: Mapmo,
        userID: String,
    ): Boolean {
        try {
            // Set current timestamp for update
            val updatedAt = Timestamp.now()
            // Create updated data mapmo
            val mapmoData = mapOf(
                FirestoreKey.DOCUMENT_KEY_CONTENT to mapmoContent.content,
                FirestoreKey.DOCUMENT_KEY_IS_NOTIFY_ENABLED to mapmoContent.isNotifyEnabled,
                FirestoreKey.DOCUMENT_KEY_LABEL_ID to mapmoContent.labelID,
                FirestoreKey.DOCUMENT_KEY_UPDATED_AT to updatedAt,
            )

            // Update the existing mapmo document
            mapmoCollection
                .document(mapmoContent.mapmoID)
                .update(mapmoData)
                .await()

            // Update cache with modified Mapmo
            mapmoCache[mapmoContent.mapmoID] = mapmoContent.copy(
                updatedAt = updatedAt.seconds
            )
            // Remove cached MapmoList Data
            mapmoListRepositoryImpl.removeCachedMapmoList(userID)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

}