package com.originisle.android.bank

import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import java.util.Locale

/**
 * Data model for a detected Vietnam Bank balance transaction.
 */
data class BankTransaction(
    val rawAmount: String,       // Chuỗi tiền đầy đủ kèm dấu, ví dụ: "+50.000 đ" hoặc "-1.250.000 VND"
    val formattedAmount: String, // Ví dụ: "+50.000 đ"
    val isIncome: Boolean,       // true: cộng tiền (tiền vào), false: trừ tiền (tiền ra/thanh toán)
    val description: String,     // Chi tiết giao dịch hoặc người gửi/nhận
    val bankName: String,        // Tên ngân hàng nhận diện
)

/**
 * Detector and parser for Vietnam banking & e-wallet notifications.
 */
object VietnamBankDetector {

    /**
     * Map of supported Vietnam bank and e-wallet package names to their display names.
     */
    val BANK_PACKAGES = mapOf(
        "com.VCB" to "Vietcombank",
        "vn.com.techcombank.bb.app" to "Techcombank",
        "com.mbmobile" to "MB Bank",
        "com.vnpay.bidv" to "BIDV",
        "com.mservice.momopay" to "MoMo",
        "vn.com.vng.zalopay" to "ZaloPay",
        "vn.com.vpbank.neo" to "VPBank",
        "com.acb.mobile" to "ACB",
        "com.vietinbank.ipay" to "VietinBank",
        "vn.tpb.ebank" to "TPBank",
        "com.vnpay.agribank" to "Agribank",
        "com.vpb.cake" to "Cake",
        "com.vnpay.shb" to "SHB",
        "com.hdbank.mbanking" to "HDBank",
        "com.sacombank.mbank" to "Sacombank",
        "com.viettel.viettelpay" to "Viettel Money",
    )

    private val INCOME_KEYWORDS = listOf(
        "cộng", "cong",
        "nhận", "nhan",
        "tiền vào", "tien vao",
        "nạp", "nap",
        "hoàn tiền", "hoan tien",
        "credited",
        "+"
    )

    private val EXPENSE_KEYWORDS = listOf(
        "trừ", "tru",
        "thanh toán", "thanh toan",
        "chuyển tiền", "chuyen tien",
        "rút", "rut",
        "debited",
        "mua",
        "chi tiêu", "chi tieu",
        "-"
    )

    // Regex bắt số tiền:
    // Hỗ trợ [+-]?\s*[\d.,]+\s*(?:VND|VNĐ|đ|d|dong|đồng) hoặc (?:VND|VNĐ|đ|d)\s*[+-]?\s*[\d.,]+
    private val POSTFIX_AMOUNT_REGEX = Regex(
        """[+-]?\s*[\d.,]+\s*(?:VND|VNĐ|dong|đồng|đ|d)(?![a-zA-Z\p{L}])""",
        RegexOption.IGNORE_CASE
    )

    private val PREFIX_AMOUNT_REGEX = Regex(
        """(?:VND|VNĐ|đ|d)\s*[+-]?\s*[\d.,]+(?![0-9.,])""",
        RegexOption.IGNORE_CASE
    )

    // Regex bóc tách nội dung giao dịch
    private val DESCRIPTION_REGEX = Regex(
        """(?:ND|Nội dung|Noi dung|Loi nhan|Lời nhắn|Ghi chu|Ghi chú|Ref|Ly do|Lý do)\s*[:：-]\s*([^|\n.]+)""",
        RegexOption.IGNORE_CASE
    )

    private val SENDER_RECEIVER_REGEX = Regex(
        """(?:từ|tu|đến|den|cho)\s+([A-Z0-9\s]+?)(?:\s*(?:vào|vao|lúc|luc|ngay|ngày|với|voi|\.|\n|$))""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Checks if a [StatusBarNotification] comes from a supported Vietnam bank or e-wallet.
     */
    fun isBankNotification(sbn: StatusBarNotification): Boolean {
        return sbn.packageName in BANK_PACKAGES
    }

    /**
     * Parses a [StatusBarNotification] into a [BankTransaction] if it is from a recognized bank
     * and contains transaction amount details.
     */
    fun parse(sbn: StatusBarNotification): BankTransaction? {
        val bankName = BANK_PACKAGES[sbn.packageName] ?: return null
        val extras = sbn.notification.extras ?: return null
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString().orEmpty()
        return parse(sbn.packageName, bankName, title, text, bigText)
    }

    /**
     * Parses notification text components into a [BankTransaction].
     */
    fun parse(
        packageName: String,
        bankName: String = BANK_PACKAGES[packageName] ?: "Ngân hàng",
        title: String,
        text: String,
        bigText: String? = null
    ): BankTransaction? {
        val fullText = buildString {
            if (title.isNotBlank()) append(title).append(". ")
            if (text.isNotBlank()) append(text).append(" ")
            if (!bigText.isNullOrBlank() && bigText != text) append(bigText)
        }.trim()

        if (fullText.isBlank()) return null

        // Tìm tất cả các ứng viên số tiền
        val allMatches = mutableListOf<MatchResult>()
        allMatches.addAll(POSTFIX_AMOUNT_REGEX.findAll(fullText))
        allMatches.addAll(PREFIX_AMOUNT_REGEX.findAll(fullText))

        if (allMatches.isEmpty()) return null

        // Lọc bỏ số dư (balance) nếu có ứng viên khác rõ ràng hơn
        val transactionMatch = allMatches.firstOrNull { match ->
            val matchValue = match.value.trim()
            val startIndex = match.range.first
            val prefixBefore = fullText.substring((startIndex - 20).coerceAtLeast(0), startIndex).lowercase(Locale.ROOT)
            val isBalance = prefixBefore.contains("số dư") || prefixBefore.contains("so du") ||
                    prefixBefore.contains("sd:") || prefixBefore.contains("sd ")
            val hasSign = matchValue.contains("+") || matchValue.contains("-")

            hasSign || !isBalance
        } ?: allMatches.first()

        val matchedAmountStr = transactionMatch.value.trim()

        // Xác định isIncome
        val isIncome: Boolean = when {
            matchedAmountStr.contains("+") -> true
            matchedAmountStr.contains("-") -> false
            else -> {
                val lower = fullText.lowercase(Locale.ROOT)
                val incomeIndex = INCOME_KEYWORDS.filter { it != "+" }
                    .map { lower.indexOf(it) }
                    .filter { it >= 0 }
                    .minOrNull() ?: Int.MAX_VALUE
                val expenseIndex = EXPENSE_KEYWORDS.filter { it != "-" }
                    .map { lower.indexOf(it) }
                    .filter { it >= 0 }
                    .minOrNull() ?: Int.MAX_VALUE

                if (incomeIndex < expenseIndex) {
                    true
                } else if (expenseIndex < incomeIndex) {
                    false
                } else {
                    false
                }
            }
        }

        // Chuẩn hóa amount có dấu
        val cleanAmount = matchedAmountStr
            .replace(Regex("""^[+-]\s*"""), "")
            .replace(Regex("""\s*[+-]"""), "")
            .trim()

        val sign = if (isIncome) "+" else "-"
        val formatted = if (cleanAmount.startsWith(sign)) cleanAmount else "$sign$cleanAmount"
        val raw = if (matchedAmountStr.contains("+") || matchedAmountStr.contains("-")) {
            matchedAmountStr
        } else {
            formatted
        }

        // Bóc tách description
        var desc = DESCRIPTION_REGEX.find(fullText)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (desc.isBlank()) {
            desc = SENDER_RECEIVER_REGEX.find(fullText)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        }
        if (desc.isBlank()) {
            val titleClean = title.trim()
            val textClean = text.trim()
            desc = if (textClean.isNotBlank() && (titleClean.contains("Biến động", ignoreCase = true) || titleClean.contains(bankName, ignoreCase = true))) {
                textClean
            } else if (titleClean.isNotBlank()) {
                titleClean
            } else {
                textClean
            }
        }
        desc = desc.take(80).trim()

        return BankTransaction(
            rawAmount = raw,
            formattedAmount = formatted,
            isIncome = isIncome,
            description = desc,
            bankName = bankName
        )
    }
}
