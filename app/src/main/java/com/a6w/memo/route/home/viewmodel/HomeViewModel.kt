package com.a6w.memo.route.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a6w.memo.common.model.MapCameraFocusData
import com.a6w.memo.common.model.MapMarkerData
import com.a6w.memo.domain.model.Mapmo
import com.a6w.memo.domain.model.MapmoList
import com.a6w.memo.domain.repository.MapmoListRepository
import com.a6w.memo.route.home.ui.model.HomeListUiItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home ViewModel Class
 * - Manage states of Home Screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mapmoListRepository: MapmoListRepository,
): ViewModel() {
    companion object {
        // TODO: User ID must be managed with User Info
        private const val TEST_USER_ID = "test_user_1"
    }

    // UI State variable
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            fetchInitialUiState()
        }
    }

    /**
     * Move camera focus to Mapmo
     */
    fun moveMapCameraToMapmo(mapmo: Mapmo?) {
        if(mapmo == null) return

        // Get mapmo location info
        val mapmoLat = mapmo.location.lat.toFloat()
        val mapmoLng = mapmo.location.lng.toFloat()

        // Generate camera focus data
        val cameraFocusData = MapCameraFocusData(
            latitude = mapmoLat,
            longitude = mapmoLng,
        )

        // Update as UI State
        _uiState.value = _uiState.value.copy(
            mapCameraFocus = cameraFocusData,
        )
    }

    /**
     * Generate init state
     */
    private suspend fun fetchInitialUiState() {
        // Initialize mapmo list state
        val mapmoList = getMapmoList()
        val mapMarkerList = getMapMarkerList(mapmoList)

        // Generate List UI Items
        val dataList = buildList {
            mapmoList?.list?.forEach { mapmoGroup ->
                // Label Item
                mapmoGroup.labelItem?.let { label ->
                    add(HomeListUiItem.LabelUiItem(
                        label = label,
                    ))
                }

                // Each Mapmo Items
                mapmoGroup.mapmoList.forEach { mapmo ->
                    add(HomeListUiItem.MapmoUiItem(
                        mapmo = mapmo,
                    ))
                }
            }
        }

        // Update UI State
        _uiState.value = _uiState.value.copy(
            dataList = dataList,
            mapMarkerList = mapMarkerList,
        )

        // Move camera focus to first mapmo
        val targetMapmo = mapmoList?.list
            ?.firstOrNull()?.mapmoList
            ?.firstOrNull()
        moveMapCameraToMapmo(targetMapmo)
    }

    /**
     * Get mapmo list instance and save as ui state
     */
    private suspend fun getMapmoList(): MapmoList? {
        // Get mapmo list from repository
        val mapmoList = mapmoListRepository.getMapmoList(TEST_USER_ID)

        return mapmoList
    }

    /**
     * Generate map markers from mapmo list
     */
    private fun getMapMarkerList(
        mapmoList: MapmoList?,
    ): List<MapMarkerData>? {
        // Generate marker list from mapmo list instance
        val markerList = mapmoList?.list?.flatMap { listItem ->
            listItem.mapmoList.map { mapmo ->
                val mapmoLocation = mapmo.location
                val mapmoLocationLat = mapmoLocation.lat.toFloat()
                val mapmoLocationLng = mapmoLocation.lng.toFloat()
                val mapmoTitle = mapmo.content

                val markerData = MapMarkerData(
                    latitude = mapmoLocationLat,
                    longitude = mapmoLocationLng,
                    markerTitle = mapmoTitle,
                )

                markerData
            }
        }

        return markerList
    }
}