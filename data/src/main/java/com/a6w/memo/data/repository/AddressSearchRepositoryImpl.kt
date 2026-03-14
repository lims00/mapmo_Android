package com.a6w.memo.data.repository

import com.a6w.memo.data.retrofit.model.AddressMapper.toDomain
import com.a6w.memo.data.retrofit.source.RetrofitAddressDataSource
import com.a6w.memo.domain.model.Address
import com.a6w.memo.domain.repository.AddressSearchRepository
import javax.inject.Inject

/**
 * AddressSearchRepository Implementation
 *
 * - Repository implementation for address searching with keyword
 * - It uses Kakao Local API for Address Searching
 */
class AddressSearchRepositoryImpl @Inject constructor(
    private val remoteDataSource: RetrofitAddressDataSource
) : AddressSearchRepository {
    // Fetch address search results with [keyword]
    override suspend fun getSearchResult(keyword: String): List<Address> {
        return try {
            // Call API with Data Source
            val response = remoteDataSource.fetchAddresses(keyword)
            // Map Kakao Local Result as Address
            response.documents.map { it.toDomain() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}