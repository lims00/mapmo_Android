package com.a6w.memo.route.label.viewmodel

import LabelEditUiState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a6w.memo.BuildConfig
import com.a6w.memo.common.model.MapCameraFocusData
import com.a6w.memo.common.model.MapMarkerData
import com.a6w.memo.data.retrofit.RetrofitRepositoryFactory
import com.a6w.memo.domain.model.Address
import com.a6w.memo.domain.model.Label
import com.a6w.memo.domain.model.Location
import com.a6w.memo.domain.repository.AddressSearchRepository
import com.a6w.memo.domain.repository.LabelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the label add/edit screen.
 * Manages label name, color, location input, and address search state.
 *
 * Supports both add mode (no labelID) and edit mode (with labelID).
 */
@HiltViewModel
@OptIn(FlowPreview::class)
class LabelEditViewModel @Inject constructor(
    private val labelRepository: LabelRepository,
): ViewModel() {

    companion object {
        private const val TEST_USER_ID = "test_user_1"
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val SEARCH_MIN_LENGTH = 2
    }

    private val _uiState = MutableStateFlow(LabelEditUiState())
    val uiState: StateFlow<LabelEditUiState> = _uiState.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var _isAddressSelected = false
    private val addressSearchRepository: AddressSearchRepository =
        RetrofitRepositoryFactory.createAddressSearchRepository(
            apiKey = BuildConfig.KAKAO_REST_API_KEY,
        )
    private var currentLabel = Label(
        id = UUID.randomUUID().toString(),
        name = "",
        color = "",
        location = Location(-1.0, -1.0),
    )

    // Flag to distinguish between add and update
    private var isEditMode = false

    init {
        observeSearchQuery()
    }

    /**
     * Initialize the screen in edit mode by loading an existing label.
     * If [labelID] is null, the screen starts in add mode with default values.
     *
     * @param labelID ID of the label to load. Null for add mode.
     */
    fun initialize(labelID: String?) {
        if (labelID == null) return
        isEditMode = true
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isLoading = true,
                )
            }

            val label = labelRepository.getLabel(
                labelID = labelID,
                userID = TEST_USER_ID,
            )

            if (label == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "라벨을 불러올 수 없습니다",
                    )
                }
                return@launch
            }
            val labelID = label.id
            val labelName = label.name
            val labelColor = label.color
            val labelLocation = label.location
            currentLabel = label
            _uiState.update {
                it.copy(
                    isLoading = false,
                    labelID = labelID,
                    name = labelName,
                    color = labelColor,
                    selectedLocation = labelLocation,
                    locationDisplayName = null,
                )
            }
        }
    }

    /**
     * Update the label name input.
     *
     * @param name New name value entered by the user.
     */
    fun updateName(name: String) {
        currentLabel = currentLabel.copy(name = name)
        _uiState.update {
            it.copy(name = name)
        }
    }

    /**
     * Update the selected color from the HSV color picker.
     *
     * @param hexColor Hex color string (e.g. "#FF5733").
     */
    fun updateColor(hexColor: String) {
        currentLabel = currentLabel.copy(color = hexColor)
        _uiState.update {
            it.copy(color = hexColor)
        }
    }


    /**
     * Update the address search query and trigger a debounced search.
     *
     * @param query Keyword entered by the user.
     */
    fun updateSearchQuery(query: String) {

        _searchQuery.update { query }
        if (query.length < SEARCH_MIN_LENGTH) {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    /**
     * Select an address from the search results.
     * Updates the location and clears the search state.
     *
     * @param address The [Address] selected by the user.
     */
    fun selectAddress(address: Address) {

        val addressLat = address.lat
        val addressLng = address.lng

        val location = Location(
            lat = addressLat,
            lng = addressLng,
        )
        currentLabel = currentLabel.copy(location = location)
        val latFloat = addressLat.toFloat()  // precision loss: Double → Float intentional
        val lngFloat = addressLng.toFloat()  // precision loss: Double → Float intentional

        val cameraFocus = MapCameraFocusData(
            latitude = latFloat,
            longitude = lngFloat,
        )

        val markers = listOf(
            MapMarkerData(
                latitude = latFloat,
                longitude = lngFloat,
                markerTitle = "",
                onClick = null,
            )
        )

        val addressName = address.name
        val addressFullAddress = address.fullAddress

        _isAddressSelected = true
        _searchQuery.update { addressName }
        _uiState.update {
            it.copy(
                selectedLocation = location,
                locationDisplayName = addressName,
                selectedFullAddress = addressFullAddress,
                searchResults = emptyList(),
                mapCameraFocus = cameraFocus,
                mapMarkerList = markers,
            )
        }
    }

    /**
     * Clear the address search query and results.
     */
    fun clearSearch() {
        _isAddressSelected = false
        _searchQuery.update { "" }
        _uiState.update { it.copy(searchResults = emptyList()) }
    }

    /**
     * Save the label. Calls add or update depending on whether [LabelEditUiState.labelID] is set.
     * Invokes [onSuccess] on completion.
     *
     * @param onSuccess Callback invoked after a successful save.
     */
    fun saveLabel(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = currentLabel

            if (state.name.isBlank()) {
                _uiState.update {
                    it.copy(errorMessage = "이름을 입력해주세요")
                }
                return@launch
            }

            val location = state.location
            if (location == null) {
                _uiState.update {
                    it.copy(errorMessage = "위치를 선택해주세요")
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isSaving = true,
                    errorMessage = null,
                )
            }
            val labelID = state.id
            val label = Label(
                id = labelID ?: UUID.randomUUID().toString(),
                name = state.name,
                color = state.color,
                location = location,
            )
            // Dispatch to add or update based on whether the screen was initialized with an existing label.
            val success = if (isEditMode) {
                labelRepository.updateLabel(
                    labelID = state.id,
                    labelContent = label,
                    userID = TEST_USER_ID,
                )
            } else {
                labelRepository.addLabel(
                    userID = TEST_USER_ID,
                    labelContent = label,
                )
            }

            if (success) {
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "저장에 실패했습니다",
                    )
                }
            }
        }
    }

    /**
     * Observe search query and fetch address results automatically.
     * Debounced to avoid excessive API calls while the user is typing.
     */
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .filter { it.length >= SEARCH_MIN_LENGTH }
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .filter {
                    if (_isAddressSelected) {
                        _isAddressSelected = false
                        return@filter false
                    }
                    true
                }
                .collect { query -> fetchAddresses(query) }
        }
    }

    /**
     * Fetch address results from the repository for the given query.
     *
     * @param query Keyword to search for.
     */
    private suspend fun fetchAddresses(query: String) {
        _uiState.update { it.copy(isSearchLoading = true) }

        val results = addressSearchRepository.getSearchResult(query)

        _uiState.update {
            it.copy(
                isSearchLoading = false,
                searchResults = results,
            )
        }
    }
}