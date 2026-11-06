package com.originisle.android.cards

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.graphics.PathParser
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.service.IconCache
import com.originisle.android.ui.PREFS_NAME

/**
 * Thẻ thông báo Đảo Origin khi thay đổi chế độ Chuông / Rung / Im lặng / Không làm phiền (Ringer & DND Mode Card).
 * Hỗ trợ 4 chế độ: Normal (Chuông), Vibrate (Rung), Silent (Im lặng), DND (Không làm phiền / Zen Mode) trên Vivo OriginOS 6.
 * Lắng nghe qua BroadcastReceiver chuẩn AudioManager.RINGER_MODE_CHANGED_ACTION và NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED.
 */
object RingerModeCard {

    const val ID = 35008
    private const val TAG = "RingerModeCard"

    const val MODE_SILENT = AudioManager.RINGER_MODE_SILENT // 0
    const val MODE_VIBRATE = AudioManager.RINGER_MODE_VIBRATE // 1
    const val MODE_NORMAL = AudioManager.RINGER_MODE_NORMAL // 2
    const val MODE_DND = 3

    private const val COLOR_NORMAL = "#FCD535" // Vàng OriginOS
    private const val COLOR_VIBRATE = "#4A90E2" // Lam sáng
    private const val COLOR_SILENT = "#FF5247"  // Đỏ cảnh báo
    private const val COLOR_DND = "#7B61FF"     // Tím Indigo

    private const val DEBOUNCE_DELAY_MS = 80L
    private const val AUTO_DISMISS_MS = 2800L

    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var lastRingerMode: Int = -1
    @Volatile private var lastDndState: Boolean? = null
    @Volatile private var lastShownMode: Int = -1
    @Volatile private var isShowing: Boolean = false
    private var isRegistered = false

    private var debounceTask: Runnable? = null
    private var dismissTask: Runnable? = null

    private val ringerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    val filter = nm?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
                    onInterruptionFilterChanged(context, filter)
                }
                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    val modeFromIntent = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1)
                    val mode = if (modeFromIntent in 0..2) {
                        modeFromIntent
                    } else {
                        (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.ringerMode ?: -1
                    }
                    if (mode in 0..2) {
                        lastRingerMode = mode
                        triggerCard(context, mode)
                    }
                }
            }
        }
    }

    /**
     * Bắt đầu giám sát chế độ chuông và phím gạt im lặng trên OriginOS 6.
     */
    fun start(context: Context) {
        register(context)
    }

    /**
     * Dừng giám sát chế độ chuông.
     */
    fun stop(context: Context) {
        unregister(context)
    }

    /**
     * Đăng ký lắng nghe broadcast thay đổi chế độ chuông / DND hệ thống.
     */
    fun register(context: Context) {
        if (isRegistered) return
        val app = context.applicationContext
        val am = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        lastRingerMode = am?.ringerMode ?: -1
        lastDndState = isDndActive(app)

        val filter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    app.registerReceiver(ringerReceiver, filter, Context.RECEIVER_EXPORTED)
                }.getOrElse {
                    app.registerReceiver(ringerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                }
            } else {
                app.registerReceiver(ringerReceiver, filter)
            }
            Log.d(TAG, "Đã đăng ký RingerReceiver thành công (ringer=$lastRingerMode, dnd=$lastDndState)")
        }.onFailure {
            Log.e(TAG, "Lỗi đăng ký RingerReceiver", it)
        }

        isRegistered = true
    }

    /**
     * Hủy đăng ký lắng nghe và dọn dẹp các tác vụ chạy ngầm.
     */
    fun unregister(context: Context) {
        if (!isRegistered) return
        val app = context.applicationContext
        runCatching {
            app.unregisterReceiver(ringerReceiver)
        }.onFailure {
            Log.e(TAG, "Lỗi hủy đăng ký RingerReceiver", it)
        }

        isRegistered = false
        debounceTask?.let(handler::removeCallbacks)
        dismissTask?.let(handler::removeCallbacks)
        cancel(app)
    }

    /**
     * Kiểm tra định kỳ hoặc tức thì khi có sự kiện thay đổi Chuông / Rung / Im lặng / DND.
     */
    fun poll(context: Context, triggerUri: String? = null) {
        val nowDnd = isDndActive(context)
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val nowRinger = am?.ringerMode ?: MODE_NORMAL

        if (lastDndState == null) {
            lastDndState = nowDnd
        } else if (nowDnd != lastDndState) {
            lastDndState = nowDnd
            lastRingerMode = nowRinger
            if (nowDnd) {
                triggerCard(context, MODE_DND)
            } else {
                triggerCard(context, nowRinger)
            }
            return
        }

        if (nowRinger != lastRingerMode) {
            lastRingerMode = nowRinger
            triggerCard(context, nowRinger)
        }
    }

    /**
     * Kiểm tra trạng thái chuông / DND hiện tại từ hệ thống để đồng bộ (tương thích ngược).
     */
    fun checkCurrentState(context: Context) {
        poll(context)
    }

    /**
     * Xử lý khi bộ lọc ngắt quãng (DND / Zen mode) thay đổi.
     * Cung cấp hàm public để NotificationCastListener có thể gọi trực tiếp.
     */
    fun onInterruptionFilterChanged(context: Context, filter: Int) {
        val isDnd = filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
            filter == NotificationManager.INTERRUPTION_FILTER_ALARMS ||
            filter == NotificationManager.INTERRUPTION_FILTER_NONE

        if (lastDndState == isDnd) return
        lastDndState = isDnd

        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val currentRinger = am?.ringerMode ?: MODE_NORMAL
        lastRingerMode = currentRinger

        if (isDnd) {
            triggerCard(context, MODE_DND)
        } else {
            triggerCard(context, currentRinger)
        }
    }

    /**
     * Kiểm tra xem thiết bị có đang ở chế độ Không làm phiền (DND / Zen Mode) hay không.
     */
    fun isDndActive(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val filter = nm?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
        if (filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
            filter == NotificationManager.INTERRUPTION_FILTER_ALARMS ||
            filter == NotificationManager.INTERRUPTION_FILTER_NONE
        ) {
            return true
        }

        val zenMode = runCatching {
            Settings.Global.getInt(context.contentResolver, "zen_mode", 0)
        }.getOrDefault(0)
        return zenMode != 0
    }

    /**
     * Xử lý sự kiện thay đổi chế độ chuông / DND (hàm public tương thích ngược).
     */
    fun onRingerModeChanged(context: Context, mode: Int) {
        if (mode == -1) return
        if (mode != MODE_DND) {
            lastRingerMode = mode
        }
        triggerCard(context, mode)
    }

    private fun triggerCard(context: Context, mode: Int) {
        if (mode == -1) return

        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("cast_notifications", true) &&
            prefs.getBoolean("cast_ringer_mode", true)

        if (!enabled) return

        debounceTask?.let(handler::removeCallbacks)
        val task = Runnable {
            showRingerCard(app, mode)
        }
        debounceTask = task
        handler.postDelayed(task, DEBOUNCE_DELAY_MS)
    }

    /**
     * Đẩy thẻ thông báo lên Đảo Origin qua PlaygroundService.
     */
    private fun showRingerCard(context: Context, mode: Int) {
        if (mode == lastShownMode && isShowing) {
            // Thẻ đang hiển thị đúng chế độ này, chỉ gia hạn thời gian tự động đóng
            dismissTask?.let(handler::removeCallbacks)
            val dismiss = Runnable {
                cancel(context)
            }
            dismissTask = dismiss
            handler.postDelayed(dismiss, AUTO_DISMISS_MS)
            return
        }

        lastShownMode = mode
        isShowing = true

        val (title, text, leftContent, rightContent, colorHex, icon) = when (mode) {
            MODE_NORMAL -> RingerStateInfo(
                title = "Chuông báo",
                text = "Đã bật âm thanh thông báo",
                leftContent = "",
                rightContent = "Chuông",
                colorHex = COLOR_NORMAL,
                icon = createBellIcon(Color.parseColor(COLOR_NORMAL)),
            )
            MODE_VIBRATE -> RingerStateInfo(
                title = "Chế độ Rung",
                text = "Chỉ rung khi có thông báo",
                leftContent = "",
                rightContent = "Rung",
                colorHex = COLOR_VIBRATE,
                icon = createVibrateIcon(Color.parseColor(COLOR_VIBRATE)),
            )
            MODE_SILENT -> RingerStateInfo(
                title = "Im lặng",
                text = "Đã tắt âm thanh thông báo",
                leftContent = "",
                rightContent = "Im lặng",
                colorHex = COLOR_SILENT,
                icon = createSilentIcon(Color.parseColor(COLOR_SILENT)),
            )
            MODE_DND -> RingerStateInfo(
                title = "Không làm phiền",
                text = "Đã bật chế độ Không làm phiền",
                leftContent = "",
                rightContent = "DND",
                colorHex = COLOR_DND,
                icon = createDndIcon(Color.parseColor(COLOR_DND)),
            )
            else -> return
        }

        CastLog.add("Hệ thống", title, "hiển thị — chế độ âm thanh: $title ($text)", true)

        IconCache.activeSmallIcons.remove(ID)

        val soundIntent = Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val clickPi = PendingIntent.getActivity(
            context,
            ID,
            soundIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("is_ongoing", false)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
            putExtra("oi_show_right_icon", false)
            putExtra("oi_right_icon", icon)
            putExtra("oi_left_icon", icon)
            putExtra("oi_large_icon", icon)
            putExtra("title", title)
            putExtra("text", text)
            putExtra("subtext", text)
            putExtra("oi_left_content", leftContent)
            putExtra("oi_right_content", rightContent)
            putExtra("status_chip_text", rightContent)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_fg_color", colorHex)
            putExtra("oi_light_color", colorHex)
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)
            putExtra("click_resp", clickPi)
            putExtra("source_content_intent", clickPi)
        }
        context.startService(intent)

        // Tự động đóng sau AUTO_DISMISS_MS (2800ms)
        dismissTask?.let(handler::removeCallbacks)
        val dismiss = Runnable {
            cancel(context)
        }
        dismissTask = dismiss
        handler.postDelayed(dismiss, AUTO_DISMISS_MS)
    }

    /**
     * Hủy hiển thị thẻ trên Đảo Origin.
     */
    fun cancel(context: Context) {
        dismissTask?.let(handler::removeCallbacks)
        dismissTask = null
        isShowing = false
        lastShownMode = -1
        IconCache.activeSmallIcons.remove(ID)
        context.startService(
            Intent(context, PlaygroundService::class.java)
                .setAction(PlaygroundService.ACTION_CANCEL)
                .putExtra("id", ID)
                .putExtra("oi_scene", "NAVIGATION"),
        )
    }

    /**
     * Dựng Icon Chuông vàng (Normal Mode) dạng Canvas vector 96x96 px (notifications_active).
     */
    fun createBellIcon(color: Int, width: Int = 96, height: Int = 96): Icon {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = width / 24f
        canvas.scale(scale, scale)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }

        val path = PathParser.createPathFromPathData(
            "M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.89,2 2,2z" +
            "M18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z" +
            "M7.58,4.08L6.15,2.65C3.75,4.48 2.17,7.3 2.03,10.5h2c0.15,-2.65 1.51,-4.97 3.55,-6.42z" +
            "M19.97,10.5h2c-0.15,-3.2 -1.73,-6.02 -4.12,-7.85l-1.42,1.43c2.02,1.45 3.39,3.77 3.54,6.42z"
        )
        canvas.drawPath(path, paint)

        return Icon.createWithBitmap(bitmap)
    }

    /**
     * Dựng Icon Chế độ Rung (Vibrate Mode) dạng Canvas vector 96x96 px chuẩn Material/OriginOS.
     * Thiết kế thân điện thoại thẳng đứng cân đối ở trung tâm, hai bên là các dải sóng rung hình cánh cung cong mượt mà.
     */
    fun createVibrateIcon(color: Int, width: Int = 96, height: Int = 96): Icon {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = width / 24f
        canvas.scale(scale, scale)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.color = color
            strokeWidth = 1.7f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // 1. Khung thân điện thoại thẳng đứng, bo góc mềm mại ở trung tâm (24x24)
        val phoneRect = RectF(7.6f, 3.8f, 16.4f, 20.2f)
        canvas.drawRoundRect(phoneRect, 2.4f, 2.4f, strokePaint)

        // Rãnh loa thoại nhỏ ở đỉnh máy
        canvas.drawLine(10.5f, 6.2f, 13.5f, 6.2f, strokePaint)
        // Vạch đáy màn hình / nút home
        canvas.drawLine(10.8f, 17.8f, 13.2f, 17.8f, strokePaint)

        // 2. Hai cặp sóng rung cong mượt mà đối xứng 2 bên (không nghiêng ngả, không zigzag sắc nhọn)
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.color = color
            strokeWidth = 1.6f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Cặp sóng bên trái
        val leftInner = Path().apply {
            moveTo(5.0f, 9.5f)
            quadTo(3.5f, 12.0f, 5.0f, 14.5f)
        }
        val leftOuter = Path().apply {
            moveTo(2.5f, 7.6f)
            quadTo(0.6f, 12.0f, 2.5f, 16.4f)
        }
        canvas.drawPath(leftInner, wavePaint)
        canvas.drawPath(leftOuter, wavePaint)

        // Cặp sóng bên phải
        val rightInner = Path().apply {
            moveTo(19.0f, 9.5f)
            quadTo(20.5f, 12.0f, 19.0f, 14.5f)
        }
        val rightOuter = Path().apply {
            moveTo(21.5f, 7.6f)
            quadTo(23.4f, 12.0f, 21.5f, 16.4f)
        }
        canvas.drawPath(rightInner, wavePaint)
        canvas.drawPath(rightOuter, wavePaint)

        return Icon.createWithBitmap(bitmap)
    }

    /**
     * Dựng Icon Chuông gạch chéo đỏ (Silent Mode) dạng Canvas vector 96x96 px (notifications_off).
     */
    fun createSilentIcon(color: Int, width: Int = 96, height: Int = 96): Icon {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = width / 24f
        canvas.scale(scale, scale)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }

        val path = PathParser.createPathFromPathData(
            "M20,18.69L7.84,6.14L5.27,3.49L4,4.76l2.8,2.8v0.01c-0.52,0.99 -0.8,2.16 -0.8,3.42v5l-2,2v1h13.73l2,2L21,19.72L20,18.69z" +
            "M12,22c1.11,0 2,-0.89 2,-2h-4C10,21.11 10.89,22 12,22z" +
            "M18,14.68V11c0,-3.08 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5S10.5,3.17 10.5,4v0.68c-0.24,0.06 -0.47,0.15 -0.69,0.23L18,14.68z"
        )
        canvas.drawPath(path, paint)

        return Icon.createWithBitmap(bitmap)
    }

    /**
     * Dựng Icon Mặt trăng khuyết tím Indigo (DND / Zen Mode) dạng Canvas vector 96x96 px.
     */
    fun createDndIcon(color: Int, width: Int = 96, height: Int = 96): Icon {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = width / 24f
        canvas.scale(scale, scale)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }

        val path = PathParser.createPathFromPathData(
            "M12.3,4.9c0.4,-0.2 0.6,-0.7 0.5,-1.1S12.2,3 11.7,3C6.8,3.2 3,7.2 3,12c0,5 4,9 9,9c3.8,0 7.1,-2.4 8.4,-5.9c0.2,-0.4 0,-0.9 -0.4,-1.2c-0.4,-0.3 -0.9,-0.2 -1.2,0.1c-1,0.9 -2.3,1.5 -3.8,1.5c-3.3,0 -6,-2.7 -6,-6C9,7.4 10.4,5.7 12.3,4.9z"
        )
        canvas.drawPath(path, paint)

        return Icon.createWithBitmap(bitmap)
    }

    private data class RingerStateInfo(
        val title: String,
        val text: String,
        val leftContent: String,
        val rightContent: String,
        val colorHex: String,
        val icon: Icon,
    )
}
