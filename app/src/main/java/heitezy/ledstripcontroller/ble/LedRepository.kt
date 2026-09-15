package heitezy.ledstripcontroller.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.content.edit

/**
 * Singleton repository to share BLE state and power status between the Activity and Quick Tile.
 */
class LedRepository private constructor(context: Context) {
    val bleManager = BleManager(context)
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private val prefs = context.getSharedPreferences("ledstripcontroller_prefs", Context.MODE_PRIVATE)
    
    private val _isPoweredOn = MutableStateFlow(prefs.getBoolean("is_powered_on", true))
    val isPoweredOn: StateFlow<Boolean> = _isPoweredOn.asStateFlow()

    fun togglePower() {
        val newState = !_isPoweredOn.value
        setPower(newState)
    }

    fun connectTo(device: BluetoothDevice) {
        prefs.edit { putString("last_device_address", device.address) }
        scope.launch {
            bleManager.connect(device)
        }
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    fun setPower(on: Boolean) {
        _isPoweredOn.value = on
        prefs.edit { putBoolean("is_powered_on", on) }
        
        scope.launch {
            ensureConnected()
            val variant = bleManager.protocolVariant.value
            val cmd = if (on) ELKBledomProtocol.powerOn(variant) else ELKBledomProtocol.powerOff(variant)
            bleManager.sendCommand(cmd)
        }
    }

    fun reconnectIfNeeded() {
        scope.launch { ensureConnected() }
    }

    private suspend fun ensureConnected() {
        val state = bleManager.connectionState.value
        if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) return
        
        val lastAddress = prefs.getString("last_device_address", null) ?: return
        val device = bleManager.getDeviceByAddress(lastAddress) ?: return
        
        bleManager.connect(device)
    }

    companion object {
        @Volatile
        private var INSTANCE: LedRepository? = null

        fun getInstance(context: Context): LedRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LedRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
