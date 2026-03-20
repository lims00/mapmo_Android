package com.a6w.memo.route.home.ui.subscreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.a6w.memo.common.model.MapCameraFocusData
import com.a6w.memo.common.model.MapMarkerData
import com.a6w.memo.common.ui.KakaoMapView
import com.a6w.memo.common.util.FirebaseLogUtil
import com.a6w.memo.route.home.ui.model.HomeListUiItem
import com.a6w.memo.route.home.viewmodel.HomeUiState
import kotlinx.coroutines.flow.distinctUntilChanged

private val BOTTOM_SHEET_HEIGHT_MINIMUM_DP = 180.dp
private val BOTTOM_SHEET_RADIUS_DP = 16.dp

/**
 * Home Normal UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeNormal(
    modifier: Modifier = Modifier,
    uiState: HomeUiState.Normal,
    deleteMapmo: (mapmoID: String) -> Unit,
    moveMapCamera: (labelID: String) -> Unit,
    navigateToMapmo: (mapmoID: String?) -> Unit,
    toggleMapmoNotify: (mapmoID: String) -> Unit,
) {
    // UI State
    val dataList = uiState.dataList
    val mapCameraFocus = uiState.mapCameraFocus
    val mapMarkerList = uiState.mapMarkerList

    // Bottom Sheet state
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState()
    )

    // Basic Scaffold UI
    // - Content UI: Bottom Sheet based Mapmo UI
    // - Floating Action Button: Create New Mapmo
    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        // FAB for Create New Mapmo
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { navigateToMapmo(null) },
            ) {
                // Edit Icon (Pencil)
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                )
            }
        },
        // Show FAB at bottom right
        floatingActionButtonPosition = FabPosition.End,
    ) { innerPadding ->
        // Bottom Sheet UI
        // - UI Background: KakaoMap View
        // - Bottom Sheet Content: Mapmo List UI
        BottomSheetScaffold(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            scaffoldState = scaffoldState,
            sheetContainerColor = Color.White,
            sheetDragHandle = { BottomSheetDefaults.DragHandle() },
            sheetPeekHeight = BOTTOM_SHEET_HEIGHT_MINIMUM_DP,
            sheetShape = RoundedCornerShape(
                topStart = BOTTOM_SHEET_RADIUS_DP,
                topEnd = BOTTOM_SHEET_RADIUS_DP,
            ),
            sheetContent = {
                // Mapmo List
                MapmoList(
                    modifier = Modifier
                        .fillMaxHeight(0.5f)
                        .fillMaxWidth(),
                    dataList = dataList,
                    onClickMapmo = navigateToMapmo,
                    onClickMapmoNotify = toggleMapmoNotify,
                    onScrollMapmoList = moveMapCamera,
                    onSwipeMapmo = deleteMapmo,
                )
            },
        ) { paddingValues ->
            // Mapmo Map
            MapmoMap(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                mapCameraFocus = mapCameraFocus,
                mapMarkerList = mapMarkerList
            )
        }
    }
}

/**
 * Mapmo Map
 * - Show map markers on [KakaoMapView]
 */
@Composable
private fun MapmoMap(
    modifier: Modifier = Modifier,
    mapCameraFocus: MapCameraFocusData? = null,
    mapMarkerList: List<MapMarkerData>? = null,
) {
    // KakaoMap View
    KakaoMapView(
        modifier = modifier
            .fillMaxSize(),
        cameraFocus = mapCameraFocus,
        markers = mapMarkerList,
    )
}

/**
 * Mapmo List
 */
@Composable
private fun MapmoList(
    modifier: Modifier = Modifier,
    dataList: List<HomeListUiItem>?,
    onClickMapmo: (mapmoID: String?) -> Unit,
    onClickMapmoNotify: (mapmoID: String) -> Unit,
    onScrollMapmoList: (labelID: String) -> Unit,
    onSwipeMapmo: (mapmoID: String) -> Unit,
) {
    // Exception when data list is null
    if(dataList == null) return

    // List state for mapmo list
    val listState = rememberLazyListState()

    // Effect for watching list scroll
    LaunchedEffect(listState) {
        // Check for first item of list, and check if it is changed
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { idx ->
                // Get target item from list
                when(val targetItem = dataList[idx]) {
                    // Label Item
                    is HomeListUiItem.LabelUiItem -> {
                        // Callback with target label
                        val labelID = targetItem.labelID
                        onScrollMapmoList(labelID)
                    }

                    // Mapmo items are not scroll target. ignore.
                    else -> {}
                }
            }
    }

    // Column for label, mapmo list
    LazyColumn(
        modifier = modifier,
        state = listState,
    ) {
        // Add each item to UI
        // - Items might be Label or Mapmo
        items(dataList.size) { idx ->
            when(val targetItem = dataList[idx]) {
                // Label Item
                is HomeListUiItem.LabelUiItem -> {
                    // Label data
                    val labelColor = targetItem.labelColor
                    val labelName = targetItem.labelName

                    LabelItem(
                        modifier = Modifier,
                        labelColor = labelColor,
                        labelName = labelName,
                    )
                }

                // Mapmo Item
                is HomeListUiItem.MapmoUiItem -> {
                    // Mapmo data
                    val mapmoID = targetItem.mapmoID
                    val mapmoTitle = targetItem.mapmoTitle
                    val mapmoUpdatedAt = targetItem.mapmoUpdatedAt
                    val mapmoIsNotifyEnabled = targetItem.mapmoIsNotifyEnabled

                    // Dismiss state for Mapmo Item
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            // If value is swipe, run callback method
                            when(value) {
                                SwipeToDismissBoxValue.StartToEnd,
                                SwipeToDismissBoxValue.EndToStart -> {
                                    onSwipeMapmo(mapmoID)
                                    true
                                }
                                else -> false
                            }
                        }
                    )

                    // Mapmo Item is wrapped with Dismiss Box
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {},
                    ) {
                        MapmoItem(
                            modifier = Modifier,
                            mapmoTitle = mapmoTitle,
                            mapmoUpdatedAt = mapmoUpdatedAt,
                            mapmoIsNotifyEnabled = mapmoIsNotifyEnabled,
                            onClick = { onClickMapmo(mapmoID) },
                            onClickNotifyIcon = { onClickMapmoNotify(mapmoID) },
                        )
                    }
                }
            }
        }

        // Mapmo banner image
        // - For scroll offset
        item {
            Image(
                modifier = Modifier
                    .fillMaxWidth(),
                painter = painterResource(com.a6w.memo.R.drawable.mapmo_banner),
                contentDescription = null,
            )
        }
    }
}

/**
 * Mapmo List Item - Label
 */
@Composable
private fun LabelItem(
    modifier: Modifier = Modifier,
    labelColor: String,
    labelName: String,
) {
    // Label Color Info
    // - If info is wrong, use black color as default
    val labelColor = try {
        Color(labelColor.toColorInt())
    } catch(e: Exception) {
        FirebaseLogUtil.logException(e, "Home")
        Color.Black
    }

    Box(
        modifier = modifier
            .padding(top = 16.dp)
            .padding(horizontal = 16.dp),
    ) {
        // Label Name Text
        Text(
            text = labelName,
            color = labelColor,
            fontSize = 18.sp,
        )
    }
}

/**
 * Mapmo List Item - Mapmo
 */
@Composable
private fun MapmoItem(
    modifier: Modifier = Modifier,
    mapmoTitle: String,
    mapmoUpdatedAt: String,
    mapmoIsNotifyEnabled: Boolean,
    onClick: () -> Unit,
    onClickNotifyIcon: () -> Unit,
) {
    // Set notification icon based on enabled state
    val notificationIcon = if(mapmoIsNotifyEnabled) {
        Icons.Default.Notifications
    } else {
        Icons.Default.NotificationsNone
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = modifier
                .weight(1f),
        ) {
            // Mapmo Title Text
            Text(
                text = mapmoTitle,
                fontSize = 18.sp,
            )

            // Mapmo Date Text
            Text(
                text = mapmoUpdatedAt,
                fontSize = 14.sp,
            )
        }

        // Mapmo notification on/off button
        Box(
            modifier = Modifier
                .clickable(onClick = onClickNotifyIcon),
        ) {
            Icon(
                modifier = Modifier
                    .size(24.dp),
                imageVector = notificationIcon,
                contentDescription = null,
            )
        }
    }
}