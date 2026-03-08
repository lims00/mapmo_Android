package com.a6w.memo.data.retrofit.model

import com.google.gson.annotations.SerializedName

/**
 * KakaoLocalResponse
 * - Kakao Local API Response Model
 */
data class KakaoLocalResponse(
    @SerializedName("documents") val documents: List<KakaoLocalItem>
)

/**
 * KakaoLocalItem
 * - Kakao Local API Response Item Model
 */
data class KakaoLocalItem(
    @SerializedName("place_name") val localName: String,
    @SerializedName("road_address_name") val localAddress: String,
    @SerializedName("x") val localLongitude: String,
    @SerializedName("y") val localLatitude: String
)