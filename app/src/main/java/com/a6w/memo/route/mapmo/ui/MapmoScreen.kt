package com.a6w.memo.route.mapmo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.a6w.memo.common.model.MapCameraFocusData
import com.a6w.memo.common.model.MapMarkerData
import com.a6w.memo.common.ui.KakaoMapView
import com.a6w.memo.common.util.DatetimeUtil
import com.a6w.memo.domain.model.Label
import com.a6w.memo.route.mapmo.viewmodel.MapmoViewModel

// ——— Constants ———————————————————————————————————————————

private val LOADING_PLACEHOLDER_HEIGHT_DP = 400.dp
private val MAP_VIEW_HEIGHT_DP = 300.dp
private val CONTENT_FONT_SIZE_SP = 18.sp
private val CONTENT_LINE_HEIGHT_SP = 28.sp
private val META_FONT_SIZE_SP = 14.sp
private val TITLE_FONT_SIZE_SP = 20.sp

// ——— Screen —————————————————————————————————————————————

/**
 * Top-level screen composable for the Mapmo detail view.
 *
 * Displays memo content, an edit/save toggle, a notification toggle,
 * and a Kakao map view fixed at the bottom of the screen.
 *
 * State is managed by [MapmoViewModel] via Hilt injection.
 *
 * @param modifier Optional [Modifier] to apply to the root layout.
 * @param viewModel ViewModel instance provided by Hilt.
 * @param navigationPop Callback invoked when the user taps the back button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapmoScreen(
    modifier: Modifier = Modifier,
    viewModel: MapmoViewModel = hiltViewModel(),
    navigationPop: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editingContent by viewModel.editingContent.collectAsStateWithLifecycle()
    val labelList by viewModel.labelList.collectAsStateWithLifecycle()

    // Triggers data load once when the screen enters composition.
    LaunchedEffect(Unit) {
        viewModel.loadMapmo()
    }

    val content = uiState.content
    val updatedAt = uiState.updatedAt
    val isNotifyEnabled = uiState.isNotifyEnabled
    val currentLabelID = uiState.currentLabelID
    val labelName = uiState.labelName
    val labelColor = uiState.labelColor
    val mapCameraFocus = uiState.mapCameraFocus
    val mapMarkerList = uiState.mapMarkerList

    val isEditing = uiState.isEditing
    val isLabelSelectorOpen = uiState.isLabelSelectorOpen
    val isLabelListLoading = uiState.isLabelListLoading

    Column(modifier = modifier.fillMaxSize()) {
        MapmoTopBar(
            isEditing = isEditing,
            onBackClick = navigationPop,
            onEditClick = { viewModel.toggleEditMode() },
            onSaveClick = { viewModel.saveContent() },
        )
        when {
            uiState.isLoading -> LoadingContent()
            uiState.errorMessage != null -> ErrorContent(message = uiState.errorMessage!!)
            else -> MapmoContent(
                modifier = Modifier.weight(1f),
                content = content,
                isNotifyEnabled = isNotifyEnabled,
                updatedAt = updatedAt,
                currentLabelID = currentLabelID ?: "",
                labelName = labelName ?: "",
                labelColor = labelColor ?: "",
                labelList = labelList,
                isEditing = isEditing,
                isLabelSelectorOpen = isLabelSelectorOpen,
                isLabelListLoading = isLabelListLoading,
                editingContent = editingContent,
                mapCameraFocus = mapCameraFocus,
                mapMarkerList = mapMarkerList,
                onContentChange = { viewModel.updateEditingContent(it) },
                onNotificationToggle = { viewModel.toggleNotification() },
                onLabelChipClick = { viewModel.loadLabelList() },
                onLabelSelect = { viewModel.selectLabel(it) },
            )
        }
    }
}

// ——— Top Bar —————————————————————————————————————————————

/**
 * Top app bar for the Mapmo screen.
 * Switches between edit and save actions based on [isEditing].
 *
 * @param isEditing Whether the screen is currently in edit mode.
 * @param onBackClick Callback invoked when the back button is tapped.
 * @param onEditClick Callback invoked when the edit icon is tapped.
 * @param onSaveClick Callback invoked when the save button is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapmoTopBar(
    isEditing: Boolean,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = "MAPMO",
                fontSize = TITLE_FONT_SIZE_SP,
                fontWeight = FontWeight.Medium,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
        actions = {
            if (isEditing) {
                TextButton(onClick = onSaveClick) {
                    Text("저장", fontWeight = FontWeight.Bold)
                }
            } else {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit",
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    )
}

// ——— State UI ————————————————————————————————————————————

/**
 * Displays a centered loading indicator while data is being fetched.
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(LOADING_PLACEHOLDER_HEIGHT_DP),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Displays an error card when data fetching fails.
 *
 * @param message Human-readable error description to display.
 */
@Composable
private fun ErrorContent(
    message: String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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

// ——— Main Content ————————————————————————————————————————

/**
 * Displays the main content section including label chip,
 * optional label selector, timestamp, notification toggle,
 * and the memo content card.
 *
 * @param content Mapmo memo content.
 * @param updatedAt Last updated timestamp (epoch millis).
 * @param isNotifyEnabled Whether notification is enabled.
 * @param currentLabelID Currently selected label ID.
 * @param labelName Name of the associated label.
 * @param labelColor Color string of the associated label.
 * @param labelList Labels available for selection.
 * @param isEditing Whether the screen is currently in edit mode.
 * @param isLabelSelectorOpen Whether the label selector is visible.
 * @param isLabelListLoading Whether the label list is loading.
 * @param editingContent Current content value while editing.
 * @param onContentChange Callback invoked when memo content changes.
 * @param onNotificationToggle Callback invoked when notification state toggles.
 * @param onLabelChipClick Callback invoked when the label chip is tapped.
 * @param onLabelSelect Callback invoked when a label is selected.
 */
@Composable
private fun MapmoContent(
    modifier: Modifier = Modifier,
    content: String?,
    updatedAt: Long,
    isNotifyEnabled: Boolean,
    currentLabelID: String,
    labelName: String,
    labelColor: String,
    labelList: List<Label>,
    isEditing: Boolean,
    isLabelSelectorOpen: Boolean,
    isLabelListLoading: Boolean,
    editingContent: String,
    mapCameraFocus: MapCameraFocusData?,
    mapMarkerList: List<MapMarkerData>?,
    onContentChange: (String) -> Unit,
    onNotificationToggle: () -> Unit,
    onLabelChipClick: () -> Unit,
    onLabelSelect: (Label) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                ContentSection(
                    content = content,
                    isNotifyEnabled = isNotifyEnabled,
                    updatedAt = updatedAt,
                    currentLabelID = currentLabelID,
                    labelName = labelName,
                    labelColor = labelColor,
                    labelList = labelList,
                    isEditing = isEditing,
                    isLabelSelectorOpen = isLabelSelectorOpen,
                    isLabelListLoading = isLabelListLoading,
                    editingContent = editingContent,
                    onContentChange = onContentChange,
                    onNotificationToggle = onNotificationToggle,
                    onLabelChipClick = onLabelChipClick,
                    onLabelSelect = onLabelSelect,
                )
            }
        }
        MapSection(
            mapCameraFocus = mapCameraFocus,
            mapMarkerList = mapMarkerList,
        )
    }
}

// ——— Content Section —————————————————————————————————————

/**
 * Displays memo related UI including label chip, label selector,
 * updated time, notification toggle, and the memo content card.
 *
 * @param content Mapmo memo content. May be null if empty.
 * @param updatedAt Last updated timestamp (epoch seconds).
 * @param isNotifyEnabled Whether notification is enabled.
 * @param currentLabelID Currently selected label ID.
 * @param labelName Name of the associated label.
 * @param labelColor Color string of the associated label.
 * @param labelList Labels available in the selector.
 * @param isEditing Whether the screen is currently in edit mode.
 * @param isLabelSelectorOpen Whether the label selector is expanded.
 * @param isLabelListLoading Whether the label list is loading.
 * @param editingContent Current text value while editing.
 * @param onContentChange Callback invoked when memo text changes.
 * @param onNotificationToggle Callback invoked when notification state toggles.
 * @param onLabelChipClick Callback invoked when the label chip is tapped.
 * @param onLabelSelect Callback invoked when a label is selected.
 */
@Composable
private fun ContentSection(
    content: String?,
    updatedAt: Long,
    isNotifyEnabled: Boolean,
    currentLabelID: String,
    labelName: String,
    labelColor: String,
    labelList: List<Label>,
    isEditing: Boolean,
    isLabelSelectorOpen: Boolean,
    isLabelListLoading: Boolean,
    editingContent: String,
    onContentChange: (String) -> Unit,
    onNotificationToggle: () -> Unit,
    onLabelChipClick: () -> Unit,
    onLabelSelect: (Label) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LabelChip(
            labelName = labelName,
            labelColor = labelColor,
            onClick = if (isEditing) onLabelChipClick else null,
        )

        if (isEditing && isLabelSelectorOpen) {
            LabelSelector(
                labelList = labelList,
                isLoading = isLabelListLoading,
                currentLabelID = currentLabelID,
                onLabelSelect = onLabelSelect,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UpdatedAtText(updatedAt = updatedAt)
            NotificationRow(
                isEnabled = isNotifyEnabled,
                onToggle = onNotificationToggle,
            )
        }
        ContentCard(
            isEditing = isEditing,
            content = if (isEditing) editingContent else content ?: "",
            onContentChange = onContentChange,
        )
    }
}

/**
 * Displays the memo text as a read-only [Text] or an editable [TextField]
 * depending on [isEditing].
 *
 * @param isEditing Whether the card is in edit mode.
 * @param content Current text to display or edit.
 * @param onContentChange Callback invoked when the user modifies the text.
 */
@Composable
private fun ContentCard(
    isEditing: Boolean,
    content: String,
    onContentChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        if (isEditing) {
            TextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = CONTENT_FONT_SIZE_SP,
                    lineHeight = CONTENT_LINE_HEIGHT_SP,
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                placeholder = { Text("내용을 입력하세요") },
            )
        } else {
            Text(
                text = content,
                fontSize = CONTENT_FONT_SIZE_SP,
                fontWeight = FontWeight.Medium,
                lineHeight = CONTENT_LINE_HEIGHT_SP,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

/**
 * Displays the formatted last updated timestamp.
 *
 * Uses [remember] to avoid recomputing the formatted string
 * on every recomposition.
 *
 * @param updatedAt Unix epoch seconds representing the last update time.
 */
@Composable
private fun UpdatedAtText(updatedAt: Long) {
    val formattedDate = remember(updatedAt) {
        DatetimeUtil.getUiDateStringFromMillis(updatedAt * 1000)

    }

    Text(
        text = formattedDate,
        fontSize = META_FONT_SIZE_SP,
        color = Color.Gray,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

/**
 * Displays a checkbox row used to toggle notification state.
 *
 * The entire row is clickable to improve touch target usability.
 *
 * @param isEnabled Current notification state.
 * @param onToggle Callback invoked when the row is tapped.
 * @param modifier Optional [Modifier] applied to the row.
 */
@Composable
private fun NotificationRow(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickable { onToggle() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isEnabled,
            // Delegate click handling to the parent Row to avoid double invocation
            onCheckedChange = null,
        )
    }
}

// ——— Label ———————————————————————————————————————————————

/**
 * Displays a pill-shaped chip representing a label.
 *
 * Shows a colored dot and label name, and highlights when selected.
 *
 * @param labelName Label name to display.
 * @param labelColor Label color string used for the chip.
 * @param isSelected Whether this chip is currently selected.
 * @param onClick Callback invoked when tapped. Null makes the chip non-interactive.
 */
@Composable
private fun LabelChip(
    labelName: String,
    labelColor: String,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val color = remember(labelColor) {
        runCatching { Color(labelColor.toColorInt()) }
            .getOrDefault(Color.Gray)
    }
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) color.copy(alpha = 0.35f) else color.copy(alpha = 0.15f))
            .then(
                if (isSelected) Modifier.border(
                    width = 1.5.dp,
                    color = color,
                    shape = RoundedCornerShape(50),
                ) else Modifier
            )
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = labelName,
            fontSize = 13.sp,
            color = color,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

/**
 * Horizontally scrollable label selector used in edit mode.
 *
 * Displays loading state, empty state, or the full label list
 * with the currently selected label highlighted.
 *
 * @param labelList All available labels.
 * @param isLoading Whether the label list is currently loading.
 * @param currentLabelID Currently selected label ID used for highlighting.
 * @param onLabelSelect Callback invoked when a label is selected.
 */
@Composable
private fun LabelSelector(
    labelList: List<Label>,
    isLoading: Boolean,
    currentLabelID: String?,
    onLabelSelect: (Label) -> Unit,
) {
    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            }
        }

        labelList.isEmpty() -> {
            Text(
                text = "라벨이 없습니다",
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color.Gray,
                fontSize = META_FONT_SIZE_SP,
            )
        }

        else -> {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(
                    items = labelList,
                    key = { it.id },
                ) { label ->
                    LabelChip(
                        labelName = label.name,
                        labelColor = label.color,
                        isSelected = label.id == currentLabelID,
                        onClick = { onLabelSelect(label) },
                    )
                }
            }
        }
    }
}

// ——— Map Section —————————————————————————————————————————

/**
 * Displays the Kakao map view in a fixed-height container anchored to the bottom.
 *
 * @param mapCameraFocus Camera focus position for the map. Null shows fallback UI.
 * @param mapMarkerList Markers to render on the map. Null shows fallback UI.
 */
@Composable
private fun MapSection(
    mapCameraFocus: MapCameraFocusData?,
    mapMarkerList: List<MapMarkerData>?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MAP_VIEW_HEIGHT_DP),
    ) {
        MapmoMapView(
            modifier = Modifier.fillMaxSize(),
            mapCameraFocus = mapCameraFocus,
            mapMarkerList = mapMarkerList,
        )
    }
}

/**
 * Wraps [KakaoMapView] with a fallback message when location data is unavailable.
 *
 * @param modifier [Modifier] to apply to the root container.
 * @param mapCameraFocus Camera focus data. Shows fallback text if null.
 * @param mapMarkerList Marker list. Shows fallback text if null.
 */
@Composable
private fun MapmoMapView(
    modifier: Modifier = Modifier,
    mapCameraFocus: MapCameraFocusData?,
    mapMarkerList: List<MapMarkerData>?,
) {
    Box(modifier = modifier) {
        if (mapCameraFocus != null && mapMarkerList != null) {
            KakaoMapView(
                modifier = Modifier.fillMaxSize(),
                cameraFocus = mapCameraFocus,
                markers = mapMarkerList,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("지도를 불러올 수 없습니다", color = Color.Gray)
            }
        }
    }
}
