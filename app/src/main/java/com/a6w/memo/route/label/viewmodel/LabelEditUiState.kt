import com.a6w.memo.common.model.MapCameraFocusData
import com.a6w.memo.common.model.MapMarkerData
import com.a6w.memo.domain.model.Address
import com.a6w.memo.domain.model.Location

/**
 * UI state for the label add/edit screen.
 *
 * @param labelID ID of the label being edited. Null in add mode.
 * @param name Current name input value.
 * @param color Current hex color string.
 * @param selectedLocation Currently selected location. Null until the user picks one.
 * @param locationDisplayName Display name of the selected location.
 * @param searchResults Current address search results.
 * @param isLoading Whether the initial label data is being fetched (edit mode only).
 * @param isSaving Whether a save request is in progress.
 * @param isSearchLoading Whether an address search request is in progress.
 * @param errorMessage Validation or network error message. Null when no issue.
 */
data class LabelEditUiState(
    val labelID: String? = null,
    val name: String = "",
    val color: String = "#FF5733",
    val selectedLocation: Location? = null,
    val locationDisplayName: String? = null,
    val selectedFullAddress: String? = null,
    val searchResults: List<Address> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSearchLoading: Boolean = false,
    val errorMessage: String? = null,
    val mapCameraFocus: MapCameraFocusData? = null,
    val mapMarkerList: List<MapMarkerData>? = null,
)