package heitezy.ledstripcontroller.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LightbulbCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import heitezy.ledstripcontroller.R
import heitezy.ledstripcontroller.ble.ConnectionState
import heitezy.ledstripcontroller.ble.LedPattern
import heitezy.ledstripcontroller.ble.ScannedDevice
import kotlinx.coroutines.delay
import kotlin.text.format
import kotlin.time.Duration.Companion.milliseconds

// ── Section enum ──────────────────────────────────────────────────────────────

private enum class TvSection(val labelRes: Int, val icon: ImageVector) {
    BLUETOOTH(R.string.label_bluetooth,   Icons.Default.Bluetooth),
    BRIGHTNESS(R.string.label_brightness_tv, Icons.Default.BrightnessHigh),
    COLOR(R.string.label_color,          Icons.Default.Palette),
    PATTERNS(R.string.label_patterns,     Icons.Default.AutoAwesome),
    MUSIC_SYNC(R.string.label_music_sync, Icons.Default.MusicNote),
    SCREEN_SYNC(R.string.label_screen_tv, Icons.AutoMirrored.Filled.ScreenShare),
    SETTINGS(R.string.label_settings,     Icons.Default.Settings),
}

// Helper to get string from labelRes
@Composable
private fun TvSection.label() = stringResource(labelRes)

// ── Entry point ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvScreen(vm: MainViewModel, onRequestMediaProjection: () -> Unit = {}) {
    val ui by vm.ui.collectAsState()
    var section by remember { mutableStateOf(TvSection.BLUETOOTH) }

    // One shared FocusRequester — passed to the first focusable element of each section.
    // When the section changes, wait 50 ms for recomposition to finish, then grab focus.
    val contentFocus = remember { FocusRequester() }
    LaunchedEffect(section) {
        delay(50.milliseconds)
        try { contentFocus.requestFocus() } catch (_: Exception) { }
    }

    LaunchedEffect(vm) {
        vm.projectionRequest.collect { onRequestMediaProjection() }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ── Sidebar ───────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 20.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color.Unspecified,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Divider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )

            TvSection.entries.forEach { s ->
                TvNavItem(s, selected = section == s, onClick = { section = s })
            }

            Spacer(Modifier.weight(1f))

            val connected = ui.connectionState == ConnectionState.CONNECTED
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Icon(
                    if (connected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    tint = if (connected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (connected) stringResource(R.string.status_connected) else stringResource(R.string.status_disconnected),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (connected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        )

        // ── Content ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 48.dp, vertical = 32.dp),
        ) {
            when (section) {
                TvSection.BLUETOOTH   -> TvBluetoothSection(ui, vm, contentFocus)
                TvSection.BRIGHTNESS  -> TvBrightnessSection(ui, vm, contentFocus)
                TvSection.COLOR       -> TvColorSection(ui, vm, contentFocus)
                TvSection.PATTERNS    -> TvPatternsSection(ui, vm, contentFocus)
                TvSection.MUSIC_SYNC  -> TvMusicSyncSection(ui, vm, contentFocus)
                TvSection.SCREEN_SYNC -> TvScreenSyncSection(ui, vm, contentFocus)
                TvSection.SETTINGS    -> TvSettingsSection(ui, vm)
            }
        }
    }
}

// ── Sidebar nav item ──────────────────────────────────────────────────────────

@Composable
private fun TvNavItem(section: TvSection, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .clickable(onClick = onClick)
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    focused  -> MaterialTheme.colorScheme.surfaceVariant
                    else     -> Color.Transparent
                },
                shape,
            )
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            section.icon,
            contentDescription = null,
            tint = if (selected || focused) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            section.label(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected || focused) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun NotConnectedHint() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.BluetoothDisabled, null,
                Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.msg_connect_first),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.msg_go_to_bt_section),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TvSectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
    Divider(
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
    )
}

// A large pill button used for +/− step controls.
// When focused: filled primary background + border. Press OK to activate.
@Composable
private fun TvStepButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .clickable(onClick = onClick)
            .background(
                if (focused) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                shape,
            )
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (focused) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── BLUETOOTH section ─────────────────────────────────────────────────────────

@Composable
private fun TvBluetoothSection(ui: UiState, vm: MainViewModel, firstFocus: FocusRequester) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TvSectionTitle(stringResource(R.string.label_bluetooth), Icons.Default.Bluetooth)

        when (ui.connectionState) {
            ConnectionState.DISCONNECTED, ConnectionState.ERROR -> {
                if (ui.connectionState == ConnectionState.ERROR) {
                    Text(stringResource(R.string.msg_connection_failed), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(4.dp))
                }
                Button(
                    onClick = { vm.startScan() },
                    modifier = Modifier
                        .height(52.dp)
                        .focusRequester(firstFocus),
                ) {
                    Icon(Icons.AutoMirrored.Filled.BluetoothSearching, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_scan), fontSize = 17.sp)
                }
            }

            ConnectionState.SCANNING -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                    Text(stringResource(R.string.status_scanning), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(4.dp))
                    FilledTonalButton(
                        onClick = { vm.stopScan() },
                        modifier = Modifier.focusRequester(firstFocus),
                    ) { Text(stringResource(R.string.btn_stop)) }
                }
                if (ui.scannedDevices.isEmpty()) {
                    Text(
                        stringResource(R.string.status_looking_ble),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ui.scannedDevices.forEachIndexed { idx, dev ->
                            TvDeviceCard(
                                dev = dev,
                                modifier = if (idx == 0) Modifier.focusRequester(firstFocus) else Modifier,
                                onClick = { vm.connectTo(dev.device) },
                            )
                        }
                    }
                }
            }

            ConnectionState.CONNECTING -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                    Text(stringResource(R.string.status_connecting), style = MaterialTheme.typography.bodyLarge)
                }
            }

            ConnectionState.CONNECTED -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        Icons.Default.BluetoothConnected, null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        stringResource(R.string.status_connected),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(
                        onClick = { vm.togglePower() },
                        modifier = Modifier
                            .height(48.dp)
                            .focusRequester(firstFocus),
                    ) {
                        Icon(
                            if (ui.isPoweredOn) Icons.Default.Lightbulb else Icons.Default.LightbulbCircle,
                            null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (ui.isPoweredOn) stringResource(R.string.btn_turn_off) else stringResource(R.string.btn_turn_on))
                    }
                    FilledTonalButton(onClick = { vm.disconnect() }, modifier = Modifier.height(48.dp)) {
                        Icon(Icons.Default.BluetoothDisabled, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_disconnect))
                    }
                }
            }
        }
    }
}

@Composable
private fun TvDeviceCard(dev: ScannedDevice, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .clickable(onClick = onClick)
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.BluetoothSearching, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(dev.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    dev.device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("${dev.rssi} dBm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── BRIGHTNESS section ────────────────────────────────────────────────────────
// Uses +/− step buttons (press OK to activate) — no sliders.

@Composable
private fun TvBrightnessSection(ui: UiState, vm: MainViewModel, firstFocus: FocusRequester) {
    if (ui.connectionState != ConnectionState.CONNECTED) { NotConnectedHint(); return }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        TvSectionTitle(stringResource(R.string.label_brightness_tv), Icons.Default.BrightnessHigh)

        Text(
            "${ui.brightness}%",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TvStepButton("−10", modifier = Modifier.focusRequester(firstFocus)) {
                vm.setBrightness((ui.brightness - 10).coerceAtLeast(1))
            }
            TvStepButton("−1") { vm.setBrightness((ui.brightness - 1).coerceAtLeast(1)) }
            TvStepButton("+1")  { vm.setBrightness((ui.brightness + 1).coerceAtMost(100)) }
            TvStepButton("+10") { vm.setBrightness((ui.brightness + 10).coerceAtMost(100)) }
        }

        Text(
            stringResource(R.string.desc_nav_buttons),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── COLOR section ────────────────────────────────────────────────────────────
// H/S/V each have −10 / −1 / +1 / +10 step buttons.

@Composable
private fun TvColorSection(ui: UiState, vm: MainViewModel, firstFocus: FocusRequester) {
    if (ui.connectionState != ConnectionState.CONNECTED) { NotConnectedHint(); return }
    val (r, g, b) = hsvToRgb(ui.hue, ui.saturation, ui.colorValue)

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TvSectionTitle(stringResource(R.string.label_color), Icons.Default.Palette)

        Row(horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(160.dp, 120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(r / 255f, g / 255f, b / 255f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TvColorStepRow(
                    label = stringResource(R.string.label_hue),
                    valueText = "${ui.hue.toInt()}°",
                    firstFocus = firstFocus,
                    onStep = { big ->
                        val step = if (big) 10f else 1f
                        vm.setHueSaturation((ui.hue + step + 360f) % 360f, ui.saturation)
                    },
                    onStepBack = { big ->
                        val step = if (big) 10f else 1f
                        vm.setHueSaturation((ui.hue - step + 360f) % 360f, ui.saturation)
                    },
                )
                TvColorStepRow(
                    label = stringResource(R.string.label_saturation),
                    valueText = "${(ui.saturation * 100).toInt()}%",
                    onStep = { big ->
                        vm.setHueSaturation(ui.hue, (ui.saturation + if (big) 0.1f else 0.01f).coerceAtMost(1f))
                    },
                    onStepBack = { big ->
                        vm.setHueSaturation(ui.hue, (ui.saturation - if (big) 0.1f else 0.01f).coerceAtLeast(0f))
                    },
                )
                TvColorStepRow(
                    label = stringResource(R.string.label_brightness_tv),
                    valueText = "${(ui.colorValue * 100).toInt()}%",
                    onStep = { big ->
                        vm.setColorValue((ui.colorValue + if (big) 0.1f else 0.01f).coerceAtMost(1f))
                    },
                    onStepBack = { big ->
                        vm.setColorValue((ui.colorValue - if (big) 0.1f else 0.01f).coerceAtLeast(0f))
                    },
                )
            }
        }

        Text(
            stringResource(R.string.desc_nav_rows),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TvColorStepRow(
    label: String,
    valueText: String,
    firstFocus: FocusRequester? = null,
    onStep: (big: Boolean) -> Unit,
    onStepBack: (big: Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(90.dp),
        )
        TvStepButton("−10", modifier = if (firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier) {
            onStepBack(true)
        }
        TvStepButton("−1") { onStepBack(false) }
        Text(
            valueText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp),
        )
        TvStepButton("+1")  { onStep(false) }
        TvStepButton("+10") { onStep(true) }
    }
}

// ── PATTERNS section ──────────────────────────────────────────────────────────

@Composable
private fun TvPatternsSection(ui: UiState, vm: MainViewModel, firstFocus: FocusRequester) {
    if (ui.connectionState != ConnectionState.CONNECTED) { NotConnectedHint(); return }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TvSectionTitle(stringResource(R.string.label_patterns), Icons.Default.AutoAwesome)
            LedPattern.entries.forEachIndexed { idx, pattern ->
                TvPatternItem(
                    pattern = pattern,
                    selected = pattern == ui.selectedPattern,
                    onClick = { vm.selectPattern(pattern) },
                    modifier = if (idx == 0) Modifier.focusRequester(firstFocus) else Modifier,
                )
            }
        }

        AnimatedVisibility(
            visible = ui.selectedPattern != LedPattern.SOLID,
            modifier = Modifier
                .width(220.dp)
                .align(Alignment.Top)
                .padding(top = 64.dp),
        ) {
            TvDelayInput(
                patternSpeedMs = ui.patternSpeedMs,
                onSpeedChanged = vm::setPatternSpeedMs,
            )
        }
    }
}

@Composable
private fun TvPatternItem(
    pattern: LedPattern,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .clickable(onClick = onClick)
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    focused  -> MaterialTheme.colorScheme.surfaceVariant
                    else     -> Color.Transparent
                },
                shape,
            )
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (selected) {
            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        } else {
            Spacer(Modifier.size(20.dp))
        }
        Text(
            stringResource(pattern.displayNameRes),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TvDelayInput(patternSpeedMs: Long, onSpeedChanged: (Long) -> Unit) {
    var text by remember(patternSpeedMs) { mutableStateOf(patternSpeedMs.toString()) }
    val focusManager = LocalFocusManager.current

    fun commit() {
        val ms = text.toLongOrNull()?.coerceIn(10L, 5000L)
        if (ms != null) { onSpeedChanged(ms); text = ms.toString() }
        else text = patternSpeedMs.toString()
        focusManager.clearFocus()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.label_animation_speed), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.filter { c -> c.isDigit() } },
            label = { Text(stringResource(R.string.label_delay_ms)) },
            supportingText = { Text(stringResource(R.string.label_delay_range)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── MUSIC SYNC section ────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TvMusicSyncSection(ui: UiState, vm: MainViewModel, firstFocus: FocusRequester) {
    if (ui.connectionState != ConnectionState.CONNECTED) { NotConnectedHint(); return }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TvSectionTitle(stringResource(R.string.label_music_sync), Icons.Default.MusicNote)

        TvToggleRow(stringResource(R.string.label_enabled), ui.isMusicSync, focusRequester = firstFocus) { vm.setMusicSync(it) }

        AnimatedVisibility(visible = ui.isMusicSync) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = ui.audioMode == AudioMode.MIC,
                        onClick = { vm.setAudioMode(AudioMode.MIC) },
                        label = { Text(stringResource(R.string.label_microphone), fontSize = 15.sp) },
                        leadingIcon = { Icon(Icons.Default.Mic, null, Modifier.size(18.dp)) },
                        modifier = Modifier.height(44.dp),
                    )
                    FilterChip(
                        selected = ui.audioMode == AudioMode.PLAYBACK,
                        onClick = { if (ui.isPlaybackSupported) vm.setAudioMode(AudioMode.PLAYBACK) },
                        enabled = ui.isPlaybackSupported,
                        label = { Text(stringResource(R.string.label_tv_audio), fontSize = 15.sp) },
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, null, Modifier.size(18.dp)) },
                        modifier = Modifier.height(44.dp),
                    )
                }

                Text(
                    stringResource(R.string.label_band_colors),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TvBandColorRow(stringResource(R.string.label_bass),  ui.bassColor,  vm::setBassColor)
                TvBandColorRow(stringResource(R.string.label_mids),  ui.midColor,   vm::setMidColor)
                TvBandColorRow(stringResource(R.string.label_highs), ui.highColor,  vm::setHighColor)

                FreqBars(
                    bass = ui.freqData.bass, mid = ui.freqData.mid, high = ui.freqData.high,
                    isBeat = ui.freqData.isBeat,
                    bassColor  = ui.bassColor.toComposeColor(),
                    midColor   = ui.midColor.toComposeColor(),
                    highColor  = ui.highColor.toComposeColor(),
                    bassActive = ui.bassColor != SyncColor.OFF,
                    midActive  = ui.midColor  != SyncColor.OFF,
                    highActive = ui.highColor != SyncColor.OFF,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvBandColorRow(label: String, selected: SyncColor, onSelect: (SyncColor) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SyncColor.entries.forEach { color ->
                TvColorChip(color = color, selected = color == selected, onClick = { onSelect(color) })
            }
        }
    }
}

@Composable
private fun TvColorChip(color: SyncColor, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .clickable(onClick = onClick)
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primary
                    focused  -> MaterialTheme.colorScheme.surfaceVariant
                    else     -> MaterialTheme.colorScheme.surface
                },
                shape,
            )
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color.toComposeColor())
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
        )
        Text(
            stringResource(color.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── SCREEN SYNC section ───────────────────────────────────────────────────────

@Composable
private fun TvScreenSyncSection(ui: UiState, vm: MainViewModel, firstFocus: FocusRequester) {
    if (ui.connectionState != ConnectionState.CONNECTED) { NotConnectedHint(); return }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TvSectionTitle(stringResource(R.string.label_screen_sync),
            Icons.AutoMirrored.Filled.ScreenShare
        )

        if (!ui.isPlaybackSupported) {
            Text(stringResource(R.string.msg_android_10_required), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        TvToggleRow(stringResource(R.string.label_enabled), ui.isScreenSync, enabled = ui.isPlaybackSupported, focusRequester = firstFocus) {
            vm.setScreenSync(it)
        }

        AnimatedVisibility(visible = ui.isScreenSync) {
            val previewColor = Color(ui.screenR / 255f, ui.screenG / 255f, ui.screenB / 255f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Box(
                    Modifier
                        .size(140.dp, 100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(previewColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                )
                Column {
                    Text(stringResource(R.string.label_dominant_color), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "#%02X%02X%02X".format(ui.screenR, ui.screenG, ui.screenB),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ── SETTINGS section ──────────────────────────────────────────────────────────

@Composable
private fun TvSettingsSection(ui: UiState, vm: MainViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TvSectionTitle(stringResource(R.string.label_settings), Icons.Default.Settings)
        TvCheckboxRow(
            label = stringResource(R.string.label_ambilight_smooth),
            description = stringResource(R.string.desc_ambilight_smooth),
            checked = ui.isAmbilightSmooth,
            onCheckedChange = { vm.setAmbilightSmooth(it) },
        )
    }
}

// ── Reusable TV control rows ──────────────────────────────────────────────────

@Composable
private fun TvToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .then(if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else Modifier)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(100.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun TvCheckboxRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .then(if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else Modifier)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}