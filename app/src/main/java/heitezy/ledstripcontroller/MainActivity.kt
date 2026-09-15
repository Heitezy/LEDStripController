package heitezy.ledstripcontroller

import android.Manifest
import android.app.UiModeManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import heitezy.ledstripcontroller.ui.AudioMode
import heitezy.ledstripcontroller.ui.MainScreen
import heitezy.ledstripcontroller.ui.MainViewModel
import heitezy.ledstripcontroller.ui.TvScreen
import heitezy.ledstripcontroller.ui.theme.LedStripController
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val isTV: Boolean by lazy {
        (getSystemService(UI_MODE_SERVICE) as UiModeManager).currentModeType ==
            Configuration.UI_MODE_TYPE_TELEVISION
    }

    private var permissionsGranted by mutableStateOf(false)
    private var bluetoothEnabled by mutableStateOf(false)

    // ── MediaProjection ───────────────────────────────────────────────────────

    private val mediaProjectionManager by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            getSystemService(MediaProjectionManager::class.java)
        else null
    }

    // Holds the consent-dialog result until the foreground service is confirmed running
    private var pendingResult: ActivityResult? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            // Service has called startForeground() — safe to call getMediaProjection() now
            val result = pendingResult ?: return
            pendingResult = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val mp = if (result.resultCode == RESULT_OK && result.data != null) {
                    mediaProjectionManager?.getMediaProjection(result.resultCode, result.data!!)
                } else null
                viewModel.onMediaProjection(mp, cancelled = mp == null)
            }
            // Unbind — service keeps running because we used startForegroundService()
            try { unbindService(this) } catch (_: IllegalArgumentException) { }
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            pendingResult = result
            // Start the foreground service first; getMediaProjection() fires in onServiceConnected
            val reason = if (viewModel.ui.value.isScreenSync)
                MediaProjectionService.Reason.SCREEN else MediaProjectionService.Reason.PHONE_AUDIO
            val intent = MediaProjectionService.intent(this, reason)
            startForegroundService(intent)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        } else {
            // User dismissed the system dialog
            viewModel.onMediaProjection(null, cancelled = true)
        }
    }

    fun launchProjectionConsent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaProjectionManager?.createScreenCaptureIntent()?.let {
                projectionLauncher.launch(it)
            }
        }
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // POST_NOTIFICATIONS is cosmetic (it only controls whether the Music
        // Sync foreground-service notification is visible) — don't let a
        // denial of it block Bluetooth/mic functionality.
        permissionsGranted = results
            .filterKeys { it != Manifest.permission.POST_NOTIFICATIONS }
            .values.all { it }
        if (permissionsGranted) checkBluetooth()
    }

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        bluetoothEnabled = bluetoothAdapter()?.isEnabled == true
    }

    // ─────────────────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        viewModel.reconnectIfNeeded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the shared MediaProjectionService's notification accurate, and stop
        // it (releasing the MediaProjection) once neither feature needs it anymore.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui
                    .map { s ->
                        when {
                            s.isScreenSync -> MediaProjectionService.Reason.SCREEN
                            s.isMusicSync && s.audioMode == AudioMode.PLAYBACK -> MediaProjectionService.Reason.PHONE_AUDIO
                            else -> null
                        }
                    }
                    .distinctUntilChanged()
                    .collect { reason ->
                        if (reason != null) {
                            // (Re)send the reason so the notification text matches what's
                            // actually running — covers switching Phone-Audio Music Sync
                            // <-> Screen Sync while the projection is already granted,
                            // when the service itself is never restarted.
                            startForegroundService(MediaProjectionService.intent(this@MainActivity, reason))
                        } else {
                            stopService(Intent(this@MainActivity, MediaProjectionService::class.java))
                            viewModel.releaseProjection()
                        }
                    }
            }
        }

        setContent {
            LedStripController {
                Surface(color = MaterialTheme.colorScheme.background) {
                    when {
                        !permissionsGranted -> PermissionGate(onRequest = ::requestPermissions)
                        !bluetoothEnabled   -> BluetoothGate(onEnable = ::requestBluetooth)
                        else -> if (isTV) {
                            TvScreen(
                                vm = viewModel,
                                onRequestMediaProjection = ::launchProjectionConsent,
                            )
                        } else {
                            MainScreen(
                                vm = viewModel,
                                onRequestMediaProjection = ::launchProjectionConsent,
                            )
                        }
                    }
                }
            }
        }

        requestPermissions()
    }

    private fun requestPermissions() {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            add(Manifest.permission.RECORD_AUDIO)
            // Needed to show the "Music Sync" foreground-service notification;
            // without it the mic capture service still runs, it just won't be
            // visible in the status bar/notification shade.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permLauncher.launch(perms.toTypedArray())
    }

    private fun checkBluetooth() {
        bluetoothEnabled = bluetoothAdapter()?.isEnabled == true
        if (!bluetoothEnabled) requestBluetooth()
    }

    private fun requestBluetooth() {
        enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    private fun bluetoothAdapter(): BluetoothAdapter? =
        (getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
}

// ── Gating composables ────────────────────────────────────────────────────────

@androidx.compose.runtime.Composable
private fun PermissionGate(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.perm_rationale),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) { Text(stringResource(R.string.btn_grant_permissions)) }
    }
}

@androidx.compose.runtime.Composable
private fun BluetoothGate(onEnable: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.bt_disabled_msg),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onEnable) { Text(stringResource(R.string.btn_enable_bluetooth)) }
    }
}
