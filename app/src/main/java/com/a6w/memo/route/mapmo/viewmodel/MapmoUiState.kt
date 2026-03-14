package com.a6w.memo.route.mapmo.viewmodel

import com.a6w.memo.common.model.MapCameraFocusData
import com.a6w.memo.common.model.MapMarkerData
import com.a6w.memo.domain.model.Label
import com.a6w.memo.domain.model.Mapmo

data class MapmoUiState(
    val mapmo: Mapmo? = null,
    val label: Label? = null,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isLabelListLoading: Boolean = false,
    val isLabelSelectorOpen: Boolean = false,
    val errorMessage: String? = null,
    val mapCameraFocus: MapCameraFocusData? = null,
    val mapMarkerList:  List<MapMarkerData>? = null,
)
