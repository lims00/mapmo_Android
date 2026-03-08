package com.a6w.memo.data.retrofit.source

import com.a6w.memo.data.retrofit.api.KakaoLocalAPI
import com.a6w.memo.data.retrofit.model.KakaoLocalResponse
import javax.inject.Inject

/**
 * Retrofit Address Data Source
 * - Implements Address Data Source with Retrofit
 */
class RetrofitAddressDataSource @Inject constructor(
    private val api: KakaoLocalAPI
) {
    suspend fun fetchAddresses(keyword: String): KakaoLocalResponse {
        return api.getLocalList(queryString = keyword)
    }
}