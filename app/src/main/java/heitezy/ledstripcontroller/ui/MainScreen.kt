package heitezy.ledstripcontroller.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LightbulbCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import heitezy.ledstripcontroller.R
import androidx.compose.ui.unit.dp
import heitezy.ledstripcontroller.ble.ConnectionState
import heitezy.ledstripcontroller.ui.components.ColorPicker
import heitezy.ledstripcontroller.ui.components.PatternSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vm: MainViewModel,
    onRequestMediaProjection: () -> Unit = {},
) {
    val ui by vm.ui.collectAsState()
    var showColorSheet by remember { mutableStateOf(false) }
    val colorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Forward ViewModel's projection requests to the Activity
    LaunchedEffect(vm) {
        vm.projectionRequest.collect { onRequestMediaProjection() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    ConnectionIcon(ui.connectionState)
                    if (ui.connectionState == ConnectionState.CONNECTED) {
                        IconButton(onClick = { vm.togglePower() }) {
                            Icon(
                                if (ui.isPoweredOn) Icons.Default.Lightbulb else Icons.Default.LightbulbCircle,
                                contentDescription = stringResource(R.string.cd_toggle_power),
                                tint = if (ui.isPoweredOn) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ConnectionCard(ui, vm)

            if (ui.connectionState == ConnectionState.CONNECTED) {
                BrightnessCard(ui.brightness, vm::setBrightness)

                AnimatedVisibility(visible = !ui.isMusicSync && !ui.isScreenSync) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ColorPreviewCard(
                            hue = ui.hue,
                            saturation = ui.saturation,
                            colorValue = ui.colorValue,
                            onClick = { showColorSheet = true },
                        )
                        SectionCard {
                            PatternSelector(
                                selectedPattern = ui.selectedPattern,
                                patternSpeedMs = ui.patternSpeedMs,
                                onPatternSelected = vm::selectPattern,
                                onSpeedChanged = vm::setPatternSpeedMs,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                MusicSyncCard(ui, vm)
                ScreenSyncCard(ui, vm)
                AmbilightCard(ui.isAmbilightSmooth, vm::setAmbilightSmooth)
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Color picker bottom sheet ────────────────────────────────────────────

    if (showColorSheet) {
        val (r, g, b) = hsvToRgb(ui.hue, ui.saturation, ui.colorValue)
        ModalBottomSheet(
            onDismissRequest = { showColorSheet = false },
            sheetState = colorSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(R.string.msg_pick_color),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                // Live preview — updates in real time as the wheel is dragged
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(r / 255f, g / 255f, b / 255f))
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(12.dp),
                            ),
                    )
                    Text(
                        "#%02X%02X%02X".format(r, g, b),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                ColorPicker(
                    hue = ui.hue,
                    saturation = ui.saturation,
                    colorValue = ui.colorValue,
                    onHueSaturationChanged = vm::setHueSaturation,
                    onValueChanged = vm::setColorValue,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ── Connection Card ───────────────────────────────────────────────────────────

@Composable
private fun ConnectionCard(ui: UiState, vm: MainViewModel) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.label_bluetooth), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))

        when (ui.connectionState) {
            ConnectionState.DISCONNECTED, ConnectionState.ERROR -> {
                if (ui.connectionState == ConnectionState.ERROR) {
                    Text(
                        stringResource(R.string.msg_connection_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.msg_ready_to_illuminate),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Button(onClick = { vm.startScan() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_scan))
                }
            }

            ConnectionState.SCANNING -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.status_scanning), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(onClick = { vm.stopScan() }) { Text(stringResource(R.string.btn_stop)) }
                }
                Spacer(Modifier.height(8.dp))
                if (ui.scannedDevices.isEmpty()) {
                    Text(
                        stringResource(R.string.status_looking_ble),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    ui.scannedDevices.forEach { dev ->
                        DeviceRow(
                            name = dev.name,
                            address = dev.device.address,
                            rssi = dev.rssi,
                            onClick = { vm.connectTo(dev.device) },
                        )
                    }
                }
            }

            ConnectionState.CONNECTING -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.status_connecting), style = MaterialTheme.typography.bodyMedium)
                }
            }

            ConnectionState.CONNECTED -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Default.BluetoothConnected, contentDescription = null,
                        tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.status_connected), color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(onClick = { vm.disconnect() }) {
                        Icon(Icons.Default.BluetoothDisabled, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_disconnect))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(name: String, address: String, rssi: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
            Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("$rssi dBm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}

// ── Color preview card (opens bottom-sheet picker on tap) ────────────────────

@Composable
private fun ColorPreviewCard(
    hue: Float,
    saturation: Float,
    colorValue: Float,
    onClick: () -> Unit,
) {
    val (r, g, b) = hsvToRgb(hue, saturation, colorValue)
    val currentColor = Color(r / 255f, g / 255f, b / 255f)

    SectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.label_color),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            // Swatch bar — wide enough to read the hue clearly
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(currentColor)
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        RoundedCornerShape(8.dp),
                    ),
            )
            Text(
                "#%02X%02X%02X".format(r, g, b),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.cd_open_color_picker),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── Brightness Card ───────────────────────────────────────────────────────────

@Composable
private fun BrightnessCard(brightness: Int, onChanged: (Int) -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Power, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.label_brightness, brightness), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value = brightness.toFloat(),
            onValueChange = { onChanged(it.toInt()) },
            valueRange = 1f..100f,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Music Sync Card ───────────────────────────────────────────────────────────

@Composable
private fun MusicSyncCard(ui: UiState, vm: MainViewModel) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.label_music_sync), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = ui.isMusicSync, onCheckedChange = { vm.setMusicSync(it) })
        }

        AnimatedVisibility(visible = ui.isMusicSync) {
            Column {
                Spacer(Modifier.height(12.dp))

                // ── Audio source selector ─────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = ui.audioMode == AudioMode.MIC,
                        onClick = { vm.setAudioMode(AudioMode.MIC) },
                        label = { Text(stringResource(R.string.label_microphone)) },
                        leadingIcon = {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                    )
                    FilterChip(
                        selected = ui.audioMode == AudioMode.PLAYBACK,
                        onClick = {
                            if (ui.isPlaybackSupported) vm.setAudioMode(AudioMode.PLAYBACK)
                        },
                        enabled = ui.isPlaybackSupported,
                        label = { Text(stringResource(R.string.label_phone_audio)) },
                        leadingIcon = {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                    )
                }

                if (!ui.isPlaybackSupported) {
                    Text(
                        stringResource(R.string.msg_android_10_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (ui.audioMode == AudioMode.PLAYBACK) {
                    Text(
                        stringResource(R.string.desc_phone_audio),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        stringResource(R.string.desc_microphone),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Per-band color pickers ───────────────────────────────
                Text(
                    stringResource(R.string.desc_band_colors),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                BandColorRow(stringResource(R.string.label_bass),  ui.bassColor,  vm::setBassColor)
                Spacer(Modifier.height(6.dp))
                BandColorRow(stringResource(R.string.label_mids),  ui.midColor,   vm::setMidColor)
                Spacer(Modifier.height(6.dp))
                BandColorRow(stringResource(R.string.label_highs), ui.highColor,  vm::setHighColor)

                Spacer(Modifier.height(16.dp))

                // ── Frequency visualiser ──────────────────────────────────
                FreqBars(
                    bass = ui.freqData.bass,
                    mid  = ui.freqData.mid,
                    high = ui.freqData.high,
                    isBeat = ui.freqData.isBeat,
                    bassColor = ui.bassColor.toComposeColor(),
                    midColor  = ui.midColor.toComposeColor(),
                    highColor = ui.highColor.toComposeColor(),
                    bassActive = ui.bassColor != SyncColor.OFF,
                    midActive  = ui.midColor  != SyncColor.OFF,
                    highActive = ui.highColor != SyncColor.OFF,
                )
            }
        }
    }
}

// ── Screen Sync Card ──────────────────────────────────────────────────────────

@Composable
private fun ScreenSyncCard(ui: UiState, vm: MainViewModel) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ScreenShare, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.label_screen_sync), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(
                checked = ui.isScreenSync,
                onCheckedChange = { vm.setScreenSync(it) },
                enabled = ui.isPlaybackSupported,
            )
        }

        if (!ui.isPlaybackSupported) {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.msg_android_10_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(visible = ui.isScreenSync) {
            Column {
                Spacer(Modifier.height(12.dp))

                val previewColor = Color(
                    red   = ui.screenR / 255f,
                    green = ui.screenG / 255f,
                    blue  = ui.screenB / 255f,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(previewColor)
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp),
                            ),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            stringResource(R.string.label_dominant_color),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "#%02X%02X%02X".format(ui.screenR, ui.screenG, ui.screenB),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.desc_screen_sync),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun FreqBars(
    bass: Float, mid: Float, high: Float, isBeat: Boolean,
    bassColor: Color, midColor: Color, highColor: Color,
    bassActive: Boolean, midActive: Boolean, highActive: Boolean,
) {
    val animBass by animateFloatAsState(targetValue = bass, animationSpec = tween(80), label = "bass")
    val animMid  by animateFloatAsState(targetValue = mid,  animationSpec = tween(80), label = "mid")
    val animHigh by animateFloatAsState(targetValue = high, animationSpec = tween(80), label = "high")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        FreqBar(stringResource(R.string.label_bass),  animBass, bassColor, bassActive, Modifier.weight(1f))
        FreqBar(stringResource(R.string.label_mids),  animMid,  midColor,  midActive,  Modifier.weight(1f))
        FreqBar(stringResource(R.string.label_highs), animHigh, highColor, highActive, Modifier.weight(1f))
    }

    if (isBeat) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFBB33), CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.label_beat), style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFBB33))
        }
    }
}

@Composable
internal fun FreqBar(label: String, level: Float, barColor: Color, active: Boolean, modifier: Modifier = Modifier) {
    val barHeight = 120.dp
    val drawColor = if (active) barColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(barHeight), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight * level.coerceIn(0.02f, 1f))
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(Brush.verticalGradient(listOf(drawColor, drawColor.copy(alpha = 0.35f)))),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (active) label else "$label ✕",
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        )
    }
}

// ── Band color picker row ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BandColorRow(
    label: String,
    selected: SyncColor,
    onSelect: (SyncColor) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(42.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = stringResource(selected.labelRes),
                onValueChange = {},
                readOnly = true,
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(selected.toComposeColor())
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                    )
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(52.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                SyncColor.entries.forEach { color ->
                    DropdownMenuItem(
                        text = { Text(stringResource(color.labelRes)) },
                        onClick = {
                            onSelect(color)
                            expanded = false
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(color.toComposeColor())
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                            )
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ColorSwatch(color: SyncColor, selected: Boolean, onClick: () -> Unit) {
    val displayColor = color.toComposeColor()
    Box(
        modifier = Modifier
            .size(28.dp)
            .border(
                width = if (selected) 2.dp else 0.5.dp,
                color = if (selected) Color.White else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = CircleShape,
            )
            .padding(if (selected) 2.dp else 0.dp)
            .clip(CircleShape)
            .background(displayColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (color == SyncColor.OFF) {
            Text("✕", style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
    }
}

// ── SyncColor → Compose Color ─────────────────────────────────────────────────

internal fun SyncColor.toComposeColor(): Color = when (this) {
    SyncColor.OFF -> Color(0xFF2A2A2A)
    else          -> Color(red = r / 255f, green = g / 255f, blue = b / 255f)
}

// ── Ambilight smooth card ─────────────────────────────────────────────────────

@Composable
private fun AmbilightCard(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    SectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onToggle(!isEnabled) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isEnabled,
                onCheckedChange = onToggle,
            )
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.label_ambilight_smooth),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    stringResource(R.string.desc_ambilight_smooth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun ConnectionIcon(state: ConnectionState) {
    val icon = when (state) {
        ConnectionState.CONNECTED -> Icons.Default.BluetoothConnected
        ConnectionState.SCANNING, ConnectionState.CONNECTING -> Icons.AutoMirrored.Filled.BluetoothSearching
        else -> Icons.Default.BluetoothDisabled
    }
    val tint = when (state) {
        ConnectionState.CONNECTED -> Color(0xFF4CAF50)
        ConnectionState.SCANNING, ConnectionState.CONNECTING -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Icon(icon, contentDescription = state.name, tint = tint, modifier = Modifier.padding(end = 4.dp))
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}
