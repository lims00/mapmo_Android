package com.a6w.memo.route.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a6w.memo.common.model.MapCameraFocusData
import com.a6w.memo.common.model.MapMarkerData
import com.a6w.memo.common.util.DatetimeUtil
import com.a6w.memo.domain.model.Label
import com.a6w.memo.domain.model.Mapmo
import com.a6w.memo.domain.model.MapmoList
import com.a6w.memo.domain.repository.MapmoListRepository
import com.a6w.memo.route.home.ui.model.HomeListUiItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    // Mapmo List
    private var mapmoList: MapmoList? = null

    init {
        fetchInitialUiState()
    }

    /**
     * Move camera focus to Label
     */
    fun moveMapCameraToLabel(labelID: String) {
        viewModelScope.launch {
            // Get target label
            val targetLabel = mapmoList?.list?.firstOrNull { item ->
                item.labelItem?.id == labelID
            }?.labelItem

            if(targetLabel == null) return@launch

            // Get label location info
            val labelLocation = targetLabel.location
            val labelLat = labelLocation.lat.toFloat()
            val labelLng = labelLocation.lng.toFloat()

            // Generate camera focus data
            val cameraFocusData = MapCameraFocusData(
                latitude = labelLat,
                longitude = labelLng,
            )

            // Update as UI State
            _uiState.update {
                it.copy(
                    mapCameraFocus = cameraFocusData,
                )
            }
        }

    }

    /**
     * Generate init state
     */
    private fun fetchInitialUiState() {
        viewModelScope.launch {
            // Initialize mapmo list state
            mapmoList = getMapmoList()

            // Generate List UI Items
            val dataList = buildList {
                mapmoList?.list?.forEach { mapmoGroup ->
                    val labelItem = mapmoGroup.labelItem
                    if(labelItem == null) return@forEach

                    // Label Item
                    labelItem.let { label ->
                        add(label.toUiItem())
                    }

                    // Each Mapmo Items
                    mapmoGroup.mapmoList.forEach { mapmo ->
                        add(mapmo.toUiItem(labelItem))
                    }
                }
            }

            // Generate Map marker list
            val mapMarkerList = getMapMarkerList(mapmoList)

            // Update UI State
            _uiState.update {
                it.copy(
                    dataList = dataList,
                    mapMarkerList = mapMarkerList,
                )
            }

            // Move camera focus to first label
            mapmoList?.list
                ?.firstOrNull()?.labelItem
                ?.let {
                    val labelID = it.id
                    moveMapCameraToLabel(labelID)
                }
        }
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
        val markerList = mapmoList?.list?.mapNotNull { listItem ->
            listItem.labelItem?.toMapMarkerData()
        }

        return markerList
    }

    /**
     * Convert [Label] to [MapMarkerData]
     */
    private fun Label.toMapMarkerData(): MapMarkerData {
        val labelTitle = this.name
        val labelLocation = this.location
        val labelLocationLat = labelLocation.lat.toFloat()
        val labelLocationLng = labelLocation.lng.toFloat()

        return MapMarkerData(
            latitude = labelLocationLat,
            longitude = labelLocationLng,
            markerTitle = labelTitle,
        )
    }

    /**
     * Convert [Label] to [HomeListUiItem.LabelUiItem]
     */
    private fun Label.toUiItem(): HomeListUiItem.LabelUiItem {
        val labelColor = this.color
        val labelID = this.id
        val labelName = this.name

        return HomeListUiItem.LabelUiItem(
            labelColor = labelColor,
            labelID = labelID,
            labelName = labelName,
        )
    }

    /**
     * Convert [Mapmo] to [HomeListUiItem.MapmoUiItem]
     */
    private fun Mapmo.toUiItem(label: Label): HomeListUiItem.MapmoUiItem {
        val mapmoID = this.mapmoID
        val mapmoTitle = this.title
        val mapmoUpdatedAt = DatetimeUtil.getUiDateStringFromMillis(this.updatedAt * 1000)

        val labelLocation = label.location
        return HomeListUiItem.MapmoUiItem(
            mapmoID = mapmoID,
            mapmoLocation = labelLocation,
            mapmoTitle = mapmoTitle,
            mapmoUpdatedAt = mapmoUpdatedAt,
        )
    }
}