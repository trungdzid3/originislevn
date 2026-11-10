package com.originisle.android.island

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.originisle.android.R
import com.originisle.android.log.CastLog
import com.originisle.android.service.IconCache
import java.util.concurrent.ConcurrentHashMap

private const val PREFS_NAME = "experimental_prefs"

/**
 * Posts, updates and ends OriginIsland "SuperX" cards.
 *
 * Callers drive it with an [Intent] full of `oi_*` extras (the same contract the original app
 * used — see [readAndPost]); the service maps those onto [OriginIslandBuilder.buildBundle] and
 * posts the result as a notification tagged [OriginIslandConstants.SUPERX_TAG]. That tag is what
 * makes OriginOS render it in the island rather than the shade.
 */
class PlaygroundService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CANCEL = "ACTION_CANCEL"
        const val ACTION_REJECTED = "ACTION_REJECTED"
        // Just brings the foreground service up (no card) so the process — and the notification
        // listener living in it — stays alive for background casts.
        const val ACTION_KEEPALIVE = "ACTION_KEEPALIVE"

        private val FANCY_TEMPLATES = setOf(
            OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL,
            OriginIslandConstants.TEMPLATE_BUTTONS,
            OriginIslandConstants.TEMPLATE_DRIVING_NAVI,
        )

        /** Start/keep the foreground service alive. Safe to call from a foreground context. */
        fun keepAlive(context: Context) {
            context.startService(Intent(context, PlaygroundService::class.java).setAction(ACTION_KEEPALIVE))
        }

        // Keep-alive channel created at IMPORTANCE_NONE (blocked): a foreground service on a blocked
        // channel keeps running but its notification is SUPPRESSED — no status-bar icon, even on
        // OriginOS which force-badges IMPORTANCE_MIN foreground services.
        const val CHANNEL_ID = "originisle_keepalive"

        /** Ids of cards currently on the island — used to detect a crowded island. */
        @Volatile
        var activeCardIds: Set<Int> = emptySet()

        const val ORIGIN_CHANNEL_ID = "originisle_island_hi"
        const val FGS_ID = 9999

        /** PendingIntent request code for the [onTaskRemoved] restart alarm. */
        private const val RESTART_REQUEST = 7001
    }

    private lateinit var notificationManager: NotificationManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isForegroundActive = false

    private val activeIds = linkedSetOf<Int>()
    private val originScenes = HashMap<Int, String>()
    private val originChangeRecord = HashMap<Int, Int>()
    private val lastPosts = HashMap<Int, Intent>()
    private val plainIds = HashSet<Int>()

    // Pending auto-dismiss timers, keyed by card id — only ever holds entries for non-live cards
    // (see readAndPost); a live card's id is simply never added here.
    private val autoDismissTasks = ConcurrentHashMap<Int, Runnable>()
    // Which card id currently occupies each scene. Only NAVIGATION is granted on most devices, so
    // every card shares it; vivo merges same-scene cards, so a new card must end the previous one
    // or it inherits stale fields (a ride ETA lingering under a flight card, etc.).
    private val sceneCurrentId = HashMap<String, Int>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        OriginIslandBuilder.grantScenes(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A null intent is a START_STICKY relaunch and ACTION_KEEPALIVE is the alarm/UI anchor
        // request: both only mean "stay up", which is pointless once casting is off. Explicit card
        // actions still work with it off, so the sample buttons are unaffected.
        if ((intent == null || intent.action == ACTION_KEEPALIVE) && !isCastingEnabled()) {
            cancelRestartAlarm()
            stopSelf()
            return START_NOT_STICKY
        }
        ensureForeground()
        try {
            when (intent?.action) {
                ACTION_START -> readAndPost(intent)
                ACTION_CANCEL -> cancelNotification(intent)
                ACTION_REJECTED -> onRejected(intent.getIntExtra("id", -1))
                ACTION_STOP -> stopEverything()
                ACTION_KEEPALIVE -> Unit // ensureForeground() above is all that's needed
            }
        } catch (e: Throwable) {
            Log.e("PlaygroundService", "onStartCommand failed", e)
        }
        // STICKY so the system relaunches the service (and our process + listener) if it's killed.
        return START_STICKY
    }

    /**
     * START_STICKY alone didn't bring the service back on OriginOS, so also leave an alarm behind
     * that restarts it a second later. The alarm survives because a swipe is a plain process kill,
     * not a force-stop (which would cancel it, and which nothing can escape).
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // With casting off there is nothing to come back for, and arming the alarm would revive the
        // service a second after the user swiped it away — for good, since nothing else stops it.
        if (!isCastingEnabled()) {
            cancelRestartAlarm()
            stopSelf()
            return
        }
        runCatching { ensureForeground() }
        runCatching {
            restartAlarmIntent(PendingIntent.FLAG_UPDATE_CURRENT)?.let { restart ->
                getSystemService(AlarmManager::class.java)?.set(
                    AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000L, restart,
                )
            }
        }
    }

    /** Matches the listener's own gate — either card source keeps the service worth running. */
    private fun isCastingEnabled(): Boolean = getSharedPreferences(PREFS_NAME, 0).let {
        it.getBoolean("cast_notifications", true) || it.getBoolean("cast_media_sessions", false)
    }

    /** [flags] picks between arming (FLAG_UPDATE_CURRENT) and looking one up (FLAG_NO_CREATE). */
    private fun restartAlarmIntent(flags: Int): PendingIntent? = PendingIntent.getForegroundService(
        this,
        RESTART_REQUEST,
        Intent(this, PlaygroundService::class.java).setAction(ACTION_KEEPALIVE),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun cancelRestartAlarm() {
        runCatching {
            restartAlarmIntent(PendingIntent.FLAG_NO_CREATE)?.let {
                getSystemService(AlarmManager::class.java)?.cancel(it)
                it.cancel()
            }
        }
    }

    private fun ensureForeground() {
        if (isForegroundActive) return
        createNotificationChannel(CHANNEL_ID)
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Origin Isle")
            .setContentText("Đang hiển thị trên Đảo Origin")
            .setSmallIcon(R.drawable.ic_transparent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setOngoing(true)
            .build()
        isForegroundActive = runCatching {
            startForeground(FGS_ID, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        }.recoverCatching {
            startForeground(FGS_ID, n)
        }.isSuccess
        if (!isForegroundActive) stopSelf()
    }

    // --- posting -----------------------------------------------------------------

    private fun resolveAllowedScene(requested: String?): String {
        val s = requested?.trim().orEmpty().ifBlank { "NAVIGATION" }
        if (s == "NAVIGATION") return "NAVIGATION"
        val granted = runCatching {
            val method = NotificationManager::class.java.getMethod(
                "getSceneStatus",
                String::class.java,
                String::class.java,
            )
            method.invoke(notificationManager, packageName, s) as? Boolean
        }.getOrNull() ?: false
        return if (granted) s else "NAVIGATION"
    }

    /** Read the `oi_*` extras contract off [intent] and post/refresh a SuperX card. */
    private fun readAndPost(intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        if (id == -1) return
        lastPosts[id] = intent
        if (id == 35004) plainIds.remove(id)
        val plain = id in plainIds

        val scene = resolveAllowedScene(intent.getStringExtra("oi_scene"))
        val title = intent.getStringExtra("title").orEmpty()
        val text = intent.getStringExtra("text").orEmpty()
        val leftContent = intent.getStringExtra("oi_left_content").orEmpty()
        val rightContent = intent.getStringExtra("oi_right_content").orEmpty()
        val statusChip = intent.getStringExtra("status_chip_text").orEmpty()

        val template = intent.getIntExtra("oi_template", 0)
            .let { if (plain && it in FANCY_TEMPLATES) OriginIslandConstants.TEMPLATE_BASE else it }
        val rightTemplate = intent.getIntExtra("oi_right_template", 0)

        val extra1 = intent.getStringExtra("oi_extra1").orEmpty()
        val extra2 = intent.getStringExtra("oi_extra2").orEmpty()
        val extra3 = intent.getStringExtra("oi_extra3").orEmpty()
        val extra4 = intent.getStringExtra("oi_extra4").orEmpty()
        val subtext = intent.getStringExtra("subtext")
        val navMsg = intent.getStringExtra("oi_nav_msg")
        val navAssist = intent.getStringExtra("oi_nav_assist")

        val bgColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_bg_color"), 0)
        val fgColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_fg_color"), 0)
        val cardBgColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_card_bg_color"), 0)
        val lightColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_light_color"), 0)

        val showProgress = intent.getBooleanExtra("show_progress", false)
        val progressMax = intent.getIntExtra("progress_max", 0)
        val rawProgress = intent.getIntExtra("progress", 0)
        val progress = when {
            intent.hasExtra("oi_progress") -> intent.getIntExtra("oi_progress", 0)
            showProgress && progressMax > 0 -> rawProgress * 100 / progressMax
            else -> rawProgress
        }

        val keepDuration = intent.getIntExtra("oi_keep_duration", 0)
        val baseDisplays = intent.getIntExtra(
            "oi_displays",
            OriginIslandConstants.DISPLAY_STATUSBAR or OriginIslandConstants.DISPLAY_AOD,
        )
        val lockscreenOptIn = getSharedPreferences(PREFS_NAME, 0)
            .getBoolean("cast_lockscreen_live_card", false)
        val displays = (baseDisplays and OriginIslandConstants.DISPLAY_LOCKSCREEN.inv()) or
            (if (lockscreenOptIn) OriginIslandConstants.DISPLAY_LOCKSCREEN else 0)
        val showNotify = intent.getBooleanExtra("oi_show_notify", false)
        val islandNotify = intent.getBooleanExtra("oi_island_notify", false)
        val islandShowTime = intent.getIntExtra("oi_island_show_time", 0)
        val forceShow = intent.getBooleanExtra("oi_force_show", false)
        val dismissWhenKill = intent.getBooleanExtra("oi_dismiss_when_kill", true)
        val keepScreenOn = intent.getBooleanExtra("oi_keep_screen_on", false)
        val disableInvert = intent.getBooleanExtra("oi_disable_invert", true)
        val showRightIcon = intent.getBooleanExtra("oi_show_right_icon", true)
        val showLeftIcon = intent.getBooleanExtra("oi_show_left_icon", true)
        val lightMode = intent.getIntExtra("oi_light_mode", 0)
        val generatingStatus = intent.getIntExtra("oi_generating_status", 0)
        val iconStatusType = intent.getIntExtra("oi_icon_status_type", -1)
        val waveState = intent.getIntExtra("oi_wave_state", 1)
        val waveColorList = intent.getStringArrayListExtra("oi_wave_color_list")
            ?: intent.getStringArrayListExtra("oi_wave_colors")
        val progressState = if (intent.getBooleanExtra("progress_indeterminate", false)) 1 else 0

        val customTemplate: RemoteViews? =
            intent.getParcelableExtra("oi_custom_template", RemoteViews::class.java)

        // Icons: prefer the ones the listener captured for this cast id, else the app icon.
        val iconRes = intent.getIntExtra("icon_res", R.mipmap.ic_launcher_round)
        val baseIcon: Icon = IconCache.activeSmallIcons[id]
            ?: IconCache.safeResourceIcon(this, iconRes)
        val leftIcon: Icon = intent.getParcelableExtra("oi_left_icon", Icon::class.java) ?: baseIcon
        val rightIcon: Icon = intent.getParcelableExtra("oi_right_icon", Icon::class.java) ?: baseIcon
        val largeIcon: Icon? = intent.getParcelableExtra("oi_large_icon", Icon::class.java)
            ?: IconCache.activeLargeIcons[id]
            ?: IconCache.activeLargeBitmaps[id]?.let { Icon.createWithBitmap(it) }
        val baseHeaderIcon: Icon? = intent.getParcelableExtra("oi_base_icon", Icon::class.java)
        val midSubMsg: String? = intent.getStringExtra("oi_mid_text")
        val progressNodeIcons = intent.getParcelableArrayListExtra("oi_progress_nodes", Icon::class.java).orEmpty()
        val progressIndicatorIcon = intent.getParcelableExtra("oi_progress_indicator", Icon::class.java)
        val progressColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_progress_color"), 0)

        val isOngoing = intent.getBooleanExtra("is_ongoing", false)

        // Actions -> OriginActions.
        val sourcePkg = intent.getStringExtra("source_pkg")
        val actions = if (plain) emptyList() else {
            val fromNotifActions = intent.getParcelableArrayListExtra("actions", Notification.Action::class.java)
                ?.map { a ->
                    OriginIslandBuilder.OriginAction(
                        title = a.title?.toString().orEmpty(),
                        icon = a.getIcon()?.ownedBy(sourcePkg),
                        pendingIntent = a.actionIntent,
                    )
                }
            if (!fromNotifActions.isNullOrEmpty()) {
                fromNotifActions
            } else {
                val btnPis = intent.getParcelableArrayListExtra("oi_button_actions", PendingIntent::class.java).orEmpty()
                val btnIcons = intent.getParcelableArrayListExtra("oi_button_icons", Icon::class.java).orEmpty()
                val btnTitles = intent.getStringArrayListExtra("oi_button_titles").orEmpty()
                btnPis.mapIndexed { idx, pi ->
                    OriginIslandBuilder.OriginAction(
                        title = btnTitles.getOrNull(idx).orEmpty(),
                        icon = btnIcons.getOrNull(idx)?.ownedBy(sourcePkg),
                        pendingIntent = pi,
                    )
                }
            }
        }

        val clickResp: PendingIntent? =
            intent.getParcelableExtra("oi_capsule_click_resp", PendingIntent::class.java)
                ?: intent.getParcelableExtra("source_content_intent", PendingIntent::class.java)
                ?: intent.getParcelableExtra("click_resp", PendingIntent::class.java)

        val leftDoubleLine = intent.getStringArrayListExtra("oi_left_doubleline") ?: emptyList()
        val rightDoubleLine = intent.getStringArrayListExtra("oi_right_doubleline") ?: emptyList()
        val buttonTitles = if (plain) emptyList() else intent.getStringArrayListExtra("oi_button_titles") ?: emptyList()

        val btnTransparent = intent.getBooleanExtra("oi_btn_transparent", false)
        val btnType = intent.getIntExtra("oi_btn_type", 1)
        val islandClickExpand = intent.getBooleanExtra("oi_island_click_expand", false)
        val subImage: Icon? = if (plain) null else intent.getParcelableExtra("oi_sub_image", Icon::class.java)
        val subClickResp: PendingIntent? = if (plain) null else intent.getParcelableExtra("oi_sub_click_resp", PendingIntent::class.java)

        // capsule / right-island chip text: prefer explicit chip, else right content.
        val chip = statusChip.ifBlank { rightContent }

        val changeRecord = ((originChangeRecord[id] ?: 0) + 1).also { originChangeRecord[id] = it }

        val bundle = OriginIslandBuilder.buildBundle(
            context = this,
            template = template,
            title = title,
            content = text,
            leftContent = leftContent,
            rightContent = if (rightContent.isNotBlank()) rightContent else chip,
            extra1 = extra1, extra2 = extra2, extra3 = extra3, extra4 = extra4,
            midSubMsg = midSubMsg,
            rightTemplate = rightTemplate,
            defaultIcon = baseIcon,
            leftIcon = leftIcon,
            rightIcon = rightIcon,
            progress = progress,
            bgColor = bgColor,
            fgColor = fgColor,
            scene = scene,
            clickResp = clickResp,
            largeIcon = largeIcon,
            baseHeaderIcon = baseHeaderIcon,
            subText = subtext,
            navMsg = navMsg,
            navAssistText = navAssist,
            actions = actions,
            keepDuration = keepDuration,
            displays = displays,
            dismissWhenKill = dismissWhenKill,
            islandShowTime = islandShowTime,
            forceShow = forceShow,
            showRightIcon = showRightIcon,
            showLeftIcon = showLeftIcon,
            progressState = progressState,
            progressNodeIcons = progressNodeIcons,
            progressIndicatorIcon = progressIndicatorIcon,
            progressColor = progressColor,
            cardBgColor = cardBgColor,
            cardRing = intent.getIntExtra("oi_card_ring", -1),
            keepScreenOn = keepScreenOn,
            disableInvertColor = disableInvert,
            lightColor = lightColor,
            lightMode = lightMode,
            generatingStatus = generatingStatus,
            iconStatusType = iconStatusType,
            leftDoubleLine = leftDoubleLine,
            rightDoubleLine = rightDoubleLine,
            buttonTitles = buttonTitles,
            customTemplate = customTemplate,
            waveState = waveState,
            waveColorList = waveColorList,
            changeRecord = changeRecord,
            showNotify = showNotify,
            islandNotify = islandNotify,
            btnTransparent = btnTransparent,
            btnType = btnType,
            subImage = subImage,
            subClickResp = subClickResp,
            islandClickExpand = islandClickExpand,
        )

        val sourceApp = intent.getStringExtra("source_app").orEmpty()

        val prevInScene = sceneCurrentId[scene]
        if (prevInScene != null && prevInScene != id) {
            activeIds.remove(prevInScene)
            originChangeRecord.remove(prevInScene)
            originScenes.remove(prevInScene)
            lastPosts.remove(prevInScene)
            plainIds.remove(prevInScene)
            endOrigin(prevInScene, scene)
            mainHandler.postDelayed({
                try {
                    notificationManager.cancel(OriginIslandConstants.SUPERX_TAG, prevInScene)
                    notificationManager.cancel(prevInScene)
                } catch (_: Exception) {}
            }, 200L)
        }
        sceneCurrentId[scene] = id
        activeIds.add(id)
        originScenes[id] = scene
        activeCardIds = activeIds.toSet()
        postSuperX(id, iconRes, title, text, sourceApp, isOngoing, scene, bundle)

        // Auto-dismiss: only for non-live cards (isOngoing is the same live/persistent flag the
        // card posters set — media, sports and any ongoing/call/progress GenericCard all mark
        // themselves live), and only when the user opted into a duration in Cast settings.
        val explicitAutoDismissMs = intent.getLongExtra("oi_auto_dismiss_ms", 0L)
        val autoDismissSec = getSharedPreferences(PREFS_NAME, 0).getInt("cast_auto_dismiss_seconds", 5)
        val isTransientCardId = id in setOf(35004, 35008, 35009, 35011, 35012, 35014, 35015) || id >= 40000 || id in 9900..9999
        val dismissDelayMs: Long = when {
            explicitAutoDismissMs > 0L -> explicitAutoDismissMs
            !isOngoing && autoDismissSec > 0 -> autoDismissSec * 1000L
            isTransientCardId && !isOngoing -> 5000L
            else -> 0L
        }
        autoDismissTasks.remove(id)?.let { mainHandler.removeCallbacks(it) }
        if (dismissDelayMs > 0L) {
            val task = Runnable {
                autoDismissTasks.remove(id)
                cancelCard(id)
            }
            autoDismissTasks[id] = task
            mainHandler.postDelayed(task, dismissDelayMs)
        }
    }

    /**
     * Post the SuperX card exactly the way the original did: a HIGH-importance channel, real
     * title/text (NOT empty), no `setSilent`. On vivo a low-importance/silent/empty notification
     * is minimised and the island never reaches the island — that was the bug.
     */
    private fun postSuperX(
        id: Int,
        iconRes: Int,
        title: String,
        text: String,
        sourceApp: String,
        isOngoing: Boolean,
        scene: String,
        bundle: android.os.Bundle,
    ) {
        OriginIslandBuilder.grantScenes(this)
        createNotificationChannel(ORIGIN_CHANNEL_ID)
        val cancel = Intent(this, PlaygroundService::class.java)
            .setAction(ACTION_CANCEL)
            .putExtra("id", id)
            .putExtra("oi_scene", scene)
        val deletePi = PendingIntent.getService(
            this, id, cancel,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, ORIGIN_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(isOngoing)
            .setAutoCancel(isOngoing)
            .setDeleteIntent(deletePi)
            // Always the launcher mipmap. Vivo OriginOS rejects SuperX posts that carry a vector
            // XML drawable as the small icon, which silently drops the card off the island.
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setExtras(bundle)
        if (sourceApp.isNotEmpty()) builder.setSubText(sourceApp)
        notificationManager.notify(OriginIslandConstants.SUPERX_TAG, id, builder.build())
    }

    // --- ending ------------------------------------------------------------------

    private fun endOrigin(id: Int, scene: String) {
        try {
            OriginIslandBuilder.grantScenes(this)
            createNotificationChannel(ORIGIN_CHANNEL_ID)
            val n = NotificationCompat.Builder(this, ORIGIN_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("")
                .setContentText("")
                .setSilent(true)
                .setExtras(OriginIslandBuilder.buildEndBundle(scene))
                .build()
            notificationManager.notify(OriginIslandConstants.SUPERX_TAG, id, n)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelCard(id: Int) {
        cancelNotification(
            Intent(this, PlaygroundService::class.java)
                .putExtra("id", id)
                .putExtra("oi_scene", originScenes[id]),
        )
    }

    private fun cancelNotification(intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        if (id == -1) return
        autoDismissTasks.remove(id)?.let { mainHandler.removeCallbacks(it) }
        val scene = originScenes.remove(id) ?: resolveAllowedScene(intent.getStringExtra("oi_scene"))
        if (sceneCurrentId[scene] == id) {
            sceneCurrentId.remove(scene)
        }
        endOrigin(id, scene)
        mainHandler.postDelayed({
            notificationManager.cancel(OriginIslandConstants.SUPERX_TAG, id)
            notificationManager.cancel(id)
        }, 350L)
        activeIds.remove(id)
        originChangeRecord.remove(id)
        lastPosts.remove(id)
        plainIds.remove(id)
        activeCardIds = activeIds.toSet()

        val nonResumableIds = setOf(35001, 35004, 35008, 35009, 35011, 35012, 35014, 35015)
        val resumeId = activeIds.lastOrNull { otherId ->
            otherId != id &&
                otherId < 40000 &&
                otherId !in nonResumableIds &&
                originScenes[otherId] == scene &&
                lastPosts[otherId]?.getBooleanExtra("is_ongoing", false) == true &&
                (lastPosts[otherId]?.getLongExtra("oi_auto_dismiss_ms", 0L) ?: 0L) <= 0L
        }
        if (resumeId != null) {
            val savedIntent = lastPosts[resumeId]
            if (savedIntent != null) {
                mainHandler.postDelayed({
                    if (resumeId in activeIds && sceneCurrentId[scene] == null) {
                        readAndPost(savedIntent)
                    }
                }, 150L)
            }
        }
    }

    private fun onRejected(id: Int) {
        val last = lastPosts[id] ?: return
        val app = last.getStringExtra("source_app").orEmpty().ifBlank { "Origin Isle" }
        val title = last.getStringExtra("title").orEmpty()
        val simpler = last.getIntExtra("oi_template", 0) in FANCY_TEMPLATES ||
            last.hasExtra("actions") || last.hasExtra("oi_button_titles")
        if (id == 35004 || id in plainIds || !simpler) {
            CastLog.add(app, title, "bị Đảo từ chối", false)
            return
        }
        plainIds.add(id)
        CastLog.add(app, title, "bị Đảo từ chối — hiển thị dạng thẻ cơ bản", true)
        readAndPost(last)
    }

    private fun Icon.ownedBy(pkg: String?): Icon? = when {
        type != Icon.TYPE_RESOURCE || !resPackage.isNullOrEmpty() -> this
        pkg.isNullOrEmpty() -> null
        else -> Icon.createWithResource(pkg, resId)
    }

    private fun stopEverything() {
        // An explicit stop must not be undone by an alarm a previous swipe left armed.
        cancelRestartAlarm()
        autoDismissTasks.values.forEach { mainHandler.removeCallbacks(it) }
        autoDismissTasks.clear()
        val hadScenes = originScenes.isNotEmpty()
        HashMap(originScenes).forEach { (id, scene) -> endOrigin(id, scene) }
        originScenes.clear()
        originChangeRecord.clear()
        sceneCurrentId.clear()
        lastPosts.clear()
        plainIds.clear()
        val ids = activeIds.toList()
        activeIds.clear()
        activeCardIds = emptySet()
        val finish = {
            ids.forEach {
                notificationManager.cancel(OriginIslandConstants.SUPERX_TAG, it)
                notificationManager.cancel(it)
            }
            // Clear the flag with the teardown, or a start arriving before we're destroyed sees a
            // stale "already foreground" and skips ensureForeground(), leaving the process unanchored.
            isForegroundActive = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        if (hadScenes) mainHandler.postDelayed({ finish() }, 350L) else finish()
    }

    private fun createNotificationChannel(channelId: String) {
        if (notificationManager.getNotificationChannel(channelId) != null) return
        val isIsland = channelId == ORIGIN_CHANNEL_ID
        val name = if (isIsland) "OriginIsland" else "Duy trì chạy ngầm"
        // Island channel must be HIGH (4) like the original — LOW gets minimised and never reaches
        // the island. The keep-alive channel is NONE (blocked) so its foreground-service notification
        // is suppressed entirely — the service keeps running with no status-bar icon.
        val importance = if (isIsland) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_NONE
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = "Channel for $name Service"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun String?.orEmptyOr(fallback: String): String {
        val v = this?.trim().orEmpty()
        return v.ifBlank { fallback }
    }
}
