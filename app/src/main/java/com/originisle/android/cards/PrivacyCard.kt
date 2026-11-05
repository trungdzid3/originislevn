package com.originisle.android.cards

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import java.util.Locale

/**
 * Chỉ báo quyền riêng tư (Camera / Micro):
 * - Camera: Màu đỏ, khung tròn (Circle).
 * - Micro: Màu xanh lá, khung hơi vuông bo góc (Squircle / Rounded Rectangle).
 */
object PrivacyCard {

    /** Fixed cast id, next to [ChargingCard.ID]. */
    const val ID = 35002

    /** A camera/mic must stay on this long before the dot shows (no flashes from brief opens)… */
    private const val SHOW_AFTER_MS = 800L
    /** …and off this long before it goes — enough to ride out a front/back camera switch. */
    private const val HIDE_AFTER_MS = 300L

    // Màu chỉ báo: Camera màu đỏ, Micro màu xanh lá
    private const val COLOR_CAMERA_RED = 0xFFFF3B30.toInt()
    private const val COLOR_MIC_GREEN = 0xFF34C759.toInt()

    private enum class Shown { NONE, CAMERA, MIC, BOTH }

    private val handler = Handler(Looper.getMainLooper())
    private var app: Context? = null

    private val camerasInUse = mutableSetOf<String>()
    /** Cameras opened while the keyguard was up (face unlock): ignored until they close. */
    private val camerasOpenedLocked = mutableSetOf<String>()
    private var micInUse = false
    private var shown = Shown.NONE

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraUnavailable(cameraId: String) {
            val context = app ?: return
            camerasInUse += cameraId
            if (context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true) {
                camerasOpenedLocked += cameraId
            }
            scheduleUpdate()
        }

        override fun onCameraAvailable(cameraId: String) {
            camerasInUse -= cameraId
            camerasOpenedLocked -= cameraId
            scheduleUpdate()
        }
    }

    private val recordingCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
            micInUse = configs.any { !it.isClientSilenced }
            scheduleUpdate()
        }
    }

    private val modeListener = AudioManager.OnModeChangedListener { scheduleUpdate() }

    private val updateTask = Runnable { update() }

    /** Called when the notification listener connects (which is what keeps our process alive). */
    fun start(context: Context) {
        if (app != null) return
        val ctx = context.applicationContext
        app = ctx
        runCatching { ctx.getSystemService(CameraManager::class.java)?.registerAvailabilityCallback(cameraCallback, handler) }
        ctx.getSystemService(AudioManager::class.java)?.let { audio ->
            runCatching { audio.registerAudioRecordingCallback(recordingCallback, handler) }
            runCatching { audio.addOnModeChangedListener(ctx.mainExecutor, modeListener) }
            micInUse = audio.activeRecordingConfigurations.any { !it.isClientSilenced }
        }
        scheduleUpdate()
    }

    fun stop() {
        val ctx = app ?: return
        runCatching { ctx.getSystemService(CameraManager::class.java)?.unregisterAvailabilityCallback(cameraCallback) }
        ctx.getSystemService(AudioManager::class.java)?.let { audio ->
            runCatching { audio.unregisterAudioRecordingCallback(recordingCallback) }
            runCatching { audio.removeOnModeChangedListener(modeListener) }
        }
        handler.removeCallbacks(updateTask)
        if (shown != Shown.NONE) cancel(ctx)
        camerasInUse.clear()
        camerasOpenedLocked.clear()
        micInUse = false
        app = null
    }

    /** Something outside changed (the torch, the casting switch): re-check. */
    fun refresh() = scheduleUpdate()

    private fun scheduleUpdate() {
        handler.removeCallbacks(updateTask)
        handler.postDelayed(updateTask, if (wanted() == Shown.NONE) HIDE_AFTER_MS else SHOW_AFTER_MS)
    }

    private fun update() {
        val context = app ?: return
        val next = wanted()
        if (next == shown) return
        shown = next
        if (next == Shown.NONE) cancel(context) else show(context, next)
    }

    private fun wanted(): Shown {
        val context = app ?: return Shown.NONE
        val prefs = context.getSharedPreferences("experimental_prefs", 0)
        val enabled = prefs.getBoolean("cast_notifications", true) && prefs.getBoolean("cast_privacy_dot", true)
        val camera = camerasInUse.any { it !in camerasOpenedLocked && !FlashlightCard.isTorchOn(it) }
        val mode = context.getSystemService(AudioManager::class.java)?.mode ?: AudioManager.MODE_NORMAL
        val inCall = mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION ||
            mode == AudioManager.MODE_CALL_SCREENING || mode == AudioManager.MODE_CALL_REDIRECT ||
            mode == AudioManager.MODE_COMMUNICATION_REDIRECT
        val mic = micInUse && !inCall
        return when {
            !enabled -> Shown.NONE
            camera && mic -> Shown.BOTH
            camera -> Shown.CAMERA
            mic -> Shown.MIC
            else -> Shown.NONE
        }
    }

    private fun show(context: Context, what: Shown) {
        val isVi = Locale.getDefault().language == "vi"
        val (title, text, glyph) = when (what) {
            Shown.CAMERA -> Triple(
                if (isVi) "Đang dùng Camera" else "Camera in use",
                if (isVi) "Một ứng dụng đang sử dụng Camera" else "An app is using the camera",
                R.drawable.ic_camera,
            )
            Shown.MIC -> Triple(
                if (isVi) "Đang dùng Micro" else "Microphone in use",
                if (isVi) "Một ứng dụng đang sử dụng Micro" else "An app is using the microphone",
                R.drawable.ic_mic,
            )
            else -> Triple(
                if (isVi) "Đang dùng Camera & Micro" else "Camera & Mic in use",
                if (isVi) "Một ứng dụng đang sử dụng Camera & Micro" else "An app is using camera & microphone",
                R.drawable.ic_camera,
            )
        }

        val indicator = indicatorDot(what)
        val icon = Icon.createWithResource(context, glyph)
        val click = PendingIntent.getActivity(
            context, ID, Intent(Settings.ACTION_PRIVACY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("is_ongoing", true)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
            putExtra("title", title)
            putExtra("text", text)
            putExtra("oi_left_content", "")
            putExtra("oi_left_icon", indicator)
            putExtra("oi_large_icon", icon)
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
            putExtra("oi_right_icon", Icon.createWithResource(context, R.drawable.ic_transparent))
            putExtra("oi_show_right_icon", true)
            putExtra("oi_right_content", "")
            putExtra("icon_res", glyph)
            putExtra("click_resp", click)
        }
        context.startService(intent)
    }

    private fun cancel(context: Context) {
        context.startService(
            Intent(context, PlaygroundService::class.java)
                .setAction(PlaygroundService.ACTION_CANCEL)
                .putExtra("id", ID)
                .putExtra("oi_scene", "NAVIGATION"),
        )
    }

    /**
     * Chỉ báo thị giác trên viên nang Đảo:
     * - Camera (hoặc Camera & Mic): Màu đỏ, khung tròn (Circle).
     * - Micro: Màu xanh lá, khung hơi vuông bo góc (Squircle / Rounded Rectangle).
     */
    private fun indicatorDot(what: Shown): Icon {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (what == Shown.MIC) {
            // Micro: Màu xanh lá, khung hơi vuông bo góc
            paint.color = COLOR_MIC_GREEN
            val half = size * 0.22f
            val cx = size / 2f
            val cy = size / 2f
            val cornerRadius = size * 0.07f // Bo góc nhẹ cho hình hơi vuông
            val rect = RectF(cx - half, cy - half, cx + half, cy + half)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        } else {
            // Camera hoặc cả hai: Màu đỏ, khung tròn
            paint.color = COLOR_CAMERA_RED
            canvas.drawCircle(size / 2f, size / 2f, size * 0.22f, paint)
        }
        return Icon.createWithBitmap(bitmap)
    }
}
