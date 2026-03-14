package com.a6w.memo.data.retrofit.api

import com.a6w.memo.data.retrofit.model.KakaoLocalResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Kakao Local API Interface
 * - Defines interface for Kakao Local API
 */
interface KakaoLocalAPI {
    // Get Local Search Result
    @GET("v2/local/search/keyword.json")
    suspend fun getLocalList(
        @Query("query") queryString: String
    ): KakaoLocalResponse
}