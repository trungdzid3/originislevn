package com.originisle.android.otp

import android.os.Bundle
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

/**
 * Trích xuất mã xác thực OTP / đăng nhập một lần từ nội dung thông báo.
 * Hỗ trợ tiếng Việt có dấu, tiếng Việt KHÔNG DẤU, tiếng Anh, tiếng Trung và format OriginOS 6 (Vivo).
 *
 * Tích hợp bộ lọc tin nhắn rác, quảng cáo ngân hàng, hotline và giao dịch số dư để không bắt nhầm.
 */
object OtpDetector {

    /** Cự ly tối đa (số ký tự) giữa từ khóa và mã ứng viên. */
    private const val MAX_DISTANCE = 120

    /** Vivo OriginOS 6 SMS verification reminder header (English, Vietnamese, Chinese). */
    private val VIVO_REMINDER = Regex(
        """(?U)\b(?:""" +
            """verification\s+reminder|verification\s+notice|verification\s+code|security\s+reminder|verification|""" +
            """nhắc\s+nhở\s+mã\s+xác\s+minh|nhac\s+nho\s+ma\s+xac\s+minh|""" +
            """tin\s+nhắn\s+xác\s+minh|tin\s+nhan\s+xac\s+minh|""" +
            """mã\s+xác\s+minh|ma\s+xac\s+minh|mã\s+xác\s+thực|ma\s+xac\s+thuc|mã\s+xác\s+nhận|ma\s+xac\s+nhan""" +
            """)\b|验证码提醒|验证码""",
        RegexOption.IGNORE_CASE,
    )

    /** Các cụm từ trên nút hành động (Notification Actions) biểu thị mã OTP cần sao chép. */
    private val COPY_ACTION_REGEX = Regex(
        """(?U)\b(?:sao\s+chép(?:\s+mã|\s+ma|\s+otp|\s+mã\s+xác\s+thực)?|sao\s+chep(?:\s+ma|\s+otp|\s+ma\s+xac\s+thuc)?|copy(?:\s+code|\s+otp)?)\b""",
        RegexOption.IGNORE_CASE,
    )

    /** Trích xuất số 4-8 chữ số trực tiếp từ nút hành động (Notification Action) */
    private val ACTION_CODE_REGEX = Regex("""\b([0-9]{4,8})\b""")

    /** Từ khóa chỉ dấu OTP mạnh kiểu Ngân hàng & dịch vụ tại Việt Nam (Bank-Style Fast OTP Marker) */
    val STRONG_OTP_MARKER = Regex(
        """(?U)\b(?:otp|smartotp|smart\s+otp|mã\s+xác\s+thực|ma\s+xac\s+thuc|mã\s+xác\s+minh|ma\s+xac\s+minh|mã\s+xác\s+nhận|ma\s+xac\s+nhan|mã\s+đăng\s+nhập|ma\s+dang\s+nhap|mã\s+bảo\s+mật|ma\s+bao\s+mat|mật\s+khẩu\s+một\s+lần|mat\s+khau\s+mot\s+lan|verification\s+code|security\s+code|auth\s+code|login\s+code|one[- ]time)\b|验证码""",
        RegexOption.IGNORE_CASE,
    )

    /** Bộ gợi ý từ khóa OTP tiếng Việt (có dấu & không dấu) và quốc tế */
    val OTP_HINT = Regex(
        """(?U)\b(?:""" +
            // Tiếng Việt có dấu & KHÔNG DẤU
            """mã\s+xác\s+thực|ma\s+xac\s+thuc|mã\s+xác\s+nhận|ma\s+xac\s+nhan|""" +
            """mã\s+kích\s+hoạt|ma\s+kich\s+hoat|mã\s+đăng\s+nhập|ma\s+dang\s+nhap|""" +
            """mã\s+bảo\s+mật|ma\s+bao\s+mat|mã\s+giao\s+dịch|ma\s+giao\s+dich|""" +
            """mã\s+xác\s+minh|ma\s+xac\s+minh|mã\s+otp|ma\s+otp|mã\s+số|ma\s+so|mã\s+pin|ma\s+pin|""" +
            """mật\s+khẩu\s+dùng\s+một\s+lần|mat\s+khau\s+dung\s+mot\s+lan|""" +
            """mật\s+khẩu\s+một\s+lần|mat\s+khau\s+mot\s+lan|""" +
            """mật\s+khẩu\s+tạm\s+thời|mat\s+khau\s+tam\s+thoi|""" +
            """smartotp|smart\s+otp|""" +
            """hiệu\s+lực\s+trong|hieu\s+luc\s+trong|hết\s+hạn\s+trong|het\s+han\s+trong|""" +
            // Tiếng Anh & quốc tế
            """verification\s+code|security\s+code|access\s+code|confirmation\s+code|validation\s+code|login\s+code|auth\s+code|""" +
            """one[- ]time\s+pass(?:word|code)|one[- ]time\s+code|one[- ]time|temp\s+password|temporary\s+password|""" +
            """your\s+code\s+is|is\s+your\s+code|use\s+code|enter\s+code|your\s+code|""" +
            """expires\s+in|valid\s+for|valid\s+within|expire\s+after|""" +
            """authen|authentication|authenticator|passcode|code|codes|otp|pin|2fa|mfa|password|""" +
            """verif\w*|confirm\w*|login|log[- ]in|sign[- ]in|""" +
            """c[oó]digo|codice|kod|bestätigung\w*|v[ée]rification|verificaci[oó]n|verifica""" +
            """)\b|验证码提醒|验证码|認証|인증""",
        RegexOption.IGNORE_CASE,
    )

    private val KEYWORD = OTP_HINT

    /**
     * Biểu thức chính quy trực tiếp bắt mã OTP 4-8 chữ số:
     * - Hỗ trợ có ngoặc đơn: Mã xác thực (OTP), Mã xác minh (OTP)...
     * - Hỗ trợ có tên thương hiệu ở giữa: Mã xác minh Google, Mã xác nhận Shopee, Mã xác thực ZaloPay...
     * - Hỗ trợ OTP: 490603, Mã OTP: 490603, OTP - 490603...
     * - Sau mã giao dịch / bảo mật / đăng nhập / kích hoạt / SmartOTP
     * - Sau mật khẩu một lần (mat khau mot lan)
     * - Đứng trước hoặc sau cụm từ hiệu lực trong / expires in
     * - Định dạng prefix G-123456, FB-123456, [123456]
     */
    private val DIRECT_PATTERNS = listOf(
        // 1. Mã xác thực/minh/nhận/OTP có (OTP) hoặc có tên thương hiệu ở giữa:
        // ví dụ: "Mã xác thực (OTP) của bạn là 490603", "Mã xác minh Google của bạn là 490603", "Mã xác nhận Shopee là 490603", "OTP của bạn là: 490603"
        Regex("""(?i)(?:(?:mã|ma)\s*)?(?:xác\s*thực|xac\s*thuc|xác\s*minh|xac\s*minh|xác\s*nhận|xac\s*nhan|otp|code)?\s*(?:\([^)]*\)\s*)?(?:[a-z0-9_.-]{2,20}\s+)?(?:otp|code)?\s*(?:của\s*bạn|cua\s*ban)?\s*(?:là|la|:)\s*[:：-]?\s*([0-9]{4,8})(?!\d)"""),

        // 2. OTP / Mã OTP: 490603, OTP - 490603, Mã xác minh (OTP): 490603, Mã OTP của bạn là: 490603
        Regex("""(?i)\b(?:mã|ma)?\s*(?:otp|code)\s*(?:\([^)]*\))?\s*[:：-]\s*([0-9]{4,8})(?!\d)"""),
        Regex("""(?i)(?:mã|ma)\s*(?:xác\s*thực|xac\s*thuc|xác\s*minh|xac\s*minh|xác\s*nhận|xac\s*nhan)\s*(?:\([^)]*\))?\s*[:：-]\s*([0-9]{4,8})(?!\d)"""),

        // 3. Mã giao dịch / bảo mật / đăng nhập / kích hoạt
        Regex("""(?i)(?:mã|ma)\s*(?:giao\s*dịch|giao\s*dich|bảo\s*mật|bao\s*mat|đăng\s*nhập|dang\s*nhap|kích\s*hoạt|kich\s*hoat)\s*(?:otp)?\s*(?:của\s*bạn|cua\s*ban)?\s*(?:là|la|:)?\s*[:：-]?\s*([0-9]{4,8})(?!\d)"""),

        // 4. SmartOTP
        Regex("""(?i)\b(?:smart\s*otp|smartotp)\s*[:：-]?\s*([0-9]{4,8})(?!\d)"""),

        // 5. Mật khẩu một lần / mat khau mot lan
        Regex("""(?i)(?:mật\s*khẩu\s*một\s*lần|mat\s*khau\s*mot\s*lan|mật\s*khẩu\s*dùng\s*một\s*lần|mat\s*khau\s*dung\s*mot\s*lan)\s*[:：-]?\s*([0-9]{4,8})(?!\d)"""),

        // 6. Passcode, Auth code, Verification code, Security code
        Regex("""(?i)(?:passcode|authen(?:tication)?\s*code)\s*[:：-]?\s*([0-9]{4,8})(?!\d)"""),
        Regex("""(?i)(?:verification|security|confirmation|validation|login|auth|one[- ]time)\s*(?:code|password|passcode)?\s*(?:is\s*)?[:：-]?\s*([0-9]{4,8})(?!\d)"""),
        Regex("""(?i)(?:your\s+code\s+is|use\s+code|enter\s+code)\s*[:：-]?\s*([0-9]{4,8})(?!\d)"""),

        // 7. Số đứng trước từ khóa: "490603 là mã...", "490603 là mã xác nhận Shopee..."
        Regex("""(?i)(?<!\d)([0-9]{4,8})\s+(?:là|la|is)\s+(?:mã|ma|code|otp|smart\s*otp|smartotp)"""),

        // 8. Hiệu lực trong / expires in
        Regex("""(?i)\b([0-9]{4,8})\s*(?:(?:co\s*|có\s*)?(?:hieu\s*luc|hiệu\s*lực)\s*trong|expires\s*in|valid\s*for)"""),
        Regex("""(?i)(?:hieu\s*luc\s*trong|hiệu\s*lực\s*trong|expires\s*in)\s*[^0-9]{0,20}\b([0-9]{4,8})(?!\d)"""),

        // 9. Tiền tố hệ thống quốc tế G-123456, FB-123456...
        Regex("""(?i)(?:G-|FB-|TG-|VK-)([0-9]{4,8})(?!\d)"""),
        Regex("""(?i)\[([0-9]{4,8})\]"""),

        // 10. Quét tổng quát có chứa từ khóa OTP trong cự ly gần
        Regex("""(?i)\b(?:otp|mã\s*xác\s*thực|ma\s*xac\s*thuc|mã\s*xác\s*nhận|ma\s*xac\s*nhan|mã\s*xác\s*minh|ma\s*xac\s*minh|mã\s*đăng\s*nhập|ma\s*dang\s*nhap|mã\s*giao\s*dịch|ma\s*giao\s*dich|tin\s*nhắn\s*xác\s*minh|tin\s*nhan\s*xac\s*minh|verification\s*code|security\s*code)\b[^\d]{0,40}?([0-9]{4,8})(?!\d)"""),
        Regex("""(?i)\b([0-9]{4,8})\b(?=.*(?:mã|ma|otp|code|xác\s*thực|xac\s*thuc|xác\s*minh|xac\s*minh|smartotp))"""),
    )

    /**
     * 4–8 chữ số, hoặc 2 nhóm 3–4 số cách nhau bởi dấu cách/gạch nối ("123 456", "123-456"),
     * có thể kèm prefix G-, FB-, TG-, VK-...
     */
    private val CANDIDATE = Regex(
        """(?<![\d.,/#+€$£¥₫đ-])(?<!\d:)(?<!\d[ -])(?:(?<![A-Za-z])[A-Za-z]{1,4}-)?(\d{3,4}[ -]\d{3,4}|\d{4,8})""" +
            """(?!\d|[.,:]\d|[ -]\d|\s*[%€$£¥₫đ]|\s*(?:EUR|USD|GBP|CHF|VND|VNĐ|dong|đồng|k)\b)""",
        RegexOption.IGNORE_CASE,
    )

    private val ACCOUNT_PREFIX = Regex(
        """(?U)\b(?:tk|stk|số\s+tk|so\s+tk|tài\s+khoản|tai\s+khoan|số\s+tài\s+khoản|so\s+tai\s+khoan|""" +
            """số\s+thẻ|so\s+the|thẻ|the|acc(?:ount)?(?:\s*(?:no|num|number|id))?|card(?:\s*(?:no|num|number))?)\s*[:#.-]?\s*$""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Tiền tố số điện thoại / hotline: chỉ loại bỏ khi đứng NGAY TRƯỚC candidate code
     * (kết thúc bằng dấu hai chấm hoặc khoảng trắng sát mã), tránh chặn oan mã OTP
     * khi tin nhắn có câu "Liên hệ 1900xxxx để được hỗ trợ".
     */
    private val PHONE_PREFIX = Regex(
        """(?U)\b(?:sđt|sdt|số\s+điện\s+thoại|so\s+dien\s+thoai|điện\s+thoại|dien\s+thoai|""" +
            """tổng\s+đài|tong\s+dai|cskh|hotline|hỗ\s+trợ|ho\s+tro|tư\s+vấn|tu\s+van|""" +
            """tel(?:ephone)?|phone(?:\s*(?:no|num|number))?|mobile(?:\s*(?:no|num|number))?|contact|gọi|goi|call|""" +
            """liên\s+hệ|lien\s+he|1800|1900)\s*[:#.-]?\s*$""",
        RegexOption.IGNORE_CASE,
    )

    private val STRICT_MONEY_PREFIX = Regex(
        """(?U)\b(?:số\s+tiền|so\s+tien|amount|price|cost)\s*[:#.-]?\s*$""",
        RegexOption.IGNORE_CASE,
    )

    private val CURRENCY_PREFIX = Regex(
        """(?:[€$£¥₫đ]|\b(?:EUR|USD|GBP|CHF|VND|VNĐ)\b)\s*$""",
        RegexOption.IGNORE_CASE,
    )

    private val CURRENCY_SUFFIX = Regex(
        """^\s*(?:[€$£¥₫đd]|EUR|USD|GBP|CHF|VND|VNĐ|dong|đồng|k)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val ORDER_PREFIX = Regex(
        """(?U)\b(?:đơn\s+hàng|don\s+hang|mã\s+đơn(?: hàng)?|ma\s+don(?: hàng)?|số\s+đơn|so\s+don|""" +
            """hóa\s+đơn|hoa\s+don|vận\s+đơn|van\s+don|mã\s+vận\s+đơn|ma\s+van\s+don|kiện\s+hàng|kien\s+hang|""" +
            """order(?:\s*(?:no|num|number|id|code))?|tracking(?:\s*(?:no|num|number|id|code))?|""" +
            """invoice(?:\s*(?:no|num|number|id))?|bill(?:\s*(?:no|num|number|id))?|parcel|shipment)\s*[:#.-]?\s*$""",
        RegexOption.IGNORE_CASE,
    )

    private val BIRTH_YEAR_PREFIX = Regex(
        """(?U)\b(?:sinh\s+năm|sinh\s+nam|năm\s+sinh|nam\s+sinh|ns|dob|birth\s*year|year\s+of\s+birth)\s*[:#.-]?\s*$""",
        RegexOption.IGNORE_CASE,
    )

    /** Từ khóa tin nhắn rác / quảng cáo cần loại trừ nếu không có từ khóa OTP cụ thể */
    private val SPAM_PROMO_REGEX = Regex(
        """(?i)\b(?:khuyen\s+mai|khuyến\s+mại|uu\s+dai|ưu\s+đãi|voucher|giam\s+gia|giảm\s+giá|tra\s+gop\s+0%|trả\s+góp\s+0%|lai\s+suat|lãi\s+suất)\b""",
    )

    /**
     * Hotline tại Việt Nam (1800xxxx, 1900xxxx) và số điện thoại di động
     * tuyệt đối không được nhận nhầm là mã xác thực OTP.
     */
    private fun isHotlineNumber(digits: String): Boolean {
        if ((digits.startsWith("1800") || digits.startsWith("1900")) && digits.length >= 6) {
            return true
        }
        if (digits.startsWith("0") && digits.length in 9..11) {
            return true
        }
        return false
    }

    /**
     * Nhận diện số điện thoại di động 10 số tại Việt Nam (09x, 03x, 07x, 08x, 05x)
     * hoặc đầu số quốc tế (+84...) để loại trừ triệt để.
     */
    private fun isPhoneNumber(digits: String): Boolean {
        if (digits.length == 10 && Regex("""^0[35789]\d{8}$""").matches(digits)) {
            return true
        }
        if (digits.startsWith("84") && digits.length in 11..12) {
            return true
        }
        return false
    }

    /**
     * Danh sách các package hệ thống và trình phát nhạc cần loại trừ khi quét OTP.
     */
    private val EXCLUDED_OTP_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.autonavi.minimap",
        "com.originisle.android",
        "com.spotify.music",
        "com.google.android.apps.youtube.music",
        "com.google.android.youtube",
        "app.revanced.android.youtube",
        "com.samsung.android.app.watchmanager",
        "com.samsung.wearable.fit3plugin",
    )

    /**
     * Nhận diện thông báo đã bị Android 15/16 hoặc Vivo OriginOS 6 ẩn nội dung nhạy cảm (FLAG_OTP_REDACTED).
     */
    val REDACTED_OTP_NOTICE = Regex(
        """(?U)(?:sensitive\s+notification\s+content\s+hidden|contents?\s+hidden|đã\s+ẩn\s+nội\s+dung\s+thông\s+báo\s+nhạy\s+cảm|nội\s+dung\s+nhạy\s+cảm\s+đã\s+được\s+ẩn|đã\s+ẩn\s+nội\s+dung|bảo\s+vệ\s+mã\s+xác\s+minh|敏感通知内容已隐藏|验证码保护)""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Kiểm tra package có khả năng là ứng dụng nhắn tin, ngân hàng, email hoặc nhận OTP hay không.
     */
    fun isLikelyMessagingOrOtpPackage(pkg: String): Boolean {
        val clean = pkg.trim().lowercase()
        if (clean.isEmpty() || clean in EXCLUDED_OTP_PACKAGES) return false
        if (clean.startsWith("com.android.providers.") || clean.startsWith("com.vivo.daemonservice")) {
            return false
        }
        return true
    }

    /**
     * Bóc tách toàn diện chuỗi văn bản từ [StatusBarNotification], bao gồm:
     * - MessagingStyle (ưu tiên tin nhắn mới nhất lên đầu tiên qua asReversed())
     * - Extras thông thường (bigText, text, subText, title, lines...)
     * - Ticker text của notification
     * - publicVersion khi máy bị khóa màn hình hoặc nội dung bị redact/ẩn
     */
    fun extractNotificationText(sbn: StatusBarNotification): String {
        val n = sbn.notification
        val parts = mutableListOf<String>()

        // 1. Ưu tiên đặt MessagingStyle LÊN ĐẦU TIÊN và duyệt ngược từ tin nhắn mới nhất về cũ nhất
        runCatching {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)?.let { style ->
                style.messages.asReversed().forEach { msg ->
                    msg.text?.toString()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                }
                style.historicMessages.asReversed().forEach { msg ->
                    msg.text?.toString()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                }
            }
        }

        // 2. Extras thông thường (ưu tiên EXTRA_MESSAGES đảo ngược, EXTRA_BIG_TEXT, EXTRA_TEXT trước EXTRA_TITLE)
        val extras = n.extras
        if (extras != null) {
            extractMessagingTexts(extras).forEach { text ->
                if (text.isNotBlank()) parts.add(text)
            }

            runCatching {
                extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES)
                    ?.mapNotNull { it?.toString() }
                    ?.asReversed()
            }.getOrNull()?.filter { it.isNotBlank() }?.let { parts.addAll(it) }

            listOf(
                NotificationCompat.EXTRA_BIG_TEXT,
                NotificationCompat.EXTRA_TEXT,
                NotificationCompat.EXTRA_SUB_TEXT,
                NotificationCompat.EXTRA_SUMMARY_TEXT,
                NotificationCompat.EXTRA_INFO_TEXT,
                NotificationCompat.EXTRA_TITLE,
                NotificationCompat.EXTRA_TITLE_BIG,
            ).forEach { key ->
                runCatching { extras.getCharSequence(key)?.toString() }
                    .getOrNull()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            }
        }

        // 3. Ticker text
        runCatching { n.tickerText?.toString() }
            .getOrNull()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }

        // 4. publicVersion (khi thông báo bị redact/ẩn trên lockscreen)
        val pubN = n.publicVersion
        if (pubN != null) {
            runCatching {
                NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(pubN)?.let { style ->
                    style.messages.asReversed().forEach { msg ->
                        msg.text?.toString()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    }
                }
            }
            if (pubN.extras != null) {
                extractMessagingTexts(pubN.extras).forEach { text ->
                    if (text.isNotBlank()) parts.add(text)
                }
                listOf(
                    NotificationCompat.EXTRA_BIG_TEXT,
                    NotificationCompat.EXTRA_TEXT,
                    NotificationCompat.EXTRA_SUB_TEXT,
                    NotificationCompat.EXTRA_TITLE,
                ).forEach { key ->
                    runCatching { pubN.extras.getCharSequence(key)?.toString() }
                        .getOrNull()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                }
                runCatching { pubN.tickerText?.toString() }
                    .getOrNull()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            }
        }

        return parts.distinct().joinToString("\n")
    }

    /**
     * Trích xuất các đoạn text trong EXTRA_MESSAGES và EXTRA_HISTORIC_MESSAGES (duyệt ngược từ mới nhất về cũ nhất)
     */
    private fun extractMessagingTexts(bundle: Bundle): List<String> {
        val results = mutableListOf<String>()
        val keys = listOf(
            "android.messages",
            NotificationCompat.EXTRA_MESSAGES,
            "android.messages.historic",
            NotificationCompat.EXTRA_HISTORIC_MESSAGES,
        )
        for (key in keys) {
            runCatching {
                val rawArray = bundle.getParcelableArray(key)
                rawArray?.reversedArray()?.forEach { item ->
                    if (item is Bundle) {
                        item.getCharSequence("text")?.toString()?.takeIf { it.isNotBlank() }?.let { results.add(it) }
                    }
                }
            }
        }
        return results
    }

    /**
     * Trích xuất mã OTP trực tiếp từ StatusBarNotification.
     */
    fun find(sbn: StatusBarNotification, actionTitles: List<String> = emptyList()): String? {
        val actions = if (actionTitles.isNotEmpty()) {
            actionTitles
        } else {
            val list = mutableListOf<String>()
            sbn.notification.actions?.forEach { a ->
                a.title?.toString()?.takeIf { it.isNotBlank() }?.let { list.add(it) }
            }
            for (i in 0 until NotificationCompat.getActionCount(sbn.notification)) {
                NotificationCompat.getAction(sbn.notification, i)?.title?.toString()
                    ?.takeIf { it.isNotBlank() }?.let { list.add(it) }
            }
            list.distinct()
        }
        val fullText = extractNotificationText(sbn)
        return find(fullText, actions)
    }

    /**
     * Trích xuất mã OTP từ chuỗi văn bản [text] và các nút hành động [actionTitles].
     */
    fun find(text: String, actionTitles: List<String> = emptyList()): String? {
        // 0. Trích xuất mã OTP trực tiếp từ Action Title của Notification nếu có:
        // Trong Vivo OriginOS SMS / Google Messages, nút hành động thường chứa: "Sao chép 490603", "Copy 490603" hoặc "490603"
        for (action in actionTitles) {
            for (m in ACTION_CODE_REGEX.findAll(action)) {
                val candidateCode = m.groupValues[1]
                if (!isHotlineNumber(candidateCode) && !isPhoneNumber(candidateCode)) {
                    return candidateCode
                }
            }
        }

        // 1. Quy tắc nhận diện nhanh kiểu Ngân hàng (Bank-Style Fast OTP Rule):
        // Nếu text chứa từ khóa chỉ dấu OTP mạnh (STRONG_OTP_MARKER), quét tìm các cụm 4-8 số độc lập,
        // loại bỏ số tiền (có chữ đ, vnd, k sát cạnh), hotline (1800/1900), SĐT 10 số (09x/03x/07x/08x/05x), năm sinh, số tài khoản.
        // Trả về ngay cụm 4-8 số gần nhất với từ khóa OTP.
        val strongMarkers = STRONG_OTP_MARKER.findAll(text).map { it.range }.toList()
        if (strongMarkers.isNotEmpty()) {
            val fastCandidates = CANDIDATE.findAll(text)
                .filter { m -> isValidCandidate(text, m, isVivoReminder = false) }
                .map { m ->
                    val dist = strongMarkers.minOf { k -> distance(m.range, k) }
                    m to dist
                }
                .filter { (_, d) -> d <= MAX_DISTANCE }
                .toList()

            if (fastCandidates.isNotEmpty()) {
                val best = fastCandidates.minByOrNull { (_, d) -> d }
                val code = best?.first?.groupValues?.get(1)?.filter(Char::isDigit)
                if (code != null) {
                    return code
                }
            }
        }

        val hasCopyAction = actionTitles.any { COPY_ACTION_REGEX.containsMatchIn(it) } ||
            COPY_ACTION_REGEX.containsMatchIn(text)
        val isVivoReminder = VIVO_REMINDER.containsMatchIn(text)

        // 2. Thử bắt bằng các biểu thức chính quy trực tiếp (quét toàn bộ các match để tránh vướng số đt / tiền tệ)
        for (pattern in DIRECT_PATTERNS) {
            for (m in pattern.findAll(text)) {
                val candidateCode = m.groupValues[1].filter(Char::isDigit)
                if (candidateCode.length in 4..8 && !isHotlineNumber(candidateCode)) {
                    val prefix = text.substring(0, m.range.first).takeLast(40)
                    val suffix = text.substring(m.range.last + 1).take(20)
                    val isMoney = STRICT_MONEY_PREFIX.containsMatchIn(prefix) ||
                        CURRENCY_PREFIX.containsMatchIn(prefix) ||
                        CURRENCY_SUFFIX.containsMatchIn(suffix)
                    val isPhone = PHONE_PREFIX.containsMatchIn(prefix)
                    if (!isMoney && !isPhone) {
                        return candidateCode
                    }
                }
            }
        }

        // 2. Nếu có Action "Sao chép mã" / "Copy code", trích xuất ngay số 4-8 chữ số hợp lệ
        if (hasCopyAction) {
            val actionCandidates = CANDIDATE.findAll(text)
                .filter { m -> isValidCandidate(text, m, isVivoReminder = false) }
                .toList()

            if (actionCandidates.isNotEmpty()) {
                val keywords = KEYWORD.findAll(text).map { it.range }.toList()
                return if (keywords.isNotEmpty()) {
                    actionCandidates.minByOrNull { m -> keywords.minOf { k -> distance(m.range, k) } }
                        ?.groupValues?.get(1)
                        ?.filter(Char::isDigit)
                } else {
                    actionCandidates.first().groupValues[1].filter(Char::isDigit)
                }
            }
        }

        // 3. Nếu là Vivo Verification Reminder, quét tất cả số 4-8 chữ số gần nhất
        if (isVivoReminder) {
            val reminderMatch = VIVO_REMINDER.find(text)
            val reminderPos = reminderMatch?.range ?: (0..0)
            val candidates = CANDIDATE.findAll(text)
                .filter { m -> isValidCandidate(text, m, isVivoReminder = true) }
                .map { m ->
                    val dist = distance(m.range, reminderPos)
                    m to dist
                }
                .toList()

            if (candidates.isNotEmpty()) {
                return candidates.minByOrNull { (_, d) -> d }
                    ?.first?.groupValues?.get(1)
                    ?.filter(Char::isDigit)
            }
        }

        // 4. Cơ chế tìm kiếm theo từ khóa thông thường
        val keywords = KEYWORD.findAll(text).map { it.range }.toList()
        if (keywords.isEmpty()) return null

        return CANDIDATE.findAll(text)
            .filter { m -> isValidCandidate(text, m, isVivoReminder = false) }
            .map { m ->
                val dist = keywords.minOf { k -> distance(m.range, k) }
                m to dist
            }
            .filter { (_, d) -> d <= MAX_DISTANCE }
            .minByOrNull { (_, d) -> d }
            ?.first?.groupValues?.get(1)
            ?.filter(Char::isDigit)
    }

    private fun isValidCandidate(text: String, match: MatchResult, isVivoReminder: Boolean): Boolean {
        val raw = match.groupValues[1]
        val digits = raw.filter(Char::isDigit)
        if (digits.length !in 4..8) return false
        if (isHotlineNumber(digits) || isPhoneNumber(digits)) return false

        val prefix = text.substring(0, match.range.first).takeLast(40)
        val suffix = text.substring(match.range.last + 1).take(20)

        // Không bao giờ nhận diện số điện thoại / hotline hỗ trợ kể cả khi là Vivo Reminder
        if (PHONE_PREFIX.containsMatchIn(prefix)) return false

        // Kiểm tra tiền tệ nghiêm ngặt
        if (STRICT_MONEY_PREFIX.containsMatchIn(prefix) || CURRENCY_PREFIX.containsMatchIn(prefix)) return false
        if (CURRENCY_SUFFIX.containsMatchIn(suffix)) return false

        // Khi là Vivo Reminder, bỏ qua các prefix chặn số tài khoản/đơn hàng
        if (isVivoReminder) return true

        if (looksLikeYear(raw)) return false
        if (ACCOUNT_PREFIX.containsMatchIn(prefix)) return false
        if (ORDER_PREFIX.containsMatchIn(prefix)) return false
        if (BIRTH_YEAR_PREFIX.containsMatchIn(prefix)) return false

        return true
    }

    /** Khoảng cách giữa 2 range, bằng 0 nếu chạm nhau hoặc giao nhau. */
    private fun distance(a: IntRange, b: IntRange): Int = when {
        a.last < b.first -> b.first - a.last
        b.last < a.first -> a.first - b.last
        else -> 0
    }

    /** Kiểm tra xem chuỗi có phải là năm (ví dụ "2026") hay không để tránh bắt nhầm. */
    private fun looksLikeYear(s: String): Boolean =
        s.length == 4 && s.toIntOrNull()?.let { it in 1900..2099 } == true
}
