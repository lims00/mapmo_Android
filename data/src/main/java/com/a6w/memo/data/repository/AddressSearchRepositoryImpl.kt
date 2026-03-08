package com.a6w.memo.data.repository

import com.a6w.memo.domain.model.Address
import com.a6w.memo.domain.repository.AddressSearchRepository

/**
 * AddressSearchRepository Implementation
 *
 * - Repository implementation for address searching with keyword
 * - It uses Kakao Local API for Address Searching
 */
class AddressSearchRepositoryImpl: AddressSearchRepository {
    // Fetch address search results with [keyword]
    override suspend fun getSearchResult(keyword: String): List<Address> {
        // TODO: Implement Kakao Local API Fetching
        return emptyList()
    }
}