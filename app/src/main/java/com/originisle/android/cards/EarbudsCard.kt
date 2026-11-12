package com.originisle.android.cards

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import java.util.concurrent.ConcurrentHashMap

/**
 * Viễn trắc tai nghe Bluetooth & Thiết bị đeo thông minh (Wearables / Smart Bands) chuẩn OriginOS 6:
 * - Tự động nhận diện tai nghe TWS, tai nghe chụp tai (Over-ear) và thiết bị đeo thông minh
 *   (Galaxy Fit 3, Galaxy Watch, Mi Band, Amazfit,...) khi kết nối.
 * - Trích xuất viễn trắc mức pin đa thành phần: Tai Trái (L), Tai Phải (R), Hộp sạc (Case),
 *   hoặc pin thiết bị đeo chính (Fit 3 / Main), kèm trạng thái sạc (tia sét ⚡).
 * - Bóc tách pin thật từ thông báo hệ thống, Broadcast Samsung (Fit3Plugin, WatchManager, Battery Manager),
 *   Google Fast Pair, Vivo TWS, hoặc AT Command HFP Vendor (+IPHONEACCEV).
 * - Máy trạng thái Kết nối (xanh #00D084, tiến độ selfie) & Mất kết nối (debounce 1500ms chống chập chờn BLE,
 *   thẻ đỏ #FF5247 với biểu tượng thất bại ✕).
 * - Thuật toán Co giãn Thích ứng (Adaptive Multi-Component Layout) cho thẻ mở rộng với TEMPLATE_BUTTONS.
 */
@SuppressLint("MissingPermission")
object EarbudsCard {

    const val ID_EARBUDS = 35004
    const val ID_WEARABLE = 35014
    const val ID = ID_EARBUDS

    fun cardIdFor(isWearable: Boolean): Int = if (isWearable) ID_WEARABLE else ID_EARBUDS

    private const val BATTERY_WAIT_MS = 2500L
    private const val SHOW_MS = 4_500L
    private const val DISCONNECT_DEBOUNCE_MS = 1200L
    private const val WEARABLE_DISCONNECT_DEBOUNCE_MS = 6000L
    private const val DISCONNECT_SHOW_MS = 3_000L
    private const val ANTI_FLAP_COOLDOWN_MS = 10_000L
    private const val LOW_LEVEL = 15

    private fun disconnectDebounceFor(isWearable: Boolean): Long =
        if (isWearable) WEARABLE_DISCONNECT_DEBOUNCE_MS else DISCONNECT_DEBOUNCE_MS

    // Màu sắc tiêu chuẩn OriginOS & Trạng thái
    private const val GREEN = "#00D084"
    private const val GREEN_TRACK = "#4C00D084"
    private const val RED = "#FF5247"
    private const val RED_TRACK = "#4CFF5247"

    private const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
    private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"

    // AT Command HFP Vendor-Specific Constants
    private const val ACTION_VENDOR_SPECIFIC_HEADSET_EVENT =
        "android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT"
    private const val EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD =
        "android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_CMD"
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

    // Samsung Broadcast Actions
    private const val ACTION_SAMSUNG_FIT_CONNECTED = "com.samsung.android.wearable.action.ACTION_WEARABLE_FIT_CONNECTED"
    private const val ACTION_SAMSUNG_FIT_DISCONNECTED = "com.samsung.android.wearable.action.ACTION_WEARABLE_FIT_DISCONNECTED"
    private const val ACTION_SAMSUNG_ACCESSORY_STATE_CHANGED = "com.samsung.accessory.device.action.ACCESSORY_CONNECTION_STATE_CHANGED"
    private const val ACTION_SAMSUNG_WEARABLE_CONNECTED = "com.samsung.android.wearable.action.WEARABLE_DEVICE_CONNECTED"
    private const val ACTION_SAMSUNG_WEARABLE_DISCONNECTED = "com.samsung.android.wearable.action.WEARABLE_DEVICE_DISCONNECTED"
    private const val ACTION_SAMSUNG_BATTERY_CHANGED = "com.samsung.battery.ACTION_BATTERY_INFO_CHANGED"
    private const val ACTION_SAMSUNG_BATTERY_ADDED = "com.samsung.battery.ACTION_BATTERY_INFO_ADDED"
    private const val ACTION_SAMSUNG_WATCH_LEVEL = "com.samsung.android.app.watchmanager.widget.getwearablebatterystatus.LEVEL"
    private const val ACTION_SAMSUNG_PLUGIN_RESPONSE = "com.samsung.android.watchmanager.ACTION_PLUGIN_RESPONSE"
    private const val ACTION_SAMSUNG_PLUGIN_REQUEST = "com.samsung.android.watchmanager.ACTION_PLUGIN_REQUEST"
    private const val ACTION_SAMSUNG_REQUEST_GEAR_STATE = "com.samsung.android.hostmanager.action.REQUEST_GEAR_CONNECTION_STATE"
    private const val FIT3_PLUGIN_PACKAGE = "com.samsung.wearable.fit3plugin"

    private const val TAG = "EarbudsCard"

    private fun resolveShowMs(context: Context): Long {
        val sec = context.getSharedPreferences(PREFS_NAME, 0).getInt("cast_island_duration_sec", 0)
        return if (sec > 0) (sec * 1000L).coerceIn(2_000L, 15_000L) else SHOW_MS
    }

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var app: Context? = null

    enum class DeviceCategory {
        EARBUDS,
        HEADPHONES,
        WEARABLE,
    }

    data class BatterySlot(
        val shortLabel: String,
        val fullLabel: String,
        val level: Int,
        val isCharging: Boolean = false,
    )

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
    private val disconnectDebounce = mutableMapOf<String, Runnable>()
    private val warnedLow = mutableSetOf<String>()
    private val wearableRegistry = mutableSetOf<String>()
    private val lastConnectionState = ConcurrentHashMap<String, Boolean>()
    private val lastConnectedTimestamp = ConcurrentHashMap<String, Long>()
    private var showingEarbudsAddress: String? = null
    private var showingWearableAddress: String? = null
    private var lastConnectedAudioDeviceName: String? = null
    private var lastConnectedAudioDeviceAddress: String? = null
    private var lastDisconnectCardShownMs: Long = 0L
    private var hideEarbudsTask: Runnable? = null
    private var hideWearableTask: Runnable? = null

    val showingAddress: String?
        get() = showingEarbudsAddress ?: showingWearableAddress

    fun getShowingAddress(isWearable: Boolean): String? =
        if (isWearable) showingWearableAddress else showingEarbudsAddress

    fun isShowing(isWearable: Boolean, nameOrAddress: String?): Boolean {
        if (nameOrAddress.isNullOrBlank()) return false
        val current = if (isWearable) showingWearableAddress else showingEarbudsAddress
        if (current == null) return false
        val target = nameOrAddress.trim().lowercase()
        val cur = current.trim().lowercase()
        return cur == target || cur.contains(target) || target.contains(cur)
    }

    /** Ghi nhận thời điểm kết nối gần nhất của thiết bị để phục vụ cơ chế chống dập tắt (Anti-Flap Cooldown). */
    fun markDeviceConnectedTime(nameOrAddress: String) {
        if (nameOrAddress.isBlank()) return
        val now = System.currentTimeMillis()
        val key = nameOrAddress.trim().lowercase()
        lastConnectedTimestamp[key] = now
        lastConnectedTimestamp[nameOrAddress] = now
    }

    /** Kiểm tra xem thiết bị có đang trong thời gian hạ nhiệt chống dập tắt (10 giây sau khi vừa kết nối) hay không. */
    fun isWithinAntiFlapCooldown(nameOrAddress: String?): Boolean {
        if (nameOrAddress.isNullOrBlank()) return false
        val key = nameOrAddress.trim().lowercase()
        val now = System.currentTimeMillis()
        val t1 = lastConnectedTimestamp[key] ?: 0L
        val t2 = lastConnectedTimestamp[nameOrAddress] ?: 0L
        val lastConn = maxOf(t1, t2)
        return (now - lastConn) in 0 until ANTI_FLAP_COOLDOWN_MS
    }

    /**
     * Kiểm tra trạng thái phần cứng Bluetooth thực tế qua BluetoothManager và BluetoothAdapter.
     * - Khi !isWearable (tai nghe âm thanh): CHỈ kiểm tra các profile âm thanh (HEADSET, A2DP, LE_AUDIO=22),
     *   tuyệt đối không kiểm tra GATT/GATT_SERVER hay device.isConnected() vì BLE GATT của Fast Pair giữ kết nối ngầm sau khi đóng nắp hộp.
     * - Khi isWearable == true (đồng hồ / vòng đeo): kiểm tra cả GATT, GATT_SERVER và device.isConnected().
     */
    fun isDeviceHardwareConnected(
        context: Context,
        nameOrAddress: String?,
        isWearable: Boolean = isWearable(null, nameOrAddress),
    ): Boolean {
        if (nameOrAddress.isNullOrBlank()) return false
        val target = nameOrAddress.trim().lowercase()
        return runCatching {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return@runCatching false
            val adapter = btManager.adapter ?: BluetoothAdapter.getDefaultAdapter() ?: return@runCatching false
            if (!adapter.isEnabled) return@runCatching false

            fun matchesDevice(dev: BluetoothDevice): Boolean {
                val addr = dev.address?.lowercase().orEmpty()
                if (addr.isNotBlank() && (addr == target || target.contains(addr))) return true
                val dName = runCatching { dev.name }.getOrNull()?.trim()?.lowercase().orEmpty()
                if (dName.isNotBlank() && (dName.contains(target) || target.contains(dName))) return true
                val dAlias = runCatching { dev.alias }.getOrNull()?.trim()?.lowercase().orEmpty()
                if (dAlias.isNotBlank() && (dAlias.contains(target) || target.contains(dAlias))) return true
                return false
            }

            val profiles = if (isWearable) {
                intArrayOf(
                    BluetoothProfile.GATT,
                    BluetoothProfile.GATT_SERVER,
                    BluetoothProfile.A2DP,
                    BluetoothProfile.HEADSET,
                )
            } else {
                intArrayOf(
                    BluetoothProfile.HEADSET,
                    BluetoothProfile.A2DP,
                    22, // BluetoothProfile.LE_AUDIO
                )
            }

            for (p in profiles) {
                val connectedDevices = runCatching { btManager.getConnectedDevices(p) }.getOrNull()
                if (connectedDevices != null && connectedDevices.any { matchesDevice(it) }) {
                    return@runCatching true
                }
            }

            val bonded = runCatching { adapter.bondedDevices }.getOrNull().orEmpty()
            val isConnectedMethod = if (isWearable) {
                runCatching { BluetoothDevice::class.java.getMethod("isConnected") }.getOrNull()
            } else {
                null
            }

            for (dev in bonded) {
                if (matchesDevice(dev)) {
                    for (p in profiles) {
                        val state = runCatching { btManager.getConnectionState(dev, p) }.getOrDefault(-1)
                        if (state == BluetoothProfile.STATE_CONNECTED) {
                            return@runCatching true
                        }
                    }
                    if (isWearable && isConnectedMethod != null) {
                        val connected = runCatching { isConnectedMethod.invoke(dev) as? Boolean }.getOrDefault(false)
                        if (connected == true) {
                            return@runCatching true
                        }
                    }
                }
            }

            false
        }.getOrDefault(false)
    }

    /**
     * Tìm kiếm BluetoothDevice từ danh sách bondedDevices theo tên, alias hoặc MAC address.
     * Quét qua BluetoothAdapter.bondedDevices tìm thiết bị có address, name, hoặc alias khớp với name
     * (chứa "fit 3", "fit3", "galaxy fit", hoặc name).
     */
    fun findBluetoothDevice(context: Context, name: String): BluetoothDevice? {
        if (!hasPermission(context)) return null
        return runCatching {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = btManager?.adapter ?: BluetoothAdapter.getDefaultAdapter() ?: return@runCatching null
            if (!adapter.isEnabled) return@runCatching null
            val bonded = adapter.bondedDevices ?: return@runCatching null
            val target = name.trim().lowercase()

            // 1. Quét theo MAC hoặc tên/alias khớp trực tiếp
            bonded.firstOrNull { dev ->
                val addr = dev.address?.lowercase().orEmpty()
                if (addr.isNotBlank() && (addr == target || target.contains(addr))) return@firstOrNull true
                val dName = runCatching { dev.name }.getOrNull()?.trim()?.lowercase().orEmpty()
                if (dName.isNotBlank() && (dName == target || dName.contains(target) || target.contains(dName))) return@firstOrNull true
                val dAlias = runCatching { dev.alias }.getOrNull()?.trim()?.lowercase().orEmpty()
                if (dAlias.isNotBlank() && (dAlias == target || dAlias.contains(target) || target.contains(dAlias))) return@firstOrNull true
                false
            } ?: runCatching {
                // 2. Tìm theo từ khóa Galaxy Fit 3 / Smart Band / Watch nếu target là thiết bị đeo
                val isWearableTarget = target.contains("fit") || target.contains("watch") || target.contains("band") || target.contains("samsung")
                val fitKeywords = listOf("fit 3", "fit3", "galaxy fit", "sm-r390", "galaxy watch", "fit")
                if (isWearableTarget) {
                    bonded.firstOrNull { dev ->
                        val dName = runCatching { dev.name }.getOrNull()?.trim()?.lowercase().orEmpty()
                        val dAlias = runCatching { dev.alias }.getOrNull()?.trim()?.lowercase().orEmpty()
                        fitKeywords.any { kw -> dName.contains(kw) || dAlias.contains(kw) }
                    }
                } else null
            }.getOrNull() ?: runCatching {
                // 3. Fallback: tìm thiết bị có tên chứa một phần target (nếu target >= 3 ký tự)
                if (target.length >= 3) {
                    bonded.firstOrNull { dev ->
                        val dName = runCatching { dev.name }.getOrNull()?.trim()?.lowercase().orEmpty()
                        val dAlias = runCatching { dev.alias }.getOrNull()?.trim()?.lowercase().orEmpty()
                        (dName.isNotBlank() && (dName.contains(target) || target.contains(dName))) ||
                            (dAlias.isNotBlank() && (dAlias.contains(target) || target.contains(dAlias)))
                    }
                } else null
            }.getOrNull()
        }.getOrNull()
    }

    /** Kiểm tra xem thiết bị theo tên hoặc MAC address có đang ở trạng thái kết nối thật hay không. */
    fun isConnected(nameOrAddress: String?): Boolean {
        if (nameOrAddress.isNullOrBlank()) return false
        val key = nameOrAddress.trim().lowercase()
        if (lastConnectionState[key] == true || lastConnectionState[nameOrAddress] == true) return true
        if (lastConnectionState.entries.any { (k, v) ->
            v && k.length >= 4 && (k.contains(key, ignoreCase = true) || key.contains(k, ignoreCase = true))
        }) return true
        val ctx = app
        if (ctx != null) {
            val prefs = ctx.getSharedPreferences(PREFS_BATTERY_CACHE, Context.MODE_PRIVATE)
            if (prefs.getBoolean("wearable_conn_state_$key", false)) return true
        }
        return false
    }

    /** Cập nhật trạng thái kết nối của thiết bị. */
    fun setConnectedState(nameOrAddress: String, connected: Boolean) {
        if (nameOrAddress.isBlank()) return
        val key = nameOrAddress.trim().lowercase()
        lastConnectionState[key] = connected
        lastConnectionState[nameOrAddress] = connected
        if (connected) {
            markDeviceConnectedTime(nameOrAddress)
        }
        app?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_BATTERY_CACHE, Context.MODE_PRIVATE)
            prefs.edit().putBoolean("wearable_conn_state_$key", connected).apply()
        }
    }

    private const val PREFS_BATTERY_CACHE = "wearable_battery_cache"
    private val BATTERY_SERVICE_UUID = java.util.UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    private val BATTERY_LEVEL_CHAR_UUID = java.util.UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    fun getCachedBattery(context: Context, name: String): Int {
        if (name.isBlank()) return -1
        val key = name.lowercase().trim()
        val prefs = context.getSharedPreferences(PREFS_BATTERY_CACHE, Context.MODE_PRIVATE)
        val direct = prefs.getInt(key, -1)
        if (direct in 0..100) return direct

        val cleanKey = key.replace(Regex("""\s*\([a-z0-9]+\)"""), "").trim()
        if (cleanKey.isNotBlank()) {
            val cleanVal = prefs.getInt(cleanKey, -1)
            if (cleanVal in 0..100) return cleanVal
        }

        if (cleanKey.isNotBlank()) {
            for ((k, v) in prefs.all) {
                if (k == "wearable_last_known_battery" || k.startsWith("wearable_conn_state_") || k.startsWith("telemetry_")) continue
                if (k.contains(cleanKey) || cleanKey.contains(k)) {
                    val lvl = (v as? Number)?.toInt() ?: (v as? String)?.toIntOrNull() ?: -1
                    if (lvl in 0..100) return lvl
                }
            }
        }

        if (key.contains("fit") || key.contains("watch") || key.contains("band")) {
            val globalWearable = prefs.getInt("wearable_last_known_battery", -1)
            if (globalWearable in 0..100) return globalWearable
        }

        return -1
    }

    fun saveCachedBattery(context: Context, name: String, level: Int) {
        if (name.isBlank() || level !in 0..100) return
        val key = name.lowercase().trim()
        val cleanKey = key.replace(Regex("""\s*\([a-z0-9]+\)"""), "").trim()
        val prefs = context.getSharedPreferences(PREFS_BATTERY_CACHE, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(key, level)
            if (cleanKey.isNotBlank()) putInt(cleanKey, level)
            // Lưu vào key toàn cầu cho thiết bị đeo
            if (key.contains("fit") || key.contains("watch") || key.contains("band")) {
                putInt("wearable_last_known_battery", level)
            }
        }.apply()
    }

    /**
     * Hợp nhất thông minh dữ liệu pin đa thành phần:
     * Tuyệt đối giữ nguyên left, right, case đã biết khi Bluetooth HFP chỉ báo 1 con số main!
     */
    fun mergeTelemetry(existing: EarbudsTelemetry?, incoming: EarbudsTelemetry): EarbudsTelemetry {
        if (existing == null) return incoming
        val left = if (incoming.left in 0..100) incoming.left else existing.left
        val right = if (incoming.right in 0..100) incoming.right else existing.right
        val case = if (incoming.case in 0..100) incoming.case else existing.case
        val main = if (incoming.main in 0..100) incoming.main else existing.main
        val isLeftCharging = if (incoming.left in 0..100 || incoming.hasDetails) incoming.isLeftCharging else existing.isLeftCharging
        val isRightCharging = if (incoming.right in 0..100 || incoming.hasDetails) incoming.isRightCharging else existing.isRightCharging
        val isCaseCharging = if (incoming.case in 0..100 || incoming.hasDetails) incoming.isCaseCharging else existing.isCaseCharging
        return EarbudsTelemetry(
            main = main,
            left = left,
            right = right,
            case = case,
            isLeftCharging = isLeftCharging,
            isRightCharging = isRightCharging,
            isCaseCharging = isCaseCharging,
        )
    }

    fun saveCachedTelemetry(context: Context, nameOrAddress: String, telemetry: EarbudsTelemetry) {
        if (nameOrAddress.isBlank()) return
        val key = nameOrAddress.lowercase().trim()
        if (key.isBlank()) return
        val existing = getCachedTelemetry(context, key)
        val merged = mergeTelemetry(existing, telemetry)
        val prefs = context.getSharedPreferences(PREFS_BATTERY_CACHE, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (merged.left in 0..100) putInt("telemetry_${key}_left", merged.left)
            if (merged.right in 0..100) putInt("telemetry_${key}_right", merged.right)
            if (merged.case in 0..100) putInt("telemetry_${key}_case", merged.case)
            if (merged.main in 0..100) putInt("telemetry_${key}_main", merged.main)
            putBoolean("telemetry_${key}_l_chg", merged.isLeftCharging)
            putBoolean("telemetry_${key}_r_chg", merged.isRightCharging)
            putBoolean("telemetry_${key}_c_chg", merged.isCaseCharging)
        }.apply()
        if (merged.effectiveLevel in 0..100) {
            saveCachedBattery(context, nameOrAddress, merged.effectiveLevel)
        }
    }

    fun getCachedTelemetry(context: Context, nameOrAddress: String): EarbudsTelemetry? {
        if (nameOrAddress.isBlank()) return null
        val key = nameOrAddress.lowercase().trim()
        if (key.isBlank()) return null
        val prefs = context.getSharedPreferences(PREFS_BATTERY_CACHE, Context.MODE_PRIVATE)
        val left = prefs.getInt("telemetry_${key}_left", -1)
        val right = prefs.getInt("telemetry_${key}_right", -1)
        val case = prefs.getInt("telemetry_${key}_case", -1)
        var main = prefs.getInt("telemetry_${key}_main", -1)
        val lChg = prefs.getBoolean("telemetry_${key}_l_chg", false)
        val rChg = prefs.getBoolean("telemetry_${key}_r_chg", false)
        val cChg = prefs.getBoolean("telemetry_${key}_c_chg", false)
        if (main !in 0..100) {
            main = getCachedBattery(context, nameOrAddress)
        }
        val t = EarbudsTelemetry(
            main = main,
            left = left,
            right = right,
            case = case,
            isLeftCharging = lChg,
            isRightCharging = rChg,
            isCaseCharging = cChg,
        )
        return if (t.effectiveLevel in 0..100 || t.hasDetails) t else null
    }

    private fun getStoredTelemetry(
        context: Context,
        address: String?,
        devName: String?,
        isWearable: Boolean = false,
    ): EarbudsTelemetry {
        var result = EarbudsTelemetry()
        val isWearableDevice = isWearable || isWearable(null, devName)
        val keys = listOfNotNull(
            address?.takeIf { it.isNotBlank() },
            address?.lowercase()?.trim()?.takeIf { it.isNotBlank() },
            devName?.takeIf { it.isNotBlank() },
            devName?.lowercase()?.trim()?.takeIf { it.isNotBlank() },
        ).distinct()
        for (k in keys) {
            deviceTelemetry[k]?.let { result = mergeTelemetry(result, it) }
        }
        if (!isWearableDevice && !result.hasDetails) {
            for ((_, v) in deviceTelemetry) {
                if (v.hasDetails && keys.any { k -> k != "device_unknown" && k != "tai nghe" }) {
                    // Nếu chỉ có 1 tai nghe TWS đang kết nối có chi tiết L/R/Case
                    result = mergeTelemetry(result, v)
                    break
                }
            }
        }
        for (k in keys) {
            getCachedTelemetry(context, k)?.let { result = mergeTelemetry(result, it) }
        }
        if (isWearableDevice) {
            result = result.copy(
                left = -1,
                right = -1,
                case = -1,
                isLeftCharging = false,
                isRightCharging = false,
                isCaseCharging = false,
            )
        }
        return result
    }

    private fun storeTelemetry(
        context: Context,
        address: String?,
        devName: String?,
        telemetry: EarbudsTelemetry,
        isWearable: Boolean = false,
    ): EarbudsTelemetry {
        val isWearableDevice = isWearable || isWearable(null, devName)
        val cleanIncoming = if (isWearableDevice) {
            telemetry.copy(
                left = -1,
                right = -1,
                case = -1,
                isLeftCharging = false,
                isRightCharging = false,
                isCaseCharging = false,
            )
        } else telemetry
        val existing = getStoredTelemetry(context, address, devName, isWearableDevice)
        val merged = mergeTelemetry(existing, cleanIncoming)
        val finalMerged = if (isWearableDevice) {
            merged.copy(
                left = -1,
                right = -1,
                case = -1,
                isLeftCharging = false,
                isRightCharging = false,
                isCaseCharging = false,
            )
        } else merged
        val keys = listOfNotNull(
            address?.takeIf { it.isNotBlank() && it != "device_unknown" },
            address?.lowercase()?.trim()?.takeIf { it.isNotBlank() && it != "device_unknown" },
            devName?.takeIf { it.isNotBlank() },
            devName?.lowercase()?.trim()?.takeIf { it.isNotBlank() },
        ).distinct()
        for (k in keys) {
            deviceTelemetry[k] = finalMerged
            saveCachedTelemetry(context, k, finalMerged)
        }
        return finalMerged
    }

    private val FIT3_BATTERY_URI by lazy { Uri.parse("content://com.samsung.wearable.providers.find.batteryproviderhelper/BatteryDBProvider/Battery_tb") }
    private val FIT3_SYNC_URI by lazy { Uri.parse("content://com.samsung.wearable.fit3plugin.BatteryInfoProvider") }
    private val FIT3_CAPSULE_URI by lazy { Uri.parse("content://com.samsung.wearable.fit3plugin.CapsuleProvider") }
    private val FIT3_QUICKSEARCH_URI by lazy { Uri.parse("content://com.samsung.wearable.fit3plugin.quicksearch") }
    private var fit3Observer: ContentObserver? = null

    /**
     * Kênh 1: Gửi broadcast yêu cầu đồng bộ pin phần cứng tới Samsung Fit 3 Plugin.
     */
    fun requestSamsungFit3BatteryUpdate(context: Context) {
        runCatching {
            val intent1 = Intent(ACTION_SAMSUNG_PLUGIN_REQUEST).apply {
                setPackage(FIT3_PLUGIN_PACKAGE)
                putExtra("request_type", "battery")
                putExtra("action", "get_battery")
                putExtra("action_type", "battery_status")
            }
            context.sendBroadcast(intent1)
        }.onFailure { Log.w(TAG, "send ACTION_PLUGIN_REQUEST failed", it) }

        runCatching {
            val intent2 = Intent(ACTION_SAMSUNG_REQUEST_GEAR_STATE).apply {
                setPackage(FIT3_PLUGIN_PACKAGE)
            }
            context.sendBroadcast(intent2)
        }.onFailure { Log.w(TAG, "send REQUEST_GEAR_CONNECTION_STATE failed", it) }
    }

    /**
     * Đa kênh đọc % pin phần cứng cho Samsung Galaxy Fit 3:
     * - Kênh 0: Đọc trực tiếp batteryOf(dev) từ BluetoothDevice nếu có trong bondedDevices
     * - Kênh 1: Kích hoạt sync qua Broadcast Widget
     * - Kênh 2: Truy vấn ContentProvider mở (CapsuleProvider & quicksearch)
     * - Kênh 3: Hỗ trợ tiếp cận (KeepAliveAccessibilityService)
     */
    fun querySamsungFit3Battery(context: Context, devName: String = "Galaxy Fit 3"): Int {
        // 0. Kênh 0: Tìm thiết bị và gọi batteryOf(dev) trước tiên
        val targetDev = findBluetoothDevice(context, devName)
        if (targetDev != null) {
            val btLevel = batteryOf(targetDev)
            if (btLevel in 0..100) {
                saveCachedBattery(context, devName, btLevel)
                targetDev.address?.let { saveCachedBattery(context, it, btLevel) }
                Log.i(TAG, "querySamsungFit3Battery (BluetoothDevice.getBatteryLevel): $devName = $btLevel%")
                return btLevel
            }
        }

        // 1. Kích hoạt sync ngầm qua Broadcast Widget (Kênh 1)
        requestSamsungFit3BatteryUpdate(context)

        // 2. Kích hoạt sync ngầm qua Fit 3 Plugin cũ (nếu có)
        runCatching {
            val syncBundle = Bundle().apply { putInt("sync_battery_data", 1) }
            context.contentResolver.call(FIT3_SYNC_URI, "sync_request", null, syncBundle)
        }

        // 3. Kênh 2: Thử gọi call() trên CapsuleProvider
        runCatching {
            val bundle = context.contentResolver.call(FIT3_CAPSULE_URI, "getBattery", null, null)
                ?: context.contentResolver.call(FIT3_CAPSULE_URI, "get_battery_level", null, null)
            if (bundle != null) {
                val b = extractBattery(bundle, "battery", "level", "battery_level", "value", "LEVEL")
                if (b in 0..100) {
                    saveCachedBattery(context, devName, b)
                    return b
                }
            }
        }

        // 4. Kênh 2: Truy vấn ContentProvider CapsuleProvider
        val fromCapsule = queryProviderForBattery(context, FIT3_CAPSULE_URI, devName)
        if (fromCapsule in 0..100) return fromCapsule

        // 5. Kênh 2: Truy vấn ContentProvider QuickSearch
        val fromQuickSearch = queryProviderForBattery(context, FIT3_QUICKSEARCH_URI, devName)
        if (fromQuickSearch in 0..100) return fromQuickSearch

        // 6. Truy vấn ContentProvider cũ FIT3_BATTERY_URI
        val fromLegacy = queryProviderForBattery(context, FIT3_BATTERY_URI, devName)
        if (fromLegacy in 0..100) return fromLegacy

        return -1
    }

    private fun queryProviderForBattery(context: Context, uri: Uri, devName: String): Int {
        return runCatching {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use { c ->
                val batteryColNames = listOf("Value", "value", "battery_level", "battery", "level", "batteryLevel", "percentage", "capacity")
                var matchedColIndex = -1
                for (name in batteryColNames) {
                    val idx = c.getColumnIndex(name)
                    if (idx != -1) {
                        matchedColIndex = idx
                        break
                    }
                }

                if (c.moveToFirst()) {
                    if (matchedColIndex != -1) {
                        val str = runCatching { c.getString(matchedColIndex) }.getOrNull()
                        val num = str?.trim()?.replace("%", "")?.toIntOrNull()
                            ?: runCatching { c.getInt(matchedColIndex) }.getOrNull()?.takeIf { it in 0..100 }
                        if (num != null && num in 0..100) {
                            saveCachedBattery(context, devName, num)
                            Log.i(TAG, "queryProviderForBattery ($uri): $devName = $num%")
                            return num
                        }
                    }
                    for (i in 0 until c.columnCount) {
                        val str = runCatching { c.getString(i) }.getOrNull()
                        val num = str?.trim()?.replace("%", "")?.toIntOrNull()
                        if (num != null && num in 0..100) {
                            saveCachedBattery(context, devName, num)
                            Log.i(TAG, "queryProviderForBattery ($uri, col $i): $devName = $num%")
                            return num
                        }
                    }
                }
                -1
            } ?: -1
        }.getOrElse { -1 }
    }

    fun readGattBatteryAsync(context: Context, device: BluetoothDevice?, devName: String, isWearable: Boolean) {
        if (device == null || !hasPermission(context)) return
        runCatching {
            var gattRef: BluetoothGatt? = null
            val gattCallback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        runCatching { gatt.discoverServices() }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        runCatching { gatt.close() }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val batteryService = gatt.getService(BATTERY_SERVICE_UUID)
                        val batteryChar = batteryService?.getCharacteristic(BATTERY_LEVEL_CHAR_UUID)
                        if (batteryChar != null) {
                            runCatching { gatt.readCharacteristic(batteryChar) }
                            return
                        }
                    }
                    runCatching { gatt.disconnect(); gatt.close() }
                }

                private fun processBatteryBytes(bytes: ByteArray?) {
                    val level = bytes?.firstOrNull()?.toInt()?.and(0xFF) ?: -1
                    if (level in 0..100) {
                        saveCachedBattery(context, devName, level)
                        device.address?.let { saveCachedBattery(context, it, level) }
                        handler.post {
                            onBatteryUpdated(context, devName, EarbudsTelemetry(main = level), isWearable = isWearable)
                        }
                    }
                }

                // Android 13+ (API 33+)
                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray,
                    status: Int
                ) {
                    if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == BATTERY_LEVEL_CHAR_UUID) {
                        processBatteryBytes(value)
                    }
                    runCatching { gatt.disconnect(); gatt.close() }
                }

                // Android 12- (API < 33)
                @Deprecated("Deprecated in Java")
                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int
                ) {
                    if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == BATTERY_LEVEL_CHAR_UUID) {
                        @Suppress("DEPRECATION")
                        processBatteryBytes(characteristic.value)
                    }
                    runCatching { gatt.disconnect(); gatt.close() }
                }
            }

            val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
            gattRef = gatt
            // Timeout an toàn đóng gatt sau 3500ms
            handler.postDelayed({
                runCatching { gattRef?.disconnect(); gattRef?.close() }
            }, 3500L)
        }.onFailure { Log.w(TAG, "readGattBatteryAsync failed", it) }
    }


    private val WEARABLE_KEYWORDS = listOf(
        "galaxy fit", "fit 3", "fit3", "sm-r390", "galaxy watch",
        "mi band", "smart band", "huawei band", "amazfit", "garmin",
        "vòng đeo tay", "đồng hồ",
    )

    private val WEARABLE_PACKAGES = listOf(
        "fit3plugin", "watchmanager", "accessory", "gearfit2plugin", "waterplugin",
        "geargplugin", "watch6plugin",
    )

    private val AUDIO_KEYWORDS = listOf(
        "earbuds", "tws", "airpods", "buds", "headphone", "headset",
        "freebuds", "wh-1000", "wf-1000", "tai nghe",
    )

    private val OVER_EAR_NAMES = listOf("airpods max", "headphone", "casque", "wh-1000")

    private val receiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                runCatching { handle(intent) }.onFailure { Log.e(TAG, "earbuds card failed", it) }
            }
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
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(ACTION_BATTERY_LEVEL_CHANGED)

            // Lắng nghe thêm các broadcast mở rộng của Vivo / Google Fast Pair / AOSP
            addAction("android.bluetooth.headset.action.HF_INDICATORS_VALUE_CHANGED")
            addAction("vivo.intent.action.BLUETOOTH_BATTERY")
            addAction("com.vivo.upslide.BLUETOOTH_BATTERY")
            addAction("com.vivo.bluetooth.BATTERY_LEVEL_CHANGED")
            addAction("com.google.android.gms.nearby.fastpair.ACTION_BATTERY_UPDATE")

            // HFP Vendor-Specific AT Command (+IPHONEACCEV)
            addAction(ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)
            addCategory("android.bluetooth.headset.intent.category.companyid.76")
            addCategory(Intent.CATEGORY_DEFAULT)

            // Samsung Wearable & Battery Broadcasts
            addAction(ACTION_SAMSUNG_FIT_CONNECTED)
            addAction(ACTION_SAMSUNG_FIT_DISCONNECTED)
            addAction(ACTION_SAMSUNG_ACCESSORY_STATE_CHANGED)
            addAction(ACTION_SAMSUNG_WEARABLE_CONNECTED)
            addAction(ACTION_SAMSUNG_WEARABLE_DISCONNECTED)
            addAction(ACTION_SAMSUNG_BATTERY_CHANGED)
            addAction(ACTION_SAMSUNG_BATTERY_ADDED)
            addAction(ACTION_SAMSUNG_WATCH_LEVEL)
            addAction(ACTION_SAMSUNG_PLUGIN_RESPONSE)
        }
        runCatching { ctx.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED) }

        // Đăng ký ContentObserver theo dõi cập nhật pin Samsung Galaxy Fit 3
        runCatching {
            if (fit3Observer == null) {
                fit3Observer = object : ContentObserver(handler) {
                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        super.onChange(selfChange, uri)
                        val level = querySamsungFit3Battery(ctx, "Galaxy Fit 3")
                        if (level in 0..100) {
                            onBatteryUpdated(ctx, "Galaxy Fit 3", EarbudsTelemetry(main = level), isWearable = true)
                        }
                    }
                }
                ctx.contentResolver.registerContentObserver(
                    FIT3_BATTERY_URI,
                    true,
                    fit3Observer!!
                )
            }
        }.onFailure { Log.w(TAG, "registerContentObserver failed", it) }
    }

    fun stop() {
        val ctx = app ?: return
        fit3Observer?.let { observer ->
            runCatching { ctx.contentResolver.unregisterContentObserver(observer) }
            fit3Observer = null
        }
        runCatching { ctx.unregisterReceiver(receiver) }
        pending.values.forEach(handler::removeCallbacks)
        pending.clear()
        disconnectDebounce.values.forEach(handler::removeCallbacks)
        disconnectDebounce.clear()
        lastConnectionState.clear()
        lastConnectedTimestamp.clear()
        hide(ctx, isWearable = null)
        app = null
    }

    fun onSettingsChanged(context: Context, listenerConnected: Boolean) {
        if (enabled(context.applicationContext) && listenerConnected) start(context) else stop()
    }

    /**
     * Nhận diện thiết bị đeo thông minh (Samsung Fit 3, Watch, Smart Band,...).
     */
    fun isWearable(device: BluetoothDevice?, name: String? = null, pkg: String? = null): Boolean {
        if (pkg != null && WEARABLE_PACKAGES.any { pkg.contains(it, ignoreCase = true) }) return true
        val devName = name ?: runCatching { device?.alias ?: device?.name }.getOrNull()
        if (devName != null && WEARABLE_KEYWORDS.any { devName.contains(it, ignoreCase = true) }) return true
        val major = runCatching { device?.bluetoothClass?.majorDeviceClass }.getOrNull() ?: -1
        if (major == BluetoothClass.Device.Major.WEARABLE || major == 0x0900 /* HEALTH */) return true
        val address = device?.address
        if (address != null && address in wearableRegistry) return true
        if (devName != null && devName in wearableRegistry) return true
        return false
    }

    /**
     * Nhận diện thiết bị âm thanh Bluetooth (Headphones, TWS Earbuds).
     */
    fun isAudio(device: BluetoothDevice): Boolean =
        runCatching {
            val major = device.bluetoothClass?.majorDeviceClass
            major == BluetoothClass.Device.Major.AUDIO_VIDEO
        }.getOrDefault(false)

    fun isSupportedDevice(device: BluetoothDevice?, name: String? = null, pkg: String? = null): Boolean {
        if (isWearable(device, name, pkg)) return true
        if (device != null && isAudio(device)) return true
        val devName = name ?: runCatching { device?.alias ?: device?.name }.getOrNull()
        if (devName != null) {
            val lower = devName.lowercase()
            if (AUDIO_KEYWORDS.any { lower.contains(it) } || WEARABLE_KEYWORDS.any { lower.contains(it) }) return true
        }
        return false
    }

    private fun isOverEar(device: BluetoothDevice?, name: String): Boolean {
        val cls = runCatching { device?.bluetoothClass?.deviceClass }.getOrNull()
        if (cls == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES) return true
        val n = name.lowercase()
        return OVER_EAR_NAMES.any { it in n }
    }

    private fun isAudioEarbudsCandidate(device: BluetoothDevice?, name: String): Boolean {
        if (device != null && isAudio(device)) return true
        val lower = name.trim().lowercase()
        return lower.isNotBlank() && AUDIO_KEYWORDS.any { lower.contains(it) }
    }

    private fun syncAudioDeviceConnectedState(context: Context, deviceName: String, explicitAddress: String? = null) {
        if (deviceName.isNotBlank()) {
            lastConnectedAudioDeviceName = deviceName
            setConnectedState(deviceName, true)
        }
        if (!explicitAddress.isNullOrBlank() && explicitAddress != "device_unknown") {
            lastConnectedAudioDeviceAddress = explicitAddress
            setConnectedState(explicitAddress, true)
            return
        }
        val matched = findBluetoothDevice(context, deviceName)
        if (matched?.address != null) {
            lastConnectedAudioDeviceAddress = matched.address
            setConnectedState(matched.address, true)
            return
        }
        runCatching {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return@runCatching
            val connectedAudio = (btManager.getConnectedDevices(BluetoothProfile.A2DP).orEmpty() +
                btManager.getConnectedDevices(BluetoothProfile.HEADSET).orEmpty())
                .firstOrNull { !isWearable(it) }
            if (connectedAudio?.address != null) {
                lastConnectedAudioDeviceAddress = connectedAudio.address
                setConnectedState(connectedAudio.address, true)
            }
        }
    }

    private fun handle(intent: Intent) {
        val context = app ?: return
        val action = intent.action ?: return

        var device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        if (device == null) {
            device = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE", BluetoothDevice::class.java)
        }

        val pkg = intent.component?.packageName ?: intent.`package`
        val rawName = intent.getStringExtra("name")
            ?: intent.getStringExtra("device_name")
            ?: intent.getStringExtra("deviceName")
            ?: intent.getStringExtra("device_title")
            ?: extractSamsungDeviceName(intent)

        val isWearableDevice = isWearable(device, rawName, pkg) || isSamsungWearableAction(action)
        if (!isSupportedDevice(device, rawName, pkg) && !isSamsungWearableAction(action)) return

        val rawAddress = intent.getStringExtra("address")
            ?: intent.getStringExtra("device_address")
            ?: intent.getStringExtra("mac")
            ?: intent.getStringExtra("BLUETOOTH_DEVICE_ADDRESS")
            ?: extractSamsungDeviceAddress(intent)
            ?: (if (isWearableDevice) showingWearableAddress else showingEarbudsAddress)

        if (device == null && !rawAddress.isNullOrBlank()) {
            device = runCatching {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                adapter?.getRemoteDevice(rawAddress)
            }.getOrNull()
        }

        val address = device?.address ?: rawAddress ?: rawName ?: "device_unknown"
        val devName = rawName ?: runCatching { device?.alias ?: device?.name }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: if (isWearableDevice) "Galaxy Fit 3" else "Tai nghe"

        if (isWearableDevice) {
            wearableRegistry.add(address)
            wearableRegistry.add(devName)
        }

        val isAudioProfileAction =
            action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED ||
            action == BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED
        val profileState = if (isAudioProfileAction) intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1) else -1
        val prevProfileState = if (isAudioProfileAction) intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1) else -1
        val isProfileConnected = isAudioProfileAction && profileState == BluetoothProfile.STATE_CONNECTED
        val isProfileDisconnected = isAudioProfileAction &&
            profileState == BluetoothProfile.STATE_DISCONNECTED &&
            (prevProfileState == BluetoothProfile.STATE_CONNECTED || prevProfileState == BluetoothProfile.STATE_DISCONNECTING)
        if (isAudioProfileAction && !isProfileConnected && !isProfileDisconnected) {
            return
        }

        when {
            // --- KẾT NỐI (CONNECTED) ---
            action == BluetoothDevice.ACTION_ACL_CONNECTED ||
            isProfileConnected ||
            action == ACTION_SAMSUNG_FIT_CONNECTED ||
            action == ACTION_SAMSUNG_WEARABLE_CONNECTED ||
            isSamsungAccessoryConnected(intent) -> {
                disconnectDebounce.remove(address)?.let(handler::removeCallbacks)
                disconnectDebounce.remove(devName)?.let(handler::removeCallbacks)
                warnedLow -= address
                pending.remove(address)?.let(handler::removeCallbacks)

                setConnectedState(address, true)
                setConnectedState(devName, true)
                if (!isWearableDevice) {
                    syncAudioDeviceConnectedState(context, devName, address)
                }

                var initial = readTelemetry(device, intent)
                if (!isWearableDevice) {
                    scanActiveNotificationsForEarbuds(context, devName)?.let { scanned ->
                        initial = mergeTelemetry(initial, scanned)
                    }
                }
                initial = mergeTelemetry(getStoredTelemetry(context, address, devName), initial)
                if (initial.effectiveLevel !in 0..100) {
                    val cached = getCachedBattery(context, devName).takeIf { it in 0..100 }
                        ?: (device?.address?.let { getCachedBattery(context, it) } ?: -1)
                    if (cached in 0..100) {
                        initial = initial.copy(main = cached)
                    }
                }
                if (initial.effectiveLevel !in 0..100) {
                    readGattBatteryAsync(context, device, devName, isWearableDevice)
                }
                if (initial.effectiveLevel in 0..100 || initial.hasDetails) {
                    initial = storeTelemetry(context, address, devName, initial)
                    show(context, device, devName, initial, low = (initial.effectiveLevel in 0..LOW_LEVEL), isWearable = isWearableDevice)
                } else {
                    val task = Runnable {
                        pending.remove(address)
                        var telemetry = readTelemetry(device, intent)
                        if (!isWearableDevice) {
                            scanActiveNotificationsForEarbuds(context, devName)?.let { scanned ->
                                telemetry = mergeTelemetry(telemetry, scanned)
                            }
                        }
                        telemetry = mergeTelemetry(getStoredTelemetry(context, address, devName), telemetry)
                        if (telemetry.effectiveLevel !in 0..100) {
                            val cached = getCachedBattery(context, devName).takeIf { it in 0..100 }
                                ?: (device?.address?.let { getCachedBattery(context, it) } ?: -1)
                            if (cached in 0..100) {
                                telemetry = telemetry.copy(main = cached)
                            }
                        }
                        if (telemetry.effectiveLevel !in 0..100) {
                            readGattBatteryAsync(context, device, devName, isWearableDevice)
                        }
                        telemetry = storeTelemetry(context, address, devName, telemetry)
                        show(context, device, devName, telemetry, low = (telemetry.effectiveLevel in 0..LOW_LEVEL), isWearable = isWearableDevice)
                    }
                    pending[address] = task
                    handler.postDelayed(task, BATTERY_WAIT_MS)
                }
            }

            // --- MẤT KẾT NỐI (DISCONNECTED) VỚI DEBOUNCE 1200MS (TAI NGHE) / 6000MS (WEARABLE) & XÁC THỰC PHẦN CỨNG ---
            action == BluetoothDevice.ACTION_ACL_DISCONNECTED ||
            isProfileDisconnected ||
            action == ACTION_SAMSUNG_FIT_DISCONNECTED ||
            action == ACTION_SAMSUNG_WEARABLE_DISCONNECTED ||
            isSamsungAccessoryDisconnected(intent) -> {
                val wasConnected = isConnected(address) || isConnected(devName)
                val allowAudioDisconnect = !isWearableDevice && (
                    wasConnected ||
                    devName.equals(lastConnectedAudioDeviceName, ignoreCase = true) ||
                    address == lastConnectedAudioDeviceAddress ||
                    isAudioEarbudsCandidate(device, devName)
                )
                pending.remove(address)?.let(handler::removeCallbacks)
                disconnectDebounce.remove(address)?.let(handler::removeCallbacks)
                disconnectDebounce.remove(devName)?.let(handler::removeCallbacks)

                if (!wasConnected && !allowAudioDisconnect) {
                    return
                }
                if (!isWearableDevice && System.currentTimeMillis() - lastDisconnectCardShownMs < 5000L) {
                    return
                }

                val disconnectTask = Runnable {
                    disconnectDebounce.remove(address)
                    disconnectDebounce.remove(devName)
                    if (isDeviceHardwareConnected(context, devName, isWearable = isWearableDevice) ||
                        isDeviceHardwareConnected(context, address, isWearable = isWearableDevice)) {
                        setConnectedState(address, true)
                        setConnectedState(devName, true)
                        return@Runnable
                    }
                    if (isWearableDevice && (isWithinAntiFlapCooldown(address) || isWithinAntiFlapCooldown(devName))) {
                        Log.i(TAG, "Anti-flap: skip disconnect popup for $devName within cooldown")
                        setConnectedState(address, true)
                        setConnectedState(devName, true)
                        return@Runnable
                    }
                    setConnectedState(address, false)
                    setConnectedState(devName, false)
                    deviceTelemetry -= address
                    deviceTelemetry -= address.lowercase()
                    deviceTelemetry -= devName
                    deviceTelemetry -= devName.lowercase()
                    warnedLow -= address
                    warnedLow -= devName
                    showDisconnected(context, devName, isWearable = isWearableDevice)
                }
                disconnectDebounce[address] = disconnectTask
                disconnectDebounce[devName] = disconnectTask
                handler.postDelayed(disconnectTask, disconnectDebounceFor(isWearableDevice))
            }

            // --- VENDOR SPECIFIC AT COMMAND ---
            action == ACTION_VENDOR_SPECIFIC_HEADSET_EVENT -> {
                disconnectDebounce.remove(address)?.let(handler::removeCallbacks)
                val cmd = intent.getStringExtra(EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD)
                if (cmd == "+IPHONEACCEV") {
                    val rawArgs = intent.getSerializableExtra(EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS) as? Array<*>
                        ?: (intent.getParcelableArrayExtra(EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS) as? Array<*>)
                    val parsed = parseIPhoneAccEv(rawArgs)
                    if (parsed != null) {
                        val wasAlreadyConnected = isConnected(address) || isConnected(devName)
                        val current = getStoredTelemetry(context, address, devName, isWearable = isWearableDevice)
                        val incoming = EarbudsTelemetry(
                            main = parsed.first,
                            isLeftCharging = if (parsed.second) true else current.isLeftCharging,
                            isRightCharging = if (parsed.second) true else current.isRightCharging,
                        )
                        val updated = storeTelemetry(context, address, devName, incoming, isWearable = isWearableDevice)
                        val changed = current != updated
                        val currentlyShowing = isShowing(isWearableDevice, address) || isShowing(isWearableDevice, devName)
                        val eff = updated.effectiveLevel
                        val low = eff in 0..LOW_LEVEL
                        if (eff > LOW_LEVEL + 5) {
                            warnedLow -= address
                            warnedLow -= devName
                        }
                        when {
                            !wasAlreadyConnected -> {
                                setConnectedState(address, true)
                                setConnectedState(devName, true)
                                show(context, device, devName, updated, low = low, isWearable = isWearableDevice, refreshDuration = true)
                            }
                            low && address !in warnedLow && devName !in warnedLow -> {
                                warnedLow += address
                                warnedLow += devName
                                show(context, device, devName, updated, low = true, isWearable = isWearableDevice, refreshDuration = true)
                            }
                            currentlyShowing && changed -> {
                                show(context, device, devName, updated, low = low, isWearable = isWearableDevice, refreshDuration = false)
                            }
                        }
                    }
                }
            }

            // --- CẬP NHẬT PIN (BATTERY UPDATED / CHANGED) ---
            else -> {
                disconnectDebounce.remove(address)?.let(handler::removeCallbacks)
                disconnectDebounce.remove(devName)?.let(handler::removeCallbacks)

                val wasAlreadyConnected = isConnected(address) || isConnected(devName)
                val before = getStoredTelemetry(context, address, devName, isWearable = isWearableDevice)
                var incoming = readTelemetry(device, intent)
                if (!isWearableDevice) {
                    scanActiveNotificationsForEarbuds(context, devName)?.let { scanned ->
                        incoming = mergeTelemetry(incoming, scanned)
                    }
                }
                val updated = storeTelemetry(context, address, devName, incoming, isWearable = isWearableDevice)
                val changed = before != updated
                val eff = updated.effectiveLevel
                val currentlyShowing = isShowing(isWearableDevice, address) || isShowing(isWearableDevice, devName)
                val low = eff in 0..LOW_LEVEL
                if (eff > LOW_LEVEL + 5) {
                    warnedLow -= address
                    warnedLow -= devName
                }

                when {
                    pending.containsKey(address) -> {
                        pending.remove(address)?.let(handler::removeCallbacks)
                        show(context, device, devName, updated, low = low, isWearable = isWearableDevice, refreshDuration = true)
                    }
                    !wasAlreadyConnected -> {
                        setConnectedState(address, true)
                        setConnectedState(devName, true)
                        show(context, device, devName, updated, low = low, isWearable = isWearableDevice, refreshDuration = true)
                    }
                    low && address !in warnedLow && devName !in warnedLow -> {
                        warnedLow += address
                        warnedLow += devName
                        show(context, device, devName, updated, low = true, isWearable = isWearableDevice, refreshDuration = true)
                    }
                    currentlyShowing && changed -> {
                        show(context, device, devName, updated, low = low, isWearable = isWearableDevice, refreshDuration = false)
                    }
                }
            }
        }
    }

    private fun isSamsungWearableAction(action: String): Boolean =
        action in setOf(
            ACTION_SAMSUNG_FIT_CONNECTED,
            ACTION_SAMSUNG_FIT_DISCONNECTED,
            ACTION_SAMSUNG_ACCESSORY_STATE_CHANGED,
            ACTION_SAMSUNG_WEARABLE_CONNECTED,
            ACTION_SAMSUNG_WEARABLE_DISCONNECTED,
            ACTION_SAMSUNG_BATTERY_CHANGED,
            ACTION_SAMSUNG_BATTERY_ADDED,
            ACTION_SAMSUNG_WATCH_LEVEL,
            ACTION_SAMSUNG_PLUGIN_RESPONSE,
        )

    private fun isSamsungAccessoryConnected(intent: Intent): Boolean {
        if (intent.action != ACTION_SAMSUNG_ACCESSORY_STATE_CHANGED) return false
        val state = intent.getIntExtra("com.samsung.accessory.device.extra.CONNECTION_STATE", -1)
            .takeIf { it != -1 } ?: intent.getIntExtra("state", -1)
        return state == 1 || state == 2 || intent.getBooleanExtra("connected", false)
    }

    private fun isSamsungAccessoryDisconnected(intent: Intent): Boolean {
        if (intent.action != ACTION_SAMSUNG_ACCESSORY_STATE_CHANGED) return false
        val state = intent.getIntExtra("com.samsung.accessory.device.extra.CONNECTION_STATE", -1)
            .takeIf { it != -1 } ?: intent.getIntExtra("state", -1)
        return state == 0 || intent.getBooleanExtra("disconnected", false)
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
                    1 -> battery = ((value + 1) * 10).coerceIn(0, 100)
                    2 -> charging = (value == 1)
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

    private fun extractSamsungBattery(intent: Intent): Int {
        val extras = intent.extras ?: return -1
        for (key in listOf("LEVEL", "level", "battery", "battery_level", "capacity", "battery_status", "EXTRA_BATTERY_LEVEL", "percentage", "batteryPct", "value")) {
            val v = extras.get(key)
            val num = when (v) {
                is Number -> v.toInt()
                is String -> v.trim().replace("%", "").toIntOrNull()
                else -> null
            }
            if (num != null && num in 0..100) return num
        }

        val infoObj = extras.get("com.samsung.battery.EXTRA_BATTERY_INFO")
            ?: extras.get("EXTRA_BATTERY_INFO")
            ?: extras.get("battery_info")
            ?: extras.get("response_data")
            ?: extras.get("data")
            ?: extras.get("result")
            ?: extras.get("bundle")
        if (infoObj != null) {
            when (infoObj) {
                is Number -> if (infoObj.toInt() in 0..100) return infoObj.toInt()
                is android.os.Bundle -> {
                    val b = extractBattery(infoObj, "level", "battery_level", "batteryLevel", "battery", "capacity", "LEVEL")
                    if (b in 0..100) return b
                }
                is Array<*> -> {
                    for (item in infoObj) {
                        if (item is android.os.Bundle) {
                            val b = extractBattery(item, "level", "battery_level", "batteryLevel", "battery", "capacity", "LEVEL")
                            if (b in 0..100) return b
                        }
                    }
                }
                is java.util.Collection<*> -> {
                    for (item in infoObj) {
                        if (item is android.os.Bundle) {
                            val b = extractBattery(item, "level", "battery_level", "batteryLevel", "battery", "capacity", "LEVEL")
                            if (b in 0..100) return b
                        }
                    }
                }
                else -> {
                    val lvl = runCatching {
                        val m = infoObj.javaClass.methods.firstOrNull { it.name.lowercase().contains("level") && it.parameterCount == 0 }
                        (m?.invoke(infoObj) as? Number)?.toInt()
                    }.getOrNull()
                    if (lvl != null && lvl in 0..100) return lvl
                }
            }
        }
        return -1
    }

    private fun extractSamsungDeviceName(intent: Intent): String? {
        val extras = intent.extras ?: return null
        for (key in listOf("name", "device_name", "deviceName", "device_title", "DEVICE_NAME")) {
            val v = extras.getString(key)
            if (!v.isNullOrBlank()) return v
        }
        val infoObj = extras.get("com.samsung.battery.EXTRA_BATTERY_INFO") ?: extras.get("EXTRA_BATTERY_INFO")
        if (infoObj is android.os.Bundle) {
            for (key in listOf("name", "device_name", "deviceName", "device_title")) {
                val v = infoObj.getString(key)
                if (!v.isNullOrBlank()) return v
            }
        }
        return null
    }

    private fun extractSamsungDeviceAddress(intent: Intent): String? {
        val extras = intent.extras ?: return null
        for (key in listOf("address", "device_address", "mac", "BLUETOOTH_DEVICE_ADDRESS", "com.samsung.accessory.extra.DEVICE_ADDRESS", "com.samsung.android.wearable.extra.DEVICE_ADDRESS")) {
            val v = extras.getString(key)
            if (!v.isNullOrBlank()) return v
        }
        val infoObj = extras.get("com.samsung.battery.EXTRA_BATTERY_INFO") ?: extras.get("EXTRA_BATTERY_INFO")
        if (infoObj is android.os.Bundle) {
            for (key in listOf("address", "device_address", "mac")) {
                val v = infoObj.getString(key)
                if (!v.isNullOrBlank()) return v
            }
        }
        return null
    }

    /** Trích xuất viễn trắc chuyên sâu (L/R/Case/Main) từ Metadata AOSP hoặc Intent Extras */
    private fun readTelemetry(device: BluetoothDevice?, intent: Intent?): EarbudsTelemetry {
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
        if (main !in 0..100 && intent != null) {
            main = extractSamsungBattery(intent)
        }
        if (main !in 0..100 && device != null) {
            main = batteryOf(device)
        }

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
        if (device != null) {
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
        }

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
     * Làm sạch các ký tự ẩn Unicode Bidi (\u2068, \u2069, \u200E, \u200F, Zero-width) và khoảng trắng đặc biệt (\u00A0)
     * do Google Play Services / BidiFormatter chèn vào thông báo Fast Pair.
     */
    internal fun cleanBatteryText(raw: String): String {
        if (raw.isEmpty()) return ""
        return raw
            .replace(Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u206F\\uFEFF]"), "")
            .replace(Regex("[\\u00A0\\u2007\\u202F\\t\\r\\n]+"), " ")
            .trim()
    }

    /**
     * Bóc tách thông số pin đa thành phần (Tai Trái / Tai Phải / Hộp sạc / Pin chính) từ văn bản thông báo.
     */
    fun parseBatteryText(
        title: String,
        text: String,
        subText: String = "",
        bigText: String = "",
        extraText: String = "",
    ): EarbudsTelemetry? {
        val cleanTitle = cleanBatteryText(title)
        val cleanText = cleanBatteryText(text)
        val cleanSub = cleanBatteryText(subText)
        val cleanBig = cleanBatteryText(bigText)
        val cleanExtra = cleanBatteryText(extraText)

        val fullText = "$cleanTitle | $cleanText | $cleanSub | $cleanBig | $cleanExtra"
        if (!fullText.contains("%")) return null

        val leftRegex = Regex(
            """(?:Tai\s*nghe\s*bên\s*trái|Tai\s*nghe\s*trái|Tai\s*trái|Bên\s*trái|Left\s*earbud|Left\s*bud|\bLeft\b|\bTrái\b|\bL\b)\s*(?:⚡\s*)?[:：·\-–—]?\s*(?:⚡\s*)?(\d{1,3})\s*%""",
            RegexOption.IGNORE_CASE,
        )
        val leftRevRegex = Regex(
            """(\d{1,3})\s*%\s*(?:⚡\s*)?(?:\(\s*(?:L|Trái|Left)\s*\)|(?:Tai\s*nghe\s*bên\s*trái|Tai\s*trái|\bLeft\b|\bTrái\b))""",
            RegexOption.IGNORE_CASE,
        )

        val rightRegex = Regex(
            """(?:Tai\s*nghe\s*bên\s*phải|Tai\s*nghe\s*phải|Tai\s*phải|Bên\s*phải|Right\s*earbud|Right\s*bud|\bRight\b|\bPhải\b|\bR\b)\s*(?:⚡\s*)?[:：·\-–—]?\s*(?:⚡\s*)?(\d{1,3})\s*%""",
            RegexOption.IGNORE_CASE,
        )
        val rightRevRegex = Regex(
            """(\d{1,3})\s*%\s*(?:⚡\s*)?(?:\(\s*(?:R|Phải|Right)\s*\)|(?:Tai\s*nghe\s*bên\s*phải|Tai\s*phải|\bRight\b|\bPhải\b))""",
            RegexOption.IGNORE_CASE,
        )

        val caseRegex = Regex(
            """(?:Hộp\s*sạc|Hộp\s*đựng|Dock\s*sạc|Kén\s*sạc|\bHộp\b|Charging\s*case|\bCase\b|\bBox\b|\bDock\b|\bC\b)\s*(?:⚡\s*)?[:：·\-–—]?\s*(?:⚡\s*)?(\d{1,3})\s*%""",
            RegexOption.IGNORE_CASE,
        )
        val caseRevRegex = Regex(
            """(\d{1,3})\s*%\s*(?:⚡\s*)?(?:\(\s*(?:Case|Hộp|Hộp\s*sạc)\s*\)|(?:Hộp\s*sạc|Charging\s*case|\bCase\b))""",
            RegexOption.IGNORE_CASE,
        )

        val mainRegex = Regex(
            """(?:Mức\s*pin|Pin|Battery|Tai\s*nghe|Fit\s*3|Galaxy\s*Fit|Watch|Band)\s*[:：·\-–—]?\s*(\d{1,3})\s*%""",
            RegexOption.IGNORE_CASE,
        )

        val left = (leftRegex.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 0..100 })
            ?: (leftRevRegex.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 0..100 })
            ?: -1

        val right = (rightRegex.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 0..100 })
            ?: (rightRevRegex.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 0..100 })
            ?: -1

        val case = (caseRegex.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 0..100 })
            ?: (caseRevRegex.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 0..100 })
            ?: -1

        var main = mainRegex.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 0..100 } ?: -1

        if (left !in 0..100 && right !in 0..100 && case !in 0..100 && main !in 0..100) {
            val anyPct = Regex("""(\d{1,3})\s*%""").find(fullText)?.groupValues?.get(1)?.toIntOrNull()
            if (anyPct != null && anyPct in 0..100) {
                main = anyPct
            } else {
                return null
            }
        }

        val isCharging = fullText.contains("⚡") ||
            fullText.contains("đang sạc", ignoreCase = true) ||
            fullText.contains("charging", ignoreCase = true)

        val isLeftCharging = isCharging && (
            fullText.contains(Regex("""(?:Tai\s*nghe\s*bên\s*trái|Tai\s*nghe\s*trái|Tai\s*trái|\bLeft\b|\bL\b|\bTrái\b)\s*[:：·\-–—]?\s*\d{1,3}\s*%\s*(?:⚡|đang\s*sạc|charging)""", RegexOption.IGNORE_CASE)) ||
            fullText.contains(Regex("""(?:⚡|đang\s*sạc|charging)\s*(?:Tai\s*nghe\s*bên\s*trái|Tai\s*nghe\s*trái|Tai\s*trái|\bLeft\b|\bL\b|\bTrái\b)""", RegexOption.IGNORE_CASE)) ||
            (left in 0..100 && right !in 0..100 && case !in 0..100)
        )
        val isRightCharging = isCharging && (
            fullText.contains(Regex("""(?:Tai\s*nghe\s*bên\s*phải|Tai\s*nghe\s*phải|Tai\s*phải|\bRight\b|\bR\b|\bPhải\b)\s*[:：·\-–—]?\s*\d{1,3}\s*%\s*(?:⚡|đang\s*sạc|charging)""", RegexOption.IGNORE_CASE)) ||
            fullText.contains(Regex("""(?:⚡|đang\s*sạc|charging)\s*(?:Tai\s*nghe\s*bên\s*phải|Tai\s*nghe\s*phải|Tai\s*phải|\bRight\b|\bR\b|\bPhải\b)""", RegexOption.IGNORE_CASE)) ||
            (right in 0..100 && left !in 0..100 && case !in 0..100)
        )
        val isCaseCharging = isCharging && (
            fullText.contains(Regex("""(?:Hộp\s*sạc|Hộp\s*đựng|\bHộp\b|Charging\s*case|\bCase\b|\bBox\b|\bDock\b)\s*[:：·\-–—]?\s*\d{1,3}\s*%\s*(?:⚡|đang\s*sạc|charging)""", RegexOption.IGNORE_CASE)) ||
            fullText.contains(Regex("""(?:⚡|đang\s*sạc|charging)\s*(?:Hộp\s*sạc|Hộp\s*đựng|\bHộp\b|Charging\s*case|\bCase\b|\bBox\b|\bDock\b)""", RegexOption.IGNORE_CASE))
        )

        return EarbudsTelemetry(
            main = main,
            left = left,
            right = right,
            case = case,
            isLeftCharging = isLeftCharging,
            isRightCharging = isRightCharging,
            isCaseCharging = isCaseCharging,
        )
    }

    /**
     * Thu thập toàn bộ văn bản từ StatusBarNotification, bao gồm tất cả key trong extras
     * và các trường CharSequence ẩn bên trong custom RemoteViews (Fast Pair / Vivo TWS).
     */
    private fun extractAllNotificationText(sbn: StatusBarNotification): String {
        val sb = StringBuilder()
        val notif = sbn.notification ?: return ""
        val extras = notif.extras
        if (extras != null) {
            val stdKeys = listOf(
                Notification.EXTRA_TITLE,
                Notification.EXTRA_TEXT,
                Notification.EXTRA_SUB_TEXT,
                Notification.EXTRA_BIG_TEXT,
                Notification.EXTRA_SUMMARY_TEXT,
                Notification.EXTRA_INFO_TEXT,
                Notification.EXTRA_TITLE_BIG,
            )
            for (k in stdKeys) {
                extras.getCharSequence(k)?.takeIf { it.isNotBlank() }?.let { sb.append(it).append(" | ") }
            }
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { line ->
                if (!line.isNullOrBlank()) sb.append(line).append(" | ")
            }
            runCatching {
                for (key in extras.keySet()) {
                    if (key in stdKeys || key == Notification.EXTRA_TEXT_LINES) continue
                    when (val v = extras.get(key)) {
                        is CharSequence -> if (v.isNotBlank()) sb.append(v).append(" | ")
                        is Array<*> -> v.forEach { item ->
                            if (item is CharSequence && item.isNotBlank()) sb.append(item).append(" | ")
                        }
                        is Iterable<*> -> v.forEach { item ->
                            if (item is CharSequence && item.isNotBlank()) sb.append(item).append(" | ")
                        }
                    }
                }
            }
        }
        notif.tickerText?.takeIf { it.isNotBlank() }?.let { sb.append(it).append(" | ") }

        // Trích xuất sâu từ RemoteViews (contentView & bigContentView) nếu dùng custom layout
        @Suppress("DEPRECATION")
        val remoteViewsList = listOfNotNull(notif.contentView, notif.bigContentView)
        for (rv in remoteViewsList) {
            runCatching {
                var cls: Class<*>? = rv.javaClass
                var actionsField: java.lang.reflect.Field? = null
                while (cls != null && actionsField == null) {
                    actionsField = runCatching { cls.getDeclaredField("mActions") }.getOrNull()
                    cls = cls.superclass
                }
                actionsField?.isAccessible = true
                val actions = actionsField?.get(rv) as? Iterable<*>
                actions?.forEach { act ->
                    if (act != null) {
                        for (fieldName in listOf("value", "mValue", "text")) {
                            val f = runCatching { act.javaClass.getDeclaredField(fieldName) }.getOrNull()
                                ?: runCatching { act.javaClass.superclass?.getDeclaredField(fieldName) }.getOrNull()
                            if (f != null) {
                                f.isAccessible = true
                                val v = runCatching { f.get(act) }.getOrNull()
                                if (v is CharSequence && v.isNotBlank()) {
                                    sb.append(v).append(" | ")
                                }
                            }
                        }
                    }
                }
            }
        }
        return sb.toString()
    }

    private val TWS_NOTIFICATION_PACKAGES = setOf(
        "com.google.android.gms",
        "com.vivo.tws",
        "com.vivo.earphone",
        "com.android.bluetooth",
        "com.vivo.audiofx",
        "com.google.android.apps.wearables.maestro.companion",
        "com.samsung.accessory.neobeanmgr",
        "com.samsung.accessory.atticmgr",
        "com.samsung.android.app.watchmanager",
        "com.samsung.android.wearable.fit3plugin",
        "com.samsung.android.wearable.watch6plugin",
        "com.samsung.android.wearable.gearfit2plugin",
        "com.samsung.android.geargplugin",
        "com.samsung.accessory",
        "com.huawei.health",
        "com.xiaomi.wearable",
    )

    /**
     * Chủ động quét toàn bộ thông báo đang hiển thị trên thanh trạng thái (kể cả thông báo Im lặng của Google Play Services)
     * để lấy ngay thông số pin Trái / Phải / Hộp sạc khi tai nghe kết nối hoặc cập nhật Bluetooth.
     */
    fun scanActiveNotificationsForEarbuds(context: Context, targetName: String? = null): EarbudsTelemetry? {
        val activeList = runCatching {
            com.originisle.android.service.NotificationCastListener.instance?.activeNotifications
        }.getOrNull() ?: return null

        val targetLower = targetName?.trim()?.lowercase()?.takeIf {
            it.isNotBlank() && it != "tai nghe" && it != "tai nghe tws" && it != "tai nghe bluetooth"
        }

        var bestMatch: EarbudsTelemetry? = null
        for (sbn in activeList) {
            val extras = sbn.notification?.extras ?: continue
            val title = cleanBatteryText(extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "")
            val text = cleanBatteryText(extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "")
            val subText = cleanBatteryText(extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "")
            val bigText = cleanBatteryText(extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: "")
            val extraAll = cleanBatteryText(extractAllNotificationText(sbn))
            val combined = "$title | $text | $subText | $bigText | $extraAll"

            val pkg = sbn.packageName
            val isTwsPkg = pkg in TWS_NOTIFICATION_PACKAGES
            val hasKeyword = (AUDIO_KEYWORDS + WEARABLE_KEYWORDS + listOf("bên trái", "bên phải", "hộp sạc")).any {
                combined.contains(it, ignoreCase = true)
            }
            val matchesTarget = targetLower != null && combined.lowercase().contains(targetLower)

            if (!isTwsPkg && !hasKeyword && !matchesTarget) continue

            val parsed = parseBatteryText(title, text, subText, bigText, extraAll) ?: continue
            if (parsed.hasDetails || matchesTarget) {
                val resolvedName = extractDeviceNameFromNotificationText(title, text, isWearable = false)
                val storeName = targetName?.takeIf { it.isNotBlank() } ?: resolvedName
                val merged = storeTelemetry(context, null, storeName, parsed)
                if (resolvedName.isNotBlank() && !resolvedName.equals(storeName, ignoreCase = true)) {
                    storeTelemetry(context, null, resolvedName, merged)
                }
                if (merged.hasDetails) {
                    return merged
                }
                if (bestMatch == null) {
                    bestMatch = merged
                }
            }
        }
        return bestMatch
    }

    private fun extractDeviceNameFromNotificationText(
        rawTitle: String,
        rawText: String,
        isWearable: Boolean,
    ): String {
        val systemPkgNames = setOf(
            "google", "google play", "dịch vụ của google play", "google play services",
            "bluetooth", "fast pair", "quick share", "chia sẻ nhanh", "hệ thống", "system",
            "thiết bị", "devices", "connected devices", " thiết bị đã kết nối"
        )
        var devName = ""
        val titleCandidate = cleanBatteryText(rawTitle)
        val textCandidate = cleanBatteryText(rawText)

        if (titleCandidate.contains(":")) {
            val prefix = titleCandidate.substringBefore(":").trim()
            if (prefix.isNotBlank() && !prefix.contains("%") && !systemPkgNames.any { prefix.equals(it, ignoreCase = true) }) {
                devName = prefix
            }
        }
        if (devName.isBlank() && titleCandidate.isNotBlank() && !titleCandidate.contains("%") &&
            !systemPkgNames.any { titleCandidate.equals(it, ignoreCase = true) }
        ) {
            devName = titleCandidate
        }
        if (devName.isBlank() && textCandidate.contains(":")) {
            val prefix = textCandidate.substringBefore(":").trim()
            if (prefix.isNotBlank() && !prefix.contains("%") && !systemPkgNames.any { prefix.equals(it, ignoreCase = true) }) {
                devName = prefix
            }
        }
        if (devName.isBlank()) {
            devName = if (isWearable) "Galaxy Fit 3" else "Tai nghe TWS"
        }
        devName = devName.replace(Regex("""\s*\(?\d{1,3}%\)?"""), "").trim()
        if (devName.isBlank() || devName.equals("Bluetooth", ignoreCase = true)) {
            devName = if (isWearable) "Thiết bị đeo" else "Tai nghe Bluetooth"
        }
        return devName
    }

    /**
     * Bóc tách thông số pin Trái, Phải, Hộp sạc, Thiết bị đeo thật từ thông báo hệ thống.
     * Bóc tách chuẩn xác Google Fast Pair (VD: "heobeos plus: Tai nghe bên trái 50% Hộp sạc 20% Tai nghe bên phải 100%"),
     * Vivo TWS, Samsung Galaxy Buds, Galaxy Fit 3, Galaxy Watch...
     */
    fun tryParseBatteryFromNotification(context: Context, sbn: StatusBarNotification): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        if (!prefs.getBoolean("cast_earbuds", true)) return false

        val extras = sbn.notification?.extras ?: return false
        val title = cleanBatteryText(extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "")
        val text = cleanBatteryText(extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "")
        val subText = cleanBatteryText(extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "")
        val bigText = cleanBatteryText(extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: "")
        val extraAll = cleanBatteryText(extractAllNotificationText(sbn))
        val fullText = "$title | $text | $subText | $bigText | $extraAll"

        val pkg = sbn.packageName
        val isTwsPkg = pkg in TWS_NOTIFICATION_PACKAGES
        val hasKeyword = (AUDIO_KEYWORDS + WEARABLE_KEYWORDS + listOf("bên trái", "bên phải", "hộp sạc")).any {
            fullText.contains(it, ignoreCase = true)
        }

        if (!isTwsPkg && !hasKeyword) return false
        if (!fullText.contains("%")) return false

        val parsed = parseBatteryText(title, text, subText, bigText, extraAll) ?: return false
        val hasMultiComponent = parsed.hasDetails

        // Phân định rạch ròi Wearable vs Earbuds: nếu có L/R/Case thì chắc chắn 100% là tai nghe TWS
        val isWearableDevice = !hasMultiComponent && (
            isWearable(null, title, pkg) ||
            listOf("fit", "watch", "band", "vòng đeo tay", "đồng hồ").any { fullText.contains(it, ignoreCase = true) }
        )

        val devName = extractDeviceNameFromNotificationText(title, text, isWearableDevice)
        val matchedBtDevice = findBluetoothDevice(context, devName)
        val btAddress = matchedBtDevice?.address

        // Hủy các timer pending 2.5s để không bắn đè main lên kết quả chi tiết L/R/Case vừa bóc tách
        if (!isWearableDevice) {
            pending.values.forEach(handler::removeCallbacks)
            pending.clear()
        } else if (btAddress != null) {
            pending.remove(btAddress)?.let(handler::removeCallbacks)
        }

        val cleanParsed = if (isWearableDevice) {
            parsed.copy(
                main = parsed.effectiveLevel,
                left = -1,
                right = -1,
                case = -1,
                isLeftCharging = false,
                isRightCharging = false,
                isCaseCharging = false
            )
        } else parsed

        val wasAlreadyConnected = isConnected(devName) || (btAddress != null && isConnected(btAddress))
        val existing = getStoredTelemetry(context, btAddress, devName, isWearable = isWearableDevice)
        val merged = storeTelemetry(context, btAddress, devName, cleanParsed, isWearable = isWearableDevice)
        setConnectedState(devName, true)
        if (btAddress != null) {
            setConnectedState(btAddress, true)
        }
        if (!isWearableDevice) {
            syncAudioDeviceConnectedState(context, devName, btAddress)
        }

        val changed = existing != merged
        val currentlyShowing = if (isWearableDevice) {
            showingWearableAddress == devName || (btAddress != null && showingWearableAddress == btAddress) ||
                isShowing(true, devName) || (btAddress != null && isShowing(true, btAddress))
        } else {
            showingEarbudsAddress == devName || (btAddress != null && showingEarbudsAddress == btAddress) ||
                isShowing(false, devName) || (btAddress != null && isShowing(false, btAddress))
        }
        val newlyDiscoveredDetails = !existing.hasDetails && merged.hasDetails && isWithinAntiFlapCooldown(devName)
        val isNewLowWarning = merged.effectiveLevel in 0..LOW_LEVEL && devName !in warnedLow
        if (isNewLowWarning) warnedLow.add(devName)
        if (merged.effectiveLevel > LOW_LEVEL + 5) warnedLow.remove(devName)

        if (!wasAlreadyConnected || newlyDiscoveredDetails || (currentlyShowing && changed) || isNewLowWarning) {
            val lowWarn = merged.effectiveLevel in 0..LOW_LEVEL
            show(
                context = context,
                device = matchedBtDevice,
                customName = devName,
                telemetry = merged,
                low = lowWarn,
                isWearable = isWearableDevice,
                refreshDuration = !currentlyShowing,
            )
        }
        return true
    }

    /**
     * Quét và lấy viễn trắc thiết bị tai nghe hoặc thiết bị đeo đang kết nối ngay lập tức.
     * Quét thêm GATT profile (7) và bonded devices matching wearable BLE (Galaxy Fit 3).
     */
    fun pollConnectedDeviceNow(context: Context): Boolean {
        if (!hasPermission(context)) return false
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return false
        val adapter = bluetoothManager.adapter ?: return false
        if (!adapter.isEnabled) return false

        // 1. Kiểm tra qua getConnectedDevices của BluetoothManager (HEADSET, A2DP, và GATT profile 7)
        val connectedFromManager = runCatching {
            val list = mutableListOf<BluetoothDevice>()
            list.addAll(bluetoothManager.getConnectedDevices(BluetoothProfile.HEADSET))
            list.addAll(bluetoothManager.getConnectedDevices(BluetoothProfile.A2DP))
            runCatching {
                list.addAll(bluetoothManager.getConnectedDevices(7 /* GATT */))
            }
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
                    (isAudio(dev) || isWearable(dev)) && (isConnectedMethod.invoke(dev) as? Boolean == true)
                }
            }.getOrDefault(emptyList())
        }

        // A. Luồng Tai nghe âm thanh (ID_EARBUDS = 35004)
        val audioDevice = connectedDevices.firstOrNull { isAudio(it) && !isWearable(it) }
        if (audioDevice != null) {
            val address = audioDevice.address
            val devName = runCatching { audioDevice.alias ?: audioDevice.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Tai nghe"
            val batteryLevel = batteryOf(audioDevice)

            var telemetry = readTelemetry(audioDevice, null)
            if (telemetry.effectiveLevel !in 0..100 && batteryLevel in 0..100) {
                telemetry = telemetry.copy(main = batteryLevel)
            }
            scanActiveNotificationsForEarbuds(context, devName)?.let { scanned ->
                telemetry = mergeTelemetry(telemetry, scanned)
            }
            telemetry = storeTelemetry(context, address, devName, telemetry, isWearable = false)

            setConnectedState(address, true)
            runCatching { audioDevice.name }.getOrNull()?.let { if (it.isNotBlank()) setConnectedState(it, true) }
            runCatching { audioDevice.alias }.getOrNull()?.let { if (it.isNotBlank()) setConnectedState(it, true) }
            show(
                context = context,
                device = audioDevice,
                customName = devName,
                telemetry = telemetry,
                low = (telemetry.effectiveLevel in 0..LOW_LEVEL),
                isWearable = false,
            )
        }

        // B. Luồng Thiết bị đeo / Đồng hồ thông minh (ID_WEARABLE = 35014)
        val wearableDevice = connectedDevices.firstOrNull { isWearable(it) }
        if (wearableDevice != null) {
            val address = wearableDevice.address
            val devName = runCatching { wearableDevice.alias ?: wearableDevice.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Galaxy Fit 3"
            val batteryLevel = batteryOf(wearableDevice)

            var telemetry = readTelemetry(wearableDevice, null)
            if (telemetry.effectiveLevel !in 0..100 && batteryLevel in 0..100) {
                telemetry = telemetry.copy(main = batteryLevel)
            }
            telemetry = telemetry.copy(left = -1, right = -1, case = -1, isLeftCharging = false, isRightCharging = false, isCaseCharging = false)
            telemetry = storeTelemetry(context, address, devName, telemetry, isWearable = true)

            setConnectedState(address, true)
            runCatching { wearableDevice.name }.getOrNull()?.let { if (it.isNotBlank()) setConnectedState(it, true) }
            runCatching { wearableDevice.alias }.getOrNull()?.let { if (it.isNotBlank()) setConnectedState(it, true) }
            show(
                context = context,
                device = wearableDevice,
                customName = devName,
                telemetry = telemetry,
                low = (telemetry.effectiveLevel in 0..LOW_LEVEL),
                isWearable = true,
            )
        }

        return audioDevice != null || wearableDevice != null
    }

    fun batteryOf(device: BluetoothDevice): Int = runCatching {
        BluetoothDevice::class.java.getMethod("getBatteryLevel").invoke(device) as Int
    }.getOrDefault(-1).takeIf { it in 0..100 } ?: -1

    private fun createSafeIcon(context: Context, glyph: Int): Icon =
        BatteryCanvasRenderer.createDeviceIcon(context, glyph)

    /**
     * Hiển thị thẻ Kết nối thành công với viền xanh #00D084, tiến độ selfie, và bố cục co giãn thích ứng.
     */
    private fun show(
        context: Context,
        device: BluetoothDevice?,
        customName: String? = null,
        telemetry: EarbudsTelemetry,
        low: Boolean,
        isWearable: Boolean = false,
        refreshDuration: Boolean = true,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        if (!prefs.getBoolean("cast_earbuds", true)) return

        val isWearableDevice = isWearable || isWearable(device, customName)
        val name = customName ?: runCatching { device?.alias ?: device?.name }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: if (isWearableDevice) "Galaxy Fit 3" else "Tai nghe"

        val targetDev = device ?: findBluetoothDevice(context, name)
        var effectivePct = telemetry.effectiveLevel

        if (effectivePct !in 0..100 && targetDev != null) {
            val devBattery = batteryOf(targetDev)
            if (devBattery in 0..100) {
                effectivePct = devBattery
            }
        }

        if (isWearableDevice && effectivePct !in 0..100) {
            val fit3Battery = querySamsungFit3Battery(context, name)
            if (fit3Battery in 0..100) {
                effectivePct = fit3Battery
            } else {
                val fallbackPct = getCachedBattery(context, name).takeIf { it in 0..100 }
                    ?: getCachedBattery(context, "wearable_last_known_battery")
                if (fallbackPct in 0..100) {
                    effectivePct = fallbackPct
                }
            }
        }

        if (effectivePct !in 0..100 && targetDev != null) {
            readGattBatteryAsync(context, targetDev, name, isWearableDevice)
        }

        val stored = getStoredTelemetry(context, (targetDev ?: device)?.address, name, isWearableDevice)
        val incomingClean = if (isWearableDevice) {
            telemetry.copy(
                left = -1,
                right = -1,
                case = -1,
                isLeftCharging = false,
                isRightCharging = false,
                isCaseCharging = false,
            )
        } else telemetry
        val mergedInitial = mergeTelemetry(stored, incomingClean)
        if (mergedInitial.effectiveLevel in 0..100 && effectivePct !in 0..100) {
            effectivePct = mergedInitial.effectiveLevel
        }

        val known = effectivePct in 0..100
        val isLow = known && (
            effectivePct <= LOW_LEVEL ||
                (!isWearableDevice && mergedInitial.left in 0..LOW_LEVEL) ||
                (!isWearableDevice && mergedInitial.right in 0..LOW_LEVEL)
            )
        val lowWarning = low || isLow

        val activeTelemetry = if (known && mergedInitial.effectiveLevel !in 0..100) {
            mergedInitial.copy(main = effectivePct)
        } else {
            mergedInitial
        }

        val safeTelemetry = if (isWearableDevice) {
            activeTelemetry.copy(
                left = -1,
                right = -1,
                case = -1,
                isLeftCharging = false,
                isRightCharging = false,
                isCaseCharging = false,
            )
        } else activeTelemetry

        storeTelemetry(context, (targetDev ?: device)?.address, name, safeTelemetry, isWearableDevice)
        if (!isWearableDevice) {
            syncAudioDeviceConnectedState(context, name, (targetDev ?: device)?.address)
        }
        if (known) {
            saveCachedBattery(context, name, effectivePct)
            (targetDev ?: device)?.address?.let { saveCachedBattery(context, it, effectivePct) }
        }

        val cardId = if (isWearableDevice) ID_WEARABLE else ID_EARBUDS
        val glyph = when {
            isWearableDevice -> R.drawable.ic_smartband
            device != null && isOverEar(device, name) -> R.drawable.ic_headphones
            else -> R.drawable.ic_earbuds
        }
        val safeIcon = createSafeIcon(context, glyph)
        IconCache.activeSmallIcons.remove(cardId)

        val click = PendingIntent.getActivity(
            context, cardId, Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // Dòng 1: oi_title
        val displayTitle = when {
            !isWearableDevice && safeTelemetry.case in 0..100 ->
                "${name.take(16)} · Hộp ${safeTelemetry.case}%${if (safeTelemetry.isCaseCharging) "⚡" else ""}"
            known ->
                "${name.take(18)} · ${effectivePct}%"
            else ->
                "${name.take(20)} · Đã kết nối"
        }

        // Dòng 2: oi_text — Hiển thị đầy đủ từng thành phần Trái / Phải / Hộp sạc chuẩn OriginOS 6
        val detailParts = if (isWearableDevice) emptyList() else buildList {
            if (safeTelemetry.left in 0..100) {
                add("Tai trái: ${safeTelemetry.left}%${if (safeTelemetry.isLeftCharging) "⚡" else ""}")
            }
            if (safeTelemetry.right in 0..100) {
                add("Tai phải: ${safeTelemetry.right}%${if (safeTelemetry.isRightCharging) "⚡" else ""}")
            }
            if (safeTelemetry.case in 0..100) {
                add("Hộp sạc: ${safeTelemetry.case}%${if (safeTelemetry.isCaseCharging) "⚡" else ""}")
            }
        }
        val displayText = when {
            detailParts.isNotEmpty() -> detailParts.joinToString("   ·   ")
            safeTelemetry.main in 0..100 ->
                if (isWearableDevice) "Pin thiết bị: ${safeTelemetry.main}% · Đang kết nối"
                else "Pin tai nghe: ${safeTelemetry.main}% · Đang kết nối âm thanh"
            else ->
                if (isWearableDevice) "Thiết bị đeo · Đã kết nối"
                else "Tai nghe Bluetooth · Đã kết nối"
        }

        val hasDetailedBuds = !isWearableDevice && (safeTelemetry.left in 0..100 || safeTelemetry.right in 0..100)
        val hasDualBuds = !isWearableDevice && (safeTelemetry.left in 0..100 && safeTelemetry.right in 0..100)
        val hasLeft = !isWearableDevice && safeTelemetry.left in 0..100
        val hasRight = !isWearableDevice && safeTelemetry.right in 0..100
        val hasSingleBud = hasDetailedBuds && !hasDualBuds
        val leftContent = when {
            hasDualBuds -> "${safeTelemetry.left}%"
            hasLeft -> "${safeTelemetry.left}%"
            hasRight -> "${safeTelemetry.right}%"
            known -> "${effectivePct}%"
            else -> ""
        }
        val pillStatus = when {
            hasDualBuds -> "${safeTelemetry.right}%"
            hasSingleBud -> ""
            !isWearableDevice && safeTelemetry.case in 0..100 -> "Case ${safeTelemetry.case}%"
            known -> "${effectivePct}%"
            else -> "Đã kết nối"
        }

        val effectiveShowMs = resolveShowMs(context)
        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", cardId)
            putExtra("is_ongoing", false)
            putExtra("oi_auto_dismiss_ms", effectiveShowMs)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("subtext", "")
            putExtra("oi_left_content", leftContent)
            putExtra("oi_left_icon", safeIcon)
            putExtra("oi_large_icon", safeIcon)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_right_content", pillStatus)
            putExtra("status_chip_text", pillStatus)
            // Khi ấn vào viên nang Đảo sẽ mở rộng thẻ (ISLAND_CLICK_SHOW_CARD)
            putExtra("oi_island_click_expand", true)

            if (hasDualBuds) {
                // Đảo thu nhỏ khi có cả 2 tai nghe:
                // Biểu tượng tai nghe 3D thuần khiết + % pin ở mỗi bên
                val leftBudIcon = BatteryCanvasRenderer.createSingleEarbudIcon(context, isLeft = true)
                val rightBudIcon = BatteryCanvasRenderer.createSingleEarbudIcon(context, isLeft = false)
                val baseHeaderIcon = BatteryCanvasRenderer.createTwsHeaderBadgeIcon(context)

                val hasCase = safeTelemetry.case in 0..100
                val centerIcon = if (hasCase) {
                    BatteryCanvasRenderer.createTwsExpandedCompositeIcon(
                        context,
                        safeTelemetry.left,
                        safeTelemetry.right,
                        safeTelemetry.case,
                    )
                } else {
                    createTransparentIcon()
                }

                val caseCharge = if (safeTelemetry.isCaseCharging) " ⚡" else ""
                val midText = if (hasCase) "Hộp sạc ${safeTelemetry.case}%$caseCharge" else ""

                val extra1 = ""
                val extra2 = "Trái ${safeTelemetry.left}%${if (safeTelemetry.isLeftCharging) " ⚡" else ""}"
                val extra3 = ""
                val extra4 = "Phải ${safeTelemetry.right}%${if (safeTelemetry.isRightCharging) " ⚡" else ""}"

                putExtra("oi_left_icon", leftBudIcon)
                putExtra("oi_left_content", "${safeTelemetry.left}%")
                putExtra("oi_right_icon", rightBudIcon)
                putExtra("oi_right_content", "${safeTelemetry.right}%")
                putExtra("status_chip_text", "${safeTelemetry.right}%")
                putExtra("oi_show_right_icon", true)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
                putExtra("oi_fg_color", "#FFFFFFFF")

                // Thẻ mở rộng đối xứng 3 cột chuẩn OriginOS 6 (TEMPLATE_TEXT_SYMMETRY = 3)
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_TEXT_SYMMETRY) // 3
                putExtra("oi_base_icon", baseHeaderIcon)
                putExtra("oi_large_icon", centerIcon)
                putExtra("oi_mid_title", "")
                putExtra("oi_mid_text", midText)
                putExtra("title", "${name.take(20)} · Đã kết nối")
                putExtra("text", if (lowWarning) "Pin yếu · Cần sạc" else "Tai nghe Bluetooth")
                putExtra("subtext", "Đã kết nối")
                putExtra("oi_extra1", extra1)
                putExtra("oi_extra2", extra2)
                putExtra("oi_extra3", extra3)
                putExtra("oi_extra4", extra4)
            } else if (hasSingleBud) {
                // Khi chỉ lấy 1 bên tai nghe ra (hasSingleBud: chỉ có left HOẶC chỉ có right)
                val isLeftOnly = hasLeft
                val singleLevel = if (isLeftOnly) safeTelemetry.left else safeTelemetry.right
                val isSingleCharging = if (isLeftOnly) safeTelemetry.isLeftCharging else safeTelemetry.isRightCharging
                val singleSideName = if (isLeftOnly) "Tai trái" else "Tai phải"
                val hasCase = safeTelemetry.case in 0..100

                val singleBudIcon = BatteryCanvasRenderer.createSingleEarbudIcon(context, isLeft = isLeftOnly)
                val baseHeaderIcon = BatteryCanvasRenderer.createTwsHeaderBadgeIcon(context)

                // Đảo thu nhỏ: chỉ hiện bên đang đeo
                putExtra("oi_left_icon", singleBudIcon)
                putExtra("oi_left_content", "${singleLevel}%")
                putExtra("oi_show_right_icon", false)
                putExtra("oi_right_content", "")
                putExtra("status_chip_text", "")
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
                putExtra("oi_fg_color", "#FFFFFFFF")

                // Thẻ mở rộng: TEMPLATE_BASE (4)
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
                putExtra("oi_base_icon", baseHeaderIcon)
                putExtra("oi_large_icon", singleBudIcon)
                putExtra("title", name.take(20))
                putExtra(
                    "text",
                    if (isSingleCharging) "$singleSideName ${singleLevel}% ⚡ · Đang đeo"
                    else "$singleSideName ${singleLevel}% · Đang đeo"
                )
                putExtra("subtext", if (hasCase) "Hộp sạc ${safeTelemetry.case}%" else "Đã kết nối")
                putExtra("oi_card_ring", singleLevel)
            } else {
                // Thiết bị đơn / Thiết bị đeo: Dùng TEMPLATE_BASE (4) kèm vòng tròn tiến độ pin cardRing
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
                putExtra("title", if (lowWarning) "Pin yếu · $displayTitle" else displayTitle)
                putExtra("text", displayText)
                if (known) {
                    putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS) // 2
                    putExtra("oi_progress", effectivePct.coerceIn(0, 100))
                    putExtra("oi_fg_color", if (isLow) RED else GREEN)
                    putExtra("oi_bg_color", if (isLow) RED_TRACK else GREEN_TRACK)
                    putExtra("oi_card_ring", effectivePct)
                } else {
                    putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
                    putExtra("oi_show_right_icon", false)
                }
            }

            // Viền xanh #00D084 khi kết nối thành công, đỏ khi pin yếu
            putExtra("oi_light_color", if (lowWarning) RED else GREEN)
            putExtra("click_resp", click)
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)
        }

        if (!isWearable) {
            lastDisconnectCardShownMs = System.currentTimeMillis()
        }
        context.startService(intent)
        val shownKey = device?.address ?: name
        if (isWearableDevice) {
            showingWearableAddress = shownKey
            if (refreshDuration || hideWearableTask == null) {
                hideWearableTask?.let(handler::removeCallbacks)
                hideWearableTask = Runnable { hide(context, isWearable = true) }.also { handler.postDelayed(it, effectiveShowMs) }
            }
        } else {
            showingEarbudsAddress = shownKey
            if (refreshDuration || hideEarbudsTask == null) {
                hideEarbudsTask?.let(handler::removeCallbacks)
                hideEarbudsTask = Runnable { hide(context, isWearable = false) }.also { handler.postDelayed(it, effectiveShowMs) }
            }
        }
    }

    private fun createTransparentIcon(): Icon =
        Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

    /**
     * Hiển thị thẻ Mất kết nối Bluetooth với huy hiệu chữ "Ngắt" đồng bộ định dạng với khi kết nối, viền đỏ #FF5247.
     * Tự động đóng sau 3000ms.
     */
    fun showDisconnected(context: Context, name: String, isWearable: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        if (!prefs.getBoolean("cast_earbuds", true)) return
        if (!isWearable && System.currentTimeMillis() - lastDisconnectCardShownMs < 5000L) {
            return
        }
        if (isDeviceHardwareConnected(context, name, isWearable = isWearable) ||
            (isWearable && isWithinAntiFlapCooldown(name))) {
            setConnectedState(name, true)
            return
        }

        val cardId = if (isWearable) ID_WEARABLE else ID_EARBUDS
        if (isWearable) {
            hideWearableTask?.let(handler::removeCallbacks)
        } else {
            hideEarbudsTask?.let(handler::removeCallbacks)
        }
        val glyph = if (isWearable) R.drawable.ic_smartband else R.drawable.ic_earbuds
        val safeIcon = createSafeIcon(context, glyph)
        IconCache.activeSmallIcons.remove(cardId)

        val click = PendingIntent.getActivity(
            context, cardId, Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val chipText = "Đã ngắt"

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", cardId)
            putExtra("is_ongoing", false)
            putExtra("oi_auto_dismiss_ms", DISCONNECT_SHOW_MS)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
            putExtra("title", "$name · Mất kết nối")
            putExtra("text", "Đã ngắt kết nối Bluetooth")
            putExtra("subtext", "")
            putExtra("oi_left_content", "")
            putExtra("oi_left_icon", safeIcon)
            putExtra("oi_large_icon", safeIcon)
            putExtra("oi_right_icon", safeIcon)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_show_right_icon", false)
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4 - Text thuần
            putExtra("oi_right_content", chipText)
            putExtra("oi_fg_color", "#FFFFFFFF")
            putExtra("oi_light_color", RED)
            putExtra("click_resp", click)
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)
        }

        if (!isWearable) {
            lastDisconnectCardShownMs = System.currentTimeMillis()
        }
        context.startService(intent)
        if (isWearable) {
            showingWearableAddress = null
            hideWearableTask = Runnable { hide(context, isWearable = true) }.also { handler.postDelayed(it, DISCONNECT_SHOW_MS) }
        } else {
            showingEarbudsAddress = null
            hideEarbudsTask = Runnable { hide(context, isWearable = false) }.also { handler.postDelayed(it, DISCONNECT_SHOW_MS) }
        }
    }

    /**
     * Cung cấp các hàm public tiện ích để NotificationCastListener có thể gọi trực tiếp.
     */
    fun onDeviceConnected(context: Context, name: String, battery: Int = -1, isWearable: Boolean = false) {
        val ctx = context.applicationContext
        if (!enabled(ctx)) return
        disconnectDebounce.remove(name)?.let(handler::removeCallbacks)
        val alreadyConnected = isConnected(name)
        setConnectedState(name, true)

        val targetDevice: BluetoothDevice? = findBluetoothDevice(ctx, name)

        var effectiveBattery = battery
        if (effectiveBattery !in 0..100 && targetDevice != null) {
            val devBattery = batteryOf(targetDevice)
            if (devBattery in 0..100) {
                effectiveBattery = devBattery
            }
        }
        if (effectiveBattery !in 0..100) {
            val cached = getCachedBattery(ctx, name)
            if (cached in 0..100) {
                effectiveBattery = cached
            }
        }
        if (effectiveBattery in 0..100) {
            saveCachedBattery(ctx, name, effectiveBattery)
            targetDevice?.address?.let { saveCachedBattery(ctx, it, effectiveBattery) }
        } else if (targetDevice != null) {
            readGattBatteryAsync(ctx, targetDevice, name, isWearable)
        }

        val incomingTelemetry = if (effectiveBattery in 0..100) {
            EarbudsTelemetry(main = effectiveBattery)
        } else {
            EarbudsTelemetry()
        }
        var telemetry = storeTelemetry(ctx, targetDevice?.address, name, incomingTelemetry)

        if (!isWearable) {
            val activeNotifTelemetry = scanActiveNotificationsForEarbuds(ctx, name)
            if (activeNotifTelemetry != null) {
                telemetry = storeTelemetry(ctx, targetDevice?.address, name, activeNotifTelemetry)
            }
        }

        if (alreadyConnected) {
            // Đã kết nối trước đó: chỉ cập nhật hiển thị nếu đang mở trên Đảo hoặc pin yếu mới
            val currentlyShowing = isShowing(isWearable, name)
            val isNewLow = telemetry.effectiveLevel in 0..LOW_LEVEL && name !in warnedLow
            if (isNewLow) warnedLow += name
            if (currentlyShowing || isNewLow) {
                show(
                    context = ctx,
                    device = targetDevice,
                    customName = name,
                    telemetry = telemetry,
                    low = (telemetry.effectiveLevel in 0..LOW_LEVEL),
                    isWearable = isWearable,
                    refreshDuration = !currentlyShowing,
                )
            }
            return
        }

        // Chuyển từ ngắt kết nối -> kết nối: Hiển thị đảo
        show(
            context = ctx,
            device = targetDevice,
            customName = name,
            telemetry = telemetry,
            low = (telemetry.effectiveLevel in 0..LOW_LEVEL),
            isWearable = isWearable,
        )
    }

    fun onDeviceDisconnected(context: Context, name: String, isWearable: Boolean = false) {
        val ctx = context.applicationContext
        if (!enabled(ctx)) return
        disconnectDebounce.remove(name)?.let(handler::removeCallbacks)
        val wasConnected = isConnected(name) ||
            (!isWearable && name.equals(lastConnectedAudioDeviceName, ignoreCase = true))
        if (!wasConnected) {
            // Thiết bị chưa từng kết nối hoặc đã ở trạng thái ngắt, không hiển thị pop-up ảo
            return
        }
        if (!isWearable && System.currentTimeMillis() - lastDisconnectCardShownMs < 5000L) {
            return
        }
        val task = Runnable {
            disconnectDebounce.remove(name)
            if (isDeviceHardwareConnected(ctx, name, isWearable = isWearable)) {
                setConnectedState(name, true)
                return@Runnable
            }
            if (isWearable && isWithinAntiFlapCooldown(name)) {
                Log.i(TAG, "Anti-flap: skip onDeviceDisconnected for $name within cooldown")
                setConnectedState(name, true)
                return@Runnable
            }
            setConnectedState(name, false)
            deviceTelemetry -= name
            warnedLow -= name
            showDisconnected(ctx, name, isWearable)
        }
        disconnectDebounce[name] = task
        handler.postDelayed(task, disconnectDebounceFor(isWearable))
    }

    /**
     * Bắt sự kiện hệ thống xóa thông báo pin tai nghe / Google Fast Pair khi đóng nắp hộp sạc.
     */
    fun onFastPairNotificationRemoved(rawTitle: String, rawText: String) {
        val ctx = app ?: return
        if (!enabled(ctx)) return
        val extracted = extractDeviceNameFromNotificationText(rawTitle, rawText, isWearable = false)
            .takeIf { it.isNotBlank() && it != "Tai nghe" }
        val targetName = extracted ?: lastConnectedAudioDeviceName ?: "Tai nghe"
        if (isWearable(null, targetName)) return

        handler.postDelayed({
            val currentCtx = app ?: return@postDelayed
            if (!enabled(currentCtx)) return@postDelayed
            val stillConnected = isDeviceHardwareConnected(currentCtx, targetName, isWearable = false) ||
                (lastConnectedAudioDeviceAddress?.let {
                    isDeviceHardwareConnected(currentCtx, it, isWearable = false)
                } == true)
            if (!stillConnected && System.currentTimeMillis() - lastDisconnectCardShownMs >= 5000L) {
                setConnectedState(targetName, false)
                lastConnectedAudioDeviceAddress?.let { setConnectedState(it, false) }
                deviceTelemetry -= targetName
                deviceTelemetry -= targetName.lowercase()
                showDisconnected(currentCtx, targetName, isWearable = false)
            }
        }, 1500L)
    }

    fun onBatteryUpdated(context: Context, name: String, telemetry: EarbudsTelemetry, isWearable: Boolean = false) {
        val ctx = context.applicationContext
        if (!enabled(ctx)) return
        disconnectDebounce.remove(name)?.let(handler::removeCallbacks)
        val isWearableDevice = isWearable || isWearable(null, name)
        val before = getStoredTelemetry(ctx, null, name, isWearable = isWearableDevice)
        val hadDetailsBefore = before.hasDetails
        val merged = storeTelemetry(ctx, null, name, telemetry, isWearable = isWearableDevice)
        val eff = merged.effectiveLevel
        if (eff in 0..100) {
            saveCachedBattery(ctx, name, eff)
        }
        val currentlyShowing = isShowing(isWearableDevice, name)
        val newlyDiscoveredDetails = !hadDetailsBefore && merged.hasDetails && isWithinAntiFlapCooldown(name)
        val isNewLowWarning = eff in 0..LOW_LEVEL && before.effectiveLevel > LOW_LEVEL && name !in warnedLow
        if (isNewLowWarning) warnedLow += name
        if (eff > LOW_LEVEL + 5) warnedLow -= name

        // Cập nhật hiển thị nếu Đang hiển thị trên đảo (và có thay đổi), vừa phát hiện thêm chi tiết L/R/Case mới trong cooldown, hoặc pin tụt xuống mức cảnh báo yếu
        if ((currentlyShowing && before != merged) || newlyDiscoveredDetails || isNewLowWarning) {
            show(
                context = ctx,
                device = null,
                customName = name,
                telemetry = merged,
                low = (eff in 0..LOW_LEVEL),
                isWearable = isWearableDevice,
                refreshDuration = !currentlyShowing,
            )
        }
    }

    /**
     * Phát thẻ mẫu Tai nghe TWS đa thành phần (Tai Trái 85%, Tai Phải 90%, Hộp sạc 50%) chuẩn poster Vivo TWS trên OriginOS 6
     * để kiểm thử trực tiếp viên nang L/R và thẻ mở rộng 3 ô khi ấn vào Đảo.
     */
    fun postSample(context: Context) {
        val ctx = context.applicationContext
        val sampleName = "vivo TWS"
        val sampleTelemetry = EarbudsTelemetry(
            main = 85,
            left = 85,
            right = 90,
            case = 50,
            isLeftCharging = false,
            isRightCharging = false,
            isCaseCharging = false,
        )
        storeTelemetry(ctx, null, sampleName, sampleTelemetry)
        show(
            context = ctx,
            device = null,
            customName = sampleName,
            telemetry = sampleTelemetry,
            low = false,
            isWearable = false,
        )
    }

    /**
     * Hiển thị thẻ Báo sạc đầy pin 100% cho thiết bị Samsung/tai nghe Bluetooth.
     * Tự động thu gọn sau 3500ms.
     */
    fun onDeviceFullyCharged(context: Context, name: String, isWearable: Boolean = false) {
        val ctx = context.applicationContext
        if (!enabled(ctx)) return
        val cardId = if (isWearable) ID_WEARABLE else ID_EARBUDS
        if (isWearable) {
            hideWearableTask?.let(handler::removeCallbacks)
        } else {
            hideEarbudsTask?.let(handler::removeCallbacks)
        }

        val glyph = if (isWearable) R.drawable.ic_smartband else R.drawable.ic_earbuds
        val safeIcon = createSafeIcon(ctx, glyph)
        IconCache.activeSmallIcons.remove(cardId)

        val click = PendingIntent.getActivity(
            ctx, cardId, Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val batteryFullIcon = BatteryCanvasRenderer.createBatteryIcon(ctx, 100, isCharging = false)

        val intent = Intent(ctx, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", cardId)
            putExtra("is_ongoing", false)
            putExtra("oi_auto_dismiss_ms", SHOW_MS)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
            putExtra("title", "$name · Đã sạc đầy")
            putExtra("text", "Pin 100% · Có thể ngắt sạc")
            putExtra("subtext", "Pin 100% ⚡ Đã sạc đầy")
            putExtra("oi_left_content", "100%")
            putExtra("oi_left_icon", safeIcon)
            putExtra("oi_large_icon", safeIcon)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_right_icon", batteryFullIcon)
            putExtra("oi_show_right_icon", true)
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS) // 2
            putExtra("oi_progress", 100)
            putExtra("oi_fg_color", GREEN)
            putExtra("oi_bg_color", GREEN_TRACK)
            putExtra("oi_right_content", "100%")
            putExtra("status_chip_text", "100%")
            putExtra("oi_card_ring", 100)
            putExtra("oi_light_color", GREEN)
            putExtra("click_resp", click)
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)
        }

        ctx.startService(intent)
        if (isWearable) {
            showingWearableAddress = name
            hideWearableTask = Runnable { hide(ctx, isWearable = true) }.also { handler.postDelayed(it, 3500L) }
        } else {
            showingEarbudsAddress = name
            hideEarbudsTask = Runnable { hide(ctx, isWearable = false) }.also { handler.postDelayed(it, 3500L) }
        }
    }

    fun hide(context: Context, isWearable: Boolean? = null) {
        if (isWearable == null || !isWearable) {
            hideEarbudsTask?.let(handler::removeCallbacks)
            hideEarbudsTask = null
            showingEarbudsAddress = null
            IconCache.activeSmallIcons.remove(ID_EARBUDS)
            context.startService(
                Intent(context, PlaygroundService::class.java)
                    .setAction(PlaygroundService.ACTION_CANCEL)
                    .putExtra("id", ID_EARBUDS)
                    .putExtra("oi_scene", "NAVIGATION"),
            )
        }
        if (isWearable == null || isWearable) {
            hideWearableTask?.let(handler::removeCallbacks)
            hideWearableTask = null
            showingWearableAddress = null
            IconCache.activeSmallIcons.remove(ID_WEARABLE)
            context.startService(
                Intent(context, PlaygroundService::class.java)
                    .setAction(PlaygroundService.ACTION_CANCEL)
                    .putExtra("id", ID_WEARABLE)
                    .putExtra("oi_scene", "NAVIGATION"),
            )
        }
    }
}
