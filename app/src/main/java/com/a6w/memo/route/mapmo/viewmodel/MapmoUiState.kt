package com.a6w.memo.route.mapmo.viewmodel

import com.a6w.memo.common.model.MapCameraFocusData
import com.a6w.memo.common.model.MapMarkerData
import com.a6w.memo.domain.model.Location

data class MapmoUiState(
    val content: String? = null,
    val updatedAt: Long = -1,
    val isNotifyEnabled: Boolean = true,
    val currentLabelID: String? = null,
    val labelName: String? = null,
    val labelColor: String? = null,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isLabelListLoading: Boolean = false,
    val isLabelSelectorOpen: Boolean = false,
    val errorMessage: String? = null,
    val mapCameraFocus: MapCameraFocusData? = null,
    val mapMarkerList: List<MapMarkerData>? = null,
)
