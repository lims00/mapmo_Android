package com.a6w.memo.route.home.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.a6w.memo.common.model.MapCameraFocusData
import com.a6w.memo.common.model.MapMarkerData
import com.a6w.memo.common.ui.KakaoMapView
import com.a6w.memo.route.home.viewmodel.HomeViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a6w.memo.domain.model.Mapmo
import androidx.core.graphics.toColorInt
import com.a6w.memo.common.util.FirebaseLogUtil
import com.a6w.memo.route.home.ui.model.HomeListUiItem
import kotlinx.coroutines.flow.distinctUntilChanged

private val BOTTOM_SHEET_HEIGHT_MINIMUM_DP = 180.dp
private val BOTTOM_SHEET_RADIUS_DP = 16.dp

/**
 * Home Screen
 * - Home UI for mapmo application
 * - Uses state from [HomeViewModel]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    navigateToMapmo: (mapmoID: String?) -> Unit,
    navigateToSetting: () -> Unit,
) {
    // Bottom Sheet state
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState()
    )

    // UI States
    val uiState = viewModel.uiState.collectAsState()
    val dataList = uiState.value.dataList
    val mapCameraFocus = uiState.value.mapCameraFocus
    val mapMarkerList = uiState.value.mapMarkerList

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetPeekHeight = BOTTOM_SHEET_HEIGHT_MINIMUM_DP,
        sheetShape = RoundedCornerShape(
            topStart = BOTTOM_SHEET_RADIUS_DP,
            topEnd = BOTTOM_SHEET_RADIUS_DP,
        ),
        sheetContent = {
            // Mapmo List
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.5f)
                    .fillMaxWidth()
            ) {
                MapmoListView(
                    modifier = Modifier
                        .fillMaxSize(),
                    dataList = dataList,
                    onClickMapmo = navigateToMapmo,
                    onScrollMapmoList = viewModel::moveMapCameraToMapmo,
                )
            }
        },
    ) {
        // Map View for Mapmo
        MapmoMapView(
            modifier = Modifier,
            mapCameraFocus = mapCameraFocus,
            mapMarkerList = mapMarkerList
        )
    }

    // TODO: Remove Debug UI
    DebugUI(
        navigateToMapmo = navigateToMapmo,
        navigateToSetting = navigateToSetting,
    )
}

/**
 * Mapmo List
 */
@Composable
private fun MapmoListView(
    modifier: Modifier = Modifier,
    dataList: List<HomeListUiItem>?,
    onClickMapmo: (mapmoID: String?) -> Unit,
    onScrollMapmoList: (mapmoID: String) -> Unit,
) {
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
                    // Mapmo Item
                    is HomeListUiItem.MapmoUiItem -> {
                        // Callback with target mapmo
                        val mapmoID = targetItem.mapmoID
                        onScrollMapmoList(mapmoID)
                    }

                    else -> {}
                }
            }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
    ) {
        // Add each items to UI
        // - Items might be Label or Mapmo
        items(dataList.size) { idx ->
            when(val targetItem = dataList[idx]) {
                // Label Item
                is HomeListUiItem.LabelUiItem -> {
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
                    val mapmoID = targetItem.mapmoID
                    val mapmoTitle = targetItem.mapmoTitle
                    val mapmoUpdatedAt = targetItem.mapmoUpdatedAt

                    MapmoItem(
                        modifier = Modifier,
                        mapmoTitle = mapmoTitle,
                        mapmoUpdatedAt = mapmoUpdatedAt,
                        onClick = { onClickMapmo(mapmoID) },
                    )
                }
            }
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
    onClick: () -> Unit,
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
}

/**
 * Map View for Mapmo
 */
@Composable
private fun MapmoMapView(
    modifier: Modifier = Modifier,
    mapCameraFocus: MapCameraFocusData? = null,
    mapMarkerList: List<MapMarkerData>? = null,
) {
    Box(
        modifier = modifier,
    ) {
        KakaoMapView(
            modifier = Modifier
                .fillMaxSize(),
            cameraFocus = mapCameraFocus,
            markers = mapMarkerList,
        )
    }
}

/**
 * TODO: Remove Debug UI
 */
@Composable
private fun DebugUI(
    modifier: Modifier = Modifier,
    navigateToMapmo: (mapmoID: String?) -> Unit,
    navigateToSetting: () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        Button(
            onClick = { navigateToMapmo(null) },
        ) {
            Text("[DEBUG] Open Mapmo Screen")
        }

        Button(
            onClick = navigateToSetting,
        ) {
            Text("[DEBUG] Open Setting Screen")
        }
    }
}