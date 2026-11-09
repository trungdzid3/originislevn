package com.originisle.android.smart

import android.graphics.Bitmap
import android.net.Uri
import java.util.regex.Pattern

object ScreenshotSmartExtractor {

    // --- Regex Vé máy bay (Flight) ---
    private val FLIGHT_CODE_REGEX = Pattern.compile(
        """\b(VN|VJ|QH|VU|BL|K6|TG|SQ|KE|OZ|CX|BR|CI|EK|QR|JL|NH)\s*([0-9]{2,4})\b""",
        Pattern.CASE_INSENSITIVE
    )
    private val AIRPORT_CODE_REGEX = Pattern.compile(
        """\b(SGN|HAN|DAD|CXR|PQC|HPH|VDO|VII|HUI|BMV|DLI|UIH|VCL|THD|TBB|DIN|VCA|VKG|CAH|BKK|DMK|SIN|ICN|NRT|HND|TPE|HKG)\b""",
        Pattern.CASE_INSENSITIVE
    )
    private val TIME_REGEX = Pattern.compile("""\b([01]?\d|2[0-3]):([0-5]\d)\b""")
    private val GATE_REGEX = Pattern.compile("""(?i)\b(?:cổng|gate|cửa)\s*[:：]?\s*([0-9A-Z]{1,4})\b""")
    private val SEAT_REGEX = Pattern.compile("""(?i)\b(?:ghế|seat)\s*[:：]?\s*([0-9]{1,2}[A-K])\b""")

    // --- Regex Mã nhận hàng / Lấy đồ ăn / Bưu kiện ---
    private val FOOD_APPS = listOf("shopeefood", "grabfood", "baemin", "be food", "gofood", "be")
    private val DELIVERY_APPS = listOf("spx", "shopee express", "ghtk", "ghn", "viettelpost", "j&t", "ninja van", "lazada", "tiki")

    private val PICKUP_CODE_REGEX = Pattern.compile(
        """(?i)(?:mã\s*(?:lấy\s*đồ|lấy\s*hàng|nhận\s*hàng|giao\s*hàng|đơn\s*hàng|pin|otp)|pickup\s*code|order\s*id|tracking)\s*[:：#-]?\s*([A-Z0-9-]{4,15})\b"""
    )
    private val SHORT_PIN_REGEX = Pattern.compile(
        """(?i)(?:mã|pin|code)\s*[:：]?\s*([0-9]{4,6})\b"""
    )
    private val PARCEL_CODE_REGEX = Pattern.compile(
        """\b(SPXVN[0-9A-Z]+|VN[0-9]{9}VN|[0-9]{10,14})\b""",
        Pattern.CASE_INSENSITIVE
    )

    // --- Regex Vé tàu / Vé xe / Sự kiện ---
    private val TRAIN_REGEX = Pattern.compile("""\b(SE[0-9]{1,2}|TN[0-9]{1,2}|SNT[0-9]|SPT[0-9])\b""", Pattern.CASE_INSENSITIVE)
    private val CINEMA_REGEX = Pattern.compile("""(?i)\b(CGV|Lotte Cinema|Beta Cinema|BHD Star|Cinestar|Galaxy Cinema)\b""")

    suspend fun extract(bitmap: Bitmap, sourceUri: Uri? = null): SmartInfo? {
        val hintText = sourceUri?.lastPathSegment.orEmpty()
        return if (hintText.isNotBlank()) parseSmartInfo(hintText, sourceUri) else null
    }

    fun analyzeText(rawText: String, uri: Uri? = null): SmartInfo? = parseSmartInfo(rawText, uri)

    private fun parseSmartInfo(rawText: String, uri: Uri?): SmartInfo? {
        val lower = rawText.lowercase()

        // 1. Phân tích Vé máy bay (Flight)
        val flightMatcher = FLIGHT_CODE_REGEX.matcher(rawText)
        if (flightMatcher.find()) {
            val flightNo = "${flightMatcher.group(1)?.uppercase().orEmpty()} ${flightMatcher.group(2).orEmpty()}"
            val airportMatcher = AIRPORT_CODE_REGEX.matcher(rawText)
            val airports = mutableListOf<String>()
            while (airportMatcher.find()) {
                val code = airportMatcher.group(1)?.uppercase().orEmpty()
                if (code.isNotBlank() && code !in airports) airports.add(code)
            }

            val route = if (airports.size >= 2) "${airports[0]} ✈ ${airports[1]}" else "Chuyến bay"
            val timeMatcher = TIME_REGEX.matcher(rawText)
            val time = if (timeMatcher.find()) timeMatcher.group() else null

            val gateMatcher = GATE_REGEX.matcher(rawText)
            val gate = if (gateMatcher.find()) gateMatcher.group(1) else null

            val seatMatcher = SEAT_REGEX.matcher(rawText)
            val seat = if (seatMatcher.find()) seatMatcher.group(1) else null

            val extras = listOfNotNull(
                time?.let { "Giờ: $it" },
                gate?.let { "Cửa: $it" },
                seat?.let { "Ghế: $it" }
            ).joinToString(" · ")

            return SmartInfo(
                type = SmartCardType.FLIGHT,
                title = "$flightNo · $route",
                code = flightNo,
                detail = if (extras.isNotBlank()) extras else "Vé máy bay $flightNo",
                subtitle = "Vé máy bay · $flightNo",
                timeString = time,
                sourceUri = uri
            )
        }

        // 2. Phân tích Mã lấy đồ ăn / Đơn hàng giao tới
        val isFood = FOOD_APPS.any { lower.contains(it) } || lower.contains("món") || lower.contains("quán")
        val isDelivery = DELIVERY_APPS.any { lower.contains(it) } || lower.contains("bưu kiện") || lower.contains("giao hàng")

        if (isFood || isDelivery) {
            val pickupMatcher = PICKUP_CODE_REGEX.matcher(rawText)
            var code = if (pickupMatcher.find()) pickupMatcher.group(1) else null

            if (code == null) {
                val pinMatcher = SHORT_PIN_REGEX.matcher(rawText)
                if (pinMatcher.find()) code = pinMatcher.group(1)
            }

            if (code == null && isDelivery) {
                val parcelMatcher = PARCEL_CODE_REGEX.matcher(rawText)
                if (parcelMatcher.find()) code = parcelMatcher.group(1)
            }

            if (code != null && code.length in 4..16) {
                val appBrand = when {
                    lower.contains("shopeefood") -> "ShopeeFood"
                    lower.contains("grab") -> "Grab"
                    lower.contains("baemin") -> "Baemin"
                    lower.contains("be food") || lower.contains(" be ") -> "Be Food"
                    lower.contains("spx") -> "SPX Express"
                    lower.contains("ghtk") -> "GHTK"
                    lower.contains("ghn") -> "GHN"
                    lower.contains("viettel") -> "Viettel Post"
                    else -> if (isFood) "Đồ ăn" else "Bưu kiện"
                }

                val title = if (isFood) "Mã lấy đồ: $code" else "Mã nhận hàng: $code"
                val detail = "$appBrand · Nhấn để sao chép mã"

                return SmartInfo(
                    type = SmartCardType.DELIVERY,
                    title = title,
                    code = code,
                    detail = detail,
                    subtitle = "$appBrand · $code",
                    sourceUri = uri
                )
            }
        }

        // 3. Phân tích Vé tàu hỏa / Vé xem phim / Sự kiện
        val trainMatcher = TRAIN_REGEX.matcher(rawText)
        if (trainMatcher.find()) {
            val trainNo = trainMatcher.group(1)?.uppercase().orEmpty()
            val timeMatcher = TIME_REGEX.matcher(rawText)
            val time = if (timeMatcher.find()) timeMatcher.group() else null
            return SmartInfo(
                type = SmartCardType.TICKET,
                title = "Tàu hỏa $trainNo",
                code = trainNo,
                detail = listOfNotNull("Đường sắt VN", time?.let { "Khởi hành $it" }).joinToString(" · "),
                subtitle = "Vé tàu $trainNo",
                timeString = time,
                sourceUri = uri
            )
        }

        val cinemaMatcher = CINEMA_REGEX.matcher(rawText)
        if (cinemaMatcher.find()) {
            val cinemaName = cinemaMatcher.group(1).orEmpty()
            val pinMatcher = SHORT_PIN_REGEX.matcher(rawText)
            val code = if (pinMatcher.find()) pinMatcher.group(1).orEmpty() else cinemaName
            return SmartInfo(
                type = SmartCardType.TICKET,
                title = "Vé xem phim $cinemaName",
                code = code,
                detail = "Mã vé: $code · Nhấn để sao chép",
                subtitle = "Vé xem phim · $cinemaName",
                sourceUri = uri
            )
        }

        return null
    }
}
