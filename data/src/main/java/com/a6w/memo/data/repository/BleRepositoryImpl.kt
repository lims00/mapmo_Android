package com.a6w.memo.data.repository

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import com.a6w.memo.domain.repository.BleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * BLE Repository Implementation
 * - Manage BLE operations
 * - Send GATT Command to BLE Device
 */
class BleRepositoryImpl(
    private val context: Context,
): BleRepository {
    companion object {
        // Timeout when ble gatt sending (20s = 20,000ms)
        private const val BLUETOOTH_TIMEOUT_MILLIS = 20_000L

        // Exception Messages
        private const val EXCEPTION_BLUETOOTH_ADAPTER_NOT_AVAILABLE = "Bluetooth Adapter not available"
        private const val EXCEPTION_BLUETOOTH_MTU_NEGOTIATION_FAILED = "MTU Negotiation Failed"
        private const val EXCEPTION_BLUETOOTH_GATT_WRITE_FAILED = "GATT Write Failed"
        private const val EXCEPTION_CHARACTERISTIC_NOT_FOUND = "Target Characteristic not found"
        private const val EXCEPTION_BLUETOOTH_DISCONNECTED = "Disconnected"
    }

    // Android Bluetooth Manager / Adapter
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    // Mutex lock for Thread-safe
    private val bluetoothLock = Mutex()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun sendGattCommand(
        btAddress: String,
        serviceUuid: UUID,
        characteristicUuid: UUID,
        payload: ByteArray
    ): Result<Unit> = bluetoothLock.withLock {
        // Get device instance from bluetooth adapter
        // - If it is null, cannot use gatt service
        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(btAddress)
            ?: return@withLock Result.failure(Exception(EXCEPTION_BLUETOOTH_ADAPTER_NOT_AVAILABLE))

        // Run GATT Service with Mutex Lock, Timeout
        return@withLock withContext(Dispatchers.IO) {
            var gattService: BluetoothGatt? = null
            try {
                withTimeout(BLUETOOTH_TIMEOUT_MILLIS) {
                    suspendCancellableCoroutine { continuation ->
                        val callback = object: BluetoothGattCallback() {
                            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                                if(status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                                    // Request MTU Size based on payload size
                                    val requiredMtu = (payload.size + 3).coerceIn(23, 517)
                                    g.requestMtu(requiredMtu)
                                } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                                    g.close()
                                    if(continuation.isActive) continuation.resume(Result.failure(Exception("$EXCEPTION_BLUETOOTH_DISCONNECTED ($status)")))
                                }
                            }

                            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                            override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
                                // If MTU Request is success, go ahead. Discover for GATT Service.
                                if(status == BluetoothGatt.GATT_SUCCESS) {
                                    g.discoverServices()
                                } else {
                                    g.disconnect()
                                    continuation.resume(Result.failure(Exception(EXCEPTION_BLUETOOTH_MTU_NEGOTIATION_FAILED)))
                                }
                            }

                            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                                if(status == BluetoothGatt.GATT_SUCCESS) {
                                    // Get specified characteristic
                                    val char = g.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
                                    if(char != null) {
                                        // Write payload data to specified characteristic
                                        writeCharacteristic(g, char, payload)
                                    } else {
                                        g.disconnect()
                                        continuation.resume(Result.failure(Exception(EXCEPTION_CHARACTERISTIC_NOT_FOUND)))
                                    }
                                }
                            }

                            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                            override fun onCharacteristicWrite(g: BluetoothGatt, char: BluetoothGattCharacteristic, status: Int) {
                                // Disconnect after GATT Done.
                                g.disconnect()
                                if(status == BluetoothGatt.GATT_SUCCESS) {
                                    continuation.resume(Result.success(Unit))
                                } else {
                                    continuation.resume(Result.failure(Exception("$EXCEPTION_BLUETOOTH_GATT_WRITE_FAILED ($status)")))
                                }
                            }
                        }

                        // Connect to BLE GATT Service
                        gattService = bluetoothDevice.connectGatt(context, false, callback)

                        // Close GATT Service if the operation is canceled
                        continuation.invokeOnCancellation {
                            gattService?.disconnect()
                            gattService?.close()
                        }
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                // Close GATT Service
                gattService?.close()
            }
        }
    }

    /**
     * Method for writing payload data to BLE GATT Characteristic
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun writeCharacteristic(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, data: ByteArray) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(char, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            @Suppress("DEPRECATION")
            char.value = data
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(char)
        }
    }
}