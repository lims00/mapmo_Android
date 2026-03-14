package com.a6w.memo.route.home.ui.model

import com.a6w.memo.domain.model.Location

/**
 * Mapmo List UI Item
 * - Used for Item of UI List
 * - Can be either [LabelUiItem] or [MapmoUiItem]
 */
sealed interface HomeListUiItem {
    // Item for label
    data class LabelUiItem(
        val labelColor: String,
        val labelID: String,
        val labelName: String,
    ): HomeListUiItem

    // Item for mapmo
    data class MapmoUiItem(
        val mapmoID: String,
        val mapmoLocation: Location,
        val mapmoTitle: String,
        val mapmoUpdatedAt: String,
    ): HomeListUiItem
}