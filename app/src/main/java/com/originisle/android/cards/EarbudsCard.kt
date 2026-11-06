package com.originisle.android.cards

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
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

    const val ID = 35004

    private const val BATTERY_WAIT_MS = 2500L
    private const val SHOW_MS = 5000L
    private const val DISCONNECT_DEBOUNCE_MS = 8000L
    private const val DISCONNECT_SHOW_MS = 3000L
    private const val ANTI_FLAP_COOLDOWN_MS = 10_000L
    private const val LOW_LEVEL = 15

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

    private val handler = Handler(Looper.getMainLooper())
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
    private var showingAddress: String? = null
    private var hideTask: Runnable? = null

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
     * Quét qua GATT, GATT_SERVER, A2DP, HEADSET và bondedDevices.
     */
    fun isDeviceHardwareConnected(context: Context, nameOrAddress: String?): Boolean {
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

            val profiles = intArrayOf(
                BluetoothProfile.GATT,
                BluetoothProfile.GATT_SERVER,
                BluetoothProfile.A2DP,
                BluetoothProfile.HEADSET,
            )

            for (p in profiles) {
                val connectedDevices = runCatching { btManager.getConnectedDevices(p) }.getOrNull()
                if (connectedDevices != null && connectedDevices.any { matchesDevice(it) }) {
                    return@runCatching true
                }
            }

            val bonded = runCatching { adapter.bondedDevices }.getOrNull().orEmpty()
            val isConnectedMethod = runCatching { BluetoothDevice::class.java.getMethod("isConnected") }.getOrNull()

            for (dev in bonded) {
                if (matchesDevice(dev)) {
                    for (p in profiles) {
                        val state = runCatching { btManager.getConnectionState(dev, p) }.getOrDefault(-1)
                        if (state == BluetoothProfile.STATE_CONNECTED) {
                            return@runCatching true
                        }
                    }
                    if (isConnectedMethod != null) {
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
                if (k == "wearable_last_known_battery") continue
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

    private val FIT3_BATTERY_URI = Uri.parse("content://com.samsung.wearable.providers.find.batteryproviderhelper/BatteryDBProvider/Battery_tb")
    private val FIT3_SYNC_URI = Uri.parse("content://com.samsung.wearable.fit3plugin.BatteryInfoProvider")
    private val FIT3_CAPSULE_URI = Uri.parse("content://com.samsung.wearable.fit3plugin.CapsuleProvider")
    private val FIT3_QUICKSEARCH_URI = Uri.parse("content://com.samsung.wearable.fit3plugin.quicksearch")
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
        if (showingAddress != null) hide(ctx)
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

    private fun handle(intent: Intent) {
        val context = app ?: return
        val action = intent.action ?: return

        var device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        if (device == null) {
            device = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE", BluetoothDevice::class.java)
        }

        val rawAddress = intent.getStringExtra("address")
            ?: intent.getStringExtra("device_address")
            ?: intent.getStringExtra("mac")
            ?: intent.getStringExtra("BLUETOOTH_DEVICE_ADDRESS")
            ?: extractSamsungDeviceAddress(intent)
            ?: showingAddress

        if (device == null && !rawAddress.isNullOrBlank()) {
            device = runCatching {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                adapter?.getRemoteDevice(rawAddress)
            }.getOrNull()
        }

        val pkg = intent.component?.packageName ?: intent.`package`
        val rawName = intent.getStringExtra("name")
            ?: intent.getStringExtra("device_name")
            ?: intent.getStringExtra("deviceName")
            ?: intent.getStringExtra("device_title")
            ?: extractSamsungDeviceName(intent)

        val isWearableDevice = isWearable(device, rawName, pkg) || isSamsungWearableAction(action)
        if (!isSupportedDevice(device, rawName, pkg) && !isSamsungWearableAction(action)) return

        val address = device?.address ?: rawAddress ?: rawName ?: "device_unknown"
        val devName = rawName ?: runCatching { device?.alias ?: device?.name }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: if (isWearableDevice) "Galaxy Fit 3" else "Tai nghe"

        if (isWearableDevice) {
            wearableRegistry.add(address)
            wearableRegistry.add(devName)
        }

        when {
            // --- KẾT NỐI (CONNECTED) ---
            action == BluetoothDevice.ACTION_ACL_CONNECTED ||
            action == ACTION_SAMSUNG_FIT_CONNECTED ||
            action == ACTION_SAMSUNG_WEARABLE_CONNECTED ||
            isSamsungAccessoryConnected(intent) -> {
                disconnectDebounce.remove(address)?.let(handler::removeCallbacks)
                disconnectDebounce.remove(devName)?.let(handler::removeCallbacks)
                warnedLow -= address
                pending.remove(address)?.let(handler::removeCallbacks)

                setConnectedState(address, true)
                setConnectedState(devName, true)

                var initial = readTelemetry(device, intent)
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
                if (initial.effectiveLevel in 0..100) {
                    saveCachedBattery(context, devName, initial.effectiveLevel)
                    deviceTelemetry[address] = initial
                    show(context, device, devName, initial, low = (initial.effectiveLevel in 0..LOW_LEVEL), isWearable = isWearableDevice)
                } else {
                    val task = Runnable {
                        pending.remove(address)
                        var telemetry = readTelemetry(device, intent)
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
                        if (telemetry.effectiveLevel in 0..100) {
                            saveCachedBattery(context, devName, telemetry.effectiveLevel)
                        }
                        deviceTelemetry[address] = telemetry
                        show(context, device, devName, telemetry, low = (telemetry.effectiveLevel in 0..LOW_LEVEL), isWearable = isWearableDevice)
                    }
                    pending[address] = task
                    handler.postDelayed(task, BATTERY_WAIT_MS)
                }
            }

            // --- MẤT KẾT NỐI (DISCONNECTED) VỚI DEBOUNCE 8000MS & XÁC THỰC PHẦN CỨNG ---
            action == BluetoothDevice.ACTION_ACL_DISCONNECTED ||
            action == ACTION_SAMSUNG_FIT_DISCONNECTED ||
            action == ACTION_SAMSUNG_WEARABLE_DISCONNECTED ||
            isSamsungAccessoryDisconnected(intent) -> {
                val wasConnected = isConnected(address) || isConnected(devName)
                pending.remove(address)?.let(handler::removeCallbacks)
                disconnectDebounce.remove(address)?.let(handler::removeCallbacks)
                disconnectDebounce.remove(devName)?.let(handler::removeCallbacks)

                if (!wasConnected) {
                    return
                }

                val disconnectTask = Runnable {
                    disconnectDebounce.remove(address)
                    disconnectDebounce.remove(devName)
                    if (isDeviceHardwareConnected(context, devName) || isDeviceHardwareConnected(context, address)) {
                        setConnectedState(address, true)
                        setConnectedState(devName, true)
                        return@Runnable
                    }
                    if (isWithinAntiFlapCooldown(address) || isWithinAntiFlapCooldown(devName)) {
                        Log.i(TAG, "Anti-flap: skip disconnect popup for $devName within cooldown")
                        setConnectedState(address, true)
                        setConnectedState(devName, true)
                        return@Runnable
                    }
                    setConnectedState(address, false)
                    setConnectedState(devName, false)
                    deviceTelemetry -= address
                    deviceTelemetry -= devName
                    warnedLow -= address
                    warnedLow -= devName
                    showDisconnected(context, devName, isWearable = isWearableDevice)
                }
                disconnectDebounce[address] = disconnectTask
                disconnectDebounce[devName] = disconnectTask
                handler.postDelayed(disconnectTask, DISCONNECT_DEBOUNCE_MS)
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
                        val current = deviceTelemetry[address] ?: EarbudsTelemetry()
                        val updated = current.copy(
                            main = parsed.first,
                            isLeftCharging = if (parsed.second) true else current.isLeftCharging,
                            isRightCharging = if (parsed.second) true else current.isRightCharging,
                        )
                        if (updated.effectiveLevel in 0..100) {
                            saveCachedBattery(context, devName, updated.effectiveLevel)
                        }
                        deviceTelemetry[address] = updated
                        show(context, device, devName, updated, low = (updated.effectiveLevel in 0..LOW_LEVEL), isWearable = isWearableDevice)
                    }
                }
            }

            // --- CẬP NHẬT PIN (BATTERY UPDATED / CHANGED) ---
            else -> {
                disconnectDebounce.remove(address)?.let(handler::removeCallbacks)
                disconnectDebounce.remove(devName)?.let(handler::removeCallbacks)

                val updated = readTelemetry(device, intent)
                if (updated.effectiveLevel in 0..100) {
                    saveCachedBattery(context, devName, updated.effectiveLevel)
                    if (isWearableDevice) {
                        onBatteryUpdated(context, devName, updated, isWearable = true)
                    }
                }
                val before = deviceTelemetry.put(address, updated)
                val eff = updated.effectiveLevel

                when {
                    pending.containsKey(address) -> {
                        pending.remove(address)?.let(handler::removeCallbacks)
                        show(context, device, devName, updated, low = eff in 0..LOW_LEVEL, isWearable = isWearableDevice)
                    }
                    showingAddress == address -> {
                        show(context, device, devName, updated, low = eff in 0..LOW_LEVEL, isWearable = isWearableDevice)
                    }
                    eff in 0..LOW_LEVEL && (before?.effectiveLevel ?: 100) > LOW_LEVEL && address !in warnedLow -> {
                        warnedLow += address
                        show(context, device, devName, updated, low = true, isWearable = isWearableDevice)
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
     * Bóc tách thông số pin Trái, Phải, Hộp sạc, Thiết bị đeo thật từ thông báo hệ thống.
     */
    fun tryParseBatteryFromNotification(context: Context, sbn: StatusBarNotification): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        if (!prefs.getBoolean("cast_earbuds", true)) return false

        val extras = sbn.notification.extras ?: return false
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
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
            "com.samsung.android.app.watchmanager",
            "com.samsung.android.wearable.fit3plugin",
            "com.samsung.android.wearable.watch6plugin",
            "com.samsung.android.wearable.gearfit2plugin",
            "com.samsung.android.geargplugin",
            "com.samsung.accessory",
            "com.huawei.health",
            "com.xiaomi.wearable",
        )
        val hasKeyword = (AUDIO_KEYWORDS + WEARABLE_KEYWORDS).any { fullText.contains(it, ignoreCase = true) }

        if (!isTwsPkg && !hasKeyword) return false
        if (!fullText.contains("%")) return false

        val leftRegex = Regex("""(?:L|Left|Trái|Tai trái)\s*[:：\-]?\s*(\d{1,3})\s*%""", RegexOption.IGNORE_CASE)
        val rightRegex = Regex("""(?:R|Right|Phải|Tai phải)\s*[:：\-]?\s*(\d{1,3})\s*%""", RegexOption.IGNORE_CASE)
        val caseRegex = Regex("""(?:Case|Box|C|Hộp|Hộp sạc|Dock)\s*[:：\-]?\s*(\d{1,3})\s*%""", RegexOption.IGNORE_CASE)
        val mainRegex = Regex("""(?:pin|battery|tai nghe|fit|watch|band)\s*[:：\-]?\s*(\d{1,3})\s*%""", RegexOption.IGNORE_CASE)

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

        val isWearableDevice = isWearable(null, title, pkg) ||
            listOf("fit", "watch", "band", "vòng đeo tay", "đồng hồ").any { fullText.contains(it, ignoreCase = true) }

        var devName = title.ifBlank { if (isWearableDevice) "Galaxy Fit 3" else "Tai nghe TWS" }
            .replace(Regex("""\s*\(?\d{1,3}%\)?"""), "")
            .trim()
        if (devName.isBlank() || devName.equals("Bluetooth", ignoreCase = true)) {
            devName = if (isWearableDevice) "Thiết bị đeo" else "Tai nghe Bluetooth"
        }

        if (telemetry.effectiveLevel in 0..100) {
            saveCachedBattery(context, devName, telemetry.effectiveLevel)
        }
        show(
            context = context,
            device = null,
            customName = devName,
            telemetry = telemetry,
            low = (telemetry.effectiveLevel in 0..LOW_LEVEL),
            isWearable = isWearableDevice,
        )
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

        val targetDevice = connectedDevices.firstOrNull { isAudio(it) || isWearable(it) } ?: connectedDevices.firstOrNull()
        if (targetDevice != null) {
            val address = targetDevice.address
            val cached = deviceTelemetry[address]
            val batteryLevel = batteryOf(targetDevice)
            val isWearableDevice = isWearable(targetDevice)
            val telemetry = if (cached != null && cached.effectiveLevel in 0..100) {
                cached
            } else {
                val read = readTelemetry(targetDevice, null)
                if (read.effectiveLevel in 0..100) read
                else if (batteryLevel in 0..100) read.copy(main = batteryLevel)
                else read
            }
            deviceTelemetry[address] = telemetry
            setConnectedState(address, true)
            runCatching { targetDevice.name }.getOrNull()?.let { if (it.isNotBlank()) setConnectedState(it, true) }
            runCatching { targetDevice.alias }.getOrNull()?.let { if (it.isNotBlank()) setConnectedState(it, true) }
            show(
                context = context,
                device = targetDevice,
                customName = null,
                telemetry = telemetry,
                low = (telemetry.effectiveLevel in 0..LOW_LEVEL),
                isWearable = isWearableDevice,
            )
            return true
        }

        return false
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

        val known = effectivePct in 0..100
        val isLow = known && effectivePct <= LOW_LEVEL

        val activeTelemetry = if (known && telemetry.effectiveLevel !in 0..100) {
            telemetry.copy(main = effectivePct)
        } else {
            telemetry
        }

        if (known) {
            saveCachedBattery(context, name, effectivePct)
            (targetDev ?: device)?.address?.let { saveCachedBattery(context, it, effectivePct) }
        }

        val glyph = when {
            isWearableDevice -> R.drawable.ic_smartband
            device != null && isOverEar(device, name) -> R.drawable.ic_headphones
            else -> R.drawable.ic_earbuds
        }
        val safeIcon = createSafeIcon(context, glyph)
        IconCache.activeSmallIcons.remove(ID)

        val click = PendingIntent.getActivity(
            context, ID, Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // Dòng 1: oi_title
        val displayTitle = if (activeTelemetry.case in 1..100) {
            "${name.take(16)} · Hộp: ${activeTelemetry.case}%${if (activeTelemetry.isCaseCharging) "⚡" else ""}"
        } else if (known) {
            "${name.take(18)} · ${effectivePct}%"
        } else {
            name.take(24)
        }

        // Dòng 2: oi_text
        val displayText = if (activeTelemetry.left in 0..100 && activeTelemetry.right in 0..100) {
            val lText = "${activeTelemetry.left}%${if (activeTelemetry.isLeftCharging) "⚡" else ""}"
            val rText = "${activeTelemetry.right}%${if (activeTelemetry.isRightCharging) "⚡" else ""}"
            "Trái (L): $lText   ·   Phải (R): $rText"
        } else if (activeTelemetry.left in 0..100) {
            val lText = "${activeTelemetry.left}%${if (activeTelemetry.isLeftCharging) "⚡" else ""}"
            "Trái (L): $lText · Đang kết nối"
        } else if (activeTelemetry.right in 0..100) {
            val rText = "${activeTelemetry.right}%${if (activeTelemetry.isRightCharging) "⚡" else ""}"
            "Phải (R): $rText · Đang kết nối"
        } else if (activeTelemetry.main in 0..100) {
            if (isWearableDevice) "Pin thiết bị: ${activeTelemetry.main}% · Đang kết nối"
            else "Pin tai nghe: ${activeTelemetry.main}% · Đang kết nối âm thanh"
        } else {
            if (isWearableDevice) "Thiết bị đeo · Đã kết nối"
            else "Tai nghe Bluetooth · Đã kết nối"
        }

        val displaySubtext = if (known) {
            "${if (isWearableDevice) "Thiết bị đeo" else "Tai nghe Bluetooth"} · Pin $effectivePct%"
        } else {
            if (isWearableDevice) "Thiết bị đeo · Đã kết nối" else "Tai nghe Bluetooth · Đã kết nối"
        }

        // Xây dựng danh sách slot pin co giãn thích ứng
        val slots = mutableListOf<BatterySlot>()
        if (activeTelemetry.left in 0..100) {
            slots.add(BatterySlot(shortLabel = "L", fullLabel = "Tai Trái: ${activeTelemetry.left}%", level = activeTelemetry.left, isCharging = activeTelemetry.isLeftCharging))
        }
        if (activeTelemetry.right in 0..100) {
            slots.add(BatterySlot(shortLabel = "R", fullLabel = "Tai Phải: ${activeTelemetry.right}%", level = activeTelemetry.right, isCharging = activeTelemetry.isRightCharging))
        }
        if (activeTelemetry.case in 0..100) {
            slots.add(BatterySlot(shortLabel = "Hộp", fullLabel = "Hộp sạc: ${activeTelemetry.case}%", level = activeTelemetry.case, isCharging = activeTelemetry.isCaseCharging))
        }
        if (slots.isEmpty() && activeTelemetry.main in 0..100) {
            val slotLabel = if (isWearableDevice) {
                if (name.contains("fit", ignoreCase = true)) "Fit 3" else "Pin"
            } else "Pin"
            slots.add(BatterySlot(shortLabel = slotLabel, fullLabel = "$slotLabel: ${activeTelemetry.main}%", level = activeTelemetry.main, isCharging = false))
        }

        val leftContent = if (known) "${effectivePct}%" else ""
        val pillStatus = if (known) "${effectivePct}%" else "Đã kết nối"

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("is_ongoing", true)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("title", if (low) "Pin yếu · $displayTitle" else displayTitle)
            putExtra("text", displayText)
            putExtra("subtext", displaySubtext)
            putExtra("oi_left_content", leftContent)
            putExtra("oi_left_icon", safeIcon)
            putExtra("oi_large_icon", safeIcon)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_right_content", pillStatus)
            putExtra("status_chip_text", pillStatus)

            // Tiến độ vòng tròn quanh camera selfie
            if (known) {
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS) // 2
                putExtra("oi_progress", effectivePct.coerceIn(0, 100))
                putExtra("oi_fg_color", if (isLow) RED else GREEN)
                putExtra("oi_bg_color", if (isLow) RED_TRACK else GREEN_TRACK)
            } else {
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
                putExtra("oi_show_right_icon", false)
            }

            // Bố cục Co giãn Thích ứng cho thẻ mở rộng (Expanded card)
            if (slots.isNotEmpty()) {
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS) // 8
                val actionsList = ArrayList<Notification.Action>()
                val buttonTitles = ArrayList<String>()
                for (slot in slots.take(3)) {
                    val icon = BatteryCanvasRenderer.createBatteryIcon(context, slot.level, slot.isCharging)
                    val btnTitle = "${slot.shortLabel} ${slot.level}%${if (slot.isCharging) "⚡" else ""}"
                    val action = Notification.Action.Builder(icon, btnTitle, click).build()
                    actionsList.add(action)
                    buttonTitles.add(btnTitle)
                }
                putParcelableArrayListExtra("actions", actionsList)
                putStringArrayListExtra("oi_button_titles", buttonTitles)
                if (slots.size == 1) {
                    putExtra("oi_card_ring", slots[0].level)
                }
            } else {
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
                if (known) putExtra("oi_card_ring", effectivePct)
            }

            // Viền xanh #00D084 khi kết nối thành công, đỏ khi pin yếu
            putExtra("oi_light_color", if (low) RED else GREEN)
            putExtra("click_resp", click)
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)
        }

        context.startService(intent)
        showingAddress = device?.address ?: name
        hideTask?.let(handler::removeCallbacks)
        hideTask = Runnable { hide(context) }.also { handler.postDelayed(it, SHOW_MS) }
    }

    /**
     * Hiển thị thẻ Mất kết nối Bluetooth với huy hiệu chữ "Ngắt" đồng bộ định dạng với khi kết nối, viền đỏ #FF5247.
     * Tự động đóng sau 3000ms.
     */
    fun showDisconnected(context: Context, name: String, isWearable: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        if (!prefs.getBoolean("cast_earbuds", true)) return
        if (isDeviceHardwareConnected(context, name) || isWithinAntiFlapCooldown(name)) {
            setConnectedState(name, true)
            return
        }

        hideTask?.let(handler::removeCallbacks)
        val glyph = if (isWearable) R.drawable.ic_smartband else R.drawable.ic_earbuds
        val safeIcon = createSafeIcon(context, glyph)
        IconCache.activeSmallIcons.remove(ID)

        val click = PendingIntent.getActivity(
            context, ID, Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val chipText = "Đã ngắt"

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("is_ongoing", false)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
            putExtra("title", "$name · Mất kết nối")
            putExtra("text", "Đã ngắt kết nối Bluetooth")
            putExtra("subtext", "Bluetooth · Đã ngắt kết nối")
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

        context.startService(intent)
        showingAddress = null
        hideTask = Runnable { hide(context) }.also { handler.postDelayed(it, DISCONNECT_SHOW_MS) }
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

        val telemetry = if (effectiveBattery in 0..100) {
            EarbudsTelemetry(main = effectiveBattery)
        } else {
            deviceTelemetry[name]?.takeIf { it.effectiveLevel in 0..100 } ?: EarbudsTelemetry()
        }
        deviceTelemetry[name] = telemetry

        if (alreadyConnected) {
            // Đã kết nối trước đó: chỉ cập nhật thông số ngầm, không kích hoạt pop-up đảo
            if (showingAddress == name || (telemetry.effectiveLevel in 0..LOW_LEVEL && name !in warnedLow)) {
                if (telemetry.effectiveLevel in 0..LOW_LEVEL) warnedLow += name
                show(
                    context = ctx,
                    device = targetDevice,
                    customName = name,
                    telemetry = telemetry,
                    low = (telemetry.effectiveLevel in 0..LOW_LEVEL),
                    isWearable = isWearable,
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
        val wasConnected = isConnected(name)
        if (!wasConnected) {
            // Thiết bị chưa từng kết nối hoặc đã ở trạng thái ngắt, không hiển thị pop-up ảo
            return
        }
        val task = Runnable {
            disconnectDebounce.remove(name)
            if (isDeviceHardwareConnected(ctx, name)) {
                setConnectedState(name, true)
                return@Runnable
            }
            if (isWithinAntiFlapCooldown(name)) {
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
        handler.postDelayed(task, DISCONNECT_DEBOUNCE_MS)
    }

    fun onBatteryUpdated(context: Context, name: String, telemetry: EarbudsTelemetry, isWearable: Boolean = false) {
        val ctx = context.applicationContext
        if (!enabled(ctx)) return
        disconnectDebounce.remove(name)?.let(handler::removeCallbacks)
        if (telemetry.effectiveLevel in 0..100) {
            saveCachedBattery(ctx, name, telemetry.effectiveLevel)
        }
        val before = deviceTelemetry.put(name, telemetry)
        val eff = telemetry.effectiveLevel
        // Chỉ cập nhật hiển thị nếu Đang hiển thị trên đảo, hoặc pin tụt xuống mức cảnh báo yếu
        if (showingAddress == name || (eff in 0..LOW_LEVEL && (before?.effectiveLevel ?: 100) > LOW_LEVEL && name !in warnedLow)) {
            if (eff in 0..LOW_LEVEL) warnedLow += name
            show(
                context = ctx,
                device = null,
                customName = name,
                telemetry = telemetry,
                low = (eff in 0..LOW_LEVEL),
                isWearable = isWearable,
            )
        }
    }

    /**
     * Hiển thị thẻ Báo sạc đầy pin 100% cho thiết bị Samsung/tai nghe Bluetooth.
     * Tự động thu gọn sau 3500ms.
     */
    fun onDeviceFullyCharged(context: Context, name: String, isWearable: Boolean = false) {
        val ctx = context.applicationContext
        if (!enabled(ctx)) return
        hideTask?.let(handler::removeCallbacks)

        val glyph = if (isWearable) R.drawable.ic_smartband else R.drawable.ic_earbuds
        val safeIcon = createSafeIcon(ctx, glyph)
        IconCache.activeSmallIcons.remove(ID)

        val click = PendingIntent.getActivity(
            ctx, ID, Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val batteryFullIcon = BatteryCanvasRenderer.createBatteryIcon(ctx, 100, isCharging = false)

        val intent = Intent(ctx, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("is_ongoing", true)
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
        showingAddress = name
        hideTask = Runnable { hide(ctx) }.also { handler.postDelayed(it, 3500L) }
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
