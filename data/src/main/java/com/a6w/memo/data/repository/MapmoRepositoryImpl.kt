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
import javax.inject.Inject
import kotlin.collections.set

/**
 * MapmoRepositoryImpl
 *
 * - Retrieve a single Mapmo document
 * - Create, update, and delete Mapmo documents
 * - Maintain an in-memory cache for individual Mapmo items
 *
 */
class MapmoRepositoryImpl @Inject constructor(): MapmoRepository {
    private val firestoreDB = FirebaseFirestore.getInstance()
    private val mapmoCollection by lazy { firestoreDB.collection(FirestoreKey.COLLECTION_KEY_MAPMO) }

    // Cache for individual Mapmo (key: mapmoID)
    private val mapmoCache = mutableMapOf<String, Mapmo>()

    // Used to invalidate Mapmo list cache when data changes
    private val mapmoListRepositoryImpl: MapmoListRepository = MapmoListRepositoryImpl()

    /**
     * Retrieves a single Mapmo by ID.
     *
     * @param mapmoID ID of the Mapmo to retrieve
     * @param userID ID of the user (currently not used for validation)
     *
     * @return Mapmo object, or null if not found or an error occurs
     */
    override suspend fun getMapmo(
        mapmoID: String,
        userID: String,
    ): Mapmo? {
        try {
            // Return cached data if exists
            mapmoCache[mapmoID]?.let { return it }

            // Fetch document from Firestore
            val document =
                mapmoCollection.document(mapmoID).get()
                    .await()
            // Return null if document does not exist
            if (document.exists().not()) return null

            // Extract fields from document
            val mapmoID = document.id
            val content = document.getString(FirestoreKey.DOCUMENT_KEY_CONTENT) ?: ""
            val isNotifyEnabled =
                document.getBoolean(FirestoreKey.DOCUMENT_KEY_IS_NOTIFY_ENABLED) ?: false
            val labelID = document.getString(FirestoreKey.DOCUMENT_KEY_LABEL_ID) // nullable
            // Convert Timestamp to seconds (fallback: -1)
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

            // Cache result
            mapmoCache[mapmoID] = mapmoResult

            return mapmoResult
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

    }

    /**
     * Creates a new Mapmo document.
     *
     * @param mapmoContent Mapmo data to create (mapmoID is ignored)
     * @param userID ID of the owner
     *
     * @return true if successful, false otherwise
     */
    override suspend fun addMapmo(
        mapmoContent: Mapmo,
        userID: String,
    ): Boolean {
        try {
             val updatedAt = Timestamp.now()

            // Create mapmo data to upload to Firestore
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

            // Invalid cached MapmoList Data
            mapmoListRepositoryImpl.removeCachedMapmoList(userID)

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }

    /**
     * Updates an existing Mapmo document.
     *
     * @param mapmoContent Updated Mapmo data
     * @param userID ID of the user
     *
     * @return true if successful, false otherwise
     */
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

    /**
     * Deletes a Mapmo document.
     *
     * @param mapmoID ID of the Mapmo to delete
     * @param userID ID of the requesting user
     *
     * @return true if deleted, false if unauthorized or failed
     */
    override suspend fun deleteMapmo(
        mapmoID: String,
        userID: String,
    ): Boolean {

        // Retrieve the mapmo document to validate ownership
        val document = mapmoCollection
            .document(mapmoID)
            .get()
            .await()

        // Ensure that only the owner can delete the mapmo
        if (document.getString(FirestoreKey.DOCUMENT_KEY_USER_ID) != userID) {
            return false
        }

        // Delete the mapmo document
        mapmoCollection
            .document(mapmoID)
            .delete()
            .await()

        // Remove from mapmo cache
        mapmoCache.remove(mapmoID)

        // Remove cached MapmoList Data
        mapmoListRepositoryImpl.removeCachedMapmoList(userID)
        return true
    }

    /**
     * Toggles the notification state of a Mapmo document.
     *
     * Fetches the current state directly from Firestore instead of using cache
     * to avoid toggling to the wrong state due to stale data.
     *
     * @param mapmoID ID of the Mapmo to toggle notification for
     *
     * @return true if successful, false if document not found or an error occurs
     */
    override suspend fun toggleNotification(
        mapmoID: String,
    ): Mapmo? {
        try {
            // Fetch the latest document directly from Firestore instead of using cache,
            // as stale cache could cause toggling to the wrong state
            val document = mapmoCollection
                .document(mapmoID)
                .get()
                .await()

            // Return false if document does not exist
            if (document.exists().not()) return null

            // Read the current notification state and invert it
            val current = document.getBoolean(FirestoreKey.DOCUMENT_KEY_IS_NOTIFY_ENABLED) ?: false
            val toggled = !current

            // Update the notification field in Firestore
            mapmoCollection
                .document(mapmoID)
                .update(FirestoreKey.DOCUMENT_KEY_IS_NOTIFY_ENABLED, toggled)
                .await()

            // Build updated Mapmo and sync into the in-memory cache
            val content = document.getString(FirestoreKey.DOCUMENT_KEY_CONTENT) ?: ""
            val labelID = document.getString(FirestoreKey.DOCUMENT_KEY_LABEL_ID)
            val updatedAt = (document.get(FirestoreKey.DOCUMENT_KEY_UPDATED_AT) as? Timestamp)?.seconds ?: -1

            val updatedMapmo = Mapmo(
                    mapmoID = mapmoID,
                    content = content,
                    isNotifyEnabled = toggled,
                    labelID = labelID,
                    updatedAt = updatedAt,
                )
            mapmoCache[mapmoID] = updatedMapmo
            return updatedMapmo
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}