package com.a6w.memo.common.model

/**
 * Map Marker
 * - Represents a marker data for Map View
 * - It can be re-used for any maps
 * @param color Color of marker (ex - #FF0000)
 * @param latitude Latitude of marker
 * @param longitude Longitude of marker
 * @param markerTitle Title of marker
 * @param onClick (Optional) Callback function when marker is clicked
 */
data class MapMarkerData(
    val color: String,
    val latitude: Float,
    val longitude: Float,
    val markerTitle: String,
    val onClick: (() -> Unit)? = null,
)