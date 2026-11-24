package com.originisle.android.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

enum class BluetoothDevicePreset(val id: String, val label: String) {
    TWS_EARBUDS("tws", "Tai nghe TWS (Đen)"),
    TWS_WHITE("tws_white", "Tai nghe TWS (Trắng)"),
    LENOVO_LE208("lenovo_le208", "Lenovo LE208 (3D)"),
    REDMI_BUDS_6_PLAY("redmi_buds_6_play", "Redmi Buds 6 Play (3D)"),
    OVER_EAR("over_ear", "Tai nghe chụp tai");

    val isInteractive3D: Boolean
        get() = this == LENOVO_LE208 || this == REDMI_BUDS_6_PLAY

    companion object {
        fun fromId(id: String?): BluetoothDevicePreset {
            return entries.firstOrNull { it.id == id } ?: TWS_EARBUDS
        }

        fun inferFromName(name: String): BluetoothDevicePreset {
            val lower = name.trim().lowercase(Locale.ROOT)
            if (lower.isEmpty()) return TWS_EARBUDS

            if (isLenovoLe208Name(lower)) {
                return LENOVO_LE208
            }
            if (isRedmiBuds6PlayName(lower)) {
                return REDMI_BUDS_6_PLAY
            }
            if (OVER_EAR_KEYWORDS.any { lower.contains(it) }) {
                return OVER_EAR
            }
            if (TWS_WHITE_KEYWORDS.any { lower.contains(it) }) {
                return TWS_WHITE
            }
            return TWS_EARBUDS
        }

        private fun isLenovoLe208Name(lower: String): Boolean {
            return lower.contains("le208") || (
                lower.contains("lenovo") && (
                    lower.contains("earbud") ||
                        lower.contains("bud") ||
                        lower.contains("pod") ||
                        lower.contains("tws") ||
                        lower.contains("le")
                    )
                )
        }

        private fun isRedmiBuds6PlayName(lower: String): Boolean {
            return lower.contains("buds 6 play") ||
                lower.contains("buds6 play") ||
                lower.contains("redmi buds 6")
        }

        private val OVER_EAR_KEYWORDS = listOf(
            "headphone",
            "headset",
            "over-ear",
            "over ear",
            "wh-1000",
            "wh1000",
            "wh-ch",
            "airpods max",
            "quietcomfort",
            "qc35",
            "qc45",
            "beats studio",
            "beats solo",
            "momentum",
            "chụp tai",
            "chup tai",
            "arctis",
            "hyperx",
        )

        private val TWS_WHITE_KEYWORDS = listOf(
            "white",
            "trắng",
            "trang",
            "pearl",
        )
    }
}

enum class BluetoothPopupTheme(val id: String, val label: String) {
    LIGHT("light", "Sáng (Chuẩn mẫu)"),
    SYSTEM("system", "Theo hệ thống"),
    DARK("dark", "Tối");

    companion object {
        fun fromId(id: String?): BluetoothPopupTheme {
            return entries.firstOrNull { it.id == id } ?: LIGHT
        }
    }
}

data class BluetoothPopupDeviceConfig(
    val key: String,
    val enabled: Boolean = true,
    val preset: BluetoothDevicePreset = BluetoothDevicePreset.TWS_EARBUDS,
    val customImagePath: String? = null,
    val customName: String? = null,
)

object BluetoothPopupStore {

    private const val EXPERIMENTAL_PREFS = "experimental_prefs"
    private const val KEY_GLOBAL_ENABLED = "cast_bt_connect_popup"

    private const val POPUP_PREFS = "bt_popup_prefs"
    private const val KEY_POPUP_THEME = "popup_theme"
    private const val IMAGES_DIR_NAME = "bt_popup_images"
    private const val MAX_GIF_BYTES = 4 * 1024 * 1024
    private const val MAX_STATIC_DIM_PX = 512

    private val BLOCKED_WEARABLE_KEYWORDS = listOf(
        "watch",
        "band",
        "miband",
        "mi band",
        "fitbit",
        "garmin",
        "amazfit",
        "smartwatch",
        "galaxy fit",
        "đồng hồ",
        "vòng tay",
        "dong ho",
        "vong tay",
    )

    private val BLOCKED_SPEAKER_KEYWORDS = listOf(
        "speaker",
        "soundbar",
        "boombox",
        "soundlink",
        "megaboom",
        "wonderboom",
        "emberton",
        "stanmore",
        "kilburn",
        "stockwell",
        "acton",
        "woburn",
        "jbl flip",
        "jbl charge",
        "jbl go",
        "jbl clip",
        "jbl pulse",
        "jbl xtreme",
        "srs-",
        "homepod",
        "nest audio",
        "loa ",
        "loa bluetooth",
        "marshall",
    )

    private val BLOCKED_OTHER_KEYWORDS = listOf(
        "car",
        "carkit",
        "ô tô",
        "xe hơi",
        "tv",
        "tivi",
        "bravia",
        "laptop",
        "macbook",
        "desktop",
        "keyboard",
        "mouse",
        "bàn phím",
        "chuột",
    )

    fun isHeadphoneNameAllowed(name: String): Boolean {
        val lower = name.trim().lowercase(Locale.ROOT)
        if (lower.isEmpty()) return true
        if (BLOCKED_WEARABLE_KEYWORDS.any { lower.contains(it) }) return false
        if (BLOCKED_SPEAKER_KEYWORDS.any { lower.contains(it) }) return false
        if (BLOCKED_OTHER_KEYWORDS.any { lower.contains(it) }) return false
        return true
    }

    @SuppressLint("MissingPermission")
    fun isHeadphoneDevice(device: BluetoothDevice?, name: String): Boolean {
        if (!isHeadphoneNameAllowed(name)) return false
        if (device != null) {
            val btClass = runCatching { device.bluetoothClass }.getOrNull()
            if (btClass != null) {
                when (btClass.majorDeviceClass) {
                    BluetoothClass.Device.Major.WEARABLE,
                    BluetoothClass.Device.Major.COMPUTER,
                    BluetoothClass.Device.Major.PHONE,
                    BluetoothClass.Device.Major.PERIPHERAL,
                    BluetoothClass.Device.Major.IMAGING -> return false
                }
                when (btClass.deviceClass) {
                    BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER,
                    BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO,
                    BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO,
                    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER,
                    BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX -> return false
                }
            }
        }
        return true
    }

    fun normalizeKey(addressOrName: String): String {
        val normalized = addressOrName
            .trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        return normalized.ifEmpty { "unknown_device" }
    }

    fun isPopupGloballyEnabled(context: Context): Boolean {
        return context.getSharedPreferences(EXPERIMENTAL_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_GLOBAL_ENABLED, false)
    }

    fun getPopupTheme(context: Context): BluetoothPopupTheme {
        val id = context.getSharedPreferences(POPUP_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_POPUP_THEME, null)
        return BluetoothPopupTheme.fromId(id)
    }

    fun setPopupTheme(context: Context, theme: BluetoothPopupTheme) {
        context.getSharedPreferences(POPUP_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_POPUP_THEME, theme.id)
            .apply()
    }

    fun getDeviceConfig(
        context: Context,
        addressOrName: String,
        deviceName: String = "",
    ): BluetoothPopupDeviceConfig {
        val key = normalizeKey(addressOrName)
        val prefs = context.getSharedPreferences(POPUP_PREFS, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(enabledKey(key), true)
        val savedPresetId = prefs.getString(presetKey(key), null)
        val preset = if (savedPresetId != null) {
            BluetoothDevicePreset.fromId(savedPresetId)
        } else {
            BluetoothDevicePreset.inferFromName(deviceName)
        }
        val imagePath = prefs.getString(imagePathKey(key), null)
            ?.takeIf { File(it).exists() }
        val customName = prefs.getString(customNameKey(key), null)
        return BluetoothPopupDeviceConfig(
            key = key,
            enabled = enabled,
            preset = preset,
            customImagePath = imagePath,
            customName = customName,
        )
    }

    fun setDeviceEnabled(context: Context, addressOrName: String, enabled: Boolean) {
        val key = normalizeKey(addressOrName)
        context.getSharedPreferences(POPUP_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(enabledKey(key), enabled)
            .apply()
    }

    fun setDevicePreset(
        context: Context,
        addressOrName: String,
        preset: BluetoothDevicePreset,
    ) {
        val key = normalizeKey(addressOrName)
        context.getSharedPreferences(POPUP_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(presetKey(key), preset.id)
            .apply()
    }

    fun saveCustomImage(context: Context, addressOrName: String, uri: Uri): String? {
        val key = normalizeKey(addressOrName)
        val dir = File(context.filesDir, IMAGES_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
        val mimeType = context.contentResolver.getType(uri)

        return try {
            if (mimeType.equals("image/gif", ignoreCase = true)) {
                saveGifImage(context, dir, key, uri)
            } else {
                saveStaticImageAsPng(context, dir, key, uri)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun clearCustomImage(context: Context, addressOrName: String) {
        val key = normalizeKey(addressOrName)
        val dir = File(context.filesDir, IMAGES_DIR_NAME)
        File(dir, "$key.png").delete()
        File(dir, "$key.gif").delete()
        context.getSharedPreferences(POPUP_PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(imagePathKey(key))
            .apply()
    }

    private fun saveGifImage(
        context: Context,
        dir: File,
        key: String,
        uri: Uri,
    ): String? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(8192)
            val output = java.io.ByteArrayOutputStream()
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                total += read
                if (total > MAX_GIF_BYTES) {
                    return null
                }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: return null

        if (bytes.isEmpty()) return null

        val destFile = File(dir, "$key.gif")
        FileOutputStream(destFile).use { it.write(bytes) }
        File(dir, "$key.png").delete()

        persistImagePath(context, key, destFile.absolutePath)
        return destFile.absolutePath
    }

    private fun saveStaticImageAsPng(
        context: Context,
        dir: File,
        key: String,
        uri: Uri,
    ): String? {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val maxDim = max(info.size.width, info.size.height)
            if (maxDim > MAX_STATIC_DIM_PX) {
                val scale = MAX_STATIC_DIM_PX.toFloat() / maxDim.toFloat()
                val targetW = (info.size.width * scale).roundToInt().coerceAtLeast(1)
                val targetH = (info.size.height * scale).roundToInt().coerceAtLeast(1)
                decoder.setTargetSize(targetW, targetH)
            }
        }

        val destFile = File(dir, "$key.png")
        try {
            FileOutputStream(destFile).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    return null
                }
            }
        } finally {
            bitmap.recycle()
        }

        File(dir, "$key.gif").delete()
        persistImagePath(context, key, destFile.absolutePath)
        return destFile.absolutePath
    }

    private fun persistImagePath(context: Context, key: String, path: String) {
        context.getSharedPreferences(POPUP_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(imagePathKey(key), path)
            .apply()
    }

    private fun enabledKey(key: String) = "device_${key}_enabled"
    private fun presetKey(key: String) = "device_${key}_preset"
    private fun imagePathKey(key: String) = "device_${key}_image_path"
    private fun customNameKey(key: String) = "device_${key}_custom_name"
}
