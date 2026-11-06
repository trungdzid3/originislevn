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
 * Detector and parser for Vietnam banking & e-wallet notifications,
 * supporting both banking apps and bank balance SMS notifications (Vivo MMS, Android Messages, etc.).
 * Includes strict spam/promo filters and 0-amount exclusion.
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

    /**
     * Common SMS packages on Android devices (Vivo, Samsung, Xiaomi, Google, etc.).
     */
    val SMS_PACKAGES = setOf(
        "com.vivo.mms",
        "com.android.mms",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.coloros.mms",
        "com.oppo.mms",
        "com.xiaomi.mms",
        "com.motorola.messaging",
    )

    /**
     * Vietnam bank brandname mappings for matching SMS titles/senders and SMS message contents.
     */
    private val BANK_BRANDS = listOf(
        listOf("vietcombank", "vcb") to "Vietcombank",
        listOf("techcombank", "tcb") to "Techcombank",
        listOf("mbbank", "mb bank", "ngan hang quan doi", "mbb") to "MB Bank",
        listOf("bidv") to "BIDV",
        listOf("vpbank", "vp bank", "vpb") to "VPBank",
        listOf("acb", "a chau") to "ACB",
        listOf("vietinbank", "ctg") to "VietinBank",
        listOf("tpbank", "tp bank", "tien phong", "tpb") to "TPBank",
        listOf("agribank", "vba") to "Agribank",
        listOf("cake by vpbank", "cake") to "Cake",
        listOf("shb") to "SHB",
        listOf("hdbank", "hd bank", "hdb") to "HDBank",
        listOf("sacombank", "stb") to "Sacombank",
        listOf("viettel money", "viettelmoney", "viettelpay") to "Viettel Money",
        listOf("momo", "ví momo", "vi momo") to "MoMo",
        listOf("zalopay", "ví zalopay", "vi zalopay") to "ZaloPay",
    )

    /**
     * Known telecom sender brandnames.
     */
    private val TELECOM_SENDERS = setOf(
        "viettel", "viettel_km", "viettel_plus", "myviettel",
        "vinaphone", "vinaphone_km",
        "mobifone", "mobifone_km",
        "vietnamobile", "vnm",
        "itel", "wintel"
    )

    /**
     * Keywords confirming legitimate balance fluctuations for Viettel Money carrier messages.
     */
    private val VIETTEL_BALANCE_CONFIRM_KEYWORDS = listOf(
        "số dư tk", "so du tk",
        "thanh toán thành công", "thanh toan thanh cong",
        "chuyển tiền thành công", "chuyen tien thanh cong"
    )

    /**
     * Keywords indicating spam, promo, data package or telecommunication advertising.
     */
    private val SPAM_PROMO_KEYWORDS = listOf(
        "quảng cáo", "quang cao",
        "qc:", "(qc)", "[qc]",
        "khuyen mai", "khuyến mại", "khuyến mãi",
        "uu dai", "ưu đãi",
        "chi tu", "chỉ từ",
        "gia chi", "giá chỉ",
        "soan", "soạn",
        "gui", "gửi",
        "cuoc phi", "cước phí",
        "mien phi", "miễn phí",
        "trung thuong", "trúng thưởng",
        "voucher",
        "giam gia", "giảm giá",
        "flash sale",
        "nhan ngay", "nhận ngay",
        "goi cuoc", "gói cước",
        "data", "4g", "5g",
        "tich diem", "tích điểm",
        "doi thuong", "đổi thưởng",
        "vay tieu dung", "vay tiêu dùng",
        "vay tiền", "vay tien",
        "mo the tin dung", "mở thẻ tín dụng"
    )

    private val INCOME_KEYWORDS = listOf(
        "cộng", "cong",
        "nhận", "nhan",
        "tiền vào", "tien vao",
        "nạp", "nap",
        "hoàn tiền", "hoan tien",
        "credited",
        "ghi có", "ghi co",
        "tăng", "tang",
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
        "ghi nợ", "ghi no",
        "giảm", "giam",
        "-"
    )

    // Regex bắt số tiền:
    // Hỗ trợ [+-]?\s*[\d.,]+\s*(?:VND|VNĐ|dong|đồng|đ|d) hoặc (?:VND|VNĐ|đ|d)\s*[+-]?\s*[\d.,]+
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
     * Checks if a package name belongs to a known SMS/MMS application.
     */
    fun isSmsPackage(pkg: String): Boolean {
        if (pkg in SMS_PACKAGES) return true
        val lower = pkg.lowercase(Locale.ROOT)
        return lower.endsWith(".mms") || lower.contains(".messaging") || lower.contains(".sms")
    }

    /**
     * Checks if sender name explicitly designates the Viettel Money / ViettelPay e-wallet.
     */
    fun isExplicitViettelWallet(sender: String): Boolean {
        val clean = sender.trim().lowercase(Locale.ROOT)
        return clean == "viettelpay" || clean == "viettel money" || clean == "viettelmoney" ||
                clean == "viettel_pay" || clean == "viettel-money"
    }

    /**
     * Checks if sender name belongs to a telecommunications carrier brandname.
     */
    fun isTelecomSender(sender: String): Boolean {
        if (isExplicitViettelWallet(sender)) return false
        val clean = sender.trim().lowercase(Locale.ROOT)
        if (clean in TELECOM_SENDERS) return true
        return clean.startsWith("viettel") || clean.startsWith("vinaphone") ||
                clean.startsWith("mobifone") || clean.startsWith("vietnamobile") ||
                clean == "vnm" || clean == "itel" || clean == "wintel"
    }

    /**
     * Checks if text contains explicit keywords confirming a Viettel Money balance fluctuation.
     */
    fun hasViettelBalanceConfirmation(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return VIETTEL_BALANCE_CONFIRM_KEYWORDS.any { lower.contains(it) }
    }

    /**
     * Checks if text contains advertising, promo, or spam keywords.
     */
    fun hasSpamPromoKeywords(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        for (kw in SPAM_PROMO_KEYWORDS) {
            if (kw.length <= 4) {
                val regex = Regex("""(?i)(?:\b|^|\s|\[|\()${Regex.escape(kw)}(?:\b|\s|:|\]|\)|$)""")
                if (regex.containsMatchIn(lower)) return true
            } else {
                if (lower.contains(kw)) return true
            }
        }
        return false
    }

    /**
     * Checks if text contains clear signs of actual balance fluctuation:
     * "số dư", "so du", "sd:", "giao dịch", "gd:", "tk".
     */
    fun hasBalanceIndicator(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        if (lower.contains("số dư") || lower.contains("so du") ||
            lower.contains("sd:") || lower.contains("sd ") ||
            lower.contains("giao dịch") || lower.contains("giao dich") ||
            lower.contains("gd:") || lower.contains("gd ") ||
            lower.contains("biến động") || lower.contains("bien dong") ||
            lower.contains("tài khoản") || lower.contains("tai khoan")
        ) {
            return true
        }
        // "tk" với boundary hoặc đứng trước số/dấu hai chấm
        if (Regex("""(?i)(?:\b|^|\s)tk(?:\s|:|\d|$)""").containsMatchIn(text)) {
            return true
        }
        return false
    }

    /**
     * Checks if text contains balance or account confirmation keywords for SMS context:
     * "số dư", "so du", "sd:", "sd ", "tk", "tai khoan", "tài khoản", "the", "thẻ", "gd:", "giao dịch", "giao dich", "biến động", "bien dong".
     */
    fun hasSmsBalanceOrAccountContext(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val basicKeywords = listOf(
            "số dư", "so du",
            "sd:", "sd ",
            "tai khoan", "tài khoản",
            "gd:", "gd ",
            "giao dịch", "giao dich",
            "biến động", "bien dong"
        )
        if (basicKeywords.any { lower.contains(it) }) return true

        // "the", "thẻ": kiểm tra ranh giới từ để tránh match chữ ghép như "thêm", "then"
        if (Regex("""(?i)(?:\b|^|\s)(?:thẻ|the)(?:\s|:|\d|$)""").containsMatchIn(text)) {
            return true
        }

        // "tk": kiểm tra ranh giới từ "tk", "tk:", "tk "
        if (Regex("""(?i)(?:\b|^|\s)tk(?:\s|:|\d|$)""").containsMatchIn(text)) {
            return true
        }

        return false
    }

    /**
     * Checks if amount has an explicit '+' or '-' sign immediately before it.
     */
    fun hasExplicitSign(amountStr: String, fullText: String, matchRange: IntRange): Boolean {
        val trimmed = amountStr.trim()
        if (trimmed.startsWith("+") || trimmed.startsWith("-")) return true
        val startIndex = matchRange.first
        val prefixBefore = fullText.substring((startIndex - 3).coerceAtLeast(0), startIndex).trim()
        return prefixBefore.endsWith("+") || prefixBefore.endsWith("-")
    }

    /**
     * Extracts actual numeric transaction amount as [Long],
     * removing non-digit characters, thousand separators (. or ,), and currency units.
     * Handles decimal cents (,00 or .00) if present.
     * Returns 0L if invalid or non-numeric.
     */
    fun extractNumericAmount(amountStr: String): Long {
        if (amountStr.isBlank()) return 0L
        // Xóa phần thập phân 1 hoặc 2 chữ số ở cuối nếu đứng sau dấu chấm hoặc phẩy và trước đơn vị tiền
        // Ví dụ: "50.000,00 đ" -> "50.000 đ", "0,00đ" -> "0đ", "100.50 VND" -> "100 VND"
        val cleanDecimal = amountStr.replace(
            Regex("""[,.]\d{1,2}(?:\s*(?:VND|VNĐ|dong|đồng|đ|d))?$""", RegexOption.IGNORE_CASE)
        ) { matchResult ->
            matchResult.value.replace(Regex("""^[,.]\d{1,2}"""), "")
        }
        val digitsOnly = cleanDecimal.filter { it.isDigit() }
        return digitsOnly.toLongOrNull() ?: 0L
    }

    /**
     * Detects bank brand name from sender/title or notification content.
     */
    fun detectBankBrand(title: String, content: String): String? {
        val titleClean = title.trim()
        val contentClean = content.trim()

        // 1. Sender là ví Viettel Money rõ ràng (ViettelPay, Viettel Money)
        if (isExplicitViettelWallet(titleClean)) {
            return "Viettel Money"
        }

        // 2. Sender là nhà mạng viễn thông thông thường (VIETTEL, Viettel_KM, VINAPHONE, MOBIFONE, ...)
        if (isTelecomSender(titleClean)) {
            val titleLower = titleClean.lowercase(Locale.ROOT)
            val isViettelCarrier = titleLower.startsWith("viettel") || titleLower == "myviettel"
            // Với Viettel viễn thông: Chỉ chấp nhận nếu nội dung có từ khóa xác nhận biến động số dư tài khoản
            if (isViettelCarrier && hasViettelBalanceConfirmation(contentClean)) {
                return "Viettel Money"
            }
            // Các nhà mạng viễn thông khác hoặc Viettel thông thường: tuyệt đối không nhận diện là ngân hàng
            return null
        }

        // 3. Khớp brandname từ title (người gửi SMS)
        if (titleClean.isNotBlank()) {
            for ((aliases, brandName) in BANK_BRANDS) {
                for (alias in aliases) {
                    val regex = Regex("""(?i)(?:\b|^|_)${Regex.escape(alias)}(?:\b|$|_)""")
                    if (regex.containsMatchIn(titleClean)) {
                        return brandName
                    }
                }
            }
        }

        // 4. Khớp brandname từ content
        if (contentClean.isNotBlank()) {
            for ((aliases, brandName) in BANK_BRANDS) {
                for (alias in aliases) {
                    val regex = Regex("""(?i)(?:\b|^|_)${Regex.escape(alias)}(?:\b|$|_)""")
                    if (regex.containsMatchIn(contentClean)) {
                        // Nếu là Viettel Money được tìm thấy trong content, yêu cầu phải có từ khóa xác nhận biến động số dư
                        if (brandName == "Viettel Money" && !hasViettelBalanceConfirmation(contentClean)) {
                            continue
                        }
                        return brandName
                    }
                }
            }
        }
        return null
    }

    /**
     * Checks if a text is an OTP or verification code SMS rather than a balance transaction.
     */
    private fun isOtpMessage(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val hasOtpKeyword = lower.contains("otp") ||
                lower.contains("mã xác thực") || lower.contains("ma xac thuc") ||
                lower.contains("mã xác minh") || lower.contains("ma xac minh") ||
                lower.contains("mã xác nhận") || lower.contains("ma xac nhan") ||
                lower.contains("verification code") || lower.contains("mã otp") || lower.contains("ma otp")
        if (!hasOtpKeyword) return false

        val hasBalanceKeywords = lower.contains("số dư") || lower.contains("so du") ||
                lower.contains("biến động") || lower.contains("bien dong")
        return !hasBalanceKeywords || lower.contains("mã otp") || lower.contains("ma otp")
    }

    /**
     * Checks if a [StatusBarNotification] comes from a supported Vietnam bank or e-wallet,
     * or is a bank SMS notification.
     */
    fun isBankNotification(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName in BANK_PACKAGES) return true
        if (isSmsPackage(sbn.packageName)) {
            val extras = sbn.notification.extras ?: return false
            val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty()
            val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString().orEmpty()
            val fullText = "$text $bigText".trim()

            // Loại trừ OTP
            if (isOtpMessage(fullText)) return false

            // Loại trừ telecom sender thông thường
            if (isTelecomSender(title) && !hasViettelBalanceConfirmation(fullText)) return false

            // Loại trừ spam/promo SMS không có dấu hiệu biến động số dư thực tế
            if (hasSpamPromoKeywords(fullText) && !hasBalanceIndicator(fullText)) return false

            return detectBankBrand(title, fullText) != null
        }
        return false
    }

    /**
     * Parses a [StatusBarNotification] into a [BankTransaction] if it is from a recognized bank
     * (native banking app or SMS) and contains transaction amount details.
     */
    fun parse(sbn: StatusBarNotification): BankTransaction? {
        val extras = sbn.notification.extras ?: return null
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val textLines = runCatching {
            extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES)
                ?.mapNotNull { it?.toString() }
                ?.joinToString(" ")
        }.getOrNull().orEmpty()

        val fullTextForDetection = "$text $bigText $textLines".trim()

        var bankName = BANK_PACKAGES[sbn.packageName]
        if (bankName == null && isSmsPackage(sbn.packageName)) {
            bankName = detectBankBrand(title, fullTextForDetection)
        }
        if (bankName == null) return null

        val combinedContent = if (textLines.isNotBlank()) {
            if (bigText.isNotBlank()) "$bigText\n$textLines" else textLines
        } else {
            bigText
        }

        return parse(sbn.packageName, bankName, title, text, combinedContent)
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

        // Không xử lý tin nhắn OTP/mã xác thực như biến động số dư
        if (isOtpMessage(fullText)) {
            return null
        }

        val isSms = isSmsPackage(packageName)

        // Đối với SMS: Kiểm tra bộ lọc spam / quảng cáo
        if (isSms && hasSpamPromoKeywords(fullText) && !hasBalanceIndicator(fullText)) {
            return null
        }

        // Nếu sender là nhà mạng viễn thông thông thường mà không có xác nhận biến động số dư Viettel Money
        if (isTelecomSender(title) && !hasViettelBalanceConfirmation(fullText)) {
            return null
        }

        // Tìm tất cả các ứng viên số tiền
        val allMatches = mutableListOf<MatchResult>()
        allMatches.addAll(POSTFIX_AMOUNT_REGEX.findAll(fullText))
        allMatches.addAll(PREFIX_AMOUNT_REGEX.findAll(fullText))

        if (allMatches.isEmpty()) return null

        // 1. Loại trừ triệt để số tiền bằng 0: lọc các ứng viên có giá trị số > 0
        val positiveMatches = allMatches.filter { match ->
            extractNumericAmount(match.value) > 0L
        }
        if (positiveMatches.isEmpty()) return null

        // Lọc bỏ số dư (balance) nếu có ứng viên khác rõ ràng hơn
        val transactionMatch = positiveMatches.firstOrNull { match ->
            val matchValue = match.value.trim()
            val startIndex = match.range.first
            val prefixBefore = fullText.substring((startIndex - 25).coerceAtLeast(0), startIndex).lowercase(Locale.ROOT)
            val isBalance = prefixBefore.contains("số dư") || prefixBefore.contains("so du") ||
                    prefixBefore.contains("sd:") || prefixBefore.contains("sd ")
            val hasSign = matchValue.contains("+") || matchValue.contains("-")

            hasSign || !isBalance
        } ?: positiveMatches.first()

        val matchedAmountStr = transactionMatch.value.trim()
        val numericAmount = extractNumericAmount(matchedAmountStr)

        // Kiểm tra bắt buộc: số tiền thực tế phải > 0L
        if (numericAmount <= 0L) {
            return null
        }

        // 4. Ngữ cảnh biến động số dư đối với SMS:
        // BẮT BUỘC thỏa mãn ít nhất một trong hai điều kiện:
        // (a) Có từ khóa xác nhận tài khoản / số dư
        // (b) Số tiền có dấu rõ ràng '+' hoặc '-' đứng ngay trước
        if (isSms) {
            val hasContext = hasSmsBalanceOrAccountContext(fullText)
            val hasSign = hasExplicitSign(matchedAmountStr, fullText, transactionMatch.range)
            if (!hasContext && !hasSign) {
                return null
            }
        }

        // Xác định isIncome linh hoạt: kiểm tra dấu trong số tiền, tiền tố trước đó hoặc từ khóa
        val startIndex = transactionMatch.range.first
        val prefixNear = fullText.substring((startIndex - 25).coerceAtLeast(0), startIndex).lowercase(Locale.ROOT)

        val isIncome: Boolean = when {
            matchedAmountStr.contains("+") -> true
            matchedAmountStr.contains("-") -> false
            prefixNear.contains("+") || prefixNear.contains("(+)") || prefixNear.contains("cộng") ||
                prefixNear.contains("cong") || prefixNear.contains("ghi có") || prefixNear.contains("ghi co") ||
                prefixNear.contains("tiền vào") || prefixNear.contains("tien vao") -> true
            prefixNear.contains("-") || prefixNear.contains("(-)") || prefixNear.contains("trừ") ||
                prefixNear.contains("tru") || prefixNear.contains("ghi nợ") || prefixNear.contains("ghi no") ||
                prefixNear.contains("thanh toán") || prefixNear.contains("thanh toan") -> false
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
