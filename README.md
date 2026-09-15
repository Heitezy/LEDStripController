# LED Strip Controller

> A fully native Android app for controlling ELK-BLEDOM and BJ_LED Bluetooth LE LED strips — built with Kotlin and Jetpack Compose.

![Min SDK](https://img.shields.io/badge/Android-8.0%2B-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF)
![Compose BOM](https://img.shields.io/badge/Compose%20BOM-2024.12.01-4285F4)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## What it does

Connect your phone to an ELK-BLEDOM or BJ_LED LED strip over Bluetooth and control every aspect of the light — color, brightness, animated patterns, real-time music sync, and a live screen mirror (Ambilight) mode. Works on both phones and Android TV boxes.

The app remembers your last connected device, so it reconnects automatically when you return — even if Android killed it in the background.

---

## Features

### Color & Brightness
- **Color picker** — HSV wheel in a bottom sheet so it never interferes with page scrolling; includes a live colour preview bar and hex code readout
- **Preset swatches** — one-tap color presets
- **Brightness control** — independent 1–100 % slider sent directly to the device firmware

### Animated Patterns

All patterns are animated entirely on the phone — no unreliable firmware effect codes involved. Only the proven `setColor` command is used, so patterns work identically across all strip revisions.

| Pattern | Description |
|---|---|
| Solid | Static color (uses the color picker) |
| Jump RGB | Instantly hops between Red → Green → Blue |
| Jump All | Instantly hops through 7 rainbow colors |
| Fade RGB | Smooth crossfade between Red, Green, Blue |
| Fade All | Smooth crossfade through 7 rainbow colors |
| Crossfade Red | Pulses red in and out |
| Crossfade Green Blue | Crossfades between green and blue |
| Crossfade Blue | Pulses blue in and out |
| Crossfade White | Pulses white in and out |
| Flash RGB | Flashes Red / Green / Blue with black gaps |
| Flash All | Flashes all 7 rainbow colors with black gaps |
| Strobe White | Rapid white strobe |

**Speed** is set by typing a delay in milliseconds directly into the input field (10 – 5000 ms). Lower = faster. The value takes effect on the next animation frame — no restart needed.

### Music Sync

Real-time FFT analysis drives the LED color from audio:

- **Microphone mode** — listens to the room; works with any audio source, including headphones
- **Phone Audio mode** *(Android 10+)* — captures internal app playback directly without a microphone; a one-time system consent prompt is shown on first use
- **Per-band color** — assign any of 11 colors (or Off) independently to Bass, Mids, and Highs
- **Additive mixing** — all active bands blend simultaneously on the strip in real time
- **Beat detection** — energy-threshold algorithm highlights kick drums and transients

> **Note:** DRM-protected apps (Spotify, Netflix, Apple Music) block internal audio capture. Use Microphone mode for those.

### Screen Sync (Ambilight)
*(Android 10+ only)*

- Captures your screen via MediaProjection at 20 fps
- Calculates the dominant color across the frame
- Mirrors it to the LED strip with configurable smoothing

### Android TV

- Dedicated two-panel layout designed for D-pad navigation
- Sidebar navigation with focus-glow highlighting
- Brightness and color adjusted with step buttons (no sliders) — fully controllable with just a remote
- Full feature parity: color (H/S/V), brightness, patterns, music sync, screen sync, and settings
- Appears in the Android TV launcher with a custom banner

### Connection Resilience

- **Auto-reconnect on resume** — the last connected device's address is saved to disk; when you return to the app (even after Android killed the process), it reconnects directly without scanning
- **Explicit disconnect clears the saved address** — the app will not auto-reconnect after you manually disconnect

### Settings
- **Ambilight smooth** — exponential blending for gradual color transitions during Music Sync and Screen Sync (reduces jarring cuts)
- **Quick Settings Tile** — toggle power for the last-connected LED strip directly from the Android notification shade

---

## Screenshots

> *(Add screenshots here)*

---

## Requirements

| Requirement | Detail |
|---|---|
| Android version | 8.0 Oreo (API 26) or higher |
| Phone Audio / Screen Sync | Android 10 (API 29) or higher |
| Device | Physical Android phone or TV box — BLE does not work on emulators |
| LED hardware | ELK-BLEDOM, BJ_LED, or compatible BLE strip (BLEDOM, LEDBLE, MohuanLED, etc.) |

---

## Build instructions

No Android Studio required. You only need the JDK and the Android command-line tools.

### Step 1 — Install JDK 21

Download **Temurin JDK 21** from [adoptium.net](https://adoptium.net/temurin/releases/?version=21) and install it.

Verify:
```bash
java -version   # should print 21.x.x
```

### Step 2 — Install the Android SDK

Download **command-line tools only** from the [Android Studio download page](https://developer.android.com/studio#command-line-tools-only).

Extract to `C:\Android\cmdline-tools\latest\`, then accept licenses and install the required components:
```bash
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

Add these environment variables:
```
ANDROID_HOME = C:\Android
PATH        += C:\Android\platform-tools
PATH        += C:\Android\cmdline-tools\latest\bin
```

### Step 3 — Configure the project

Create a `local.properties` file in the project root:
```properties
sdk.dir=C\:\\Android
```

### Step 4 — Get the Gradle wrapper

If you don't have Gradle installed, download the wrapper JAR directly:
```
https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar
```
Place it at `gradle/wrapper/gradle-wrapper.jar`.

Or, if Gradle is available locally:
```bash
gradle wrapper --gradle-version=8.9
```

### Step 5 — Build

```bash
./gradlew assembleDebug
```

The APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Step 6 — Install

Enable **USB Debugging** on your device (*Settings → Developer options*), then:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

> **Out of memory during build?**
> Open `gradle.properties` and increase the heap: set `org.gradle.jvmargs=-Xmx4g` (use `-Xmx6g` on machines with 12 GB+ RAM).

---

## Usage

### Connecting

1. Power on your LED strip
2. Open the app and tap **Scan**
3. Tap your device in the list (usually named `ELK-BLEDOM` or `BJ_LED`)
4. The status indicator turns green when connected

The app saves your device. Next time you open it, it reconnects automatically — no need to scan again.

### Setting a color

- Tap the **Color** row to open the color picker sheet
- Drag the wheel to choose hue and saturation; drag the bar below for brightness
- Tap a preset swatch for an instant color

### Choosing a pattern

1. Open the **Pattern** dropdown and select an effect
2. Type a delay in the **Delay (ms)** field that appears — this controls the speed of the animation
3. Select **Solid** to stop the animation and return to static color

### Music Sync

1. Toggle **Music Sync** on
2. Select your audio source — **Microphone** or **Phone Audio**
3. Tap the color swatches in the **Band Colors** row to assign a color to each band:
   - **Bass** (60–250 Hz)
   - **Mids** (250–4000 Hz)
   - **Highs** (4000–16 000 Hz)
   - Tap **Off** to silence a band
4. Enable **Ambilight smooth** in Settings for gradual color blending

### Screen Sync

1. Toggle **Screen Sync** on
2. Accept the system screen-capture consent prompt
3. The dominant screen color is mirrored to the strip at up to 20 fps
4. Enable **Ambilight smooth** for a softer, more cinematic look

---

## How it works

### BLE protocol (ELK-BLEDOM)

All ELK-BLEDOM commands are 9-byte frames sent with `WRITE_TYPE_NO_RESPONSE` for minimum latency.

| Command | Frame |
|---|---|
| Power on | `7E 04 04 F0 00 01 FF 00 EF` |
| Power off | `7E 04 04 00 00 00 FF 00 EF` |
| Set color (R, G, B) | `7E 07 05 03 RR GG BB 10 EF` |
| Set brightness (0–100) | `7E 04 01 BR 00 00 00 00 EF` |

The app auto-detects which ELK-BLEDOM UUID pair the strip uses: primary (`FFF0` / `FFF3`) or alternate (`FFE5` / `FFE9`).

### BLE protocol (BJ_LED)

BJ_LED commands use a different header and structure, communicating over the `0000EE01` characteristic UUID.

| Command | Frame |
|---|---|
| Power on | `69 96 02 01 01` |
| Power off | `69 96 02 01 00` |
| Set color (R, G, B) | `69 96 05 02 RR GG BB` |

*Note: BJ_LED strips do not have a dedicated brightness command. The app handles brightness seamlessly by scaling the RGB values before transmitting them to the device.*

> The firmware's built-in effect commands (`7E 05 03 …` / `69 96 03 …`) were found to be unreliable across devices. All patterns in this app are animated in software using only the `setColor` command.

### Connection resilience

The last connected device's MAC address is written to `SharedPreferences` on every successful connect. On `onResume()`, the app calls `bluetoothAdapter.getRemoteDevice(address)` — which produces a `BluetoothDevice` for a known address without scanning — and re-establishes the GATT connection. This handles the common case where Android kills the app process in the background while the strip stays connected at the hardware level.

### Audio analysis

| Parameter | Value |
|---|---|
| Sample rate | 44 100 Hz |
| FFT size | 4096 points (Cooley-Tukey radix-2) |
| Window | Hann |
| Frequency resolution | ≈ 10.8 Hz / bin |
| Update rate | ≈ 10 Hz |
| Normalisation | Per-band rolling peak with 0.5 % / frame decay |
| Beat detection | Bass energy vs. 4-second rolling average; fires at > 1.4× |

### Color mixing

Each band's energy (0–1) scales its assigned color; all three are summed and clamped:

```
R = clamp(bass × bassR  +  mid × midR  +  high × highR,  0, 255)
G = clamp(bass × bassG  +  mid × midG  +  high × highG,  0, 255)
B = clamp(bass × bassB  +  mid × midB  +  high × highB,  0, 255)
```

### Pattern animation

Every non-Solid pattern runs as a coroutine loop in the ViewModel, sending `setColor` frames at the user-configured delay. The delay is read on each iteration, so changing the value in the text field takes effect immediately without restarting the animation.

### Screen capture

`ScreenAnalyzer` creates a `VirtualDisplay` at 160 px wide via MediaProjection and reads `RGBA_8888` frames through an `ImageReader` at a 20 FPS cap (sampling every 4th pixel). To avoid inaccurate colors and muddy results from simple RGB averaging:

1. **Linear-Light Conversion:** Encoded sRGB pixels are converted to linear light using a precomputed lookup table before any blending occurs, preventing the mid-gray shift and desaturation typical of raw byte averaging.
2. **Dominant-Hue Histogram:** Saturated pixels are grouped into 24 narrow hue bins (15° each). The histogram is circularly smoothed across neighbouring bins to prevent noise at bin edges.
3. **HSV Re-grading:** If a dominant hue satisfies the minimum color share threshold (2%), it is selected and re-graded. Its saturation is boosted by 1.25× (compensating for diffuse LED strips versus emissive screens), and its value is blended 50% with the overall scene brightness to ensure dark scenes stay dim.
4. **Neutral Fallback:** If no single hue dominates (e.g., text-heavy white UIs or black loading screens), it falls back gracefully to a neutral tone matched to the scene's actual overall linear-light brightness.

Exponential smoothing (α = 0.07 for smooth mode, 0.25 for snappy) is applied to the final colors to blend consecutive frames.

---

## Project structure

```
app/src/main/java/heitezy/ledstripcontroller/
├── MainActivity.kt                   # Permission flow, BT enable, TV detection, MediaProjection, auto-reconnect
├── MediaProjectionService.kt         # Foreground service (required before getMediaProjection())
├── MicCaptureService.kt              # Foreground service for microphone-mode Music Sync
├── LedTileService.kt                 # Quick Settings Tile service
├── ble/
│   ├── BleManager.kt                 # BLE scan, GATT connect, command sender, device lookup
│   └── ELKBledomProtocol.kt          # Byte-frame builders + LedPattern enum
├── audio/
│   └── AudioAnalyzer.kt              # AudioRecord → Hann → FFT → band energy → Flow
├── screen/
│   └── ScreenAnalyzer.kt             # MediaProjection → VirtualDisplay → dominant color → Flow
└── ui/
    ├── MainViewModel.kt              # UiState, pattern coroutines, sync logic, persistent reconnect
    ├── MainScreen.kt                 # Phone UI (scrollable, bottom sheet color picker)
    ├── TvScreen.kt                   # TV UI (two-panel D-pad layout, step-button controls)
    ├── components/
    │   ├── ColorPicker.kt            # HSV color wheel (Canvas) + sliders
    │   └── PatternSelector.kt        # Pattern dropdown + ms delay text input
    └── theme/
        └── Theme.kt                  # Dark Material 3 color scheme
```

---

## Permissions

| Permission | Purpose |
|---|---|
| `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` | BLE discovery and connection (Android 12+) |
| `ACCESS_FINE_LOCATION` | Required for BLE scanning on Android 11 and below |
| `RECORD_AUDIO` | Microphone input for Music Sync |
| `FOREGROUND_SERVICE` | Keeps the capture service alive in the background |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Required for MediaProjection foreground service (Android 14+) |
| `FOREGROUND_SERVICE_MICROPHONE` | Required for Microphone foreground service (Android 14+) |
| `POST_NOTIFICATIONS` | Allows showing the persistent sync notifications (Android 13+) |

---

## Known limitations

- **DRM content** (Spotify, Netflix, Apple Music) cannot be captured in Phone Audio mode — use Microphone mode instead
- **BLE throughput** is limited by the device firmware; pattern animations aim for ~20 fps but the actual update rate depends on the controller
- **TV boxes without a microphone** — Music Sync falls back gracefully; the toggle reverts if the audio device is unavailable
- **First build** downloads Gradle and compiles the Compose compiler — allow 5–10 minutes; subsequent builds are incremental

---

## Toolchain

| Tool | Version |
|---|---|
| Kotlin | 2.1.0 |
| Android Gradle Plugin | 8.7.3 |
| Gradle | 8.9 |
| Compose BOM | 2024.12.01 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |
| JDK | 21 |
