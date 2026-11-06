package com.originisle.android.service

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.cards.ChargingCard
import com.originisle.android.cards.BatteryCanvasRenderer
import com.originisle.android.cards.createRedCrossCircleIcon
import com.originisle.android.cards.EarbudsCard
import com.originisle.android.cards.FlashlightCard
import com.originisle.android.cards.GenericCard
import com.originisle.android.cards.MediaCard
import com.originisle.android.cards.MessageCard
import com.originisle.android.cards.NavigationCard
import com.originisle.android.cards.OtpCard
import com.originisle.android.cards.PaymentCard
import com.originisle.android.cards.PrivacyCard
import com.originisle.android.cards.RideCard
import com.originisle.android.cards.RingerModeCard
import com.originisle.android.cards.DownloadCard
import com.originisle.android.cards.QuickShareCard
import com.originisle.android.quickshare.QuickShareDetector
import com.originisle.android.download.DownloadDetector
import com.originisle.android.bank.VietnamBankDetector
import com.originisle.android.ride.VietnamRideDetector
import com.originisle.android.cards.SportsCard
import com.originisle.android.cards.fallbackClickIntent
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.otp.OtpDetector
import com.originisle.android.sports.SportsFeed
import com.originisle.android.sports.SportsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Listens to every notification on the device and re-casts eligible ones into the OriginIsland.
 *
 * This class owns only the CAST/SKIP DECISION for each notification — which kind of card (if any)
 * it becomes, and why one wasn't posted. The actual card is built and posted by the type-specific
 * object in `cards/` ([GenericCard], [MediaCard], [PaymentCard], [SportsCard]); [IconCache] is the
 * state they + [MediaControlReceiver] share.
 */
class NotificationCastListener : NotificationListenerService() {

    /** Outcome of [forceRebind], so callers can tell the user something truthful. */
    enum class RebindResult { REBINDING, ALREADY_CONNECTED, NO_ACCESS, FAILED }

    companion object {
        /** The bound listener instance, or null if the service isn't connected. */
        @Volatile
        var instance: NotificationCastListener? = null

        /** Health signals for the in-app status line. 0 = never. */
        @Volatile
        var connectedAt: Long = 0L
        @Volatile
        var lastEventAt: Long = 0L

        /**
         * Force the system to re-bind this listener after the process was killed (OriginOS kills it
         * on swipe-away), and report whether a rebind could even be attempted.
         *
         * [NotificationListenerService.requestRebind] alone is NOT enough — the whole reason the
         * Reconnect button did nothing. It early-returns unless the component was explicitly
         * snoozed by [requestUnbind], and a process kill never snoozes it, so the system still
         * believes we're bound. Toggling the component's enabled state instead fires
         * ACTION_PACKAGE_CHANGED, which makes NotificationManagerService rebind every
         * approved-but-unbound listener. The grant lives in Settings.Secure keyed by component and
         * survives the toggle; DONT_KILL_APP keeps our own process up across it.
         */
        fun forceRebind(context: Context): RebindResult {
            val component = ComponentName(context.applicationContext, NotificationCastListener::class.java)
            if (!NotificationManagerCompat.getEnabledListenerPackages(context)
                    .contains(context.packageName)
            ) {
                return RebindResult.NO_ACCESS
            }
            if (instance != null) return RebindResult.ALREADY_CONNECTED
            // The rebind is asynchronous (it waits on a package broadcast), so a burst of callers —
            // onResume firing repeatedly while the listener is still down — must not keep tearing
            // down a binding that is already on its way up.
            val now = System.currentTimeMillis()
            if (now - lastRebindAt < REBIND_THROTTLE_MS) return RebindResult.REBINDING

            // Separate runCatching per step: the re-enable must be attempted even if the disable
            // threw. See [ensureEnabled] for why being left disabled is unrecoverable.
            val pm = context.applicationContext.packageManager
            val disabled = runCatching {
                pm.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }.isSuccess
            val enabled = runCatching {
                pm.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }.isSuccess
            // Only arm the throttle once the toggle actually landed, so a failure doesn't leave the
            // next 10s of taps reporting "rebinding" when nothing is.
            if (!disabled || !enabled) return RebindResult.FAILED
            lastRebindAt = now
            // After the toggle the component IS considered unbound, so this call is no longer a
            // no-op and can shortcut the wait for the package broadcast to land.
            runCatching { requestRebind(component) }
            return RebindResult.REBINDING
        }

        /**
         * Undo a [forceRebind] that only got halfway — the process can die between its two calls,
         * and DONT_KILL_APP only stops PackageManager from killing us, not OriginOS. A component
         * left DISABLED persists across reboots and never binds again, while the grant it's checked
         * against lives in Settings.Secure and still reads as granted, so the UI would show a
         * healthy listener forever. Called on every process start.
         */
        fun ensureEnabled(context: Context) {
            val pm = context.applicationContext.packageManager
            val component = ComponentName(context.applicationContext, NotificationCastListener::class.java)
            runCatching {
                if (pm.getComponentEnabledSetting(component) ==
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                ) {
                    Log.w(TAG, "listener component was left disabled by a partial rebind — re-enabling")
                    pm.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP,
                    )
                }
            }
        }

        private const val PREFS = "experimental_prefs"
        private const val TAG = "NotificationCast"

        /** Minimum gap between component toggles in [forceRebind]. */
        private const val REBIND_THROTTLE_MS = 10_000L

        @Volatile
        private var lastRebindAt = 0L

        /** Framework/system packages whose notifications are noise on the island. */
        private val SYSTEM_PKGS = setOf(
            "android",
            "com.android.systemui",
            "com.android.shell",
            "com.vivo.upslide",
            "com.vivo.smartmultiwindow",
            "com.vivo.musicwidgetmix",
            "com.vivo.mediacontrol",
            "com.android.bbkmusic",
            "com.vivo.music",
        )

        /** Ứng dụng quản lý thiết bị đeo & đồng hồ thông minh Samsung (Fit 3, Galaxy Watch, Buds). */
        private val WEARABLE_SAMSUNG_PKGS = setOf(
            "com.samsung.wearable.fit3plugin",
            "com.samsung.android.wearable.fit3plugin",
            "com.samsung.android.waterplugin",
            "com.samsung.android.app.watchmanager",
            "com.samsung.accessory",
            "com.samsung.accessory.neobeanmgr",
            "com.samsung.accessory.atticmgr",
        )

        /** Score apps whose live-match notifications get parsed into a football card with crests. */
        private val SCORE_APPS = setOf(
            "com.google.android.googlequicksearchbox", // Google (favourite-team live scores)
            "com.sofascore.results",       // SofaScore
            "com.mobilefootie.fotmobpro",  // FotMob
            "com.mobilefootie.wc2010",     // FotMob (free)
            "de.motain.iliga",             // OneFootball
            "com.scores365",               // 365Scores
            "com.livescore",               // LiveScore
        )

        /**
         * Turn-by-turn navigation apps. Most set [Notification.CATEGORY_NAVIGATION] and are matched
         * on that alone; Maps is listed by package because it's worth casting even on a build that
         * doesn't set the category.
         *
         * Waze is deliberately NOT here. Its only notification is a `CLOSE_WAZE_CHANNEL`
         * foreground-service notice — title "Waze", text "Đang hoạt động. Chạm để mở.", one "Tắt"
         * action — with no subText, no large icon, no `shortCriticalText` and all three RemoteViews
         * slots null (verified against `dumpsys notification --noredact`). It carries no turn data
         * to build a card from, and it is byte-identical whether Waze is navigating or merely open,
         * so there's nothing to key a "navigating" card off either. Waze renders its turn banner
         * inside its own app and publishes none of it to the notification system.
         *
         * This only steers Waze away from [NavigationCard] — it still falls through to the generic
         * ongoing card below, same as any other app's plain foreground-service notice. No special
         * filtering for that pattern: a user who doesn't want it can turn Waze off in the Apps tab,
         * the same way they'd opt out of any other app's notifications.
         */
        private val NAV_APPS = setOf(
            "com.google.android.apps.maps", // Google Maps
        )

        /** Wallet / bank / money apps whose notifications are treated as payments (Apple-Pay card). */
        private val PAYMENT_APPS = setOf(
            "com.google.android.apps.walletnfcrel",     // Google Wallet
            "com.paypal.android.p2pmobile",             // PayPal
            // Ngan hang & Vi dien tu Viet Nam
            "com.VCB",                                  // Vietcombank
            "vn.com.techcombank.bb.app",                // Techcombank
            "com.mbmobile",                             // MB Bank
            "com.vnpay.bidv",                           // BIDV
            "com.mservice.momopay",                     // MoMo
            "vn.com.vng.zalopay",                       // ZaloPay
            "com.zing.zalo",                            // Zalo
            "vn.com.vpbank.neo",                        // VPBank NEO
            "com.acb.mobile",                           // ACB ONE
        )

        /** Payment verbs — any app whose notification pairs one of these with an amount is a payment. */
        private val PAYMENT_WORDS = listOf(
            "paid", "payment", "you sent", "you received", "charged", "purchase",
            "transaction", "debited", "credited", "receipt", "spent", "approved",
            "thanh toán", "chuyển tiền", "chuyển khoản", "nhận được", "biến động số dư",
            "giao dịch", "số dư", "thẻ", "tiền vào", "tiền ra",
        )

        /** "€12.90", "12,90 €", "$5", "£3.20", "EUR 40" … first match is used as the amount. */
        private val AMOUNT = Regex(
            """(?:[€$£¥đ]|\b(?:EUR|USD|GBP|CHF|VND|VNĐ)\b)\s?\d[\d.,]*|\d[\d.,]*\s?(?:[€$£¥đ]|\b(?:EUR|USD|GBP|CHF|VND|VNĐ)\b)""",
            RegexOption.IGNORE_CASE,
        )

        /** Danh sách các ứng dụng âm nhạc phổ biến (Zing MP3, Nhaccuatui, Spotify, YouTube Music, Soundcloud, ...). */
        val KNOWN_MUSIC_APPS = setOf(
            "com.zing.mp3",
            "com.vng.zingmp3",
            "ht.bkit.nhaccuatui",
            "com.spotify.music",
            "com.spotify.music.canary",
            "com.google.android.apps.youtube.music",
            "com.soundcloud.android",
            "com.apple.android.music",
            "deezer.android.app",
            "com.amazon.mp3",
            "com.aspiro.tidal",
            "com.audiomack",
            "tunein.player",
            "fm.player",
            "vstudio.android.musicplayer",
        )

        fun isKnownMusicApp(pkg: String?): Boolean {
            if (pkg.isNullOrBlank()) return false
            val lower = pkg.lowercase()
            return lower in KNOWN_MUSIC_APPS ||
                lower.contains("zing.mp3") ||
                lower.contains("zingmp3") ||
                lower.contains("nhaccuatui") ||
                lower.contains("spotify") ||
                lower.contains("soundcloud") ||
                lower.contains("youtubemusic")
        }

        fun isMediaStyleOrTransport(n: Notification): Boolean {
            val template = n.extras?.getString(NotificationCompat.EXTRA_TEMPLATE)
            return n.category == Notification.CATEGORY_TRANSPORT ||
                template == "android.app.Notification\$MediaStyle" ||
                template == "androidx.media.app.NotificationCompat\$MediaStyle" ||
                template == "android.app.Notification\$DecoratedMediaCustomViewStyle" ||
                template == "androidx.media.app.NotificationCompat\$DecoratedMediaCustomViewStyle"
        }

        fun extractMediaSessionToken(extras: android.os.Bundle?): MediaSession.Token? {
            if (extras == null) return null
            return runCatching {
                extras.getParcelable(NotificationCompat.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
            }.getOrNull() ?: runCatching {
                val raw = extras.get(NotificationCompat.EXTRA_MEDIA_SESSION)
                when (raw) {
                    is MediaSession.Token -> raw
                    is android.os.Parcelable -> {
                        val tokenMethod = runCatching { raw.javaClass.getMethod("getToken") }.getOrNull()
                            ?: runCatching { raw.javaClass.getMethod("getInner") }.getOrNull()
                        tokenMethod?.invoke(raw) as? MediaSession.Token
                    }
                    else -> null
                }
            }.getOrNull()
        }
    }

    private val pollHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Last-logged outcome per notification key, so repeated updates don't flood the log. */
    private val lastOutcome = ConcurrentHashMap<String, String>()

    /**
     * Last successfully-resolved crest icons per match id. A goal update's notification text can
     * name a team slightly differently than the kickoff notification did (or a lookup can just fail
     * transiently) — refetching fresh every time then means a badge that WAS showing can vanish the
     * moment its team scores. Keep the last good icon per side and only replace it when a fresh fetch
     * actually finds one, instead of overwriting a known-good badge with a failed lookup's null.
     */
    private val lastMatchIcons = ConcurrentHashMap<Int, SportsFeed.MatchIcons>()

    /**
     * Last-seen [StatusBarNotification.getPostTime] per score-app notification key. onNotificationPosted
     * is meant to fire on every update, but OriginOS can visibly delay/batch that delivery for a
     * specific app — the score card was observed sitting stale until some UNRELATED notification came
     * in and (presumably) flushed the batch. Polled the same way media/chronometer already are, but
     * gated on postTime actually changing so this doesn't reprocess (and repost) unchanged content
     * every second — a live score's own post frequency is way under 1/sec anyway.
     */
    private val lastScorePostTime = ConcurrentHashMap<String, Long>()

    /**
     * Last [StatusBarNotification.getPostTime] actually cast, per notification key — for one-shot
     * "message"/payment casts only (see the two call sites). OriginOS (verified on vivo) redelivers
     * onNotificationPosted for an app's OTHER already-cast notifications whenever any one of that
     * app's notifications changes — WhatsApp and Instagram threads, and wallet payment apps, all
     * exhibit this: a brand new message elsewhere recasts every older, untouched thread's card back
     * onto the island. A genuine update always carries a fresh postTime from the source app's own
     * notify() call, so gating on postTime being unchanged catches only the redelivery, never real
     * content. Ongoing/call/progress cards don't use this — their own ticking/polling paths
     * deliberately re-cast with an unchanged postTime (see [pollRunnable]).
     */
    private val lastCastPostTime = ConcurrentHashMap<String, Long>()

    /** Lưu trữ tiến độ và trạng thái hoàn tất cuối cùng của tệp đang truyền / tải xuống. */
    private val transferProgressMap = ConcurrentHashMap<Int, Int>()
    private val transferFinishedMap = ConcurrentHashMap<Int, Boolean>()
    private val activeDownloadIds = ConcurrentHashMap.newKeySet<Int>()
    private val activeQuickShareIds = ConcurrentHashMap.newKeySet<Int>()

    /** Lưu trạng thái kết nối cuối cùng và mốc báo sạc đầy của thiết bị đeo để chống pop-up ảo. */
    private val wearableConnectionState = ConcurrentHashMap<String, Boolean>()
    private val fullyChargedNoticeMap = ConcurrentHashMap<String, Long>()

    private fun isWearableConnected(deviceKey: String, devName: String): Boolean {
        val inRam = wearableConnectionState[deviceKey]
        if (inRam != null) return inRam
        val prefs = getSharedPreferences(PREFS, 0)
        val inPrefs = prefs.getBoolean("wearable_conn_state_$deviceKey", false)
        wearableConnectionState[deviceKey] = inPrefs
        return inPrefs || EarbudsCard.isConnected(devName)
    }

    private fun setWearableConnected(deviceKey: String, devName: String, connected: Boolean) {
        wearableConnectionState[deviceKey] = connected
        val prefs = getSharedPreferences(PREFS, 0)
        prefs.edit().putBoolean("wearable_conn_state_$deviceKey", connected).apply()
        EarbudsCard.setConnectedState(devName, connected)
    }

    /**
     * Bóc tách chuyên sâu % pin từ notification của Samsung Galaxy Wearable / Fit 3 Plugin:
     * Quét qua extras, tickerText, EXTRA_TEXT_LINES, android.messages, các sub-bundle, subText, v.v.
     */
    private fun extractDeepWearableBattery(sbn: StatusBarNotification): Int {
        val n = sbn.notification
        val extras = n.extras ?: return -1

        // 1. Kiểm tra trực tiếp các key số nguyên trong extras
        val directKeys = listOf(
            "battery_level", "level", "percentage", "battery", "capacity",
            "batteryLevel", "EXTRA_BATTERY_LEVEL", "value", "LEVEL", "battery_status"
        )
        for (key in directKeys) {
            val v = extras.get(key)
            val num = when (v) {
                is Number -> v.toInt()
                is String -> v.trim().replace("%", "").toIntOrNull()
                else -> null
            }
            if (num != null && num in 0..100) return num
        }

        // 2. Thu thập toàn bộ chuỗi văn bản từ notification
        val textSnippets = mutableListOf<String>()

        n.tickerText?.toString()?.takeIf { it.isNotBlank() }?.let { textSnippets.add(it) }

        val stringKeys = listOf(
            NotificationCompat.EXTRA_TITLE,
            NotificationCompat.EXTRA_TEXT,
            NotificationCompat.EXTRA_SUB_TEXT,
            NotificationCompat.EXTRA_BIG_TEXT,
            NotificationCompat.EXTRA_SUMMARY_TEXT,
            NotificationCompat.EXTRA_INFO_TEXT,
            NotificationCompat.EXTRA_TITLE_BIG,
        )
        for (k in stringKeys) {
            runCatching { extras.getCharSequence(k)?.toString() }.getOrNull()?.takeIf { it.isNotBlank() }?.let {
                textSnippets.add(it)
            }
        }

        // EXTRA_TEXT_LINES
        runCatching {
            extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES)?.forEach { line ->
                line?.toString()?.takeIf { it.isNotBlank() }?.let { textSnippets.add(it) }
            }
        }

        // android.messages & android.messages.historic
        runCatching {
            val msgArr = extras.getParcelableArray("android.messages")
                ?: extras.getParcelableArray("android.messages.historic")
            msgArr?.forEach { item ->
                if (item is android.os.Bundle) {
                    item.getCharSequence("text")?.toString()?.takeIf { it.isNotBlank() }?.let { textSnippets.add(it) }
                    item.getCharSequence("sender")?.toString()?.takeIf { it.isNotBlank() }?.let { textSnippets.add(it) }
                }
            }
        }

        // Duyệt qua tất cả key trong extras tìm Bundle con hoặc chuỗi văn bản
        runCatching {
            for (key in extras.keySet()) {
                val v = extras.get(key)
                when (v) {
                    is CharSequence -> if (v.isNotBlank()) textSnippets.add(v.toString())
                    is android.os.Bundle -> {
                        for (subKey in v.keySet()) {
                            val subVal = v.get(subKey)
                            if (subVal is CharSequence && subVal.isNotBlank()) {
                                textSnippets.add(subVal.toString())
                            } else if (subVal is Number && (subKey.contains("battery", ignoreCase = true) || subKey.contains("level", ignoreCase = true))) {
                                val num = subVal.toInt()
                                if (num in 0..100) return num
                            }
                        }
                    }
                }
            }
        }

        // 3. Quét regex trên từng text snippet
        // Định dạng "Pin: 85%", "Galaxy Fit3 · 85%", "Fit 3: 85%", "Mức pin: 85%", hoặc "85%"
        val labelRegex = Regex("""(?:pin|battery|mức pin|fit\s*3|galaxy\s*fit|watch)\s*[:：·\-]?\s*(\d{1,3})\s*%?""", RegexOption.IGNORE_CASE)
        val percentRegex = Regex("""(\d{1,3})\s*%""")

        for (text in textSnippets) {
            val lblMatch = labelRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()
            if (lblMatch != null && lblMatch in 0..100) return lblMatch
            val pctMatch = percentRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()
            if (pctMatch != null && pctMatch in 0..100) return pctMatch
        }

        return -1
    }

    /**
     * Kiểm tra xem Đảo nhạc gốc của Vivo THỰC SỰ đang active với bài hát của hệ thống hay không.
     * Đảm bảo chỉ trả về true khi gói nhạc hệ thống Vivo thực sự đang phát bài hát của nó,
     * tuyệt đối không tự ý xóa thẻ của Zing MP3, Spotify hay các app nhạc bên thứ ba khi người dùng đang nghe từ các app này.
     */
    private fun isOriginNativePlayerActive(): Boolean {
        val notifs = runCatching { activeNotifications }.getOrNull() ?: return false

        // Kiểm tra xem có app nhạc bên thứ 3 (Zing MP3, Spotify, Nhaccuatui...) đang phát / có thông báo nhạc không
        val hasActiveThirdPartyMusic = notifs.any { active ->
            val pkg = active.packageName.lowercase()
            if (pkg in MediaCard.ORIGIN_NATIVE_MUSIC_PACKAGES || pkg == packageName || pkg in SYSTEM_PKGS) {
                false
            } else {
                val isKnown = isKnownMusicApp(pkg)
                val hasToken = active.notification.extras.containsKey(NotificationCompat.EXTRA_MEDIA_SESSION)
                val isMedia = isMediaStyleOrTransport(active.notification)
                (isKnown || hasToken || isMedia) && (active.isOngoing || (active.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0)
            }
        }

        // Nếu người dùng đang nghe nhạc từ Zing MP3, Spotify, YouTube Music..., không coi Đảo gốc Vivo là active độc chiếm
        if (hasActiveThirdPartyMusic) {
            return false
        }

        // Đảo nhạc gốc Vivo chỉ thực sự active khi có thông báo ongoing từ gói nhạc hệ thống với bài hát thực sự
        for (active in notifs) {
            val pkg = active.packageName.lowercase()
            if (active.packageName == packageName) continue

            val isNativeMusicPkg = pkg == "com.android.bbkmusic" || pkg == "com.vivo.music" || pkg == "com.vivo.musicwidgetmix"
            val isOngoing = active.isOngoing || (active.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0

            if (isNativeMusicPkg && isOngoing) {
                val title = active.notification.extras?.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim()
                if (!title.isNullOrBlank()) {
                    return true
                }
            }

            // Hoặc có SuperX card với scene MUSIC/AUDIO từ gói hệ thống
            val extras = active.notification.extras
            val isSuperX = active.tag == OriginIslandConstants.SUPERX_TAG ||
                extras?.containsKey(OriginIslandConstants.BUNDLE_KEY_SCENE) == true ||
                extras?.getBoolean(OriginIslandConstants.BUNDLE_KEY_ISLAND_NOTIFY, false) == true
            if (isSuperX) {
                val scene = extras?.getString(OriginIslandConstants.BUNDLE_KEY_SCENE)?.uppercase().orEmpty()
                if ((scene == "MUSIC" || scene == "AUDIO") && isNativeMusicPkg && isOngoing) {
                    return true
                }
            }
        }
        return false
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            try {
                if (isOriginNativePlayerActive()) {
                    MediaCard.clearAll(applicationContext)
                }
                val prefs = getSharedPreferences(PREFS, 0)
                val castNotifs = prefs.getBoolean("cast_notifications", true)
                val castMedia = prefs.getBoolean("cast_media_sessions", true)
                activeNotifications?.forEach { sbn ->
                    val pkgLower = sbn.packageName.lowercase()
                    if (pkgLower in MediaCard.ORIGIN_NATIVE_MUSIC_PACKAGES ||
                        sbn.tag == OriginIslandConstants.SUPERX_TAG
                    ) {
                        return@forEach
                    }
                    val extras = sbn.notification.extras
                    val token = extractMediaSessionToken(extras)
                    val isMusicApp = isKnownMusicApp(sbn.packageName)
                    val isMediaStyle = isMediaStyleOrTransport(sbn.notification)
                    if (token != null) {
                        if (castMedia && isAppEnabled(prefs, sbn.packageName)) {
                            MediaCard.post(
                                applicationContext, sbn,
                                MediaController(this@NotificationCastListener, token),
                            )
                        }
                    } else if (castMedia && isAppEnabled(prefs, sbn.packageName) && (isMusicApp || isMediaStyle)) {
                        val fallbackController = findMediaControllerFallback(sbn.packageName)
                        if (fallbackController != null) {
                            MediaCard.post(applicationContext, sbn, fallbackController)
                        }
                    } else if (castNotifs && sbn.isOngoing &&
                        extras.getBoolean(NotificationCompat.EXTRA_SHOW_CHRONOMETER, false)
                    ) {
                        onNotificationPosted(sbn) // refresh ticking chronometer
                    } else if (castNotifs && sbn.packageName in SCORE_APPS &&
                        isAppEnabled(prefs, sbn.packageName) &&
                        lastScorePostTime.put(sbn.key, sbn.postTime) != sbn.postTime
                    ) {
                        onNotificationPosted(sbn) // catch a score update onNotificationPosted was slow to deliver
                    } else if (castNotifs && prefs.getBoolean("cast_downloads", true) &&
                        sbn.isOngoing && DownloadDetector.isDownload(sbn) && isAppEnabled(prefs, sbn.packageName)
                    ) {
                        val dlInfo = DownloadDetector.parse(sbn)
                        if (dlInfo != null) {
                            val id = IconCache.castIdFor(sbn)
                            activeDownloadIds.add(id)
                            transferProgressMap[id] = dlInfo.progress
                            transferFinishedMap[id] = dlInfo.isFinished
                            DownloadCard.post(applicationContext, dlInfo, sbn)
                        }
                    } else if (castNotifs && prefs.getBoolean("cast_quickshare", true) &&
                        QuickShareDetector.isQuickShare(sbn) && isAppEnabled(prefs, sbn.packageName)
                    ) {
                        val id = IconCache.castIdFor(sbn)
                        if (QuickShareDetector.isIdleVisibilityNotification(sbn)) {
                            if (activeQuickShareIds.remove(id) || QuickShareCard.isTransferring()) {
                                activeQuickShareIds.clear()
                                transferProgressMap.remove(id)
                                transferFinishedMap.remove(id)
                                QuickShareCard.postFinished(applicationContext, id, sbn.packageName, "Quick Share")
                            }
                        } else {
                            val qsInfo = QuickShareDetector.parse(sbn)
                            if (qsInfo != null) {
                                if (qsInfo.isFinished || qsInfo.isCanceled || qsInfo.isRequest) {
                                    activeQuickShareIds.remove(id)
                                } else {
                                    activeQuickShareIds.add(id)
                                }
                                transferProgressMap[id] = if (qsInfo.isRequest && qsInfo.progress >= 100) 0 else qsInfo.progress
                                transferFinishedMap[id] = if (qsInfo.isRequest) false else qsInfo.isFinished
                                QuickShareCard.post(applicationContext, qsInfo, sbn)
                            }
                        }
                    }
                }
                SportsCard.refresh(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "poll error", e)
            }
            pollHandler.postDelayed(this, 1000L)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        connectedAt = System.currentTimeMillis()
        if (getSharedPreferences(PREFS, 0).getBoolean("cast_notifications", true)) {
            runCatching { PlaygroundService.keepAlive(this) }
        }
        reload()
        pollHandler.post(pollRunnable)
        registerBatteryReceiver()
        startSystemCards()
    }



    /** Cards fed by the system rather than by notifications: camera/mic, flashlight, earbuds, ringer. */
    private fun startSystemCards() {
        runCatching { FlashlightCard.start(this) }.onFailure { Log.e(TAG, "flashlight card failed", it) }
        runCatching { PrivacyCard.start(this) }.onFailure { Log.e(TAG, "privacy card failed", it) }
        runCatching { EarbudsCard.start(this) }.onFailure { Log.e(TAG, "earbuds card failed", it) }
        runCatching { RingerModeCard.register(this) }.onFailure { Log.e(TAG, "ringer card failed", it) }
    }

    private fun stopSystemCards() {
        runCatching { PrivacyCard.stop() }
        runCatching { FlashlightCard.stop() }
        runCatching { EarbudsCard.stop() }
        runCatching { RingerModeCard.unregister(this) }
    }

    /** Feeds [ChargingCard]. Registered while bound — the bound listener is what keeps us alive anyway. */
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runCatching { ChargingCard.onBatteryChanged(context, intent) }
                .onFailure { Log.e(TAG, "charging card failed", it) }
        }
    }
    private var batteryReceiverRegistered = false

    private fun registerBatteryReceiver() {
        if (batteryReceiverRegistered) return
        runCatching {
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), RECEIVER_NOT_EXPORTED)
            batteryReceiverRegistered = true
        }
    }

    private fun unregisterBatteryReceiver() {
        if (!batteryReceiverRegistered) return
        runCatching { unregisterReceiver(batteryReceiver) }
        batteryReceiverRegistered = false
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        unregisterBatteryReceiver()
        stopSystemCards()
        wearableConnectionState.clear()
        fullyChargedNoticeMap.clear()
        if (instance === this) instance = null
        connectedAt = 0L
        pollHandler.removeCallbacks(pollRunnable)
        // A system-initiated unbind DOES snooze the component, so unlike the process-kill case a
        // plain requestRebind works here (see [forceRebind]). Self-heals transient disconnects.
        runCatching { requestRebind(ComponentName(this, NotificationCastListener::class.java)) }
    }

    override fun onInterruptionFilterChanged(interruptionFilter: Int) {
        super.onInterruptionFilterChanged(interruptionFilter)
        runCatching { RingerModeCard.onInterruptionFilterChanged(this, interruptionFilter) }
    }

    /**
     * The Apps tab's per-app switch is a single master kill switch — it must gate every card type
     * (media sessions included), not just plain notifications. Checked once, up front, in
     * [onNotificationPosted] and [pollRunnable] so a disabled app never casts anything.
     */
    private fun isAppEnabled(prefs: SharedPreferences, pkg: String): Boolean {
        val ignored = prefs.getStringSet("cast_ignored_apps", emptySet()).orEmpty()
        if (pkg in ignored) return false
        val enabledApps = prefs.getStringSet("cast_enabled_apps", null)
        return enabledApps == null || pkg in enabledApps
    }

    /** Record a decision for the in-app Log screen (deduped: only when the outcome changes). */
    private fun log(sbn: StatusBarNotification, outcome: String, cast: Boolean) {
        lastEventAt = System.currentTimeMillis()
        if (lastOutcome.put(sbn.key, outcome) == outcome) return
        val app = appLabel(sbn.packageName)
        val title = sbn.notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()
        CastLog.add(app, title, outcome, cast)
    }

    /**
     * Replay every notification currently on the device through the normal cast path, so anything
     * eligible (downloads, navigation, calls, media, live scores) is (re)cast to the island at once.
     * Backs the "Recast all" button. Returns the number of notifications examined.
     */
    fun recastAll(): Int {
        wearableConnectionState.clear()
        fullyChargedNoticeMap.clear()
        val notifs = activeNotifications ?: return 0
        notifs.forEach { sbn ->
            // Force it past the [lastCastPostTime] redelivery guard: this is a deliberate replay, not
            // a redelivery, and it's the one way to bring back a message/payment card the user
            // dismissed from the island while its source notification is still on the device.
            lastCastPostTime.remove(sbn.key)
            if (sbn.packageName != packageName) runCatching { onNotificationPosted(sbn) }
        }
        return notifs.size
    }

    private fun reload() {
        val prefs = getSharedPreferences(PREFS, 0)
        val enabled = prefs.getBoolean("cast_notifications", true)
        val apps = prefs.getStringSet("cast_enabled_apps", null)
        activeNotifications?.forEach { sbn ->
            val eligible = apps == null || sbn.packageName in apps
            if (enabled && sbn.packageName != packageName && eligible) onNotificationPosted(sbn)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            if (sbn.packageName == packageName) {
                cleanUpRejectedSuperX(sbn)
                return
            }

            val pkgLower = sbn.packageName.lowercase()
            if (pkgLower in MediaCard.ORIGIN_NATIVE_MUSIC_PACKAGES) {
                if (isOriginNativePlayerActive()) {
                    MediaCard.clearAll(applicationContext)
                    MediaCard.clear(applicationContext, IconCache.castIdFor(sbn))
                }
                return
            }

            val rawExtras = sbn.notification.extras
            if (sbn.tag == OriginIslandConstants.SUPERX_TAG ||
                rawExtras?.containsKey(OriginIslandConstants.BUNDLE_KEY_SCENE) == true ||
                rawExtras?.getBoolean(OriginIslandConstants.BUNDLE_KEY_ISLAND_NOTIFY, false) == true
            ) {
                val scene = rawExtras?.getString(OriginIslandConstants.BUNDLE_KEY_SCENE)
                if ((scene.equals("MUSIC", ignoreCase = true) || scene.equals("AUDIO", ignoreCase = true)) &&
                    isOriginNativePlayerActive()
                ) {
                    MediaCard.clearAll(applicationContext)
                }
                return
            }

            val prefs = getSharedPreferences(PREFS, 0)
            val n = sbn.notification
            val extras = n.extras
            val template = extras.getString(NotificationCompat.EXTRA_TEMPLATE)
            val isCall = n.category == NotificationCompat.CATEGORY_CALL ||
                template == "android.app.Notification\$CallStyle"

            // --- Ưu tiên tuyệt đối: Ngân hàng -> Quick Share -> Tải xuống -> Mã OTP --------------------------
            // Ba loại này chạy TRƯỚC mọi bộ lọc phía dưới (SYSTEM_PKGS, group summary, per-app,
            // quiet hours, media…). Trên OriginOS chúng có thể đến từ gói hệ thống hoặc mang cờ
            // ongoing/progress nên nếu để sau các filter thì card bị nuốt mất.

            // 1) Biến động số dư ngân hàng Việt Nam (Vietcombank, Techcombank, MB, MoMo, …).
            //    Phải kiểm tra đầu tiên để không bị bộ lọc OTP bắt nhầm mã giao dịch/số tài khoản.
            val bankTx = VietnamBankDetector.parse(sbn)
            if (bankTx != null && prefs.getBoolean("cast_vietnam_banks", true)) {
                val id = IconCache.castIdFor(sbn)
                val appIcon = IconCache.safeAppIcon(applicationContext, sbn.packageName)
                val click = sbn.notification.contentIntent
                    ?: fallbackClickIntent(applicationContext, sbn.packageName)
                PaymentCard.postBankTransaction(applicationContext, id, appIcon, bankTx, click)
                val dir = if (bankTx.isIncome) "cộng tiền" else "trừ tiền"
                log(sbn, "hiển thị — biến động số dư $dir: ${bankTx.formattedAmount}", true)
                return
            }

            // 2) Quick Share (Google Play Services, Google Quick Share, Vivo EasyShare, Samsung Quick Share)
            //    BẮT BUỘC chạy trước OTP và Tải xuống để không bị OTP Detector bắt nhầm dung lượng/số file/PIN.
            if (prefs.getBoolean("cast_quickshare", true) && QuickShareDetector.isQuickShare(sbn)) {
                if (QuickShareDetector.isIdleVisibilityNotification(sbn)) {
                    if (activeQuickShareIds.isNotEmpty() || QuickShareCard.isTransferring()) {
                        val id = IconCache.castIdFor(sbn)
                        activeQuickShareIds.clear()
                        transferProgressMap.remove(id)
                        transferFinishedMap.remove(id)
                        QuickShareCard.postFinished(applicationContext, id, sbn.packageName, "Quick Share")
                        log(sbn, "hiển thị — Quick Share hoàn tất truyền tệp", true)
                    }
                    return
                }

                val qsInfo = QuickShareDetector.parse(sbn)
                if (qsInfo != null) {
                    val id = IconCache.castIdFor(sbn)
                    if (qsInfo.isFinished || qsInfo.isCanceled || qsInfo.isRequest) {
                        activeQuickShareIds.remove(id)
                    } else {
                        activeQuickShareIds.add(id)
                    }
                    transferProgressMap[id] = if (qsInfo.isRequest && qsInfo.progress >= 100) 0 else qsInfo.progress
                    transferFinishedMap[id] = if (qsInfo.isRequest) false else qsInfo.isFinished
                    QuickShareCard.post(applicationContext, qsInfo, sbn)
                    val mode = if (qsInfo.isRequest) "yêu cầu nhận tệp" else "tiến trình truyền tệp"
                    log(sbn, "hiển thị — Quick Share $mode", true)
                    return
                }
            }

            // 3) Tải xuống ứng dụng & tệp tin (Google Play Store, Chrome, Edge, Download Manager, ...)
            //    BẮT BUỘC chạy TRƯỚC OTP để thông báo tải file có tên chứa số (OriginIsle-10208.apk, media_179397.jpg,...)
            //    hoặc có progress bar không bị OtpDetector bắt nhầm.
            if (prefs.getBoolean("cast_downloads", true) && DownloadDetector.isDownload(sbn)) {
                val dlInfo = DownloadDetector.parse(sbn)
                if (dlInfo != null) {
                    val id = IconCache.castIdFor(sbn)
                    activeDownloadIds.add(id)
                    transferProgressMap[id] = dlInfo.progress
                    transferFinishedMap[id] = dlInfo.isFinished
                    log(sbn, "hiển thị — tiến độ tải xuống", true)
                    DownloadCard.post(applicationContext, dlInfo, sbn)
                    return
                }
            }

            // 4) Mã OTP / đăng nhập. Bóc tách cả từ khóa SMS và action "Sao chép mã" / "Copy code".
            //    KHÔNG dùng lại time-guard: notification OTP thường được update tại chỗ với cùng postTime.
            if (prefs.getBoolean("cast_otp", true) && !DownloadDetector.isDownload(sbn)) {
                val actionTitles = sbn.notification.actions?.mapNotNull { it.title?.toString() }.orEmpty()
                OtpDetector.find(sbn, actionTitles)?.let { code ->
                    val title = sbn.notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
                    val sender = title.ifBlank { appLabel(sbn.packageName) }
                    OtpCard.post(applicationContext, sbn, code, sender)
                    lastEventAt = System.currentTimeMillis()
                    val outcome = "hiển thị — mã OTP: $code ($sender)"
                    lastOutcome[sbn.key] = outcome
                    CastLog.add(sender, title.ifBlank { "OTP" }, outcome, true)
                    return
                }
            }

            // 5) Thiết bị đeo thông minh & phụ kiện Samsung (Galaxy Fit 3, Galaxy Watch, Galaxy Buds)
            if (prefs.getBoolean("cast_earbuds", true) && (sbn.packageName in WEARABLE_SAMSUNG_PKGS ||
                    sbn.packageName.contains("fit3", ignoreCase = true) ||
                    sbn.packageName.contains("watchmanager", ignoreCase = true) ||
                    sbn.packageName.contains("waterplugin", ignoreCase = true))
            ) {
                val isOngoingNotification = (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 || sbn.isOngoing
                val fullText = notificationText(sbn)
                val rawTitle = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
                val rawText = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
                val channelId = sbn.notification.channelId?.lowercase().orEmpty()
                val hay = "$rawTitle $rawText $channelId $fullText".lowercase()

                val devName = when {
                    rawTitle.isNotBlank() && !rawTitle.equals("Samsung", ignoreCase = true) && !rawTitle.equals("Galaxy Wearable", ignoreCase = true) -> rawTitle
                    rawText.isNotBlank() && rawText.length < 30 -> rawText
                    sbn.packageName.contains("fit3", ignoreCase = true) -> "Galaxy Fit 3"
                    sbn.packageName.contains("watch", ignoreCase = true) -> "Galaxy Watch"
                    else -> appLabel(sbn.packageName)
                }

                val isWearableDevice = sbn.packageName.contains("fit3", ignoreCase = true) ||
                    sbn.packageName.contains("watch", ignoreCase = true) ||
                    devName.contains("Fit", ignoreCase = true) ||
                    devName.contains("Watch", ignoreCase = true) ||
                    devName.contains("Band", ignoreCase = true)

                // 2.a) Bóc tách sâu notification của Samsung Galaxy Wearable / Fit 3 Plugin
                var batteryPct = extractDeepWearableBattery(sbn)

                if (batteryPct == -1) {
                    val notifs = runCatching { activeNotifications }.getOrNull()
                    if (notifs != null) {
                        for (active in notifs) {
                            val activePkg = active.packageName
                            val isSamsungWearPkg = activePkg in WEARABLE_SAMSUNG_PKGS ||
                                activePkg.contains("fit3", ignoreCase = true) ||
                                activePkg.contains("watchmanager", ignoreCase = true) ||
                                activePkg.contains("waterplugin", ignoreCase = true) ||
                                activePkg.contains("accessory", ignoreCase = true)
                            if (isSamsungWearPkg) {
                                val match = extractDeepWearableBattery(active)
                                if (match in 0..100) {
                                    batteryPct = match
                                    break
                                }
                            }
                        }
                    }
                }

                // Nếu notification của Samsung Wearable chứa % pin ở bất kỳ trường nào, bóc tách ngay và gọi EarbudsCard.onBatteryUpdated
                if (batteryPct in 0..100) {
                    EarbudsCard.saveCachedBattery(applicationContext, devName, batteryPct)
                    EarbudsCard.onBatteryUpdated(
                        applicationContext,
                        devName,
                        EarbudsCard.EarbudsTelemetry(main = batteryPct),
                        isWearable = isWearableDevice,
                    )
                }

                val isFullyCharged = listOf("đã sạc đầy", "sạc đầy", "fully charged", "pin đầy", "sạc hoàn tất").any { hay.contains(it) } ||
                    (batteryPct == 100 && listOf("sạc", "charging", "sạc pin").any { hay.contains(it) })
                val isConnect = listOf("đã kết nối", "connected", "kết nối thành công").any { hay.contains(it) }
                val isExcludedDisconnect = listOf(
                    "máy chủ", "server", "wifi", "wi-fi", "mạng", "internet", "cloud", "tài khoản", "account"
                ).any { hay.contains(it) }
                val isDisconnect = !isExcludedDisconnect && listOf(
                    "đã ngắt kết nối", "mất kết nối với", "mất kết nối thiết bị",
                    "thiết bị đã ngắt", "vòng đeo tay đã ngắt", "đồng hồ đã ngắt", "disconnected from"
                ).any { hay.contains(it) }
                val hasBattery = batteryPct != -1 || listOf("%", "pin", "battery").any { hay.contains(it) }

                val deviceKey = devName.lowercase().trim()
                val isCurrentlyConnected = isWearableConnected(deviceKey, devName)
                val notifAge = System.currentTimeMillis() - sbn.postTime
                val isOldOngoingNotification = isOngoingNotification && notifAge > 10_000L

                // 5.1) Xử lý thông báo báo sạc đầy pin 100%
                if (isFullyCharged) {
                    val now = System.currentTimeMillis()
                    val lastNotified = fullyChargedNoticeMap[deviceKey] ?: 0L
                    if (now - lastNotified > 300_000L) { // Cooldown 5 phút tránh pop-up liên tục khi vẫn cắm sạc
                        fullyChargedNoticeMap[deviceKey] = now
                        setWearableConnected(deviceKey, devName, true)
                        EarbudsCard.onDeviceFullyCharged(applicationContext, devName, isWearable = isWearableDevice)
                        log(sbn, "hiển thị — thiết bị đeo đã sạc đầy pin 100%", true)
                        return
                    } else {
                        log(sbn, "bỏ qua — thông báo sạc đầy lặp lại", false)
                        return
                    }
                }

                var handled = false
                if (isConnect) {
                    // 2.b) Nếu batteryPct == -1, chủ động gọi EarbudsCard.querySamsungFit3Battery
                    if (batteryPct == -1) {
                        val fit3Battery = EarbudsCard.querySamsungFit3Battery(applicationContext, devName)
                        if (fit3Battery in 0..100) {
                            batteryPct = fit3Battery
                        }
                    }
                    if (batteryPct == -1) {
                        val cached = EarbudsCard.getCachedBattery(applicationContext, devName)
                        if (cached in 0..100) {
                            batteryPct = cached
                        }
                    }
                    if (batteryPct in 0..100) {
                        EarbudsCard.saveCachedBattery(applicationContext, devName, batteryPct)
                    }
                    val isAlreadyActive = isCurrentlyConnected || isOldOngoingNotification ||
                        (EarbudsCard.isDeviceHardwareConnected(applicationContext, devName) && EarbudsCard.isConnected(devName))

                    if (isAlreadyActive) {
                        // Thiết bị đã kết nối hoặc thông báo nền thường trực cũ (>10s) được redeliver khi vuốt đa nhiệm:
                        // Tuyệt đối không gọi onDeviceConnected để tránh pop-up đảo lặp đi lặp lại
                        setWearableConnected(deviceKey, devName, true)
                        if (batteryPct != -1) {
                            EarbudsCard.onBatteryUpdated(
                                applicationContext,
                                devName,
                                EarbudsCard.EarbudsTelemetry(main = batteryPct),
                                isWearable = isWearableDevice,
                            )
                        }
                        log(sbn, "bỏ qua — thông báo nền thiết bị đeo duy trì kết nối (age=${notifAge}ms)", false)
                        return
                    }

                    // Chuyển từ DISCONNECTED -> CONNECTED với sự kiện mới thật (< 10s)
                    setWearableConnected(deviceKey, devName, true)
                    EarbudsCard.onDeviceConnected(applicationContext, devName, batteryPct, isWearable = isWearableDevice)
                    handled = true
                } else if (isDisconnect) {
                    if (EarbudsCard.isDeviceHardwareConnected(applicationContext, devName)) {
                        log(sbn, "bỏ qua — phần cứng vẫn đang kết nối", false)
                        return
                    }
                    if (!isCurrentlyConnected) {
                        // Đã ở trạng thái ngắt rồi, bỏ qua để tránh pop-up mất kết nối ảo
                        log(sbn, "bỏ qua — thiết bị đeo đã ở trạng thái ngắt kết nối", false)
                        return
                    }
                    // KHÔNG gọi setWearableConnected(deviceKey, devName, false) ngay lập tức;
                    // để EarbudsCard.onDeviceDisconnected kiểm soát debounce và xác thực phần cứng
                    EarbudsCard.onDeviceDisconnected(applicationContext, devName, isWearable = isWearableDevice)
                    handled = true
                } else if (hasBattery && batteryPct != -1) {
                    val isAlreadyActive = isCurrentlyConnected || isOldOngoingNotification ||
                        (EarbudsCard.isDeviceHardwareConnected(applicationContext, devName) && EarbudsCard.isConnected(devName))

                    if (isAlreadyActive) {
                        setWearableConnected(deviceKey, devName, true)
                        EarbudsCard.onBatteryUpdated(
                            applicationContext,
                            devName,
                            EarbudsCard.EarbudsTelemetry(main = batteryPct),
                            isWearable = isWearableDevice,
                        )
                    } else {
                        setWearableConnected(deviceKey, devName, true)
                        EarbudsCard.onDeviceConnected(applicationContext, devName, batteryPct, isWearable = isWearableDevice)
                    }
                    handled = true
                } else if (isOngoingNotification) {
                    // Thông báo thường trực nền khác của Fit 3 (bước chân, đồng bộ)
                    log(sbn, "bỏ qua — thông báo thường trực nền thiết bị đeo", false)
                    return
                }

                if (handled) {
                    log(sbn, "hiển thị — thiết bị đeo thông minh", true)
                    return
                }
            }

            // Bỏ qua thông báo hệ thống thông thường nếu không phải tiến trình tải xuống
            if (sbn.packageName in SYSTEM_PKGS && !DownloadDetector.isDownload(sbn) && !QuickShareDetector.isQuickShare(sbn)) return

        // A group summary ("3 new messages") stays active alongside the individual notifications it
        // summarizes — casting both means the same event shows up on the island twice (Gmail is the
        // common case). The individual notifications carry the actual content, so skip the summary.
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            log(sbn, "bỏ qua — tóm tắt nhóm", false); return
        }

        // Per-app exclusion: never cast apps the user explicitly turned off in the Apps tab
        if (!isAppEnabled(prefs, sbn.packageName)) {
            log(sbn, "bỏ qua — ứng dụng đã bị tắt", false); return
        }

        val castNotifs = prefs.getBoolean("cast_notifications", true)
        val castMedia = prefs.getBoolean("cast_media_sessions", true)
        if (!castNotifs && !castMedia) { log(sbn, "bỏ qua — tính năng truyền thông báo đang tắt", false); return }

        val isMusicApp = isKnownMusicApp(sbn.packageName)
        val token = extractMediaSessionToken(extras)
        val isMediaStyle = isMediaStyleOrTransport(n)

        // Media sessions get the dedicated media path.
        if (!isCall) {
            val isOngoingNotif = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
            val controller = token?.let { MediaController(this, it) }
                ?: if (isMusicApp || isOngoingNotif || isMediaStyle) findMediaControllerFallback(sbn.packageName) else null
            if (controller != null) {
                if (!castMedia) { log(sbn, "bỏ qua — trình phát nhạc đang tắt", false); return }
                // Nếu Đảo nhạc gốc của Vivo đang hoạt động thực sự, nhường quyền hoàn toàn
                if (isOriginNativePlayerActive()) {
                    MediaCard.clear(applicationContext, IconCache.castIdFor(sbn))
                    MediaCard.clearAll(applicationContext)
                    log(sbn, "bỏ qua — nhường Đảo nhạc gốc OriginOS 6", false)
                    return
                }
                log(sbn, "hiển thị — trình phát nhạc", true)
                MediaCard.post(applicationContext, sbn, controller)
                return
            } else if (isMusicApp || token != null || isMediaStyle) {
                // Thử tìm lại controller sau 300ms nếu session vừa khởi tạo bất đồng bộ để không bao giờ để rơi thông báo nhạc
                pollHandler.postDelayed({
                    val retryController = findMediaControllerFallback(sbn.packageName)
                    if (retryController != null && castMedia && isAppEnabled(prefs, sbn.packageName)) {
                        if (!isOriginNativePlayerActive()) {
                            MediaCard.post(applicationContext, sbn, retryController)
                            log(sbn, "hiển thị — trình phát nhạc (sau đồng bộ session)", true)
                        }
                    }
                }, 300L)
                log(sbn, "chờ đồng bộ phiên phát nhạc — ${appLabel(sbn.packageName)}", false)
                return
            }
        }
        if (!castNotifs) { log(sbn, "bỏ qua — chỉ nhạc, thông báo đang tắt", false); return }

        // Quiet hours / Do Not Disturb: only what someone is actively waiting on gets through —
        // calls, navigation and timers/alarms here; media and login codes were handled above.
        val isOngoingNow = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
        if (QuietHours.isActive(prefs, currentInterruptionFilter) &&
            !isCall && !isNavigation(sbn, isOngoingNow) && !isTimer(sbn) && !DownloadDetector.isDownload(sbn) && !QuickShareDetector.isQuickShare(sbn)
        ) {
            log(sbn, "bỏ qua — giờ yên tĩnh", false); return
        }

        // Score apps: if the notification parses as a match, post a football card with crests.
        // If it doesn't (Google also posts news/weather/etc.), fall through to the normal path.
        if (sbn.packageName in SCORE_APPS) {
            // A post-match "recap"/"highlights" notification isn't a live update - it's often sent
            // well after full time, sometimes repeatedly, and re-parsing its "<team> <score> -
            // <score> <team>" line as a match just pops the card back up for a game that's already
            // over. Drop it before handleScore ever sees it, unconditionally (not just when the
            // generic message-cast setting is off), so it displays nothing rather than falling
            // through to a plain-message card.
            if (isRecapOrHighlights(sbn)) { log(sbn, "skipped — recap/highlights, not live", false); return }
            if (handleScore(sbn)) { log(sbn, "hiển thị — tỷ số thể thao", true); return }
        }

        // Vietnam Ride & Delivery (Grab, Be, Xanh SM, ShopeeFood)
        if (VietnamRideDetector.isSupported(sbn.packageName)) {
            val rideInfo = VietnamRideDetector.parse(sbn)
            if (rideInfo != null) {
                log(sbn, "hiển thị — chuyến đi / giao hàng", true)
                RideCard.post(applicationContext, rideInfo, sbn.id)
                return
            }
        }

        // Ngân hàng Việt Nam đã được xử lý ưu tiên ở đầu onNotificationPosted.

        // Payments quốc tế (Wallet, Revolut, PayPal, …) -> Apple-Pay-style success card.
        detectPayment(sbn)?.let {
            if (lastCastPostTime.put(sbn.key, sbn.postTime) == sbn.postTime) {
                log(sbn, "bỏ qua — thông tin chưa thay đổi", false); return
            }
            castPayment(sbn, it); log(sbn, "hiển thị — biến động số dư", true); return
        }

        val isOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val hasProgress = extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX, 0) > 0
        val isLive = isOngoing || isCall || hasProgress

        // Turn-by-turn navigation -> vivo's own driving card (template 9) instead of the generic one.
        if (isNavigation(sbn, isOngoing)) {
            log(sbn, "hiển thị — điều hướng", true)
            NavigationCard.post(applicationContext, sbn)
            return
        }

        // A download finishing is usually an UPDATE to the same notification (progress/ongoing flags
        // just get cleared), not a cancel+repost — browsers do this. If we already have a live card up
        // for this id, this update must still go through even though it now looks like "just a plain
        // message", or the card is stuck showing its last progress forever with nothing to ever tell
        // it otherwise. Only apply the message/silent filters to notifications we haven't already cast.
        val wasLive = IconCache.castIdFor(sbn) in PlaygroundService.activeCardIds
        if (!isLive && !wasLive) {
            val includeMessages = prefs.getBoolean("cast_include_messages", false)
            if (!includeMessages) { log(sbn, "bỏ qua — tin nhắn thông thường", false); return }

            val ignoreSilent = prefs.getBoolean("cast_ignore_silent", true)
            if (ignoreSilent && isSilent(sbn)) { log(sbn, "bỏ qua — thông báo im lặng", false); return }
        }

        val kind = when {
            isCall -> "call"
            hasProgress -> "progress"
            isOngoing -> "ongoing"
            else -> "message"
        }
        if (kind == "message" && lastCastPostTime.put(sbn.key, sbn.postTime) == sbn.postTime) {
            log(sbn, "bỏ qua — thông tin chưa thay đổi", false); return
        }
        log(sbn, "hiển thị — $kind", true)
        if (kind == "message") {
            // A message card that can't be built must never take the listener down with it (a
            // crash here repeats on every reconnect): fall back to the plain generic card.
            runCatching { MessageCard.post(applicationContext, sbn, appLabel(sbn.packageName)) }
                .onFailure {
                    Log.e(TAG, "message card failed, falling back", it)
                    GenericCard.post(applicationContext, sbn, isLive)
                }
        } else {
            GenericCard.post(applicationContext, sbn, isLive)
        }
        } catch (t: Throwable) {
            Log.e(TAG, "Loi onNotificationPosted", t)
        }
    }

    /** The user-visible name of [pkg], or the package name if it can't be resolved. */
    private fun appLabel(pkg: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    /**
     * Whether [sbn] is an active turn-by-turn navigation notification, i.e. one [NavigationCard]
     * can build a driving card out of.
     *
     * The category is the reliable signal and picks up Organic Maps, Sygic and the rest for free.
     * The package check is the fallback for builds that don't set it — but it additionally demands
     * real text, because Maps also keeps a bare ongoing "Maps is running" notification alive with
     * nothing in it, and a driving card built from that is an empty card.
     */
    private fun isNavigation(sbn: StatusBarNotification, isOngoing: Boolean): Boolean {
        if (!isOngoing) return false
        val extras = sbn.notification.extras
        if (sbn.notification.category == Notification.CATEGORY_NAVIGATION) return true
        if (sbn.packageName !in NAV_APPS) return false
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        return text.isNotBlank() || subText.isNotBlank()
    }

    /** A timer, stopwatch or alarm — by category, or an ongoing countdown chronometer. */
    private fun isTimer(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (n.category == Notification.CATEGORY_ALARM || n.category == Notification.CATEGORY_STOPWATCH) return true
        val extras = n.extras
        return sbn.isOngoing &&
            extras.getBoolean(NotificationCompat.EXTRA_SHOW_CHRONOMETER, false) &&
            extras.getBoolean(NotificationCompat.EXTRA_CHRONOMETER_COUNT_DOWN, false)
    }

    /**
     * Title + text + big text + sub text + summary/info + ticker + messages, as one string to search.
     *
     * Deliberately limited to [NotificationCompat] string keys, the ticker and MessagingStyle
     * messages: iterating `extras.keySet()` and materialising arbitrary values throws
     * BadParcelableException on some source apps (custom Parcelables / classloader mismatch), which
     * would kill the listener for every later notification.
     */
    private fun notificationText(sbn: StatusBarNotification): String {
        val n = sbn.notification
        val extras = n.extras
        val base = listOf(
            NotificationCompat.EXTRA_TITLE,
            NotificationCompat.EXTRA_TEXT,
            NotificationCompat.EXTRA_BIG_TEXT,
            NotificationCompat.EXTRA_SUB_TEXT,
            NotificationCompat.EXTRA_SUMMARY_TEXT,
            NotificationCompat.EXTRA_INFO_TEXT,
            NotificationCompat.EXTRA_TITLE_BIG,
        ).mapNotNull { key ->
            runCatching { extras.getCharSequence(key)?.toString() }.getOrNull()
        }

        val ticker = listOfNotNull(runCatching { n.tickerText?.toString() }.getOrNull())

        val textLines = runCatching {
            extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES)
                ?.mapNotNull { it?.toString() }
        }.getOrNull().orEmpty()

        val dynamicExtras = runCatching {
            extras.keySet().mapNotNull { key ->
                runCatching {
                    val v = extras.get(key)
                    if (v is CharSequence) v.toString() else null
                }.getOrNull()
            }
        }.getOrNull().orEmpty()

        val actionTitles = runCatching {
            n.actions?.mapNotNull { action ->
                action.title?.toString()
            }
        }.getOrNull().orEmpty()

        val messagingMessages = runCatching {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
                ?.messages?.mapNotNull { it.text?.toString() }
        }.getOrNull().orEmpty()

        val parcelableMessages = runCatching {
            val arr = extras.getParcelableArray("android.messages")
                ?: extras.getParcelableArray("android.messages.historic")
            arr?.mapNotNull { item ->
                when (item) {
                    is android.os.Bundle -> {
                        val text = item.getCharSequence("text")?.toString()
                        val sender = item.getCharSequence("sender")?.toString()
                        listOfNotNull(sender, text).joinToString("\n").ifBlank { null }
                    }
                    else -> null
                }
            }
        }.getOrNull().orEmpty()

        val allParts = (base + ticker + textLines + dynamicExtras + actionTitles + messagingMessages + parcelableMessages)
            .filter { it.isNotBlank() }
            .distinct()

        return allParts.joinToString("\n")
    }

    /**
     * Whether [sbn]'s notification channel is currently below default importance — muted, or set to
     * "silent"/"minimal" by the user. Used to keep muted chat threads off the island.
     */
    private fun isSilent(sbn: StatusBarNotification): Boolean {
        val ranking = Ranking()
        if (currentRanking?.getRanking(sbn.key, ranking) != true) return false
        return ranking.importance < NotificationManager.IMPORTANCE_DEFAULT
    }

    /**
     * Recover a [MediaController] for a media notification that has no session token attached
     * (some browsers, e.g. Firefox playing a video, omit it) by matching the package name against
     * the system's active media sessions. Requires notification-listener access, which this class
     * has by construction.
     */
    private fun findMediaControllerFallback(pkg: String): MediaController? = try {
        val msm = getSystemService(MediaSessionManager::class.java) ?: return null
        val listenerComponent = ComponentName(this, NotificationCastListener::class.java)
        val sessions = msm.getActiveSessions(listenerComponent)
        sessions?.firstOrNull { it.packageName.equals(pkg, ignoreCase = true) }
            ?: if (isKnownMusicApp(pkg)) {
                sessions?.firstOrNull { s -> isKnownMusicApp(s.packageName) }
            } else null
    } catch (e: SecurityException) {
        null
    } catch (e: Exception) {
        null
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterBatteryReceiver()
        stopSystemCards()
        if (instance === this) instance = null
        scope.cancel()
    }

    /** Whether [sbn] (from a [SCORE_APPS] package) is a post-match recap/highlights notification. */
    /**
     * vivo's OriginOS validates every SuperX card we post (e.g. it can only stack so many live
     * cards on the island at once — confirmed live via `dumpsys notification` while several Play
     * Store downloads were casting concurrently). When it rejects one, it doesn't drop it — it
     * rewrites our own notification in place: the [OriginIslandConstants.SUPERX_TAG] tag is
     * stripped to null, every `notification.superx.*` bundle we built is gone, and the OS stamps
     * its own "SUPERX_VALUE"="false" marker on what's left, then shows THAT as a plain, visible
     * shade notification. Left alone, a card the island couldn't take shows up as a stray
     * notification instead of nothing. Since this is our own repost of our own failed card, cancel
     * it outright the moment it appears — a rejected island should show nothing, not a plain copy.
     */
    private fun cleanUpRejectedSuperX(sbn: StatusBarNotification) {
        if (sbn.tag != null) return // still tagged VIVO_SUPERX_TAG -> a real island, leave it alone
        if (sbn.notification.channelId != PlaygroundService.ORIGIN_CHANNEL_ID) return
        if (sbn.notification.extras.get("SUPERX_VALUE")?.toString() != "false") return
        Log.w(TAG, "vivo rejected a SuperX card (id=${sbn.id}) — cancelling its shade fallback")
        getSystemService(NotificationManager::class.java)?.cancel(sbn.id)
        runCatching {
            startService(
                Intent(this, PlaygroundService::class.java)
                    .setAction(PlaygroundService.ACTION_REJECTED)
                    .putExtra("id", sbn.id),
            )
        }
    }

    private fun isRecapOrHighlights(sbn: StatusBarNotification): Boolean {
        val extras = sbn.notification.extras
        val hay = listOf(
            NotificationCompat.EXTRA_TITLE, NotificationCompat.EXTRA_TEXT,
            NotificationCompat.EXTRA_BIG_TEXT, NotificationCompat.EXTRA_SUB_TEXT,
        ).joinToString(" ") { extras.getCharSequence(it)?.toString().orEmpty() }.lowercase()
        return hay.contains("recap") || hay.contains("highlight") || hay.contains("position")
    }

    /**
     * A score app's live-match notification -> parse it, fetch both club crests, and post/refresh
     * the football card. The same cast id is reused so score updates refresh one card, and the card
     * is cancelled when the notification is dismissed. Falls back to a generic cast if unparseable.
     */
    private fun handleScore(sbn: StatusBarNotification): Boolean {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty()
        val big = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val sub = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString().orEmpty()

        val match = SportsParser.parse(title, text, big, sub) ?: return false
        // Identity-based on the teams, NOT IconCache.castIdFor(sbn) — a post-match "recap" notification
        // (sent after the user dismisses the live one, or just later) is a genuinely different
        // notification object with a different key, so a notification-identity id would treat it as a
        // brand new match and pop the pill back up. Keying off the teams instead means it always lands
        // on the same card, so an already-cleared match's recap just re-triggers that card's own
        // (short) auto-clear instead of resurrecting it as new.
        val id = matchCastId(match.home, match.away)
        val competition = sub.ifBlank { "Live" }
        val clickResp = sbn.notification.contentIntent ?: fallbackClickIntent(applicationContext, sbn.packageName)
        scope.launch {
            val fresh = SportsFeed.iconsFor(match.home, match.away, competition)
            val prev = lastMatchIcons[id]
            val icons = SportsFeed.MatchIcons(
                home = fresh.home ?: prev?.home,
                away = fresh.away ?: prev?.away,
                center = fresh.center ?: prev?.center,
            )
            lastMatchIcons[id] = icons
            SportsCard.post(
                context = applicationContext,
                home = match.home, homeScore = match.homeScore,
                away = match.away, awayScore = match.awayScore,
                clock = match.minute,
                competition = competition,
                id = id,
                homeLogo = icons.home, awayLogo = icons.away, centerLogo = icons.center,
                clickResp = clickResp,
            )
        }
        return true
    }

    /** Stable per-match cast id (band 30000-34999), independent of which notification reported it. */
    private fun matchCastId(home: String, away: String): Int =
        ("$home|$away".lowercase().hashCode() and Int.MAX_VALUE) % 5000 + 30000

    // --- payments -----------------------------------------------------------------

    private data class PaymentInfo(val amount: String, val merchant: String)

    /**
     * Decide whether a notification is a payment, and pull the amount + merchant out of it.
     * A payment is either a known wallet/bank app carrying an amount or a payment verb, or *any*
     * app whose text pairs a payment verb with a currency amount.
     */
    private fun detectPayment(sbn: StatusBarNotification): PaymentInfo? {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty()
        val big = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val hay = "$title $text $big"

        val amount = AMOUNT.find(hay)?.value?.trim()
        val known = sbn.packageName in PAYMENT_APPS
        val hasVerb = PAYMENT_WORDS.any { hay.contains(it, ignoreCase = true) }
        val isPayment = (known && (amount != null || hasVerb)) || (hasVerb && amount != null)
        if (!isPayment) return null

        // Merchant: if the title itself is the amount (or empty), the payee is usually in the body.
        val titleIsAmount = amount != null && title.contains(amount)
        val merchant = (if (titleIsAmount || title.isBlank()) text else title)
            .trim().ifBlank { "Payment" }.take(40)
        return PaymentInfo(amount.orEmpty(), merchant)
    }

    private fun castPayment(sbn: StatusBarNotification, info: PaymentInfo) {
        val id = IconCache.castIdFor(sbn)
        val appIcon = runCatching {
            val ai = packageManager.getApplicationInfo(sbn.packageName, 0)
            Icon.createWithResource(sbn.packageName, ai.icon)
        }.getOrNull()
        val click = sbn.notification.contentIntent ?: fallbackClickIntent(applicationContext, sbn.packageName)
        PaymentCard.post(applicationContext, id, appIcon, info.amount, info.merchant, appLabel(sbn.packageName), click)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        lastEventAt = System.currentTimeMillis()
        lastOutcome.remove(sbn.key)
        lastScorePostTime.remove(sbn.key)
        lastCastPostTime.remove(sbn.key)

        // Score cards are keyed by team names (matchCastId), not by this notification's own identity
        // (see handleScore) — cancelling by IconCache.castIdFor(sbn) here would target the wrong id and
        // leave the actual card stuck up. Route through SportsCard.clear() too, so its own auto-clear
        // timer/state gets torn down, not just the island post.
        if (sbn.packageName in SCORE_APPS) {
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty()
            val big = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString().orEmpty()
            val sub = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString().orEmpty()
            val match = SportsParser.parse(title, text, big, sub)
            if (match != null) {
                val id = matchCastId(match.home, match.away)
                lastMatchIcons.remove(id)
                SportsCard.clear(applicationContext, id)
                return
            }
        }

        val id = IconCache.castIdFor(sbn)

        val isMusicApp = isKnownMusicApp(sbn.packageName)
        val isMedia = sbn.notification.extras.containsKey(NotificationCompat.EXTRA_MEDIA_SESSION) ||
            isMediaStyleOrTransport(sbn.notification) ||
            isMusicApp ||
            IconCache.activeControllers.containsKey(id)
        if (isMedia) {
            runCatching {
                IconCache.activeControllers.remove(id)
                MediaCard.clear(applicationContext, id)
            }.onFailure { Log.e(TAG, "Lỗi giải phóng thẻ nhạc khi thông báo bị xóa", it) }
            return
        }

        val isQs = QuickShareDetector.isQuickShare(sbn) || activeQuickShareIds.remove(id)
        val isDl = DownloadDetector.isDownload(sbn) || activeDownloadIds.remove(id)

        if (isQs || isDl) {
            val lastProg = transferProgressMap.remove(id) ?: -1
            val isFinished = transferFinishedMap.remove(id) ?: false
            val appPkg = sbn.packageName
            val title = sbn.notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
                .ifBlank { if (isQs) "Quick Share" else "Tải xuống" }

            IconCache.clear(id)
            lastMatchIcons.remove(id)

            if (isQs) {
                activeQuickShareIds.remove(id)
                val ex = sbn.notification.extras
                val rawText = "${ex.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()} ${ex.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty()}".lowercase()
                val wasCanceledText = rawText.contains("hủy") || rawText.contains("cancel") ||
                    rawText.contains("từ chối") || rawText.contains("decline") ||
                    rawText.contains("thất bại") || rawText.contains("failed")

                val isRequest = QuickShareDetector.parse(sbn)?.isRequest == true
                val prog = if (lastProg >= 0) lastProg else QuickShareCard.getLastProgress(id)
                val isFinishedEffective = isFinished || QuickShareCard.isFinished(id)
                val isActuallyFinished = !isRequest &&
                    !QuickShareDetector.isIdleVisibilityNotification(sbn) &&
                    (isFinishedEffective || prog >= 80)

                if (isActuallyFinished) {
                    QuickShareCard.postFinished(applicationContext, id, appPkg, title)
                } else if (wasCanceledText) {
                    QuickShareCard.postCanceled(applicationContext, id, appPkg)
                } else {
                    QuickShareCard.clear(id)
                    startService(
                        Intent(this, PlaygroundService::class.java)
                            .setAction(PlaygroundService.ACTION_CANCEL)
                            .putExtra("id", id)
                            .putExtra("oi_scene", "NAVIGATION"),
                    )
                }
                return
            }

            if (isDl) {
                if (isFinished || lastProg >= 80) {
                    postDownloadFinished(id, appPkg, title)
                } else {
                    postDownloadCanceled(id, appPkg)
                }
                return
            }
        }

        if (QuickShareDetector.isQuickShare(sbn)) {
            QuickShareCard.clear(id)
        }
        IconCache.clear(id)
        lastMatchIcons.remove(id)
        startService(
            Intent(this, PlaygroundService::class.java)
                .setAction(PlaygroundService.ACTION_CANCEL)
                .putExtra("id", id),
        )
    }

    private fun postDownloadFinished(id: Int, appPkg: String, title: String) {
        DownloadCard.clear(id)
        IconCache.activeSmallIcons.remove(id)
        val leftIcon = IconCache.safeAppIcon(applicationContext, appPkg)
        val clickPi = fallbackClickIntent(applicationContext, appPkg)
        val intent = Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("source_pkg", appPkg)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_left_icon", leftIcon)
            putExtra("oi_large_icon", leftIcon)
            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
            putExtra("oi_force_show", true)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)
            putExtra("is_ongoing", false)
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
            putExtra("oi_show_right_icon", false)
            putExtra("oi_left_content", "Tải xong")
            putExtra("oi_right_content", "100%")
            putExtra("status_chip_text", "100%")
            putExtra("oi_card_ring", 100)
            putExtra("oi_icon_status_type", OriginIslandConstants.ICON_STATUS_SUCCESS) // 0
            putExtra("oi_light_color", "#00D084")
            putExtra("oi_bg_color", "#4C00D084")
            putExtra("oi_fg_color", "#00D084")
            putExtra("title", "Tải xong · ${title.take(22)}")
            putExtra("text", "Đã hoàn tất tải xuống")
        }
        startService(intent)
        pollHandler.postDelayed({
            startService(
                Intent(this, PlaygroundService::class.java)
                    .setAction(PlaygroundService.ACTION_CANCEL)
                    .putExtra("id", id)
                    .putExtra("oi_scene", "NAVIGATION"),
            )
        }, 3500L)
    }

    private fun postDownloadCanceled(id: Int, appPkg: String) {
        DownloadCard.clear(id)
        IconCache.activeSmallIcons.remove(id)
        val leftIcon = IconCache.safeAppIcon(applicationContext, appPkg)
        val clickPi = fallbackClickIntent(applicationContext, appPkg)
        val intent = Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("source_pkg", appPkg)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_left_icon", leftIcon)
            putExtra("oi_large_icon", leftIcon)
            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
            putExtra("oi_force_show", true)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)
            putExtra("is_ongoing", false)
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
            putExtra("oi_show_right_icon", false)
            putExtra("oi_left_content", "")
            putExtra("oi_right_content", "Đã hủy")
            putExtra("status_chip_text", "Đã hủy")
            putExtra("oi_icon_status_type", OriginIslandConstants.ICON_STATUS_FAIL) // 1
            putExtra("oi_light_color", "#FF5247")
            putExtra("oi_bg_color", "#FF5247")
            putExtra("oi_fg_color", "#FF5247")
            putExtra("title", "Đã hủy")
            putExtra("text", "Đã hủy tải xuống")
        }
        startService(intent)
        pollHandler.postDelayed({
            startService(
                Intent(this, PlaygroundService::class.java)
                    .setAction(PlaygroundService.ACTION_CANCEL)
                    .putExtra("id", id)
                    .putExtra("oi_scene", "NAVIGATION"),
            )
        }, 2500L)
    }
}
