package com.a6w.memo.domain.repository

import java.util.UUID

/**
 * BLE Repository
 * - Manage BLE operations
 * - Send GATT Command to BLE Device
 */
interface BleRepository {
    suspend fun sendGattCommand(
        btAddress: String,
        serviceUuid: UUID,
        characteristicUuid: UUID,
        payload: ByteArray,
    ): Result<Unit>
}