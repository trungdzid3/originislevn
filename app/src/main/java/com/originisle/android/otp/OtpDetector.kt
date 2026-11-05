package com.originisle.android.otp

/**
 * Pulls a one-time login / verification code out of a notification's text ("Your code is 482913",
 * "G-123456 is your Google verification code", "Code de vérification : 1234", "Mã xác thực của bạn là 849201").
 *
 * A code only counts when a code-ish keyword sits near it — a bare number in a chat message, an
 * order number or a price must never turn into a login-code card.
 *
 * On OriginOS 6 (Vivo), SMS notifications with "Verification Reminder" or "Nhắc nhở mã xác minh"
 * bypass the maximum distance check and regular prefix restrictions to reliably capture login codes.
 */
object OtpDetector {

    /** How far (in chars) a candidate may sit from the nearest keyword and still count. */
    private const val MAX_DISTANCE = 120

    /** Vivo OriginOS 6 SMS verification reminder header (English, Vietnamese, Chinese). */
    private val VIVO_REMINDER = Regex(
        """(?U)\b(?:""" +
            """verification\s+reminder|verification\s+notice|verification\s+code|security\s+reminder|verification|""" +
            """nhắc\s+nhở\s+mã\s+xác\s+minh|nhac\s+nho\s+ma\s+xac\s+minh|""" +
            """tin\s+nhắn\s+xác\s+minh|tin\s+nhan\s+xac\s+minh|""" +
            """mã\s+xác\s+minh|ma\s+xac\s+minh""" +
            """)\b|验证码提醒|验证码""",
        RegexOption.IGNORE_CASE,
    )

    private val KEYWORD = Regex(
        """(?U)\b(?:""" +
            // Vivo / Reminder headers
            """verification\s+reminder|verification\s+notice|security\s+reminder|""" +
            """nhắc\s+nhở\s+mã\s+xác\s+minh|nhac\s+nho\s+ma\s+xac\s+minh|""" +
            """tin\s+nhắn\s+xác\s+minh|tin\s+nhan\s+xac\s+minh|""" +
            // Vietnamese compound keywords (accented and unaccented)
            """mã\s+xác\s+minh|mã\s+xác\s+thực|mã\s+xác\s+nhận|mã\s+bảo\s+mật|mã\s+kích\s+hoạt|mã\s+otp|mã\s+đăng\s+nhập|mã\s+truy\s+cập|""" +
            """ma\s+xac\s+minh|ma\s+xac\s+thuc|ma\s+xac\s+nhan|ma\s+bao\s+mat|ma\s+kich\s+hoat|ma\s+otp|ma\s+dang\s+nhap|ma\s+truy\s+cap|""" +
            """mật\s+khẩu\s+tạm\s+thời|mat\s+khau\s+tam\s+thoi|mật\s+khẩu\s+dùng\s+một\s+lần|mat\s+khau\s+dung\s+mot\s+lan|""" +
            """xác\s+minh|xác\s+thực|xác\s+nhận|xac\s+minh|xac\s+thuc|xac\s+nhan|""" +
            // English phrases
            """verification\s+code|security\s+code|access\s+code|confirmation\s+code|validation\s+code|login\s+code|auth\s+code|""" +
            """one[- ]time\s+pass(?:word|code)|one[- ]time\s+code|one[- ]time|""" +
            """temp\s+password|temporary\s+password|""" +
            """your\s+code\s+is|is\s+your\s+code|use\s+code|enter\s+code|your\s+code|""" +
            // Single keywords & international
            """code|codes|otp|passcode|pin|2fa|mfa|password|""" +
            """verif\w*|authenticat\w*|confirm\w*|login|log[- ]in|sign[- ]in|""" +
            """c[oó]digo|codice|kod|bestätigung\w*|v[ée]rification|verificaci[oó]n|verifica""" +
            """)\b|验证码提醒|验证码|認証|인증""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Các biểu thức chính quy bắt trực tiếp mã OTP:
     * - (?i)(?:mã|ma|code|otp|passcode|pin)[:\s]+([0-9]{4,8})
     * - (?i)(?:là|la|is)[:\s]+([0-9]{4,8})
     * - (?i)([0-9]{4,8})\s+(?:là|la|is)\s+(?:mã|ma|code|otp)
     * - (?i)(?:G-|FB-|TG-|VK-)([0-9]{4,8})
     * - (?i)\[([0-9]{4,8})\]
     * - (?i)(?:\bOTP\b|\bmã xác thực\b|...)[^\d]{0,35}?(?<!\d)(\d{4,8})(?!\d)
     * - (?i)\bcode\s*[:\s]\s*([0-9]{4,8})\b
     */
    private val DIRECT_PATTERNS = listOf(
        Regex("""(?i)(?:\bOTP\b|\bma\s*xac\s*thuc\b|\bmã\s*xác\s*thực\b|\bverification\s*code\b|\bsecurity\s*code\b|\bcode\b)[^\d]{0,35}?(?<!\d)(\d{4,8})(?!\d)"""),
        Regex("""(?i)\bcode\s*[:\s]\s*([0-9]{4,8})\b"""),
        Regex("""(?i)(?:mã|ma|code|otp|passcode|pin)[:\s]+([0-9]{4,8})"""),
        Regex("""(?i)(?:là|la|is)[:\s]+([0-9]{4,8})"""),
        Regex("""(?i)([0-9]{4,8})\s+(?:là|la|is)\s+(?:mã|ma|code|otp)"""),
        Regex("""(?i)(?:G-|FB-|TG-|VK-)([0-9]{4,8})"""),
        Regex("""(?i)\[([0-9]{4,8})\]"""),
    )

    /**
     * 4–8 digits, or two groups of 3–4 split by a space/dash ("123 456", "123-456"), optionally
     * with common OTP prefixes such as "G-", "FB-", "TG-", "VK-", etc. Not part of a longer
     * number, a decimal, a time, a price, a percentage or a phone number.
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

    private val PHONE_PREFIX = Regex(
        """(?U)\b(?:sđt|sdt|số\s+điện\s+thoại|so\s+dien\s+thoai|điện\s+thoại|dien\s+thoai|""" +
            """liên\s+hệ|lien\s+he|tổng\s+đài|tong\s+dai|cskh|""" +
            """hotline|tel(?:ephone)?|phone(?:\s*(?:no|num|number))?|mobile(?:\s*(?:no|num|number))?|contact)\s*[:#.-]?\s*$""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Chỉ coi là tiền nếu đứng ngay sau "số tiền:" hoặc có ký hiệu tiền tệ rõ ràng.
     * BỎ việc "thanh toán", "chuyển khoản", "nạp", "rút", "payment", "paid" loại trừ nhầm OTP.
     */
    private val STRICT_MONEY_PREFIX = Regex(
        """(?U)\b(?:số\s+tiền|so\s+tien|amount|price|cost)\s*[:#.-]?\s*$""",
        RegexOption.IGNORE_CASE,
    )

    private val CURRENCY_PREFIX = Regex(
        """(?:[€$£¥₫đ]|\b(?:EUR|USD|GBP|CHF|VND|VNĐ)\b)\s*$""",
        RegexOption.IGNORE_CASE,
    )

    private val CURRENCY_SUFFIX = Regex(
        """^\s*(?:[€$£¥₫đ]|EUR|USD|GBP|CHF|VND|VNĐ|dong|đồng|k)\b""",
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

    /** The code (digits only) in [text], or null if it doesn't read like a login code. */
    fun find(text: String): String? {
        val isVivoReminder = VIVO_REMINDER.containsMatchIn(text)

        // 1. Thử bắt bằng các biểu thức chính quy trực tiếp
        for (pattern in DIRECT_PATTERNS) {
            val m = pattern.find(text)
            if (m != null) {
                val candidateCode = m.groupValues[1].filter(Char::isDigit)
                if (candidateCode.length in 4..8) {
                    val prefix = text.substring(0, m.range.first).takeLast(40)
                    val suffix = text.substring(m.range.last + 1).take(20)
                    val isMoney = STRICT_MONEY_PREFIX.containsMatchIn(prefix) ||
                        CURRENCY_PREFIX.containsMatchIn(prefix) ||
                        CURRENCY_SUFFIX.containsMatchIn(suffix)
                    if (!isMoney) {
                        return candidateCode
                    }
                }
            }
        }

        // 2. Nếu là Vivo Verification Reminder, quét tất cả số 4-8 chữ số gần nhất
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

        // 3. Cơ chế tìm kiếm theo từ khóa thông thường
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
        if (!isVivoReminder && looksLikeYear(raw)) return false

        val prefix = text.substring(0, match.range.first).takeLast(40)
        val suffix = text.substring(match.range.last + 1).take(20)

        // Kiểm tra tiền tệ nghiêm ngặt
        if (STRICT_MONEY_PREFIX.containsMatchIn(prefix) || CURRENCY_PREFIX.containsMatchIn(prefix)) return false
        if (CURRENCY_SUFFIX.containsMatchIn(suffix)) return false

        // Khi là Vivo Reminder, bỏ qua các prefix chặn thông thường để không bao giờ bỏ sót OTP
        if (isVivoReminder) return true

        if (ACCOUNT_PREFIX.containsMatchIn(prefix)) return false
        if (PHONE_PREFIX.containsMatchIn(prefix)) return false
        if (ORDER_PREFIX.containsMatchIn(prefix)) return false
        if (BIRTH_YEAR_PREFIX.containsMatchIn(prefix)) return false

        return true
    }

    /** Gap between two ranges, 0 if they touch or overlap. */
    private fun distance(a: IntRange, b: IntRange): Int = when {
        a.last < b.first -> b.first - a.last
        b.last < a.first -> a.first - b.last
        else -> 0
    }

    /** "2026" next to "code" is far more often a date ("code valid until 2026") or birth year than a code. */
    private fun looksLikeYear(s: String): Boolean =
        s.length == 4 && s.toIntOrNull()?.let { it in 1900..2099 } == true
}
