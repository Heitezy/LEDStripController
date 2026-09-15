package heitezy.ledstripcontroller.ble

import java.util.UUID

import heitezy.ledstripcontroller.R

enum class ProtocolVariant { ELK_BLEDOM, BJ_LED }

object ELKBledomProtocol {

    // ELK-BLEDOM UUIDs
    val SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
    val WRITE_CHAR_UUID: UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
    val SERVICE_UUID_ALT: UUID = UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb")
    val WRITE_CHAR_UUID_ALT: UUID = UUID.fromString("0000ffe9-0000-1000-8000-00805f9b34fb")

    // BJ_LED UUIDs
    val BJ_WRITE_CHAR_UUID: UUID = UUID.fromString("0000ee01-0000-1000-8000-00805f9b34fb")

    fun powerOn(variant: ProtocolVariant = ProtocolVariant.ELK_BLEDOM): ByteArray {
        return if (variant == ProtocolVariant.BJ_LED) {
            byteArrayOf(0x69, 0x96.toByte(), 0x02, 0x01, 0x01)
        } else {
            byteArrayOf(0x7E, 0x04, 0x04, 0xF0.toByte(), 0x00, 0x01, 0xFF.toByte(), 0x00, 0xEF.toByte())
        }
    }

    fun powerOff(variant: ProtocolVariant = ProtocolVariant.ELK_BLEDOM): ByteArray {
        return if (variant == ProtocolVariant.BJ_LED) {
            byteArrayOf(0x69, 0x96.toByte(), 0x02, 0x01, 0x00)
        } else {
            byteArrayOf(0x7E, 0x04, 0x04, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00, 0xEF.toByte())
        }
    }

    fun setColor(r: Int, g: Int, b: Int, variant: ProtocolVariant = ProtocolVariant.ELK_BLEDOM): ByteArray {
        return if (variant == ProtocolVariant.BJ_LED) {
            byteArrayOf(0x69, 0x96.toByte(), 0x05, 0x02, r.toByte(), g.toByte(), b.toByte())
        } else {
            byteArrayOf(
                0x7E, 0x07, 0x05, 0x03,
                r.toByte(), g.toByte(), b.toByte(),
                0x10, 0xEF.toByte()
            )
        }
    }

    /** brightness: 0–100 */
    fun setBrightness(brightness: Int, variant: ProtocolVariant = ProtocolVariant.ELK_BLEDOM): ByteArray {
        val b = brightness.coerceIn(0, 100).toByte()
        return if (variant == ProtocolVariant.BJ_LED) {
            // BJ_LED handles brightness by scaling the RGB values. 
            // In the app architecture, we don't know the color here easily if we just send brightness.
            // Wait! The bj_led integration uses `set_rgb_color` with brightness.
            // For now, if we just want to set brightness independently without color on BJ_LED, is there a command?
            // Python implementation just scales the stored RGB color:
            // rgb_packet.append(red * brightness_percent); ...
            // We can return an empty byte array or a special case. 
            // Since `BJ_LED` doesn't have an independent brightness command, we will handle this in ViewModel.
            byteArrayOf() // Or we could just ignore
        } else {
            byteArrayOf(0x7E, 0x04, 0x01, b, 0x00, 0x00, 0x00, 0x00, 0xEF.toByte())
        }
    }
}

enum class LedPattern(val displayNameRes: Int) {
    SOLID        (R.string.pattern_solid),
    JUMP_RGB     (R.string.pattern_jump_rgb),
    JUMP_ALL     (R.string.pattern_jump_all),
    FADE_RGB     (R.string.pattern_fade_rgb),
    FADE_ALL     (R.string.pattern_fade_all),
    CROSSFADE_R  (R.string.pattern_crossfade_red),
    CROSSFADE_GB  (R.string.pattern_crossfade_green_blue),
    CROSSFADE_BO  (R.string.pattern_crossfade_blue_orange),
    CROSSFADE_B  (R.string.pattern_crossfade_blue),
    CROSSFADE_W  (R.string.pattern_crossfade_white),
    FLASH_RGB    (R.string.pattern_flash_rgb),
    FLASH_ALL    (R.string.pattern_flash_all),
    STROBE_W     (R.string.pattern_strobe_white),
}