package com.originisle.android.cards

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.util.Log
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.IconCache
import com.originisle.android.ui.PREFS_NAME

/**
 * Viễn trắc tai nghe Bluetooth chuẩn OriginOS 6:
 * - Tự động nhận diện tai nghe TWS khi kết nối hoặc mở nắp hộp sạc.
 * - Trích xuất viễn trắc mức pin đa thành phần: Tai Trái (L), Tai Phải (R), và Hộp sạc (Case),
 *   kèm trạng thái sạc (tia sét ⚡).
 * - Bóc tách pin thật từ thông báo Google Fast Pair / Vivo TWS hoặc AT Command HFP Vendor (+IPHONEACCEV).
 * - Hiển thị viễn trắc 2 dòng ngắn gọn không bị cắt chữ trên Đảo Origin.
 */
@SuppressLint("MissingPermission")
object EarbudsCard {

    const val ID = 35004

    private const val BATTERY_WAIT_MS = 2500L
    private const val SHOW_MS = 5000L
    private const val LOW_LEVEL = 15

    // Màu pin tiêu chuẩn OriginOS
    private const val GREEN = "#FF29E683"
    private const val GREEN_TRACK = "#4C29E683"
    private const val RED = "#FFE44C55"
    private const val RED_TRACK = "#4CE44C55"

    private const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
    private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"

    // AT Command HFP Vendor-Specific Constants
    private const val ACTION_VENDOR_SPECIFIC_HEADSET_EVENT =
        "android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT"
    private const val EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD =
        "android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_CMD"
    private const val EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE =
        "android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE"
    private const val EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS =
        "android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_ARGS"

    // Metadata keys chuẩn Android AOSP cho tai nghe TWS
    private const val METADATA_UNTHETHERED_LEFT_BATTERY = 10
    private const val METADATA_UNTHETHERED_RIGHT_BATTERY = 11
    private const val METADATA_UNTHETHERED_CASE_BATTERY = 12
    private const val METADATA_UNTHETHERED_LEFT_CHARGING = 13
    private const val METADATA_UNTHETHERED_RIGHT_CHARGING = 14
    private const val METADATA_UNTHETHERED_CASE_CHARGING = 15
    private const val METADATA_MAIN_BATTERY = 18

    private const val TAG = "EarbudsCard"

    private val handler = Handler(Looper.getMainLooper())
    private var app: Context? = null

    data class EarbudsTelemetry(
        val main: Int = -1,
        val left: Int = -1,
        val right: Int = -1,
        val case: Int = -1,
        val isLeftCharging: Boolean = false,
        val isRightCharging: Boolean = false,
        val isCaseCharging: Boolean = false,
    ) {
        val hasDetails: Boolean get() = (left in 0..100 || right in 0..100 || case in 0..100)
        val effectiveLevel: Int get() = when {
            left in 0..100 && right in 0..100 -> (left + right) / 2
            left in 0..100 -> left
            right in 0..100 -> right
            case in 0..100 -> case
            main in 0..100 -> main
            else -> -1
        }
    }

    private val deviceTelemetry = mutableMapOf<String, EarbudsTelemetry>()
    private val pending = mutableMapOf<String, Runnable>()
    private val warnedLow = mutableSetOf<String>()
    private var showingAddress: String? = null
    private var hideTask: Runnable? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runCatching { handle(intent) }.onFailure { Log.e(TAG, "earbuds card failed", it) }
        }
    }

    fun hasPermission(context: Context) =
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun enabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        return prefs.getBoolean("cast_notifications", true) && prefs.getBoolean("cast_earbuds", true) &&
            hasPermission(context)
    }

    fun start(context: Context) {
        val ctx = context.applicationContext
        if (app != null || !enabled(ctx)) return
        app = ctx
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(ACTION_BATTERY_LEVEL_CHANGED)
            // Lắng nghe thêm các broadcast mở rộng của Vivo / Google Fast Pair / AOSP
            addAction("android.bluetooth.headset.action.HF_INDICATORS_VALUE_CHANGED")
            addAction("vivo.intent.action.BLUETOOTH_BATTERY")
            addAction("com.vivo.upslide.BLUETOOTH_BATTERY")
            addAction("com.vivo.bluetooth.BATTERY_LEVEL_CHANGED")
            addAction("com.google.android.gms.nearby.fastpair.ACTION_BATTERY_UPDATE")
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            // HFP Vendor-Specific AT Command (+IPHONEACCEV)
            addAction(ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)
            addCategory("android.bluetooth.headset.intent.category.companyid.76")
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        runCatching { ctx.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED) }
    }

    fun stop() {
        val ctx = app ?: return
        runCatching { ctx.unregisterReceiver(receiver) }
        pending.values.forEach(handler::removeCallbacks)
        pending.clear()
        if (showingAddress != null) hide(ctx)
        app = null
    }

    fun onSettingsChanged(context: Context, listenerConnected: Boolean) {
        if (enabled(context.applicationContext) && listenerConnected) start(context) else stop()
    }

    private fun handle(intent: Intent) {
        val context = app ?: return
        var device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        if (device == null) {
            val address = intent.getStringExtra("address")
                ?: intent.getStringExtra("device_address")
                ?: intent.getStringExtra("mac")
                ?: intent.getStringExtra("BLUETOOTH_DEVICE_ADDRESS")
                ?: showingAddress
            if (!address.isNullOrBlank()) {
                device = runCatching {
                    val adapter = BluetoothAdapter.getDefaultAdapter()
                    adapter?.getRemoteDevice(address)
                }.getOrNull()
            }
        }
        if (device == null) return
        if (!isAudio(device)) return
        val address = device.address

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                warnedLow -= address
                pending.remove(address)?.let(handler::removeCallbacks)

                val task = Runnable {
                    pending.remove(address)
                    val telemetry = readTelemetry(device, intent)
                    deviceTelemetry[address] = telemetry
                    show(context, device, null, telemetry, low = false)
                }
                pending[address] = task
                handler.postDelayed(task, BATTERY_WAIT_MS)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                pending.remove(address)?.let(handler::removeCallbacks)
                deviceTelemetry -= address
                warnedLow -= address
                if (showingAddress == address) hide(context)
            }
            ACTION_VENDOR_SPECIFIC_HEADSET_EVENT -> {
                val cmd = intent.getStringExtra(EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD)
                if (cmd == "+IPHONEACCEV") {
                    val rawArgs = intent.getSerializableExtra(EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS) as? Array<*>
                        ?: (intent.getParcelableArrayExtra(EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS) as? Array<*>)
                    val parsed = parseIPhoneAccEv(rawArgs)
                    if (parsed != null) {
                        val current = deviceTelemetry[address] ?: EarbudsTelemetry()
                        val updated = current.copy(
                            main = parsed.first,
                            isLeftCharging = if (parsed.second) true else current.isLeftCharging,
                            isRightCharging = if (parsed.second) true else current.isRightCharging,
                        )
                        deviceTelemetry[address] = updated
                        show(context, device, null, updated, low = (updated.effectiveLevel in 0..LOW_LEVEL))
                    }
                }
            }
            else -> {
                // Pin thay đổi (BATTERY_LEVEL_CHANGED hoặc intent viễn trắc)
                val updated = readTelemetry(device, intent)
                val before = deviceTelemetry.put(address, updated)

                val eff = updated.effectiveLevel
                when {
                    pending.containsKey(address) -> {
                        pending.remove(address)?.let(handler::removeCallbacks)
                        show(context, device, null, updated, low = false)
                    }
                    showingAddress == address -> show(context, device, null, updated, low = eff in 0..LOW_LEVEL)
                    eff in 0..LOW_LEVEL && (before?.effectiveLevel ?: 100) > LOW_LEVEL && address !in warnedLow -> {
                        warnedLow += address
                        show(context, device, null, updated, low = true)
                    }
                }
            }
        }
    }

    private fun parseIPhoneAccEv(args: Array<*>?): Pair<Int, Boolean>? {
        if (args == null || args.size < 2) return null
        var battery = -1
        var charging = false
        var i = 0
        if (args.size % 2 != 0) {
            i = 1 // Phần tử đầu là số lượng chỉ số
        }
        while (i + 1 < args.size) {
            val key = (args[i] as? Number)?.toInt() ?: (args[i] as? String)?.toIntOrNull()
            val value = (args[i + 1] as? Number)?.toInt() ?: (args[i + 1] as? String)?.toIntOrNull()
            if (key != null && value != null) {
                when (key) {
                    1 -> {
                        // Key 1 = Battery level (0..9 -> 10%..100%)
                        battery = ((value + 1) * 10).coerceIn(0, 100)
                    }
                    2 -> {
                        // Key 2 = Dock/Charging (0 = false, 1 = true)
                        charging = (value == 1)
                    }
                }
            }
            i += 2
        }
        return if (battery in 0..100) Pair(battery, charging) else null
    }

    fun parseMetadataInt(bytes: ByteArray?): Int? {
        if (bytes == null || bytes.isEmpty()) return null
        val ascii = runCatching { String(bytes).trim().toIntOrNull() }.getOrNull()
        if (ascii != null && ascii in 0..100) return ascii
        val binary = bytes[0].toInt() and 0xFF
        return binary.takeIf { it in 0..100 }
    }

    fun parseMetadataBoolean(bytes: ByteArray?): Boolean {
        if (bytes == null || bytes.isEmpty()) return false
        val s = runCatching { String(bytes).trim().lowercase() }.getOrDefault("")
        if (s == "true" || s == "1") return true
        if (s == "false" || s == "0") return false
        return bytes[0].toInt() != 0
    }

    private fun extractBattery(bundle: android.os.Bundle?, vararg keys: String): Int {
        if (bundle == null) return -1
        for (k in keys) {
            val v = bundle.get(k) ?: continue
            val num = when (v) {
                is Number -> v.toInt()
                is String -> v.trim().toIntOrNull()
                is ByteArray -> parseMetadataInt(v)
                else -> null
            }
            if (num != null && num in 0..100) return num
        }
        return -1
    }

    private fun scanKeysForBattery(
        bundle: android.os.Bundle?,
        matchWords: List<String>,
        excludeWords: List<String> = emptyList(),
    ): Int {
        if (bundle == null) return -1
        val keys = runCatching { bundle.keySet() }.getOrNull() ?: return -1
        for (key in keys) {
            val lowerKey = key.lowercase()
            if (excludeWords.any { lowerKey.contains(it) }) continue
            if (matchWords.any { lowerKey.contains(it) }) {
                if (lowerKey.contains("charg") || lowerKey.contains("state") || lowerKey.contains("status")) {
                    if (!lowerKey.contains("battery") && !lowerKey.contains("level")) continue
                }
                val v = bundle.get(key)
                val num = when (v) {
                    is Number -> v.toInt()
                    is String -> v.trim().toIntOrNull()
                    is ByteArray -> parseMetadataInt(v)
                    else -> null
                }
                if (num != null && num in 0..100) return num
            }
        }
        return -1
    }

    private fun extractCharging(bundle: android.os.Bundle?, vararg keys: String): Boolean {
        if (bundle == null) return false
        for (k in keys) {
            val v = bundle.get(k) ?: continue
            when (v) {
                is Boolean -> if (v) return true
                is Number -> if (v.toInt() == 1) return true
                is String -> {
                    val s = v.trim().lowercase()
                    if (s == "true" || s == "1" || s == "charging") return true
                }
                is ByteArray -> if (parseMetadataBoolean(v)) return true
            }
        }
        return false
    }

    private fun scanKeysForCharging(
        bundle: android.os.Bundle?,
        matchWords: List<String>,
        excludeWords: List<String> = emptyList(),
    ): Boolean {
        if (bundle == null) return false
        val keys = runCatching { bundle.keySet() }.getOrNull() ?: return false
        for (key in keys) {
            val lowerKey = key.lowercase()
            if (excludeWords.any { lowerKey.contains(it) }) continue
            if (matchWords.any { lowerKey.contains(it) }) {
                if (lowerKey.contains("charg") || lowerKey.contains("charging")) {
                    val v = bundle.get(key)
                    when (v) {
                        is Boolean -> if (v) return true
                        is Number -> if (v.toInt() == 1) return true
                        is String -> {
                            val s = v.trim().lowercase()
                            if (s == "true" || s == "1" || s == "charging") return true
                        }
                        is ByteArray -> if (parseMetadataBoolean(v)) return true
                    }
                }
            }
        }
        return false
    }

    /** Trích xuất viễn trắc chuyên sâu (L/R/Case) từ Metadata AOSP hoặc Intent Extras */
    private fun readTelemetry(device: BluetoothDevice, intent: Intent?): EarbudsTelemetry {
        val extras = intent?.extras

        // 1. Mức pin chính (Main / fallback)
        var main = intent?.getIntExtra(EXTRA_BATTERY_LEVEL, -1) ?: -1
        if (main !in 0..100) {
            main = extractBattery(
                extras,
                "battery_level", "battery", "level", "BATTERY_LEVEL",
                "android.bluetooth.device.extra.BATTERY_LEVEL",
            )
        }
        if (main !in 0..100) main = batteryOf(device)

        // 2. Mức pin Tai Trái (Left)
        var left = extractBattery(
            extras,
            "android.bluetooth.device.extra.BATTERY_LEVEL_LEFT",
            "left_battery", "battery_left", "left_level", "batteryLevelLeft",
            "KEY_BATTERY_LEFT", "leftBattery", "vivo_battery_left", "left",
        )
        if (left !in 0..100) {
            left = scanKeysForBattery(
                extras,
                listOf("left"),
                listOf("charg", "state", "status", "right", "case", "box", "dock"),
            )
        }

        // 3. Mức pin Tai Phải (Right)
        var right = extractBattery(
            extras,
            "android.bluetooth.device.extra.BATTERY_LEVEL_RIGHT",
            "right_battery", "battery_right", "right_level", "batteryLevelRight",
            "KEY_BATTERY_RIGHT", "rightBattery", "vivo_battery_right", "right",
        )
        if (right !in 0..100) {
            right = scanKeysForBattery(
                extras,
                listOf("right"),
                listOf("charg", "state", "status", "left", "case", "box", "dock"),
            )
        }

        // 4. Mức pin Hộp sạc (Case / Box / Dock)
        var case = extractBattery(
            extras,
            "android.bluetooth.device.extra.BATTERY_LEVEL_CASE",
            "case_battery", "box_battery", "battery_case", "battery_box",
            "case_level", "box_level", "batteryLevelCase", "batteryLevelBox",
            "KEY_BATTERY_CASE", "KEY_BATTERY_BOX", "caseBattery", "boxBattery",
            "vivo_battery_case", "vivo_battery_box", "case", "box", "dock",
        )
        if (case !in 0..100) {
            case = scanKeysForBattery(
                extras,
                listOf("case", "box", "dock"),
                listOf("charg", "state", "status", "left", "right"),
            )
        }

        // 5. Trạng thái sạc (Charging)
        var leftCharging = extractCharging(
            extras,
            "android.bluetooth.device.extra.CHARGE_STATUS_LEFT",
            "left_charge", "charge_left", "left_charging", "is_left_charging",
            "charge_status_left", "vivo_charge_left",
        )
        if (!leftCharging) {
            leftCharging = scanKeysForCharging(
                extras,
                listOf("left"),
                listOf("right", "case", "box", "dock"),
            )
        }

        var rightCharging = extractCharging(
            extras,
            "android.bluetooth.device.extra.CHARGE_STATUS_RIGHT",
            "right_charge", "charge_right", "right_charging", "is_right_charging",
            "charge_status_right", "vivo_charge_right",
        )
        if (!rightCharging) {
            rightCharging = scanKeysForCharging(
                extras,
                listOf("right"),
                listOf("left", "case", "box", "dock"),
            )
        }

        var caseCharging = extractCharging(
            extras,
            "android.bluetooth.device.extra.CHARGE_STATUS_CASE",
            "case_charge", "box_charge", "charge_case", "charge_box",
            "case_charging", "box_charging", "is_case_charging",
            "charge_status_case", "charge_status_box", "vivo_charge_case",
        )
        if (!caseCharging) {
            caseCharging = scanKeysForCharging(
                extras,
                listOf("case", "box", "dock"),
                listOf("left", "right"),
            )
        }

        // 6. Đọc metadata qua reflection chuẩn Android AOSP nếu intent không mang đủ
        runCatching {
            val getMetaMethod = BluetoothDevice::class.java.getMethod("getMetadata", Int::class.javaPrimitiveType)
            fun getMetaBytes(key: Int): ByteArray? = getMetaMethod.invoke(device, key) as? ByteArray

            if (left !in 0..100) left = parseMetadataInt(getMetaBytes(METADATA_UNTHETHERED_LEFT_BATTERY)) ?: -1
            if (right !in 0..100) right = parseMetadataInt(getMetaBytes(METADATA_UNTHETHERED_RIGHT_BATTERY)) ?: -1
            if (case !in 0..100) case = parseMetadataInt(getMetaBytes(METADATA_UNTHETHERED_CASE_BATTERY)) ?: -1
            if (main !in 0..100) main = parseMetadataInt(getMetaBytes(METADATA_MAIN_BATTERY)) ?: -1

            if (!leftCharging) leftCharging = parseMetadataBoolean(getMetaBytes(METADATA_UNTHETHERED_LEFT_CHARGING))
            if (!rightCharging) rightCharging = parseMetadataBoolean(getMetaBytes(METADATA_UNTHETHERED_RIGHT_CHARGING))
            if (!caseCharging) caseCharging = parseMetadataBoolean(getMetaBytes(METADATA_UNTHETHERED_CASE_CHARGING))
        }.onFailure { Log.w(TAG, "getMetadata reflection failed", it) }

        return EarbudsTelemetry(
            main = main,
            left = left,
            right = right,
            case = case,
            isLeftCharging = leftCharging,
            isRightCharging = rightCharging,
            isCaseCharging = caseCharging,
        )
    }

    /**
     * Bóc tách thông số pin Trái, Phải, Hộp sạc thật từ thông báo hệ thống (Fast Pair, Vivo TWS, Bluetooth).
     */
    fun tryParseBatteryFromNotification(context: Context, sbn: StatusBarNotification): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        if (!prefs.getBoolean("cast_earbuds", true)) return false

        val extras = sbn.notification.extras ?: return false
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val fullText = "$title | $text | $subText | $bigText"

        val pkg = sbn.packageName
        val isTwsPkg = pkg in setOf(
            "com.google.android.gms",
            "com.vivo.tws",
            "com.vivo.earphone",
            "com.android.bluetooth",
            "com.vivo.audiofx",
            "com.google.android.apps.wearables.maestro.companion",
            "com.samsung.accessory.neobeanmgr",
            "com.samsung.accessory.atticmgr",
        )
        val hasKeyword = listOf(
            "earbuds", "tws", "airpods", "buds", "tai nghe", "hộp sạc", "charging case", "headset", "freebuds",
        ).any { fullText.contains(it, ignoreCase = true) }

        if (!isTwsPkg && !hasKeyword) return false
        if (!fullText.contains("%")) return false

        val leftRegex = Regex("""(?:L|Left|Trái|Tai trái)\s*[:：\-]?\s*(\d{1,3})\s*%""", RegexOption.IGNORE_CASE)
        val rightRegex = Regex("""(?:R|Right|Phải|Tai phải)\s*[:：\-]?\s*(\d{1,3})\s*%""", RegexOption.IGNORE_CASE)
        val caseRegex = Regex("""(?:Case|Box|C|Hộp|Hộp sạc|Dock)\s*[:：\-]?\s*(\d{1,3})\s*%""", RegexOption.IGNORE_CASE)
        val mainRegex = Regex("""(?:pin|battery|tai nghe)\s*[:：\-]?\s*(\d{1,3})\s*%""", RegexOption.IGNORE_CASE)

        val left = leftRegex.find(fullText)?.groupValues?.get(1)?.toIntOrNull() ?: -1
        val right = rightRegex.find(fullText)?.groupValues?.get(1)?.toIntOrNull() ?: -1
        val case = caseRegex.find(fullText)?.groupValues?.get(1)?.toIntOrNull() ?: -1
        var main = mainRegex.find(fullText)?.groupValues?.get(1)?.toIntOrNull() ?: -1

        if (left !in 0..100 && right !in 0..100 && case !in 0..100 && main !in 0..100) {
            val anyPct = Regex("""(\d{1,3})\s*%""").find(fullText)?.groupValues?.get(1)?.toIntOrNull()
            if (anyPct != null && anyPct in 0..100) {
                main = anyPct
            } else {
                return false
            }
        }

        val isCharging = fullText.contains("⚡") || fullText.contains("đang sạc", ignoreCase = true) || fullText.contains("charging", ignoreCase = true)
        val isLeftCharging = isCharging && (fullText.contains("L") || fullText.contains("Trái"))
        val isRightCharging = isCharging && (fullText.contains("R") || fullText.contains("Phải"))
        val isCaseCharging = isCharging && (fullText.contains("Case") || fullText.contains("Hộp") || fullText.contains("Box"))

        val telemetry = EarbudsTelemetry(
            main = main,
            left = left,
            right = right,
            case = case,
            isLeftCharging = isLeftCharging,
            isRightCharging = isRightCharging,
            isCaseCharging = isCaseCharging,
        )

        var devName = title.ifBlank { "Tai nghe TWS" }
            .replace(Regex("""\s*\(?\d{1,3}%\)?"""), "")
            .trim()
        if (devName.isBlank() || devName.equals("Bluetooth", ignoreCase = true)) {
            devName = "Tai nghe Bluetooth"
        }

        show(
            context = context,
            device = null,
            customName = devName,
            telemetry = telemetry,
            low = (telemetry.effectiveLevel in 0..LOW_LEVEL),
        )
        return true
    }

    /**
     * Quét và lấy viễn trắc thiết bị tai nghe thật đang kết nối ngay lập tức.
     */
    fun pollConnectedDeviceNow(context: Context): Boolean {
        if (!hasPermission(context)) return false
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return false
        val adapter = bluetoothManager.adapter ?: return false
        if (!adapter.isEnabled) return false

        // 1. Kiểm tra qua getConnectedDevices của BluetoothManager (HEADSET & A2DP)
        val connectedFromManager = runCatching {
            val list = mutableListOf<BluetoothDevice>()
            list.addAll(bluetoothManager.getConnectedDevices(BluetoothProfile.HEADSET))
            list.addAll(bluetoothManager.getConnectedDevices(BluetoothProfile.A2DP))
            list.distinctBy { it.address }
        }.getOrDefault(emptyList())

        // 2. Kiểm tra qua bondedDevices và reflection isConnected()
        val connectedDevices = if (connectedFromManager.isNotEmpty()) {
            connectedFromManager
        } else {
            runCatching {
                val bonded = adapter.bondedDevices ?: emptySet()
                val isConnectedMethod = BluetoothDevice::class.java.getMethod("isConnected")
                bonded.filter { dev ->
                    isAudio(dev) && (isConnectedMethod.invoke(dev) as? Boolean == true)
                }
            }.getOrDefault(emptyList())
        }

        val targetDevice = connectedDevices.firstOrNull { isAudio(it) } ?: connectedDevices.firstOrNull()
        if (targetDevice != null) {
            val address = targetDevice.address
            val cached = deviceTelemetry[address]
            val batteryLevel = batteryOf(targetDevice)
            val telemetry = if (cached != null && cached.effectiveLevel in 0..100) {
                cached
            } else {
                val read = readTelemetry(targetDevice, null)
                if (read.effectiveLevel in 0..100) read
                else if (batteryLevel in 0..100) read.copy(main = batteryLevel)
                else read
            }
            deviceTelemetry[address] = telemetry
            show(
                context = context,
                device = targetDevice,
                customName = null,
                telemetry = telemetry,
                low = (telemetry.effectiveLevel in 0..LOW_LEVEL),
            )
            return true
        }

        return false
    }

    private fun isAudio(device: BluetoothDevice): Boolean =
        runCatching { device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO }
            .getOrDefault(false)

    private fun isOverEar(device: BluetoothDevice, name: String): Boolean {
        val cls = runCatching { device.bluetoothClass?.deviceClass }.getOrNull()
        if (cls == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES) return true
        val n = name.lowercase()
        return OVER_EAR_NAMES.any { it in n }
    }

    private val OVER_EAR_NAMES = listOf("airpods max", "headphone", "casque", "wh-1000")

    private fun batteryOf(device: BluetoothDevice): Int = runCatching {
        BluetoothDevice::class.java.getMethod("getBatteryLevel").invoke(device) as Int
    }.getOrDefault(-1).takeIf { it in 0..100 } ?: -1

    private fun createSafeIcon(context: Context, glyph: Int): Icon {
        return runCatching {
            val drawable = context.getDrawable(glyph) ?: return Icon.createWithResource(context, R.mipmap.ic_launcher_round)
            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            Icon.createWithBitmap(bitmap)
        }.getOrElse {
            Icon.createWithResource(context, R.mipmap.ic_launcher_round)
        }
    }

    private fun show(
        context: Context,
        device: BluetoothDevice?,
        customName: String? = null,
        telemetry: EarbudsTelemetry,
        low: Boolean,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        if (!prefs.getBoolean("cast_earbuds", true)) return

        val name = customName ?: runCatching { device?.alias ?: device?.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Tai nghe"
        val effectivePct = telemetry.effectiveLevel
        val known = effectivePct in 0..100
        val isLow = known && effectivePct <= LOW_LEVEL
        val glyph = if (device != null && isOverEar(device, name)) R.drawable.ic_headphones else R.drawable.ic_earbuds
        val safeIcon = createSafeIcon(context, glyph)
        IconCache.activeSmallIcons.remove(ID)

        val click = PendingIntent.getActivity(
            context, ID, Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // Dòng 1: oi_title (Nếu có Hộp sạc thật thì hiện tên + Hộp sạc)
        val displayTitle = if (telemetry.case in 1..100) {
            "${name.take(16)} · Hộp: ${telemetry.case}%${if (telemetry.isCaseCharging) "⚡" else ""}"
        } else {
            name.take(24)
        }

        // Dòng 2: oi_text (Ngắn gọn 1 dòng không bị cắt chữ ...)
        val displayText = if (telemetry.left in 0..100 && telemetry.right in 0..100) {
            val lText = "${telemetry.left}%${if (telemetry.isLeftCharging) "⚡" else ""}"
            val rText = "${telemetry.right}%${if (telemetry.isRightCharging) "⚡" else ""}"
            "Trái (L): $lText   ·   Phải (R): $rText"
        } else if (telemetry.left in 0..100) {
            val lText = "${telemetry.left}%${if (telemetry.isLeftCharging) "⚡" else ""}"
            "Trái (L): $lText · Đang kết nối âm thanh"
        } else if (telemetry.right in 0..100) {
            val rText = "${telemetry.right}%${if (telemetry.isRightCharging) "⚡" else ""}"
            "Phải (R): $rText · Đang kết nối âm thanh"
        } else if (telemetry.main in 0..100) {
            "Pin tai nghe: ${telemetry.main}% · Đang kết nối âm thanh"
        } else {
            "Tai nghe Bluetooth · Đã kết nối"
        }

        // Nội dung thu gọn
        val leftContent = if (known) "${effectivePct}%" else "Tai nghe"
        val pillStatus = if (known) "${effectivePct}%" else "Đã kết nối"

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("is_ongoing", true)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
            putExtra("title", if (low) "Pin yếu · $displayTitle" else displayTitle)
            putExtra("text", displayText)
            putExtra("subtext", "Tai nghe Bluetooth · Đã kết nối")
            putExtra("oi_left_content", leftContent)
            putExtra("oi_left_icon", safeIcon)
            putExtra("oi_large_icon", safeIcon)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_right_content", "")
            putExtra("status_chip_text", pillStatus)

            if (known) {
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS) // 2
                putExtra("oi_progress", effectivePct)
                putExtra("oi_fg_color", if (isLow) RED else GREEN)
                putExtra("oi_bg_color", if (isLow) RED_TRACK else GREEN_TRACK)
            } else {
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
                putExtra("oi_show_right_icon", false)
            }

            if (low) putExtra("oi_light_color", RED)
            putExtra("click_resp", click)
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)
        }

        context.startService(intent)
        showingAddress = device?.address ?: "notification_tws"
        hideTask?.let(handler::removeCallbacks)
        hideTask = Runnable { hide(context) }.also { handler.postDelayed(it, SHOW_MS) }
    }

    private fun hide(context: Context) {
        hideTask?.let(handler::removeCallbacks)
        hideTask = null
        showingAddress = null
        IconCache.activeSmallIcons.remove(ID)
        context.startService(
            Intent(context, PlaygroundService::class.java)
                .setAction(PlaygroundService.ACTION_CANCEL)
                .putExtra("id", ID)
                .putExtra("oi_scene", "NAVIGATION"),
        )
    }
}
