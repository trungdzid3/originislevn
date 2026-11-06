package com.originisle.android.cards

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.service.IconCache
import com.originisle.android.ui.PREFS_NAME

/**
 * Thẻ thông báo Đảo Origin khi thay đổi chế độ Chuông / Rung / Im lặng / Không làm phiền (Ringer & DND Mode Card).
 * Hỗ trợ 4 chế độ: Normal (Chuông), Vibrate (Rung), Silent (Im lặng), DND (Không làm phiền / Zen Mode) trên Vivo OriginOS 6.
 * Lắng nghe đa tầng qua BroadcastReceiver, ContentObserver, polling mỗi giây và callback trực tiếp từ NotificationListenerService.
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

    private const val DEBOUNCE_DELAY_MS = 150L
    private const val AUTO_DISMISS_MS = 2800L

    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var lastRingerMode: Int = -1
    @Volatile private var lastDndState: Boolean? = null
    @Volatile private var lastVibrateSetting: Int = -1
    @Volatile private var lastRingerChangeAt: Long = 0L
    private var isRegistered = false

    private var debounceTask: Runnable? = null
    private var dismissTask: Runnable? = null
    private var contentObserver: ContentObserver? = null

    private fun readVibrateSetting(context: Context): Int {
        val resolver = context.contentResolver
        fun readSetting(key: String): Int {
            return runCatching { Settings.System.getInt(resolver, key) }.getOrNull()
                ?: runCatching { Settings.Global.getInt(resolver, key) }.getOrDefault(0)
        }
        val vibRing = readSetting("vibrate_when_ringing").coerceAtLeast(0)
        val vibSilent = readSetting("vibrate_in_silent").coerceAtLeast(0)
        val vivoVib = readSetting("vivo_vibrate_state").coerceAtLeast(0)
        val modeRinger = readSetting("mode_ringer").coerceAtLeast(0)
        return ((modeRinger and 0xF) shl 12) or ((vibRing and 0xF) shl 8) or ((vibSilent and 0xF) shl 4) or (vivoVib and 0xF)
    }

    private val ringerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED,
                NotificationManager.ACTION_NOTIFICATION_POLICY_CHANGED,
                "android.app.action.INTERRUPTION_FILTER_CHANGED" -> {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    val filter = nm?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
                    onInterruptionFilterChanged(context, filter)
                }
                AudioManager.RINGER_MODE_CHANGED_ACTION,
                "android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION" -> {
                    val modeFromIntent = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1)
                    val mode = if (modeFromIntent != -1) {
                        modeFromIntent
                    } else {
                        (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.ringerMode ?: -1
                    }
                    if (mode != -1 && mode != lastRingerMode) {
                        lastRingerMode = mode
                        lastRingerChangeAt = System.currentTimeMillis()
                        lastDndState = isDndActive(context)
                        lastVibrateSetting = readVibrateSetting(context)
                        triggerCard(context, mode)
                    }
                }
                "android.media.VIBRATE_SETTING_CHANGED" -> {
                    poll(context, "vibrate")
                }
                "android.media.VOLUME_CHANGED_ACTION",
                "android.media.STREAM_MUTE_CHANGED_ACTION" -> {
                    poll(context, "volume")
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
     * Đăng ký lắng nghe broadcast và ContentObserver thay đổi chế độ chuông / DND hệ thống.
     */
    fun register(context: Context) {
        if (isRegistered) return
        val app = context.applicationContext
        val am = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        lastRingerMode = am?.ringerMode ?: -1
        lastDndState = isDndActive(app)
        lastVibrateSetting = readVibrateSetting(app)

        val filter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION")
            addAction("android.media.VIBRATE_SETTING_CHANGED")
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction("android.media.STREAM_MUTE_CHANGED_ACTION")
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(NotificationManager.ACTION_NOTIFICATION_POLICY_CHANGED)
            addAction("android.app.action.INTERRUPTION_FILTER_CHANGED")
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

        // Đăng ký ContentObserver theo dõi các URI cài đặt hệ thống trên OriginOS
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                Log.d(TAG, "ContentObserver nhận thay đổi URI: $uri")
                poll(app, uri?.toString())
            }
        }
        contentObserver = observer
        val resolver = app.contentResolver
        listOfNotNull(
            Settings.Global.getUriFor("mode_ringer"),
            Settings.System.getUriFor("mode_ringer"),
            Settings.System.getUriFor("vibrate_in_silent"),
            Settings.Global.getUriFor("vibrate_in_silent"),
            Settings.Global.getUriFor("zen_mode"),
            Settings.System.getUriFor("vibrate_when_ringing"),
            Settings.System.getUriFor("vivo_vibrate_state"),
            Settings.Global.getUriFor("vivo_vibrate_state"),
            Settings.System.getUriFor("volume_ring_speaker"),
            Settings.System.getUriFor("volume_ring"),
        ).forEach { uri ->
            runCatching {
                resolver.registerContentObserver(uri, false, observer)
            }.onFailure {
                Log.w(TAG, "Không thể đăng ký ContentObserver cho $uri", it)
            }
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

        contentObserver?.let {
            runCatching {
                app.contentResolver.unregisterContentObserver(it)
            }
            contentObserver = null
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
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val nowRinger = am?.ringerMode ?: -1
        if (nowRinger != -1) {
            if (lastRingerMode == -1) {
                lastRingerMode = nowRinger
            } else if (nowRinger != lastRingerMode) {
                lastRingerMode = nowRinger
                lastRingerChangeAt = System.currentTimeMillis()
                lastDndState = isDndActive(context)
                lastVibrateSetting = readVibrateSetting(context)
                triggerCard(context, nowRinger)
                return
            }
        }

        val nowDnd = isDndActive(context)
        if (lastDndState == null) {
            lastDndState = nowDnd
        } else if (nowDnd != lastDndState) {
            lastDndState = nowDnd
            if (System.currentTimeMillis() - lastRingerChangeAt >= 500L) {
                if (nowDnd) {
                    triggerCard(context, MODE_DND)
                } else {
                    val currentMode = if (nowRinger != -1) nowRinger else MODE_NORMAL
                    lastRingerMode = currentMode
                    triggerCard(context, currentMode)
                }
                return
            }
        }

        val nowVib = readVibrateSetting(context)
        if (lastVibrateSetting == -1) {
            lastVibrateSetting = nowVib
        } else if (nowVib != lastVibrateSetting) {
            lastVibrateSetting = nowVib
            val resolver = context.contentResolver
            fun readSetting(key: String): Int {
                return runCatching { Settings.System.getInt(resolver, key) }.getOrNull()
                    ?: runCatching { Settings.Global.getInt(resolver, key) }.getOrDefault(0)
            }
            val vibRing = readSetting("vibrate_when_ringing")
            val vivoVib = readSetting("vivo_vibrate_state")
            if (vibRing == 1 || vivoVib == 1 || nowRinger == MODE_VIBRATE) {
                triggerCard(context, MODE_VIBRATE)
            } else if (nowRinger == MODE_SILENT) {
                triggerCard(context, MODE_SILENT)
            } else {
                triggerCard(context, MODE_NORMAL)
            }
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

        if (System.currentTimeMillis() - lastRingerChangeAt < 500L) return

        if (isDnd) {
            triggerCard(context, MODE_DND)
        } else {
            val currentMode = (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.ringerMode ?: MODE_NORMAL
            lastRingerMode = currentMode
            triggerCard(context, currentMode)
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
        val (title, text, leftContent, rightContent, colorHex, icon) = when (mode) {
            MODE_NORMAL -> RingerStateInfo(
                title = "Chuông",
                text = "Đã bật chuông thông báo",
                leftContent = "Chuông",
                rightContent = "Bật",
                colorHex = COLOR_NORMAL,
                icon = createBellIcon(Color.parseColor(COLOR_NORMAL)),
            )
            MODE_VIBRATE -> RingerStateInfo(
                title = "Rung",
                text = "Đã bật chế độ rung",
                leftContent = "Rung",
                rightContent = "Rung",
                colorHex = COLOR_VIBRATE,
                icon = createVibrateIcon(Color.parseColor(COLOR_VIBRATE)),
            )
            MODE_SILENT -> RingerStateInfo(
                title = "Im lặng",
                text = "Đã tắt toàn bộ âm thanh",
                leftContent = "Im lặng",
                rightContent = "Tắt",
                colorHex = COLOR_SILENT,
                icon = createSilentIcon(Color.parseColor(COLOR_SILENT)),
            )
            MODE_DND -> RingerStateInfo(
                title = "Không làm phiền",
                text = "Đã bật chế độ Không làm phiền",
                leftContent = "Không làm phiền",
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
            putExtra("oi_bg_color", colorHex)
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
        IconCache.activeSmallIcons.remove(ID)
        context.startService(
            Intent(context, PlaygroundService::class.java)
                .setAction(PlaygroundService.ACTION_CANCEL)
                .putExtra("id", ID)
                .putExtra("oi_scene", "NAVIGATION"),
        )
    }

    /**
     * Dựng Icon Chuông vàng (Normal Mode) dạng Canvas vector 96x96 px.
     */
    fun createBellIcon(color: Int, width: Int = 96, height: Int = 96): Icon {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val loopPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.5f
            strokeCap = Paint.Cap.ROUND
            this.color = color
        }
        canvas.drawArc(RectF(42f, 16f, 54f, 28f), 180f, 180f, false, loopPaint)

        val bellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }
        val path = Path().apply {
            moveTo(48f, 26f)
            cubicTo(40f, 26f, 32f, 42f, 30f, 58f)
            lineTo(24f, 66f)
            quadTo(22f, 70f, 27f, 70f)
            lineTo(69f, 70f)
            quadTo(74f, 70f, 72f, 66f)
            lineTo(66f, 58f)
            cubicTo(64f, 42f, 56f, 26f, 48f, 26f)
            close()
        }
        canvas.drawPath(path, bellPaint)
        canvas.drawCircle(48f, 75f, 6f, bellPaint)

        return Icon.createWithBitmap(bitmap)
    }

    /**
     * Dựng Icon Điện thoại Rung lam (Vibrate Mode) dạng Canvas vector 96x96 px.
     */
    fun createVibrateIcon(color: Int, width: Int = 96, height: Int = 96): Icon {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val fillPhone = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = Color.argb(45, Color.red(color), Color.green(color), Color.blue(color))
        }
        val phoneRect = RectF(34f, 22f, 62f, 74f)
        canvas.drawRoundRect(phoneRect, 6f, 6f, fillPhone)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.5f
            strokeCap = Paint.Cap.ROUND
            this.color = color
        }
        canvas.drawRoundRect(phoneRect, 6f, 6f, strokePaint)

        // Loa thoại trên & nút home dưới
        canvas.drawLine(44f, 28f, 52f, 28f, strokePaint)
        canvas.drawCircle(48f, 68f, 2.5f, strokePaint)

        // Sóng rung 2 bên
        val leftInner = Path().apply {
            moveTo(24f, 36f)
            quadTo(18f, 48f, 24f, 60f)
        }
        canvas.drawPath(leftInner, strokePaint)

        val leftOuter = Path().apply {
            moveTo(16f, 30f)
            quadTo(9f, 48f, 16f, 66f)
        }
        canvas.drawPath(leftOuter, strokePaint)

        val rightInner = Path().apply {
            moveTo(72f, 36f)
            quadTo(78f, 48f, 72f, 60f)
        }
        canvas.drawPath(rightInner, strokePaint)

        val rightOuter = Path().apply {
            moveTo(80f, 30f)
            quadTo(87f, 48f, 80f, 66f)
        }
        canvas.drawPath(rightOuter, strokePaint)

        return Icon.createWithBitmap(bitmap)
    }

    /**
     * Dựng Icon Chuông gạch chéo đỏ (Silent Mode) dạng Canvas vector 96x96 px.
     */
    fun createSilentIcon(color: Int, width: Int = 96, height: Int = 96): Icon {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }

        val loopPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.5f
            strokeCap = Paint.Cap.ROUND
            this.color = color
        }
        canvas.drawArc(RectF(42f, 16f, 54f, 28f), 180f, 180f, false, loopPaint)

        val path = Path().apply {
            moveTo(48f, 26f)
            cubicTo(40f, 26f, 32f, 42f, 30f, 58f)
            lineTo(24f, 66f)
            quadTo(22f, 70f, 27f, 70f)
            lineTo(69f, 70f)
            quadTo(74f, 70f, 72f, 66f)
            lineTo(66f, 58f)
            cubicTo(64f, 42f, 56f, 26f, 48f, 26f)
            close()
        }
        canvas.drawPath(path, bellPaint)
        canvas.drawCircle(48f, 75f, 6f, bellPaint)

        // Đường gạch chéo sắc nét với viền đen nền
        val slashMask = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 9f
            strokeCap = Paint.Cap.ROUND
            this.color = Color.BLACK
        }
        canvas.drawLine(20f, 20f, 76f, 76f, slashMask)

        val slashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5.5f
            strokeCap = Paint.Cap.ROUND
            this.color = Color.WHITE
        }
        canvas.drawLine(20f, 20f, 76f, 76f, slashPaint)

        return Icon.createWithBitmap(bitmap)
    }

    /**
     * Dựng Icon Mặt trăng khuyết tím Indigo (DND / Zen Mode) dạng Canvas vector 96x96 px.
     */
    fun createDndIcon(color: Int, width: Int = 96, height: Int = 96): Icon {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val dndPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }

        // Mặt trăng khuyết: hình tròn chính tại (46, 48) r=28 trừ đi hình tròn cắt tại (58, 40) r=23
        val moonOuter = Path().apply {
            addCircle(46f, 48f, 28f, Path.Direction.CW)
        }
        val moonInner = Path().apply {
            addCircle(58f, 40f, 23f, Path.Direction.CW)
        }
        moonOuter.op(moonInner, Path.Op.DIFFERENCE)
        canvas.drawPath(moonOuter, dndPaint)

        // Hai ngôi sao nhỏ điểm xuyết
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }
        canvas.drawCircle(70f, 30f, 2.8f, starPaint)
        canvas.drawCircle(78f, 46f, 2.0f, starPaint)

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
