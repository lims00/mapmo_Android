package com.a6w.memo.data.retrofit.model

import com.a6w.memo.domain.model.Address

/**
 * Address Mapper
 * - Convert [KakaoLocalItem] to [Address] model
 */
object AddressMapper {
    fun KakaoLocalItem.toDomain(): Address {
        return Address(
            name = this.localName,
            fullAddress = this.localAddress,
            lat = this.localLatitude.toDoubleOrNull() ?: 0.0,
            lng = this.localLongitude.toDoubleOrNull() ?: 0.0,
        )
    }
}