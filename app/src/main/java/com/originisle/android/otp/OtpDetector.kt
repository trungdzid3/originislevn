package com.originisle.android.otp

/**
 * Pulls a one-time login / verification code out of a notification's text ("Your code is 482913",
 * "G-123456 is your Google verification code", "Code de vérification : 1234", "Mã xác thực của bạn là 849201",
 * "Nha Thuoc FPT Long Chau: Ma xac thuc OTP cua ban la 918119...").
 *
 * A code only counts when a code-ish keyword sits near it — a bare number in a chat message, an
 * order number, a price, or a customer service hotline (1800xxxx, 1900xxxx) must never turn into a login-code card.
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
            """mã\s+xác\s+minh|ma\s+xac\s+minh|mã\s+xác\s+thực|ma\s+xac\s+thuc|mã\s+xác\s+nhận|ma\s+xac\s+nhan""" +
            """)\b|验证码提醒|验证码""",
        RegexOption.IGNORE_CASE,
    )

    /** Notification action button phrases indicating an OTP code to copy. */
    private val COPY_ACTION_REGEX = Regex(
        """(?U)\b(?:sao\s+chép(?:\s+mã|\s+ma|\s+otp|\s+mã\s+xác\s+thực)?|sao\s+chep(?:\s+ma|\s+otp|\s+ma\s+xac\s+thuc)?|copy(?:\s+code|\s+otp)?)\b""",
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
            """mã\s+xác\s+thực\s+otp|ma\s+xac\s+thuc\s+otp|mã\s+xác\s+nhận\s+otp|ma\s+xac\s+nhan\s+otp|""" +
            """mã\s+giao\s+dịch|ma\s+giao\s+dich|mã\s+pin|ma\s+pin|""" +
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
     * Biểu thức chính quy trực tiếp bắt mã OTP 4-8 chữ số:
     * - Sau OTP / mã xác thực / mã xác nhận / mã xác minh kèm "của bạn là", "là", ":", v.v.
     * - Sau mã giao dịch / mã bảo mật / mã đăng nhập / mã kích hoạt
     * - Sau "otp", "code", "mã", "ma" kèm dấu phân tách
     * - Trước "là mã", "la ma", "là OTP", "is code"
     * - Từ khóa OTP/xác thực/xác nhận nằm trong cự ly 40 ký tự
     * - Dạng prefix G-123456, FB-123456
     * - Đóng trong ngoặc vuông [123456]
     */
    private val DIRECT_PATTERNS = listOf(
        Regex("""(?i)(?:(?:mã|ma)\s*)?(?:xác\s*thực|xac\s*thuc|xác\s*minh|xac\s*minh|xác\s*nhận|xac\s*nhan)?\s*(?:otp|code)?\s*(?:của\s*bạn|cua\s*ban)?\s*(?:là|la)\s*[:：-]?\s*([0-9]{4,8})(?!\d)"""),
        Regex("""(?i)(?:mã|ma)\s*(?:giao\s*dịch|giao\s*dich|bảo\s*mật|bao\s*mat|đăng\s*nhập|dang\s*nhap|kích\s*hoạt|kich\s*hoat)?\s*(?:otp)?\s*(?:của\s*bạn|cua\s*ban)?\s*(?:là|la)\s*[:：-]?\s*([0-9]{4,8})(?!\d)"""),
        Regex("""(?i)(?:otp|code|mã|ma)\s*[:：-]\s*([0-9]{4,8})(?!\d)"""),
        Regex("""(?i)(?<!\d)([0-9]{4,8})\s+(?:là|la|is)\s+(?:mã|ma|code|otp)"""),
        Regex("""(?i)\b(?:otp|mã\s*xác\s*thực|ma\s*xac\s*thuc|mã\s*xác\s*nhận|ma\s*xac\s*nhan|mã\s*xác\s*minh|ma\s*xac\s*minh|mã\s*đăng\s*nhập|ma\s*dang\s*nhap|mã\s*giao\s*dịch|ma\s*giao\s*dich|tin\s*nhắn\s*xác\s*minh|tin\s*nhan\s*xac\s*minh|verification\s*code|security\s*code)\b[^\d]{0,40}?([0-9]{4,8})(?!\d)"""),
        Regex("""(?i)\bcode\s*[:：]?\s*([0-9]{4,8})(?!\d)"""),
        Regex("""(?i)(?:\blà\b|\bla\b|\bis\b)\s*[:：-]?\s*([0-9]{4,8})(?!\d)"""),
        Regex("""(?i)(?:G-|FB-|TG-|VK-)([0-9]{4,8})(?!\d)"""),
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
        """(?U)(?:\b(?:sđt|sdt|số\s+điện\s+thoại|so\s+dien\s+thoai|điện\s+thoại|dien\s+thoai|""" +
            """tổng\s+đài|tong\s+dai|cskh|hotline|hỗ\s+trợ|ho\s+tro|tư\s+vấn|tu\s+van|""" +
            """tel(?:ephone)?|phone(?:\s*(?:no|num|number))?|mobile(?:\s*(?:no|num|number))?|contact|gọi|goi|call|""" +
            """1800|1900)\s*[:#.-]?\s*$|""" +
            """\b(?:liên\s+hệ|lien\s+he)\b.{0,25}$)""",
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

    /**
     * Vietnam hotline numbers (1800xxxx, 1900xxxx) are toll-free or paid customer service numbers
     * that must never be mistaken for an OTP verification code.
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

    /** The code (digits only) in [text], or null if it doesn't read like a login code. */
    fun find(text: String, actionTitles: List<String> = emptyList()): String? {
        val hasCopyAction = actionTitles.any { COPY_ACTION_REGEX.containsMatchIn(it) } ||
            COPY_ACTION_REGEX.containsMatchIn(text)
        val isVivoReminder = VIVO_REMINDER.containsMatchIn(text)

        // 1. Thử bắt bằng các biểu thức chính quy trực tiếp
        for (pattern in DIRECT_PATTERNS) {
            val m = pattern.find(text)
            if (m != null) {
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
        if (isHotlineNumber(digits)) return false

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
