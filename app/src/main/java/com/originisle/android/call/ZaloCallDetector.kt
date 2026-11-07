package com.originisle.android.call

import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import java.lang.reflect.Field

typealias CallState = ZaloCallDetector.CallState

data class ZaloCallInfo(
    val callerName: String,
    val statusText: String,
    val state: ZaloCallDetector.CallState,
    val isVideoCall: Boolean,
    val avatar: Bitmap?,
    val declineIntent: PendingIntent?,
    val answerIntent: PendingIntent?,
    val contentIntent: PendingIntent?,
    val chronometerBaseMs: Long = 0L,
)

/**
 * Bộ nhận diện và bóc tách chuyên sâu thông báo cuộc gọi Zalo (`com.zing.zalo`),
 * được thiết kế dựa trên kết quả dịch ngược APK Zalo v26.10.02 (`Lmd2/l1;` - `VoIPNotifications`).
 *
 * Trên các thiết bị Vivo / OriginOS, Zalo xóa sạch `n.actions` và gắn `RemoteViews` tùy chỉnh
 * vào `contentView`, `bigContentView`, `headsUpContentView`. Module này trích xuất trực tiếp
 * các thành phần bên trong `RemoteViews` kết hợp với `Notification.extras`.
 */
object ZaloCallDetector {

    const val ZALO_PKG = "com.zing.zalo"
    const val ACTION_ZALO_END_CALL = "m.voip.api.action.ACTION_NOTI_END_CALL"

    enum class CallState {
        INCOMING,
        ONGOING,
        MISSED,
    }

    // Layout IDs của RemoteViews thông báo cuộc gọi Zalo (v26.10.02)
    private const val LAYOUT_CALL_COLLAP = 0x7f0c04c1
    private const val LAYOUT_CALL_EXPAND = 0x7f0c04c2
    private const val LAYOUT_CALL_EXPAND_DARK = 0x7f0c04c3
    private val CALL_LAYOUT_IDS = setOf(
        LAYOUT_CALL_COLLAP,
        LAYOUT_CALL_EXPAND,
        LAYOUT_CALL_EXPAND_DARK,
    )

    // View IDs cố định bên trong RemoteViews cuộc gọi Zalo (v26.10.02)
    private const val TV_TITLE_ID = 0x7f091d27      // tv_title: Tên người gọi / nhóm
    private const val TV_CONTENT_ID = 0x7f091b86    // tv_content: Trạng thái cuộc gọi
    private const val IMV_AVATAR_ID = 0x7f090bbd    // imv_avatar: Ảnh đại diện người gọi
    private const val FL_BTN_LEFT_ID = 0x7f090977   // fl_btn_left: Nút Từ chối / Kết thúc
    private const val TV_BTN_LEFT_ID = 0x7f091b66   // tv_btn_left
    private const val TV_CLOSE_ID = 0x7f091b7b      // tv_close
    private const val FL_BTN_RIGHT_ID = 0x7f090978  // fl_btn_right: Nút Trả lời / Mở cuộc gọi
    private const val TV_BTN_RIGHT_ID = 0x7f091b67  // tv_btn_right
    private const val TV_OPEN_ID = 0x7f091c56       // tv_open
    private const val ELAPSE_TIME_ID = 0x7f090863   // elapse_time: Chronometer

    private val DECLINE_VIEW_IDS = setOf(FL_BTN_LEFT_ID, TV_BTN_LEFT_ID, TV_CLOSE_ID)
    private val ANSWER_VIEW_IDS = setOf(FL_BTN_RIGHT_ID, TV_BTN_RIGHT_ID, TV_OPEN_ID)
    private val CALL_SPECIFIC_VIEW_IDS = setOf(
        TV_TITLE_ID,
        TV_CONTENT_ID,
        FL_BTN_LEFT_ID,
        TV_BTN_LEFT_ID,
        TV_CLOSE_ID,
        FL_BTN_RIGHT_ID,
        TV_BTN_RIGHT_ID,
        TV_OPEN_ID,
        ELAPSE_TIME_ID,
    )

    private val CALL_KEYWORDS = listOf(
        "cuộc gọi thoại đến",
        "cuộc gọi video đến",
        "zalo: cuộc gọi",
        "đang gọi cho bạn",
        "cuộc gọi đang diễn ra",
        "đang trong cuộc gọi",
        "mời bạn vào cuộc gọi",
        "cuộc gọi nhỡ",
        "đang đổ chuông",
        "đang gọi...",
        "incoming call",
        "ongoing call",
        "missed call",
        "video call",
        "voice call",
    )

    private val STATUS_KEYWORDS = listOf(
        "cuộc gọi",
        "đang gọi",
        "đổ chuông",
        "đang diễn ra",
        "incoming",
        "ongoing",
        "calling",
        "ringing",
        "zalo:",
    )

    private data class RemoteViewsSnapshot(
        var layoutMatched: Boolean = false,
        var viewIdMatched: Boolean = false,
        var title: String? = null,
        var content: String? = null,
        val texts: MutableList<String> = mutableListOf(),
        var avatar: Bitmap? = null,
        var declinePi: PendingIntent? = null,
        var answerPi: PendingIntent? = null,
        val clickIntents: MutableList<PendingIntent> = mutableListOf(),
        var chronometerBaseMs: Long = 0L,
    )

    fun isZaloCall(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName != ZALO_PKG) return false
        val n = sbn.notification ?: return false
        val extras = n.extras

        if (n.category == Notification.CATEGORY_CALL) return true
        if (n.fullScreenIntent != null) return true

        val channelId = n.channelId?.lowercase().orEmpty()
        if (channelId.contains("call") || channelId.contains("voip")) return true

        val template = extras?.getString(NotificationCompat.EXTRA_TEMPLATE).orEmpty()
        if (template.contains("CallStyle", ignoreCase = true)) return true

        val extraTexts = buildString {
            append(extras?.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty())
            append(' ')
            append(extras?.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty())
            append(' ')
            append(extras?.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString().orEmpty())
            append(' ')
            append(n.tickerText?.toString().orEmpty())
        }.lowercase()

        if (CALL_KEYWORDS.any { extraTexts.contains(it) }) return true

        val rvSnap = inspectAllRemoteViews(null, n)
        if (rvSnap.layoutMatched || rvSnap.viewIdMatched) return true

        val rvCombinedLower = rvSnap.texts.joinToString(" ").lowercase()
        return CALL_KEYWORDS.any { rvCombinedLower.contains(it) }
    }

    fun parse(context: Context, sbn: StatusBarNotification): ZaloCallInfo? {
        if (!isZaloCall(sbn)) return null
        val n = sbn.notification ?: return null
        val extras = n.extras ?: Bundle.EMPTY

        val rvSnap = inspectAllRemoteViews(context, n)

        val extraTitle = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val extraText = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val extraBigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val callPersonName = extractCallPersonName(extras)

        val nonStatusRvText = rvSnap.texts.firstOrNull { txt ->
            val lower = txt.lowercase()
            STATUS_KEYWORDS.none { lower.contains(it) } &&
                !lower.equals("từ chối", ignoreCase = true) &&
                !lower.equals("trả lời", ignoreCase = true) &&
                !lower.equals("kết thúc", ignoreCase = true) &&
                !lower.equals("mở", ignoreCase = true)
        }

        val statusRvText = rvSnap.texts.firstOrNull { txt ->
            val lower = txt.lowercase()
            STATUS_KEYWORDS.any { lower.contains(it) }
        } ?: rvSnap.texts.getOrNull(1)

        var callerName = rvSnap.title?.takeIf { it.isNotBlank() }
            ?: callPersonName?.takeIf { it.isNotBlank() }
            ?: extraTitle.takeIf { it.isNotBlank() }
            ?: nonStatusRvText?.takeIf { it.isNotBlank() }
            ?: rvSnap.texts.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: "Cuộc gọi Zalo"

        var statusText = rvSnap.content?.takeIf { it.isNotBlank() }
            ?: extraText.takeIf { it.isNotBlank() }
            ?: extraBigText.takeIf { it.isNotBlank() }
            ?: statusRvText?.takeIf { it.isNotBlank() && it != callerName }
            ?: "Zalo: Cuộc gọi đến"

        // Nếu callerName chứa "cuộc gọi" còn statusText là tên riêng (không chứa từ khóa trạng thái), tự động hoán đổi
        val callerLower = callerName.lowercase()
        val statusLower = statusText.lowercase()
        if (callerLower.contains("cuộc gọi") && !statusLower.contains("cuộc gọi") &&
            STATUS_KEYWORDS.none { statusLower.contains(it) }
        ) {
            val tmp = callerName
            callerName = statusText
            statusText = tmp
        }

        val combinedLower = "$callerName $statusText $extraTitle $extraText $extraBigText ${rvSnap.texts.joinToString(" ")}".lowercase()
        val channelLower = n.channelId?.lowercase().orEmpty()

        val isVideoCall = combinedLower.contains("video") || channelLower.contains("video")

        val isOngoingFlag = sbn.isOngoing || (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val hasMissedKeyword = combinedLower.contains("cuộc gọi nhỡ") || combinedLower.contains("missed call") ||
            combinedLower.contains("nhỡ cuộc gọi")
        val hasOngoingKeyword = channelLower.contains("incall") ||
            combinedLower.contains("đang diễn ra") ||
            combinedLower.contains("đang trong cuộc gọi") ||
            combinedLower.contains("ongoing") ||
            combinedLower.contains("00:") ||
            (rvSnap.chronometerBaseMs > 0L && rvSnap.answerPi == null && rvSnap.clickIntents.size <= 1)

        val state = when {
            hasMissedKeyword -> CallState.MISSED
            !isOngoingFlag && n.fullScreenIntent == null && rvSnap.declinePi == null && rvSnap.answerPi == null -> CallState.MISSED
            hasOngoingKeyword -> CallState.ONGOING
            else -> CallState.INCOMING
        }

        if (state == CallState.MISSED) {
            return null
        }

        val avatar = rvSnap.avatar
            ?: extractLargeIconBitmap(context, n, extras)

        val declineFromActions = findActionIntent(
            n,
            listOf("từ chối", "kết thúc", "decline", "end", "hang up", "reject", "hủy"),
        )
        val answerFromActions = findActionIntent(
            n,
            listOf("trả lời", "nghe", "chấp nhận", "answer", "accept", " mở ", "mở"),
        )

        val hangUpExtraPi = runCatching {
            @Suppress("DEPRECATION")
            extras.getParcelable("android.hangUpIntent") as? PendingIntent
        }.getOrNull()

        val answerExtraPi = runCatching {
            @Suppress("DEPRECATION")
            extras.getParcelable("android.answerIntent") as? PendingIntent
        }.getOrNull()

        val fallbackDeclinePi = PendingIntent.getBroadcast(
            context,
            35011,
            Intent(ACTION_ZALO_END_CALL).setPackage(ZALO_PKG),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val declineIntent = rvSnap.declinePi
            ?: hangUpExtraPi
            ?: declineFromActions
            ?: rvSnap.clickIntents.getOrNull(0)
            ?: fallbackDeclinePi

        val answerIntent = rvSnap.answerPi
            ?: answerExtraPi
            ?: answerFromActions
            ?: rvSnap.clickIntents.getOrNull(1)
            ?: n.fullScreenIntent
            ?: n.contentIntent

        val contentIntent = n.contentIntent
            ?: n.fullScreenIntent
            ?: answerIntent

        return ZaloCallInfo(
            callerName = callerName,
            statusText = statusText,
            state = state,
            isVideoCall = isVideoCall,
            avatar = avatar,
            declineIntent = declineIntent,
            answerIntent = answerIntent,
            contentIntent = contentIntent,
            chronometerBaseMs = rvSnap.chronometerBaseMs,
        )
    }

    @Suppress("DEPRECATION")
    private fun inspectAllRemoteViews(context: Context?, n: Notification): RemoteViewsSnapshot {
        val snap = RemoteViewsSnapshot()
        val views = listOfNotNull(n.bigContentView, n.headsUpContentView, n.contentView).distinct()
        for (rv in views) {
            inspectRemoteViews(context, rv, snap)
        }
        return snap
    }

    private fun inspectRemoteViews(context: Context?, rv: RemoteViews, snap: RemoteViewsSnapshot) {
        runCatching {
            if (rv.layoutId in CALL_LAYOUT_IDS) {
                snap.layoutMatched = true
            }

            val bitmapsCache = extractBitmapCacheList(rv)
            val actionsField = findField(rv.javaClass, "mActions") ?: return
            actionsField.isAccessible = true
            val actions = actionsField.get(rv) as? Iterable<*> ?: return

            for (action in actions) {
                if (action == null) continue
                inspectAction(context, action, bitmapsCache, snap)
            }
        }
    }

    private fun inspectAction(
        context: Context?,
        action: Any,
        bitmapsCache: List<Bitmap?>,
        snap: RemoteViewsSnapshot,
    ) {
        runCatching {
            val actionClass = action.javaClass
            val className = actionClass.simpleName

            val viewId = (findField(actionClass, "viewId") ?: findField(actionClass, "mViewId"))
                ?.apply { isAccessible = true }
                ?.get(action) as? Int ?: -1

            if (viewId in CALL_SPECIFIC_VIEW_IDS) {
                snap.viewIdMatched = true
            }

            // Nếu là ViewGroupActionAdd / nested RemoteViews thì đệ quy vào mNestedViews
            findField(actionClass, "mNestedViews")?.let { nestedField ->
                nestedField.isAccessible = true
                (nestedField.get(action) as? RemoteViews)?.let { nestedRv ->
                    inspectRemoteViews(context, nestedRv, snap)
                }
            }

            val methodName = (findField(actionClass, "methodName") ?: findField(actionClass, "mMethodName"))
                ?.apply { isAccessible = true }
                ?.get(action) as? String

            val value = (findField(actionClass, "value") ?: findField(actionClass, "mValue"))
                ?.apply { isAccessible = true }
                ?.get(action)

            // 1. Trích xuất văn bản (setText / CharSequence)
            if (methodName == "setText" || value is CharSequence) {
                val textStr = (value as? CharSequence)?.toString()?.trim().orEmpty()
                if (textStr.isNotEmpty()) {
                    if (viewId == TV_TITLE_ID && snap.title.isNullOrBlank()) {
                        snap.title = textStr
                    } else if (viewId == TV_CONTENT_ID && snap.content.isNullOrBlank()) {
                        snap.content = textStr
                    }
                    if (textStr !in snap.texts) {
                        snap.texts.add(textStr)
                    }
                }
            }

            // 2. Trích xuất Bitmap avatar (setImageBitmap / BitmapReflectionAction / setImageIcon)
            if (methodName == "setImageBitmap" || className.contains("BitmapReflectionAction", ignoreCase = true)) {
                val bmp = (value as? Bitmap)
                    ?: (findField(actionClass, "bitmap") ?: findField(actionClass, "mBitmap"))
                        ?.apply { isAccessible = true }
                        ?.get(action) as? Bitmap
                    ?: ((findField(actionClass, "bitmapId") ?: findField(actionClass, "mBitmapId"))
                        ?.apply { isAccessible = true }
                        ?.get(action) as? Int)?.let { id -> bitmapsCache.getOrNull(id) }

                if (bmp != null && (viewId == IMV_AVATAR_ID || snap.avatar == null)) {
                    snap.avatar = bmp
                }
            } else if (methodName == "setImageIcon" && value is Icon && context != null) {
                if (viewId == IMV_AVATAR_ID || snap.avatar == null) {
                    iconToBitmap(context, value)?.let { snap.avatar = it }
                }
            }

            // 3. Trích xuất PendingIntent (SetOnClickPendingIntent / SetOnClickResponse)
            val pi = extractPendingIntentFromAction(action, actionClass, className)
            if (pi != null) {
                when (viewId) {
                    in DECLINE_VIEW_IDS -> if (snap.declinePi == null) snap.declinePi = pi
                    in ANSWER_VIEW_IDS -> if (snap.answerPi == null) snap.answerPi = pi
                }
                if (pi !in snap.clickIntents) {
                    snap.clickIntents.add(pi)
                }
            }

            // 4. Trích xuất Chronometer base time
            if (className.contains("Chronometer", ignoreCase = true) ||
                viewId == ELAPSE_TIME_ID ||
                methodName == "setBase"
            ) {
                val baseVal = (value as? Long)
                    ?: (findField(actionClass, "base") ?: findField(actionClass, "mBase"))
                        ?.apply { isAccessible = true }
                        ?.get(action) as? Long
                if (baseVal != null && baseVal > 0L) {
                    snap.chronometerBaseMs = baseVal
                }
            }
        }
    }

    private fun extractPendingIntentFromAction(
        action: Any,
        actionClass: Class<*>,
        className: String,
    ): PendingIntent? {
        runCatching {
            val directPiField = findField(actionClass, "pendingIntent")
                ?: findField(actionClass, "mPendingIntent")
            if (directPiField != null) {
                directPiField.isAccessible = true
                (directPiField.get(action) as? PendingIntent)?.let { return it }
            }

            val responseField = findField(actionClass, "mResponse")
                ?: findField(actionClass, "response")
            if (responseField != null) {
                responseField.isAccessible = true
                val responseObj = responseField.get(action)
                if (responseObj != null) {
                    val respPiField = findField(responseObj.javaClass, "mPendingIntent")
                        ?: findField(responseObj.javaClass, "pendingIntent")
                    if (respPiField != null) {
                        respPiField.isAccessible = true
                        (respPiField.get(responseObj) as? PendingIntent)?.let { return it }
                    }
                }
            }

            if (className.contains("OnClick", ignoreCase = true)) {
                var c: Class<*>? = actionClass
                while (c != null && c != Any::class.java) {
                    for (f in c.declaredFields) {
                        f.isAccessible = true
                        when (val v = f.get(action)) {
                            is PendingIntent -> return v
                            else -> if (v != null && v.javaClass.simpleName.contains("Response", ignoreCase = true)) {
                                val innerPi = (findField(v.javaClass, "mPendingIntent") ?: findField(v.javaClass, "pendingIntent"))
                                    ?.apply { isAccessible = true }
                                    ?.get(v) as? PendingIntent
                                if (innerPi != null) return innerPi
                            }
                        }
                    }
                    c = c.superclass
                }
            }
        }
        return null
    }

    private fun extractBitmapCacheList(rv: RemoteViews): List<Bitmap?> {
        return runCatching {
            val cacheField = findField(rv.javaClass, "mBitmapCache") ?: return emptyList()
            cacheField.isAccessible = true
            val cacheObj = cacheField.get(rv) ?: return emptyList()
            val bitmapsField = findField(cacheObj.javaClass, "mBitmaps") ?: return emptyList()
            bitmapsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            (bitmapsField.get(cacheObj) as? List<Bitmap?>).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun findField(clazz: Class<*>, name: String): Field? {
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            runCatching { return current.getDeclaredField(name) }
            current = current.superclass
        }
        return null
    }

    private fun extractCallPersonName(extras: Bundle): String? {
        return runCatching {
            @Suppress("DEPRECATION")
            val p = extras.getParcelable("android.callPerson") as? Person
            p?.name?.toString()?.trim()
        }.getOrNull()
    }

    private fun extractLargeIconBitmap(context: Context, n: Notification, extras: Bundle): Bitmap? {
        return runCatching {
            @Suppress("DEPRECATION")
            when (val raw = extras.get(NotificationCompat.EXTRA_LARGE_ICON)) {
                is Bitmap -> raw
                is Icon -> iconToBitmap(context, raw)
                else -> n.getLargeIcon()?.let { iconToBitmap(context, it) }
            }
        }.getOrNull()
    }

    private fun iconToBitmap(context: Context, icon: Icon): Bitmap? {
        return runCatching {
            val drawable = icon.loadDrawable(context) ?: return null
            val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceIn(64, 256) else 160
            val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceIn(64, 256) else 160
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            bmp
        }.getOrNull()
    }

    private fun findActionIntent(n: Notification, keywords: List<String>): PendingIntent? {
        return n.actions?.firstOrNull { action ->
            val title = action.title?.toString()?.lowercase().orEmpty()
            keywords.any { title.contains(it) }
        }?.actionIntent
    }
}
