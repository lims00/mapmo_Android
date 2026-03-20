package com.a6w.memo.route.mapmo.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.a6w.memo.common.model.MapCameraFocusData
import com.a6w.memo.common.model.MapMarkerData
import com.a6w.memo.domain.model.Label
import com.a6w.memo.domain.model.Mapmo
import com.a6w.memo.domain.repository.GeofenceRepository
import com.a6w.memo.domain.repository.LabelRepository
import com.a6w.memo.domain.repository.MapmoRepository
import com.a6w.memo.navigation.MapmoNavRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Mapmo detail screen.
 * Manages UI state including memo content, edit mode, label selection, and map data.
 */
@HiltViewModel
class MapmoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mapmoRepository: MapmoRepository,
    private val labelRepository: LabelRepository,
    private val geofenceRepository: GeofenceRepository,
) : ViewModel() {

    companion object {
        // TODO: User ID must be managed with User Info
        private const val TEST_USER_ID = "test_user_1"
    }

    private val route = savedStateHandle.toRoute<MapmoNavRoute.Mapmo>()
    private val mapmoID: String = route.mapmoID ?: ""

    // Domain state for the current Mapmo and Label
    private var currentMapmo: Mapmo? = null
    private var currentLabel: Label? = null
    var isEditing: Boolean = false
    private val _uiState = MutableStateFlow(MapmoUiState())
    val uiState: StateFlow<MapmoUiState> = _uiState.asStateFlow()

    // Temporary content used during edit mode
    private val _editingContent = MutableStateFlow("")
    val editingContent: StateFlow<String> = _editingContent.asStateFlow()

    // Cached label list
    private val _labelList = MutableStateFlow<List<Label>>(emptyList())
    val labelList: StateFlow<List<Label>> = _labelList.asStateFlow()

    /**
     * Loads the Mapmo and its associated Label.
     * Also prepares map camera focus and marker data based on the label location.
     */
    fun loadMapmo() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            val mapmo = mapmoRepository.getMapmo(
                mapmoID = mapmoID,
                userID = TEST_USER_ID,
            )
            currentMapmo = mapmo

            // Mapmo not found
            if (currentMapmo == null) {
                _uiState.value = MapmoUiState(
                    isLoading = false,
                    errorMessage = "Mapmo를 찾을 수 없습니다",
                )
                return@launch
            }
            val mapmoContent = currentMapmo?.content
            if(mapmoContent == null){
                _uiState.value = MapmoUiState(
                    isLoading = false,
                    errorMessage = "mapmo 내용을 찾을 수 없습니다",
                )
                return@launch
            }
            _editingContent.value = mapmoContent
            val label = runCatching {
                labelRepository.getLabel(
                    labelID = currentMapmo?.labelID ?: "",
                    userID = TEST_USER_ID,
                )
            }.getOrNull()
            currentLabel = label
            if (currentLabel == null) {
                _uiState.value = MapmoUiState(
                    isLoading = false,
                    errorMessage = "label정보를 찾을 수 없습니다",
                )
                return@launch
            }

            val labelLat = currentLabel?.location?.lat
            val labelLng = currentLabel?.location?.lng
            val markerTitle = currentMapmo?.content

            if(labelLat == null || labelLng == null){
                _uiState.value = MapmoUiState(
                    isLoading = false,
                    errorMessage = "Mapmo의 위치 정보를 찾을 수 없습니다",
                )
                return@launch
            }
            if(markerTitle == null){
                _uiState.value = MapmoUiState(
                    isLoading = false,
                    errorMessage = "Mapmo의 내용을 찾을 수 없습니다",
                )
                return@launch
            }
            val cameraFocus = createCameraFocus(lat = labelLat, lng = labelLng)
            val markers = createMarkers(lat = labelLat, lng = labelLng, markerTitle = markerTitle)
            val content = currentMapmo?.content ?: ""
            val updatedAt = currentMapmo?.updatedAt ?: 0
            val isNotifyEnabled = currentMapmo?.isNotifyEnabled ?: false
            val labelName = currentLabel?.name ?: ""
            val labelColor = currentLabel?.color ?: ""
            val labelID = currentLabel?.id ?: ""
            _uiState.value = MapmoUiState(
                content = content,
                updatedAt = updatedAt,
                isNotifyEnabled = isNotifyEnabled,
                currentLabelID = labelID,
                labelName = labelName,
                labelColor = labelColor,
                isLoading = false,
                errorMessage = null,
                mapCameraFocus = cameraFocus,
                mapMarkerList = markers,
            )
        }
    }

    /**
     * Toggle between view and edit mode.
     */
    fun toggleEditMode() {
        _uiState.update {
            it.copy(
                isEditing = !isEditing,
            )
        }
    }

    /**
     * Update the in-progress editing content.
     *
     * @param newContent The latest text entered by the user.
     */
    fun updateEditingContent(newContent: String) {
        _editingContent.value = newContent
    }

    /**
     * Persist the edited content to the repository.
     * Applies an optimistic UI update and rolls back on failure.
     */
    fun saveContent() {
        viewModelScope.launch {

            if(currentMapmo == null) return@launch
            val labelName = currentLabel?.name
            val labelID = currentLabel?.id
            val labelColor = currentLabel?.color
            val location = currentLabel?.location
            val mapmoContent =  _editingContent.value
            val updatedAt = System.currentTimeMillis() / 1000
            val isNotify = currentMapmo?.isNotifyEnabled ?: return@launch

            val updatedMapmo = currentMapmo!!.copy(
                content = mapmoContent,
                labelID = labelID,
                updatedAt = updatedAt,
                isNotifyEnabled = isNotify,
            )
            val success = mapmoRepository.updateMapmo(
                mapmoContent = updatedMapmo,
                userID = TEST_USER_ID,
            )

            if (success) {
                // Optimistic update: reflect changes in UI after server confirms
                _uiState.update {
                    it.copy(
                        content = mapmoContent,
                        updatedAt = updatedAt,
                        isNotifyEnabled = isNotify,
                        labelName = labelName,
                        labelColor = labelColor,
                        isEditing = false,
                        currentLabelID = labelID,
                    )
                }
            }else{
                _uiState.update {
                    it.copy(
                        errorMessage = "Mapmo 업데이트 실패하였습니다.",
                    )
                }
                // Rollback to previous state on failure
                _editingContent.value = currentMapmo?.content ?: return@launch
            }
        }
    }

    /**
     * Toggle the notification enabled state for the current Mapmo.
     * Applies an optimistic UI update and rolls back on failure.
     */
    fun toggleNotification() {
        viewModelScope.launch {
            if(currentMapmo == null) return@launch
            val currentNotification = currentMapmo?.isNotifyEnabled ?: return@launch

            val isNotifyEnabled = !currentNotification
            val updatedMapmo = currentMapmo?.copy(
                isNotifyEnabled = isNotifyEnabled
            )

            if(updatedMapmo == null) return@launch

            val success = mapmoRepository.updateMapmo(
                mapmoContent = updatedMapmo,
                userID = TEST_USER_ID,
            )

            if (!success) {
                // Rollback to previous notification state on failure
                _uiState.update {
                    it.copy(
                        isNotifyEnabled = currentNotification,
                        errorMessage = "알림 상태 업데이트 실패",
                    )
                }
            } else {
                // Register to Geofencing Service
                val mapmoID = updatedMapmo.mapmoID
                if(isNotifyEnabled) {
                    val location = currentLabel!!.location
                    geofenceRepository.registerGeofence(mapmoID, location)
                } else {
                    geofenceRepository.unregisterGeofence(mapmoID)
                }

                _uiState.update {
                    it.copy(
                        isNotifyEnabled = !currentNotification,
                        errorMessage = null,
                    )
                }
            }
        }
    }

    /**
     * Fetch all labels available for the current user.
     * Opens the label selector on success, shows an error on failure.
     */
    fun loadLabelList() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLabelListLoading = true,
                )
            }
            val result = runCatching {
                labelRepository.getLabelList(userID = TEST_USER_ID)
            }.getOrNull()

            _uiState.update {
                it.copy(
                    isLabelListLoading = false,
                )
            }
            if (result == null) {
                _uiState.update {
                    it.copy(
                        errorMessage = "라벨 목록을 불러올 수 없습니다",
                    )
                }
                return@launch
            }
            _labelList.value = result.list
            _uiState.update {
                it.copy(
                    isLabelSelectorOpen = true,
                )
            }
        }
    }

    /**
     * Replace the current label with the selected one.
     * Closes the label selector after selection.
     *
     * @param label The [Label] selected by the user.
     */
    fun selectLabel(label: Label) {

        val labelName = label.name
        val labelColor = label.color
        val labelID = label.id
        val labelLat = label.location.lat
        val labelLng = label.location.lng
        val markerTitle = currentMapmo?.content

        // check markerTitle available
        if(markerTitle == null){
            _uiState.value = MapmoUiState(
                isLoading = false,
                errorMessage = "label의 내용을 찾을 수 없습니다",
            )
            return
        }

        val cameraFocus = createCameraFocus(lat = labelLat, lng = labelLng)
        val markers = createMarkers(lat = labelLat, lng = labelLng, markerTitle = markerTitle)

        currentLabel = label
        _uiState.update {
            it.copy(
                labelName = labelName,
                labelColor = labelColor,
                currentLabelID = labelID,
                isLabelListLoading = false,
                mapCameraFocus = cameraFocus,
                mapMarkerList = markers
            )
        }
    }

    /**
     * Close the label selector without changing the current label.
     */
    fun closeLabelSelector() {
        _uiState.update {
            it.copy(
                isLabelSelectorOpen = false,
            )
        }
    }

    /**
     * Create a [MapCameraFocusData] centered on the Mapmo's location.
     *
     * @param lat latitude of Label Location
     * @param lng longitude of Label Location
     */
    private fun createCameraFocus(lat: Double, lng: Double): MapCameraFocusData {
        val lat = lat.toFloat()  // precision loss: Double → Float intentional
        val lng = lng.toFloat()  // precision loss: Double → Float intentional

        return MapCameraFocusData(
            latitude = lat,
            longitude = lng,
        )
    }

    /**
     * Create a single-item marker list from the Mapmo's location.
     *
     *  @param lat latitude of Label Location
     *  @param lng longitude of Label Location
     *  @param markerTitle title of mapmo
     */
    private fun createMarkers(
        lat: Double,
        lng: Double,
        markerTitle: String,
    ): List<MapMarkerData> {
        val lat = lat.toFloat()  // precision loss: Double → Float intentional
        val lng = lng.toFloat()  // precision loss: Double → Float intentional

        return listOf(
            MapMarkerData(
                latitude = lat,
                longitude = lng,
                markerTitle = markerTitle,
                onClick = null,
            )
        )
    }
}