package heitezy.ledstripcontroller

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import heitezy.ledstripcontroller.ble.LedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LedTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: LedRepository
    private var syncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = LedRepository.getInstance(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        syncJob?.cancel()
        // Sync tile state with repository
        syncJob = repository.isPoweredOn
            .onEach { updateTile(it) }
            .launchIn(serviceScope)
    }

    override fun onStopListening() {
        super.onStopListening()
        syncJob?.cancel()
        syncJob = null
    }

    override fun onClick() {
        super.onClick()
        repository.togglePower()
        updateTile(repository.isPoweredOn.value)
    }

    private fun updateTile(isPoweredOn: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isPoweredOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.led_tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_led_power)
        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
