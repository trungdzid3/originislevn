package com.originisle.android.ui

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.originisle.android.log.CastLog
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/** Snapshot thông số phần cứng, hệ điều hành và thống kê Đảo trong ngày. */
data class DeviceSnapshot(
    val modelName: String,
    val marketBrand: String,
    val originOsVersion: String,
    val androidVersion: String,
    val socName: String,
    val totalRamGb: String,
    val availRamGb: String,
    val batteryPct: Int,
    val batteryTempC: Float,
    val isCharging: Boolean,
    val todayCastCount: Int,
)

/** Thu thập thông số thiết bị thực tế trên OriginOS / Android để hiển thị trên thẻ Hero. */
object DeviceInfoHelper {

    fun collect(context: Context): DeviceSnapshot {
        val modelName = Build.MODEL.orEmpty().trim().ifBlank { "Thiết bị Android" }
        val marketBrand = Build.MANUFACTURER.orEmpty().trim()
            .ifBlank { "Android" }
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        val displayId = getSystemProperty("ro.vivo.os.build.display.id")
        val osVersion = getSystemProperty("ro.vivo.os.version")
        val productVersion = getSystemProperty("ro.vivo.product.version")
        val originOsVersion = when {
            displayId.isNotBlank() && displayId.contains("OriginOS", ignoreCase = true) -> displayId
            osVersion.isNotBlank() -> {
                val clean = osVersion.removePrefix("OriginOS").trim()
                if (clean.isNotBlank()) "OriginOS $clean" else "OriginOS"
            }
            displayId.isNotBlank() -> displayId
            productVersion.isNotBlank() -> productVersion
            else -> "OriginOS (Tương thích)"
        }

        val androidVersion = Build.VERSION.RELEASE.orEmpty().trim()
            .ifBlank { Build.VERSION.SDK_INT.toString() }

        val socModel = Build.SOC_MODEL.orEmpty()
            .takeIf { it.isNotBlank() && !it.equals(Build.UNKNOWN, ignoreCase = true) }
        val hardware = Build.HARDWARE.orEmpty()
            .takeIf { it.isNotBlank() && !it.equals(Build.UNKNOWN, ignoreCase = true) }
        val boardPlatform = getSystemProperty("ro.board.platform").takeIf { it.isNotBlank() }
        val socName = socModel ?: hardware ?: boardPlatform ?: "Không rõ"

        val memInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        activityManager?.getMemoryInfo(memInfo)
        val totalRamGb = formatGb(memInfo.totalMem)
        val availRamGb = formatGb(memInfo.availMem)

        val batteryIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
        } else {
            0
        }
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val batteryTempC = tempTenths / 10f
        val batteryStatus = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
            batteryStatus == BatteryManager.BATTERY_STATUS_FULL

        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayCastCount = runCatching {
            CastLog.entries.count { it.cast && it.time >= startOfDay }
        }.getOrDefault(0)

        return DeviceSnapshot(
            modelName = modelName,
            marketBrand = marketBrand,
            originOsVersion = originOsVersion,
            androidVersion = androidVersion,
            socName = socName,
            totalRamGb = totalRamGb,
            availRamGb = availRamGb,
            batteryPct = batteryPct,
            batteryTempC = batteryTempC,
            isCharging = isCharging,
            todayCastCount = todayCastCount,
        )
    }

    /** Kiểm tra hệ thống có hỗ trợ API SuperX của Vivo OriginOS hay không. */
    fun isSuperXSupported(context: Context): Boolean = runCatching {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return@runCatching false
        NotificationManager::class.java.getMethod(
            "setSuperXInfosSceneList",
            List::class.java,
            List::class.java,
            List::class.java,
            List::class.java,
        )
        nm.areNotificationsEnabled() || true
    }.getOrDefault(false)

    private fun getSystemProperty(key: String): String = runCatching {
        val systemPropsClass = Class.forName("android.os.SystemProperties")
        val getMethod = systemPropsClass.getMethod("get", String::class.java, String::class.java)
        (getMethod.invoke(null, key, "") as? String)?.trim().orEmpty()
    }.getOrDefault("")

    private fun formatGb(bytes: Long): String {
        if (bytes <= 0L) return "-- GB"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f GB", gb)
    }
}
