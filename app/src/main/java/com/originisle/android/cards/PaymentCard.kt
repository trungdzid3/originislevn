package com.originisle.android.cards

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import com.originisle.android.R
import com.originisle.android.bank.BankTransaction
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.IconCache

/**
 * Apple-Pay-style payment card and Vietnam Bank transaction card for the OriginIsland.
 *
 * A payment notification (Google Wallet, Google Pay, Revolut, PayPal, a bank, …) is re-cast as a
 * short two-phase animation, mirroring how Apple shows a tap-to-pay:
 *
 *  1. **Processing** — the app icon with a loading spinner and a "…" chip.
 *  2. **Done** — a green success tick overlays the icon, the amount shows big, the pill turns into a
 *     green "✓" capsule and the island edge glows green. Auto-dismisses a few seconds later.
 *
 * For Vietnam Bank transactions:
 *  - Income (tiền vào): Emerald teal color (#00D084), title "+<Số tiền>", subtitle "Tiền vào · <Ngân hàng>".
 *  - Expense (tiền ra): Coral red color (#FF5247), title "-<Số tiền>", subtitle "Tiền ra · <Ngân hàng>".
 *  - Displays transaction description directly on the card.
 */
object PaymentCard {

    /** Apple-style system green. */
    private const val GREEN = "#FF34C759"
    private const val WHITE = "#FFFFFFFF"

    /** Vietnam Bank transaction theme colors. */
    const val COLOR_INCOME = "#00D084"
    const val COLOR_EXPENSE = "#FF5247"

    private const val PROCESS_MS = 850L   // spinner phase
    private const val HOLD_MS = 6000L     // how long "Hoàn tất" stays before auto-dismiss

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Show the standard tap-to-pay payment animation for [id].
     *
     * @param amount pre-extracted amount string (e.g. "€12.90"); may be blank.
     * @param merchant merchant / payee, or a short description.
     * @param appLabel the source app's name, shown as "Paid with <app>".
     */
    fun post(
        context: Context,
        id: Int,
        appIcon: Icon?,
        amount: String,
        merchant: String,
        appLabel: String,
        clickResp: PendingIntent?,
    ) {
        val app = context.applicationContext

        // Phase 1 — processing (spinner).
        startService(
            app, id,
            title = "Đang xử lý…",
            content = amount.ifBlank { merchant },
            subtext = "Thanh toán qua $appLabel",
            appIcon = appIcon,
            clickResp = clickResp,
            generating = 1,
            iconStatus = -1,
            rightTemplate = OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_LOADING, // 3
            chip = "",
        )

        // Phase 2 — done (green tick + glow), a beat later so the transition animates.
        handler.postDelayed({
            startService(
                app, id,
                title = amount.ifBlank { "Thành công" },
                content = merchant,
                subtext = "Thanh toán qua $appLabel",
                appIcon = appIcon,
                clickResp = clickResp,
                generating = 0,
                iconStatus = OriginIslandConstants.ICON_STATUS_SUCCESS, // 0 -> green tick
                rightTemplate = OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT, // 6
                chip = "✓",
            )
        }, PROCESS_MS)

        // Auto-dismiss, the way Apple's pill fades out after a payment.
        handler.postDelayed({
            app.startService(
                Intent(app, PlaygroundService::class.java)
                    .setAction(PlaygroundService.ACTION_CANCEL)
                    .putExtra("id", id)
                    .putExtra("oi_scene", "NAVIGATION"),
            )
        }, PROCESS_MS + HOLD_MS)
    }

    /**
     * Post a Vietnam bank transaction (biến động số dư) to OriginIsland.
     *
     * - Income: emerald teal accent (#00D084), title "+<Số tiền>", subtitle "Tiền vào · <Ngân hàng>".
     * - Expense: coral red accent (#FF5247), title "-<Số tiền>", subtitle "Tiền ra · <Ngân hàng>".
     * - Displays transaction description on the card content area.
     */
    fun postBankTransaction(
        context: Context,
        id: Int,
        appIcon: Icon?,
        transaction: BankTransaction,
        clickResp: PendingIntent?,
    ) {
        val app = context.applicationContext
        val isIncome = transaction.isIncome
        val accentColor = if (isIncome) COLOR_INCOME else COLOR_EXPENSE

        // Ensure title is formatted as "+<Số tiền>" or "-<Số tiền>"
        val rawAmountClean = transaction.formattedAmount.trim()
            .replace(Regex("""^[+-]\s*"""), "")
            .replace(Regex("""\s*[+-]$"""), "")
            .trim()
        val title = if (isIncome) "+$rawAmountClean" else "-$rawAmountClean"

        val subtitle = if (isIncome) {
            "Tiền vào · ${transaction.bankName}"
        } else {
            "Tiền ra · ${transaction.bankName}"
        }

        val content = transaction.description.ifBlank {
            if (isIncome) "Nhận tiền" else "Giao dịch thành công"
        }

        startService(
            app, id,
            title = title,
            content = content,
            subtext = subtitle,
            appIcon = appIcon,
            clickResp = clickResp,
            generating = 0,
            iconStatus = if (isIncome) OriginIslandConstants.ICON_STATUS_SUCCESS else -1,
            rightTemplate = OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT,
            chip = rawAmountClean,
            bgColor = accentColor,
            lightColor = accentColor,
            fgColor = WHITE,
            leftContent = transaction.bankName,
            rightContent = rawAmountClean,
        )

        // Auto-dismiss
        handler.postDelayed({
            app.startService(
                Intent(app, PlaygroundService::class.java)
                    .setAction(PlaygroundService.ACTION_CANCEL)
                    .putExtra("id", id)
                    .putExtra("oi_scene", "NAVIGATION"),
            )
        }, HOLD_MS)
    }

    /** Overload to accept BankTransaction directly in [post]. */
    fun post(
        context: Context,
        id: Int,
        appIcon: Icon?,
        transaction: BankTransaction,
        clickResp: PendingIntent?,
    ) = postBankTransaction(context, id, appIcon, transaction, clickResp)

    /** Overload to post bank transaction with individual fields. */
    fun post(
        context: Context,
        id: Int,
        appIcon: Icon?,
        amount: String,
        description: String,
        bankName: String,
        isIncome: Boolean,
        clickResp: PendingIntent?,
    ) = postBankTransaction(
        context, id, appIcon,
        BankTransaction(
            rawAmount = amount,
            formattedAmount = amount,
            isIncome = isIncome,
            description = description,
            bankName = bankName
        ),
        clickResp
    )

    private fun startService(
        context: Context,
        id: Int,
        title: String,
        content: String,
        subtext: String,
        appIcon: Icon?,
        clickResp: PendingIntent?,
        generating: Int,
        iconStatus: Int,
        rightTemplate: Int,
        chip: String,
        bgColor: String = GREEN,
        lightColor: String = GREEN,
        fgColor: String = if (chip == "✓") WHITE else GREEN,
        leftContent: String? = null,
        rightContent: String? = null,
    ) {
        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("oi_scene", "NAVIGATION") // granted carrier scene
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
            putExtra("oi_right_template", rightTemplate)
            putExtra("title", title)
            putExtra("text", content)
            putExtra("subtext", subtext)
            putExtra("oi_left_content", leftContent ?: title)
            putExtra("oi_right_content", rightContent ?: chip)
            putExtra("status_chip_text", chip)
            // Small notification/status icon must always be a mipmap (ic_launcher_round).
            // Clear from activeSmallIcons to ensure PlaygroundService never picks a vector XML from another notification.
            IconCache.activeSmallIcons.remove(id)

            // Use the PAYMENT app's own icon for the card image AND the pill's left icon. Without
            // oi_left_icon the pill falls back to icon_res (our launcher), which looked wrong.
            val fallbackIcon = Icon.createWithResource(context, R.mipmap.ic_launcher_round)
            val leftIcon = appIcon ?: fallbackIcon
            putExtra("oi_large_icon", leftIcon)
            putExtra("oi_left_icon", leftIcon)

            // Small notification/status icon + fallback when there's no app icon. Must be a mipmap:
            // a vector XML drawable here makes OriginOS reject the SuperX post.
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_generating_status", generating)
            putExtra("oi_icon_status_type", iconStatus)
            // Accent colors: chip fill, chip text and the island edge glow.
            putExtra("oi_bg_color", bgColor)
            putExtra("oi_fg_color", fgColor)
            putExtra("oi_light_color", lightColor)
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)
            clickResp?.let { putExtra("click_resp", it) }
        }
        context.startService(intent)
    }
}
