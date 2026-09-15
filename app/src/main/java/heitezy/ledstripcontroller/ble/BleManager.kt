package heitezy.ledstripcontroller.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

enum class ConnectionState { DISCONNECTED, SCANNING, CONNECTING, CONNECTED, ERROR }

data class ScannedDevice(
    val device: BluetoothDevice,
    val name: String,
    val rssi: Int,
)

private sealed class GattEvent {
    data class StateChanged(val status: Int, val newState: Int) : GattEvent()
    data class ServicesDiscovered(val status: Int) : GattEvent()
}

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices.asStateFlow()

    private val _protocolVariant = MutableStateFlow(ProtocolVariant.ELK_BLEDOM)
    val protocolVariant: StateFlow<ProtocolVariant> = _protocolVariant.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val gattEvents = Channel<GattEvent>(Channel.UNLIMITED)

    private val bluetoothAdapter by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    // ── Scanning ─────────────────────────────────────────────────────────────

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name = result.device.name ?: return
            val entry = ScannedDevice(result.device, name, result.rssi)
            _scannedDevices.update { list ->
                val idx = list.indexOfFirst { it.device.address == result.device.address }
                if (idx >= 0) list.toMutableList().also { it[idx] = entry }
                else (list + entry).sortedWith(
                    compareByDescending<ScannedDevice> { "BLEDOM" in it.name.uppercase(Locale.ROOT) || "BJ_LED" in it.name.uppercase(Locale.ROOT) }
                        .thenByDescending { it.rssi }
                )
            }
        }
    }

    fun startScan() {
        _scannedDevices.value = emptyList()
        _connectionState.value = ConnectionState.SCANNING
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bluetoothAdapter.bluetoothLeScanner?.startScan(null, settings, scanCallback)
    }

    fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    // ── Connection ───────────────────────────────────────────────────────────

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            gattEvents.trySend(GattEvent.StateChanged(status, newState))
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            gattEvents.trySend(GattEvent.ServicesDiscovered(status))
        }
    }

    suspend fun connect(device: BluetoothDevice) {
        stopScan()
        _connectionState.value = ConnectionState.CONNECTING

        // Close any lingering GATT and drain stale events before a new connection attempt
        bluetoothGatt?.close()
        bluetoothGatt = null
        writeCharacteristic = null
        var drain = gattEvents.tryReceive()
        while (drain.isSuccess) drain = gattEvents.tryReceive()

        bluetoothGatt =
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)

        for (event in gattEvents) {
            when (event) {
                is GattEvent.StateChanged -> {
                    if (event.newState == BluetoothProfile.STATE_CONNECTED &&
                        event.status == BluetoothGatt.GATT_SUCCESS
                    ) {
                        kotlinx.coroutines.delay(600.milliseconds)
                        bluetoothGatt?.discoverServices()
                    } else if (event.newState == BluetoothProfile.STATE_DISCONNECTED) {
                        writeCharacteristic = null
                        _connectionState.value = if (event.status == BluetoothGatt.GATT_SUCCESS)
                            ConnectionState.DISCONNECTED else ConnectionState.ERROR
                        return
                    }
                }

                is GattEvent.ServicesDiscovered -> {
                    if (event.status == BluetoothGatt.GATT_SUCCESS) {
                        writeCharacteristic = resolveWriteCharacteristic()
                        _connectionState.value = if (writeCharacteristic != null)
                            ConnectionState.CONNECTED else ConnectionState.ERROR
                        // Do NOT return here — keep the loop alive so future
                        // STATE_DISCONNECTED callbacks are handled correctly.
                        if (writeCharacteristic == null) return
                    } else {
                        _connectionState.value = ConnectionState.ERROR
                        return
                    }
                }
            }
        }
    }

    private fun resolveWriteCharacteristic(): BluetoothGattCharacteristic? {
        val gatt = bluetoothGatt ?: return null

        // Try BJ_LED
        for (service in gatt.services) {
            val bjChar = service.getCharacteristic(ELKBledomProtocol.BJ_WRITE_CHAR_UUID)
            if (bjChar != null) {
                _protocolVariant.value = ProtocolVariant.BJ_LED
                return bjChar
            }
        }

        // Try ELK-BLEDOM Primary UUID pair
        _protocolVariant.value = ProtocolVariant.ELK_BLEDOM
        val service = gatt.getService(ELKBledomProtocol.SERVICE_UUID)
            ?: gatt.getService(ELKBledomProtocol.SERVICE_UUID_ALT)
        
        if (service != null) {
            return service.getCharacteristic(ELKBledomProtocol.WRITE_CHAR_UUID)
                ?: service.getCharacteristic(ELKBledomProtocol.WRITE_CHAR_UUID_ALT)
                ?: service.characteristics.firstOrNull { char ->
                    char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
                        || char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
                }
        }

        // Fallback: any write characteristic
        val isBj = "BJ_LED" in (gatt.device.name?.uppercase(Locale.ROOT) ?: "")
        if (isBj) {
            _protocolVariant.value = ProtocolVariant.BJ_LED
        }
        for (svc in gatt.services) {
            val char = svc.characteristics.firstOrNull { char ->
                char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
                    || char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
            }
            if (char != null) return char
        }
        
        return null
    }

    fun getDeviceByAddress(address: String): BluetoothDevice? = try {
        bluetoothAdapter.getRemoteDevice(address)
    } catch (_: Exception) { null }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        writeCharacteristic = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    // ── Command sending ───────────────────────────────────────────────────────

    fun sendCommand(data: ByteArray) {
        val gatt = bluetoothGatt ?: return
        val char = writeCharacteristic ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    char, data, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                )
            } else {
                @Suppress("DEPRECATION")
                char.value = data
                @Suppress("DEPRECATION")
                char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(char)
            }
        } catch (_: SecurityException) {
        }
    }
}
