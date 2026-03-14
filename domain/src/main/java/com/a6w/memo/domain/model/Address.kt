package com.a6w.memo.domain.model

/**
 * Address Model
 * - An specific address info
 * - Includes lat, lng coordinates
 */
data class Address(
    // Name of address (ex - Chung-Ang University)
    val name: String,
    // Full address info (ex - 서울시 동작구 흑석로 84 중앙대학교)
    val fullAddress: String,
    // Latitude
    val lat: Double,
    // Longitude
    val lng: Double,
)
