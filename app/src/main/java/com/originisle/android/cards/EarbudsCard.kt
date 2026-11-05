package com.originisle.android.cards

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.ui.PREFS_NAME

/**
 * Viễn trắc tai nghe Bluetooth chuẩn OriginOS 7:
 * - Tự động nhận diện tai nghe TWS khi kết nối hoặc mở nắp hộp sạc.
 * - Trích xuất viễn trắc mức pin đa thành phần: Tai Trái (L), Tai Phải (R), và Hộp sạc (Case),
 *   kèm trạng thái sạc (tia sét ⚡).
 * - Hiển thị viễn trắc sống động tức thì lên Đảo Origin.
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
            main in 0..100 -> main
            left in 0..100 && right in 0..100 -> (left + right) / 2
            left in 0..100 -> left
            right in 0..100 -> right
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
            // Lắng nghe thêm các broadcast mở rộng của Vivo / Google Fast Pair
            addAction("android.bluetooth.headset.action.HF_INDICATORS_VALUE_CHANGED")
            addAction("vivo.intent.action.BLUETOOTH_BATTERY")
            addAction("com.vivo.upslide.BLUETOOTH_BATTERY")
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
        val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
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
                    show(context, device, telemetry, low = false)
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
            else -> {
                // Pin thay đổi (BATTERY_LEVEL_CHANGED hoặc intent viễn trắc)
                val updated = readTelemetry(device, intent)
                val before = deviceTelemetry.put(address, updated)

                val eff = updated.effectiveLevel
                when {
                    pending.containsKey(address) -> {
                        pending.remove(address)?.let(handler::removeCallbacks)
                        show(context, device, updated, low = false)
                    }
                    showingAddress == address -> show(context, device, updated, low = eff in 0..LOW_LEVEL)
                    eff in 0..LOW_LEVEL && (before?.effectiveLevel ?: 100) > LOW_LEVEL && address !in warnedLow -> {
                        warnedLow += address
                        show(context, device, updated, low = true)
                    }
                }
            }
        }
    }

    /** Trích xuất viễn trắc chuyên sâu (L/R/Case) từ Metadata AOSP hoặc Intent Extras */
    private fun readTelemetry(device: BluetoothDevice, intent: Intent?): EarbudsTelemetry {
        var main = intent?.getIntExtra(EXTRA_BATTERY_LEVEL, -1) ?: -1
        if (main !in 0..100) main = batteryOf(device)

        var left = intent?.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL_LEFT", -1) ?: -1
        if (left !in 0..100) left = intent?.getIntExtra("left_battery", -1) ?: -1

        var right = intent?.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL_RIGHT", -1) ?: -1
        if (right !in 0..100) right = intent?.getIntExtra("right_battery", -1) ?: -1

        var case = intent?.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL_CASE", -1) ?: -1
        if (case !in 0..100) case = intent?.getIntExtra("case_battery", -1) ?: (intent?.getIntExtra("box_battery", -1) ?: -1)

        var leftCharging = false
        var rightCharging = false
        var caseCharging = false

        // Đọc metadata qua reflection chuẩn Android AOSP nếu intent không mang đủ
        runCatching {
            val getMetaMethod = BluetoothDevice::class.java.getMethod("getMetadata", Int::class.javaPrimitiveType)
            fun getMetaString(key: Int): String? = (getMetaMethod.invoke(device, key) as? ByteArray)?.let { String(it).trim() }

            if (left !in 0..100) left = getMetaString(METADATA_UNTHETHERED_LEFT_BATTERY)?.toIntOrNull() ?: -1
            if (right !in 0..100) right = getMetaString(METADATA_UNTHETHERED_RIGHT_BATTERY)?.toIntOrNull() ?: -1
            if (case !in 0..100) case = getMetaString(METADATA_UNTHETHERED_CASE_BATTERY)?.toIntOrNull() ?: -1
            if (main !in 0..100) main = getMetaString(METADATA_MAIN_BATTERY)?.toIntOrNull() ?: -1

            leftCharging = getMetaString(METADATA_UNTHETHERED_LEFT_CHARGING).toBoolean()
            rightCharging = getMetaString(METADATA_UNTHETHERED_RIGHT_CHARGING).toBoolean()
            caseCharging = getMetaString(METADATA_UNTHETHERED_CASE_CHARGING).toBoolean()
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

    private fun show(context: Context, device: BluetoothDevice, telemetry: EarbudsTelemetry, low: Boolean) {
        if (!enabled(context)) return
        val name = runCatching { device.alias ?: device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Tai nghe"
        val level = telemetry.effectiveLevel
        val known = level in 0..100
        val isLow = known && level <= LOW_LEVEL
        val glyph = if (isOverEar(device, name)) R.drawable.ic_headphones else R.drawable.ic_earbuds
        val icon = Icon.createWithResource(context, glyph)

        val click = PendingIntent.getActivity(
            context, ID, Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // Tạo chuỗi mô tả viễn trắc đa thành phần cho thẻ mở rộng
        val details = if (telemetry.hasDetails) {
            val parts = mutableListOf<String>()
            if (telemetry.left in 0..100) {
                parts.add("Tai Trái: ${telemetry.left}%" + if (telemetry.isLeftCharging) " ⚡" else "")
            }
            if (telemetry.right in 0..100) {
                parts.add("Tai Phải: ${telemetry.right}%" + if (telemetry.isRightCharging) " ⚡" else "")
            }
            if (telemetry.case in 0..100) {
                parts.add("Hộp sạc: ${telemetry.case}%" + if (telemetry.isCaseCharging) " ⚡" else "")
            }
            parts.joinToString("   ")
        } else {
            name
        }

        // Tạo nội dung viên thuốc thu gọn (Capsule pill)
        val pillStatus = when {
            telemetry.left in 0..100 && telemetry.right in 0..100 -> {
                "L ${telemetry.left}% · R ${telemetry.right}%"
            }
            known -> "$level%"
            else -> "Đã kết nối"
        }

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("is_ongoing", true)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
            putExtra("title", if (low) "Pin yếu" else name)
            putExtra("text", details)
            putExtra("oi_left_content", "")
            putExtra("oi_left_icon", icon)
            putExtra("oi_large_icon", icon)
            putExtra("icon_res", glyph)

            if (telemetry.hasDetails) {
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
                putExtra("oi_right_content", pillStatus)
                putExtra("status_chip_text", pillStatus)
                putExtra("oi_card_ring", level)
                putExtra("oi_fg_color", if (isLow) RED else GREEN)
                putExtra("oi_bg_color", if (isLow) RED_TRACK else GREEN_TRACK)
            } else if (known) {
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS) // 2
                putExtra("oi_progress", level)
                putExtra("oi_card_ring", level)
                putExtra("oi_fg_color", if (isLow) RED else GREEN)
                putExtra("oi_bg_color", if (isLow) RED_TRACK else GREEN_TRACK)
            } else {
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
                putExtra("oi_show_right_icon", false)
                putExtra("oi_right_content", "Đã kết nối")
                putExtra("status_chip_text", "Đã kết nối")
            }

            if (low) putExtra("oi_light_color", RED)
            putExtra("click_resp", click)
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)
        }

        context.startService(intent)
        showingAddress = device.address
        hideTask?.let(handler::removeCallbacks)
        hideTask = Runnable { hide(context) }.also { handler.postDelayed(it, SHOW_MS) }
    }

    private fun hide(context: Context) {
        hideTask?.let(handler::removeCallbacks)
        hideTask = null
        showingAddress = null
        context.startService(
            Intent(context, PlaygroundService::class.java)
                .setAction(PlaygroundService.ACTION_CANCEL)
                .putExtra("id", ID)
                .putExtra("oi_scene", "NAVIGATION"),
        )
    }
}
