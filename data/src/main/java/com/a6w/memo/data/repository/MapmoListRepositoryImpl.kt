package com.a6w.memo.data.repository

import com.a6w.memo.data.firebase.FirestoreKey
import com.a6w.memo.domain.model.Label
import com.a6w.memo.domain.model.Location
import com.a6w.memo.domain.model.Mapmo
import com.a6w.memo.domain.model.MapmoList
import com.a6w.memo.domain.model.MapmoListItem
import com.a6w.memo.domain.repository.MapmoListRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.collections.mutableMapOf

/**
 * MapmoListRepositoryImpl
 *
 * - Fetch Mapmo and Label data from Firestore
 * - Combine them into MapmoList structure
 * - Cache results per user to reduce unnecessary network calls
 *
 */
class MapmoListRepositoryImpl @Inject constructor(): MapmoListRepository {
    private val firestoreDB = FirebaseFirestore.getInstance()
    private val mapmoCollection by lazy { firestoreDB.collection(FirestoreKey.COLLECTION_KEY_MAPMO) }
    private val labelCollection by lazy { firestoreDB.collection(FirestoreKey.COLLECTION_KEY_LABEL) }

    // Cache for MapmoList (key: userID)
    private var mapmoListCache = mutableMapOf<String, MapmoList>()

    /**
     * Fetch MapmoList for a user.
     *
     * Retrieves Mapmo and Label data from Firestore,
     * then groups Mapmo by label and combines them into a structured MapmoList.
     *
     * Uses in-memory cache to improve performance by skipping redundant network calls.
     *
     * @param userID user identifier
     * @return MapmoList or null if failed
     */
    override suspend fun getMapmoList(
        userID: String,
    ): MapmoList? {
        try {
            // Return cached data if available to avoid unnecessary Firestore requests
            mapmoListCache[userID]?.let { return it }

            // Fetch all mapmo documents for the given user
            val snapshot = mapmoCollection
                .whereEqualTo(FirestoreKey.DOCUMENT_KEY_USER_ID, userID)
                .get()
                .await()

            // Fetch all label documents for the given user
            val labelSnapshot = labelCollection
                .whereEqualTo(FirestoreKey.DOCUMENT_KEY_USER_ID, userID)
                .get()
                .await()

            // Convert Firestore documents into Label objects
            val labels = labelSnapshot.documents.mapNotNull { document ->
                val geoPoint = document.getGeoPoint(FirestoreKey.DOCUMENT_KEY_LOCATION)
                // Skip if location does not exist (invalid label data)
                    ?: return@mapNotNull null

                // Extract latitude/longitude from GeoPoint
                val labelLat = geoPoint.latitude
                val labelLng = geoPoint.longitude

                // Build Location model
                val location = Location(
                    lat = labelLat,
                    lng = labelLng,
                )

                // Extract label fields with safe defaults
                val id = document.id
                val name = document.getString(FirestoreKey.DOCUMENT_KEY_NAME) ?: ""
                val color = document.getString(FirestoreKey.DOCUMENT_KEY_COLOR) ?: ""

                // Create Label only if required data is valid
                Label(
                    id = id,
                    name = name,
                    color = color,
                    location = location,
                )
            }

            // Convert Firestore documents into a list of Mapmo objects
            val mapmoList = snapshot.documents.mapNotNull { document ->
                val mapmoID = document.id
                val content = document.getString(FirestoreKey.DOCUMENT_KEY_CONTENT) ?: ""
                // Default to false if field is missing
                val isNotifyEnabled =
                    document.getBoolean(FirestoreKey.DOCUMENT_KEY_IS_NOTIFY_ENABLED) ?: false
                val labelID = document.getString(FirestoreKey.DOCUMENT_KEY_LABEL_ID)
                // Use seconds value from timestamp, fallback to -1 if missing
                val updatedAt =
                    document.getTimestamp(FirestoreKey.DOCUMENT_KEY_UPDATED_AT)?.seconds ?: -1

                // Mapmo Data
                Mapmo(
                    mapmoID = mapmoID,
                    content = content,
                    isNotifyEnabled = isNotifyEnabled,
                    labelID = labelID,
                    updatedAt = updatedAt,
                )
            }

            // Group Mapmo list by labelID
            // This allows combining each group with its corresponding Label
            val grouped = mapmoList.groupBy { it.labelID }

            // Match each Mapmo group with its Label and build MapmoListItem
            val listItem = grouped.mapNotNull { (labelID, mapmoList) ->
                val label = labels.find { it.id == labelID }
                // Even if label is null, structure is kept
                // (nullable handling is delegated to UI/domain)
                MapmoListItem(
                    labelItem = label,
                    mapmoList = mapmoList,
                )
            }
            // Final aggregated result
            val mapmoListResult = MapmoList(
                count = mapmoList.size,
                list = listItem,
            )

            // Cache the result per user for subsequent requests
            mapmoListCache[userID] = mapmoListResult

            // Return final MapmoList result
            return mapmoListResult
        } catch (e: Exception) {
            e.printStackTrace()
            // Return null if any error occurs during fetch or mapping
            return null
        }
    }

    /**
     * Remove cached MapmoList for a user.
     *
     * Clears the in-memory cache entry to force fresh data fetch
     * on the next request.
     *
     * @param userID user identifier
     * @return true if removed successfully, false otherwise
     */
    override suspend fun removeCachedMapmoList(
        userID: String,
    ): Boolean {
        try {
            // Remove cached mapmoList
            mapmoListCache.remove(userID)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            // Return null if an error occurs
            return false
        }
    }
}