package com.a6w.memo.route.label.ui

import LabelEditUiState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.a6w.memo.common.model.MapCameraFocusData
import com.a6w.memo.common.model.MapMarkerData
import com.a6w.memo.common.ui.KakaoMapView
import com.a6w.memo.domain.model.Address
import com.a6w.memo.route.label.viewmodel.LabelEditViewModel
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

// ——— Constants ———————————————————————————————————————————

private val SEARCH_RESULT_MAX_HEIGHT_DP = 200.dp
private val COLOR_PREVIEW_SIZE_DP = 30.dp
private val COLOR_PICKER_HEIGHT_DP = 200.dp
private val MAP_VIEW_HEIGHT_DP = 180.dp

// ——— Entry Points ————————————————————————————————————————

/**
 * Full-screen label add/edit screen wrapped in a [Scaffold].
 * Used from the Settings screen via normal navigation.
 *
 * @param modifier Optional [Modifier] to apply to the root layout.
 * @param labelID ID of the label to edit. Null for add mode.
 * @param viewModel ViewModel instance provided by Hilt.
 * @param navigationPop Callback invoked when the user taps the back button or saves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelEditScreen(
    modifier: Modifier = Modifier,
    labelID: String? = null,
    viewModel: LabelEditViewModel = hiltViewModel(),
    navigationPop: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val name = uiState.name
    val color = uiState.color
    val searchResults = uiState.searchResults
    val isSearchLoading = uiState.isSearchLoading
    val selectedLocation = uiState.selectedLocation
    val locationDisplayName = uiState.locationDisplayName
    val selectedFullAddress = uiState.selectedFullAddress
    val mapCameraFocus = uiState.mapCameraFocus
    val mapMarkerList = uiState.mapMarkerList
    val isLoading = uiState.isLoading
    val isSaving = uiState.isSaving
    val errorMessage = uiState.errorMessage
    val labelID_ = uiState.labelID

    LaunchedEffect(labelID) {
        viewModel.initialize(labelID)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (labelID != null) "라벨 수정" else "라벨 추가",
                        fontWeight = FontWeight.Medium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigationPop) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        LabelEditContent(
            modifier = Modifier.padding(innerPadding),
            name = name,
            color = color,
            searchQuery = searchQuery,
            searchResults = searchResults,
            isSearchLoading = isSearchLoading,
            selectedLocation = selectedLocation,
            locationDisplayName = locationDisplayName,
            selectedFullAddress = selectedFullAddress,
            mapCameraFocus = mapCameraFocus,
            mapMarkerList = mapMarkerList,
            isLoading = isLoading,
            isSaving = isSaving,
            errorMessage = errorMessage,
            labelID = labelID_,
            onNameChange = { viewModel.updateName(it) },
            onColorChange = { viewModel.updateColor(it) },
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            onSearchClear = { viewModel.clearSearch() },
            onAddressSelect = { viewModel.selectAddress(it) },
            onSave = { viewModel.saveLabel(onSuccess = navigationPop) },
        )
    }
}

/**
 * Bottom sheet content for adding a label.
 * Used from the Mapmo screen via [androidx.compose.material3.ModalBottomSheet].
 *
 * @param modifier Optional [Modifier] to apply to the root layout.
 * @param viewModel ViewModel instance provided by Hilt.
 * @param onDismiss Callback invoked after a successful save or user dismissal.
 */
@Composable
fun LabelEditBottomSheetContent(
    modifier: Modifier = Modifier,
    viewModel: LabelEditViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val name = uiState.name
    val color = uiState.color
    val searchResults = uiState.searchResults
    val isSearchLoading = uiState.isSearchLoading
    val selectedLocation = uiState.selectedLocation
    val locationDisplayName = uiState.locationDisplayName
    val selectedFullAddress = uiState.selectedFullAddress
    val mapCameraFocus = uiState.mapCameraFocus
    val mapMarkerList = uiState.mapMarkerList
    val isLoading = uiState.isLoading
    val isSaving = uiState.isSaving
    val errorMessage = uiState.errorMessage
    val labelID = uiState.labelID
    LabelEditContent(
        modifier = modifier,
        name = name,
        color = color,
        searchResults = searchResults,
        searchQuery = searchQuery,
        isSearchLoading = isSearchLoading,
        selectedLocation = selectedLocation,
        locationDisplayName = locationDisplayName,
        selectedFullAddress = selectedFullAddress,
        mapCameraFocus = mapCameraFocus,
        mapMarkerList = mapMarkerList,
        isLoading = isLoading,
        isSaving = isSaving,
        errorMessage = errorMessage,
        labelID = labelID,
        onNameChange = { viewModel.updateName(it) },
        onColorChange = { viewModel.updateColor(it) },
        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
        onSearchClear = { viewModel.clearSearch() },
        onAddressSelect = { viewModel.selectAddress(it) },
        onSave = { viewModel.saveLabel(onSuccess = onDismiss) },
    )
}

// ——— Core Content ————————————————————————————————————————

/**
 * Shared label add/edit content used by both [LabelEditScreen] and [LabelEditBottomSheetContent].
 *
 * @param modifier [Modifier] to apply to the root layout.
 * @param uiState Current UI state from [LabelEditViewModel].
 * @param searchQuery Current address search query text.
 * @param onNameChange Callback invoked when the name input changes.
 * @param onColorChange Callback invoked when the color picker value changes.
 * @param onSearchQueryChange Callback invoked when the address search input changes.
 * @param onSearchClear Callback invoked when the search clear button is tapped.
 * @param onAddressSelect Callback invoked when an address result is selected.
 * @param onSave Callback invoked when the save button is tapped.
 */
@Composable
private fun LabelEditContent(
    modifier: Modifier = Modifier,
    name: String,
    color: String,
    searchQuery: String,
    searchResults: List<Address>,
    isSearchLoading: Boolean,
    selectedLocation: com.a6w.memo.domain.model.Location?,
    locationDisplayName: String?,
    selectedFullAddress: String?,
    mapCameraFocus: MapCameraFocusData?,
    mapMarkerList: List<MapMarkerData>?,
    isLoading: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    labelID: String?,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchClear: () -> Unit,
    onAddressSelect: (Address) -> Unit,
    onSave: () -> Unit,
) {
    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val currentColor = remember(color) {
        runCatching { Color(color.toColorInt()) }.getOrDefault(Color.Gray)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 20.dp,
            vertical = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Error message
       errorMessage?.let { error ->
            item {
                ErrorBanner(message = error)
            }
        }

        // Label name input
        item {
            TextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                placeholder = { Text("라벨 이름을 입력하세요") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            )
        }


        // Address search
        item {
            AddressSearchSection(
                searchQuery = searchQuery,
                searchResults = searchResults,
                isSearchLoading = isSearchLoading,
                selectedLocation = selectedLocation,
                locationDisplayName = locationDisplayName,
                fullAddress = selectedFullAddress,
                mapCameraFocus = mapCameraFocus,
                mapMarkerList = mapMarkerList,
                onSearchQueryChange = onSearchQueryChange,
                onSearchClear = onSearchClear,
                onAddressSelect = onAddressSelect,
            )
        }

        // Color picker
        item {
            ColorPickerSection(
                currentColor = currentColor,
                onColorChange = onColorChange,
            )
        }

        // Save button
        item {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = if (labelID != null) "수정 완료" else "추가 완료",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ——— Section Wrapper ————————————————————————————————————

///**
// * Section container with a small title label above the content.
// *
// * @param title Section title displayed above [content].
// * @param content Composable content of the section.
// */
//@Composable
//private fun LabelSection(
//    title: String,
//    content: @Composable () -> Unit,
//) {
//    Row(horizontalArrangement = Arrangement.Center) {
//        content()
//    }
//}

// ——— Color Picker ————————————————————————————————————————

/**
 * HSV color picker with a live preview dot.
 *
 * @param currentColor Currently selected [Color] for preview.
 * @param onColorChange Callback invoked with the updated hex string when the color changes.
 */
@Composable
private fun ColorPickerSection(
    currentColor: Color,
    onColorChange: (String) -> Unit,
) {
    val controller = rememberColorPickerController()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Color preview dot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(COLOR_PREVIEW_SIZE_DP)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
            Text(
                text = "선택된 색상",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HsvColorPicker(
            modifier = Modifier
                .fillMaxWidth()
                .height(COLOR_PICKER_HEIGHT_DP),
            controller = controller,
            onColorChanged = { colorEnvelope ->
                // colorEnvelope.hexCode format: "AARRGGBB" → convert to "#RRGGBB"
                val hex = "#${colorEnvelope.hexCode.drop(2)}"
                onColorChange(hex)
            },
        )
    }
}

// ——— Address Search ——————————————————————————————————————

/**
 * Address search input, result list, and selected location display.
 *
 * @param searchQuery Current search input text.
 * @param searchResults Current list of address results.
 * @param isSearchLoading Whether a search is in progress.
 * @param selectedLocation Currently selected [com.a6w.memo.domain.model.Location]. Null if not yet set.
 * @param locationDisplayName Display name of the selected location.
 * @param onSearchQueryChange Callback invoked when the search input changes.
 * @param onSearchClear Callback invoked when the clear button is tapped.
 * @param onAddressSelect Callback invoked when an address result is tapped.
 */
@Composable
private fun AddressSearchSection(
    searchQuery: String,
    searchResults: List<Address>,
    isSearchLoading: Boolean,
    selectedLocation: com.a6w.memo.domain.model.Location?,
    locationDisplayName: String?,
    fullAddress: String?,
    mapCameraFocus: MapCameraFocusData?,
    mapMarkerList: List<MapMarkerData>?,
    onSearchQueryChange: (String) -> Unit,
    onSearchClear: () -> Unit,
    onAddressSelect: (Address) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("장소를 검색하세요") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                )
            },
            trailingIcon = {
                if (isSearchLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onSearchClear) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                        )
                    }
                }
            },
            singleLine = true,
            // Search is triggered automatically via debounce in the ViewModel
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {}),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            )
        )

        // Search results
        if (searchResults.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = SEARCH_RESULT_MAX_HEIGHT_DP),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                LazyColumn {
                    items(
                        items = searchResults,
                        key = { it.name },
                    ) { address ->
                        AddressResultItem(
                            address = address,
                            onAddressSelect = onAddressSelect,
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        // Selected location display
        if (selectedLocation != null) {
            SelectedLocationCard(
                displayName = locationDisplayName,
                fullAddress = fullAddress
            )
        }
        if (mapCameraFocus != null && mapMarkerList != null) {
            LabelMapPreview(
                mapCameraFocus = mapCameraFocus,
                mapMarkerList = mapMarkerList,
            )
        }
    }
}

/**
 * Single address result row showing address name and road address.
 *
 * @param address The [Address] to display.
 * @param onAddressSelect Callback invoked when the item is tapped.
 */
@Composable
private fun AddressResultItem(
    address: Address,
    onAddressSelect: (Address) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddressSelect(address) }
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {

        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = address.name,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )

            if (address.fullAddress.isNotEmpty()) {
                Text(
                    text = address.fullAddress,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Card displaying the currently selected location name and coordinates.
 *
 * @param displayName Human-readable name of the location. Shows coordinates if null.
 * @param latitude Latitude of the selected location.
 * @param longitude Longitude of the selected location.
 */
@Composable
private fun SelectedLocationCard(
    displayName: String?,
    fullAddress: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Column {
                Text(
                    text = displayName ?: "선택된 위치",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                if (fullAddress != null) {
                    Text(
                        text = fullAddress,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ——— Map Preview ————————————————————————————————————————

/**
 * Map preview shown after an address is selected.
 * Displays the selected location with a marker on the Kakao map.
 *
 * @param mapCameraFocus Camera focus for the selected location.
 * @param mapMarkerList Markers to render on the map.
 */
@Composable
private fun LabelMapPreview(
    mapCameraFocus: MapCameraFocusData,
    mapMarkerList: List<MapMarkerData>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MAP_VIEW_HEIGHT_DP),
        ) {
            KakaoMapView(
                modifier = Modifier.fillMaxSize(),
                cameraFocus = mapCameraFocus,
                markers = mapMarkerList,
            )
        }
    }
}

// ——— Error Banner ————————————————————————————————————————

/**
 * Red error banner displayed at the top of the form when validation or network errors occur.
 *
 * @param message Error message to display.
 */
@Composable
private fun ErrorBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0.1f),
        ),
    ) {
        Text(
            text = "❌ $message",
            modifier = Modifier.padding(16.dp),
            color = Color.Red,
        )
    }
}

