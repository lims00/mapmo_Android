package com.a6w.memo.route.home.viewmodel

import com.a6w.memo.common.model.MapCameraFocusData
import com.a6w.memo.common.model.MapMarkerData
import com.a6w.memo.domain.model.MapmoList
import com.a6w.memo.route.home.ui.model.HomeListUiItem

/**
 * Home UI State
 * - Defines each ui states for Home Screen
 */
data class HomeUiState(
    // All mapmo / label list data
    val dataList: List<HomeListUiItem>? = null,
    // Map camera focus for selected mapmo
    val mapCameraFocus: MapCameraFocusData? = null,
    // Map markers for each mapmo
    val mapMarkerList: List<MapMarkerData>? = null,
)
