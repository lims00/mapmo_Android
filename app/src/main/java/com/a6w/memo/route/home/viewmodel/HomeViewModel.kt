package com.a6w.memo.route.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a6w.memo.common.def.ErrorMessageDef
import com.a6w.memo.common.model.MapCameraFocusData
import com.a6w.memo.common.model.MapMarkerData
import com.a6w.memo.common.util.DatetimeUtil
import com.a6w.memo.domain.model.Label
import com.a6w.memo.domain.model.Mapmo
import com.a6w.memo.domain.model.MapmoList
import com.a6w.memo.domain.repository.MapmoListRepository
import com.a6w.memo.domain.repository.MapmoRepository
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
    private val mapmoRepository: MapmoRepository,
): ViewModel() {
    companion object {
        // TODO: User ID must be managed with User Info
        private const val TEST_USER_ID = "test_user_1"
    }

    // UI State variable
    // - Init state is Loading
    private val _uiState: MutableStateFlow<HomeUiState> = MutableStateFlow(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // Mapmo List instance
    private var mapmoList: MapmoList? = null

    /**
     * Load mapmo list from repository, and update as state
     */
    fun loadMapmoList() {
        viewModelScope.launch {
            // Set UI STate as Loading
            _uiState.update { HomeUiState.Loading }

            // Initialize mapmo list state
            mapmoList = getMapmoList()

            // Error handling
            if(mapmoList == null) {
                // Set ui state as Error and return
                _uiState.update {
                    HomeUiState.Error(
                        errMessage = ErrorMessageDef.HOME_MAPMO_LOAD_FAIL,
                    )
                }
                return@launch
            }

            // Generate UI Model List
            val dataList = buildList {
                mapmoList?.list?.forEach { mapmoGroup ->
                    // If label is null, mapmo group is unavailable (Rule)
                    val labelItem = mapmoGroup.labelItem ?: return@forEach

                    // Label Item
                    add(labelItem.toUiItem())

                    // Each Mapmo Items
                    mapmoGroup.mapmoList.forEach { mapmo ->
                        add(mapmo.toUiItem(labelItem))
                    }
                }
            }

            // Generate Map marker list
            val mapMarkerList = getMapMarkerList(mapmoList)

            // Set UI State as Normal
            _uiState.update {
                HomeUiState.Normal(
                    dataList = dataList,
                    mapMarkerList = mapMarkerList,
                )
            }

            // Move map camera focus to first label
            mapmoList?.list
                ?.firstOrNull()?.labelItem
                ?.let {
                    val labelID = it.id
                    moveMapCameraToLabel(labelID)
                }
        }
    }

    /**
     * Move camera focus to Label
     */
    fun moveMapCameraToLabel(labelID: String) {
        viewModelScope.launch {
            // Get target label
            // - If label is null, cannot move camera
            val targetLabel = mapmoList?.list?.firstOrNull { item ->
                item.labelItem?.id == labelID
            }?.labelItem ?: return@launch

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
            // - Camera cannot be updated if UI State is not Normal
            _uiState.update {
                when(it) {
                    is HomeUiState.Normal -> {
                        it.copy(
                            mapCameraFocus = cameraFocusData,
                        )
                    }
                    else -> { it }
                }
            }
        }
    }

    /**
     * Toggle mapmo notify enabled state
     */
    fun toggleMapmoNotify(
        mapmoID: String,
    ) {
        viewModelScope.launch {
            // Set UI STate as Loading
            _uiState.update { HomeUiState.Loading }

            // TODO: Repository 연동

            // Reload Mapmo list data
            loadMapmoList()
        }
    }


    /**
     * Get mapmo list from repository and return it
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
        // Label Data
        val labelTitle = this.name
        val labelColor = this.color
        val labelLocation = this.location
        val labelLocationLat = labelLocation.lat.toFloat()
        val labelLocationLng = labelLocation.lng.toFloat()

        // Generate Map Marker Data model
        return MapMarkerData(
            color = labelColor,
            latitude = labelLocationLat,
            longitude = labelLocationLng,
            markerTitle = labelTitle,
        )
    }

    /**
     * Convert [Label] to [HomeListUiItem.LabelUiItem]
     */
    private fun Label.toUiItem(): HomeListUiItem.LabelUiItem {
        // Label Data
        val labelColor = this.color
        val labelID = this.id
        val labelName = this.name

        // Generate Label UI model
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
        // Mapmo Data
        val mapmoID = this.mapmoID
        val mapmoTitle = this.title
        val mapmoUpdatedAt = DatetimeUtil.getUiDateStringFromMillis(this.updatedAt * 1000)
        val mapmoIsNotifyEnabled = this.isNotifyEnabled

        // Label Data
        val labelLocation = label.location

        // Generate Mapmo UI model
        return HomeListUiItem.MapmoUiItem(
            mapmoID = mapmoID,
            mapmoLocation = labelLocation,
            mapmoTitle = mapmoTitle,
            mapmoUpdatedAt = mapmoUpdatedAt,
            mapmoIsNotifyEnabled = mapmoIsNotifyEnabled,
        )
    }
}