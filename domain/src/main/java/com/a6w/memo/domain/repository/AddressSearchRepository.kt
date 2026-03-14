package com.a6w.memo.domain.repository

import com.a6w.memo.domain.model.Address

/**
 * Address Search Repository
 */
interface AddressSearchRepository {
    // Fetch address search results with [keyword]
    suspend fun getSearchResult(keyword: String): List<Address>
}