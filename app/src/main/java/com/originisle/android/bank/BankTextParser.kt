package com.originisle.android.bank

import java.text.Normalizer
import java.util.Locale

/**
 * Parser for Vietnamese bank transfer messages, chat snippets, and plain-text payment requests.
 */
object BankTextParser {

    private val LABELED_ACCOUNT_REGEX = Regex(
        """(?i)(?:\bstk\b|so\s*tk|so\s*tai\s*khoan|tai\s*khoan|\btk\b|account|acct|acc)\s*[:#\-]?\s*(\d{6,19})\b"""
    )

    private val STANDALONE_ACCOUNT_REGEX = Regex("""\b(\d{6,19})\b""")

    private val AMOUNT_TR_REGEX = Regex(
        """(?i)\b(\d+(?:[.,]\d+)?)\s*(?:tr|trieu|m)\s*(\d{1,3})?\b"""
    )

    private val AMOUNT_K_REGEX = Regex(
        """(?i)\b(\d+(?:[.,]\d+)?)\s*k\b"""
    )

    private val AMOUNT_VND_REGEX = Regex(
        """(?i)\b(\d{1,3}(?:[.,]\d{3})+|\d+)\s*(?:vnd|vndong|vn[dđ]|[dđ])\b"""
    )

    private val AMOUNT_GROUPED_REGEX = Regex(
        """\b(\d{1,3}(?:[.,]\d{3})+)\b"""
    )

    private val NOTE_LABEL_REGEX = Regex(
        """(?i)(?:\bndck\b|noi\s*dung\s*ck|noi\s*dung|\bnd\b|content|memo|note|loi\s*nhan)\s*[:\-]\s*(.+)"""
    )

    private val TRANSFER_SIGNAL_REGEX = Regex(
        """(?i)\b(?:stk|so\s*tk|so\s*tai\s*khoan|tai\s*khoan|chuyen\s*khoan|chuyen\s*tien|ck|bank|ngan\s*hang|vietqr|qr)\b"""
    )

    /**
     * Parses a Vietnamese bank transfer text into [ParsedBankTransfer], or returns null when
     * no valid account number (6-19 digits) or transfer signal is found.
     */
    fun parse(text: String): ParsedBankTransfer? {
        val rawTrimmed = text.trim()
        if (rawTrimmed.isEmpty()) return null

        val normalized = normalizeVietnamese(rawTrimmed)
        val bankName = detectBankName(normalized).orEmpty()

        val (note, noteRange) = extractLabeledNote(rawTrimmed, normalized)
        val amountMatch = extractAmount(normalized)
        val amount = amountMatch?.first.orEmpty()
        val amountRange = amountMatch?.second

        val maskedForAccount = buildMaskedText(normalized, amountRange, noteRange)
        val accountNumber = extractAccountNumber(maskedForAccount, normalized) ?: return null

        val hasSignal = bankName.isNotBlank() ||
            amount.isNotBlank() ||
            TRANSFER_SIGNAL_REGEX.containsMatchIn(normalized)
        if (!hasSignal) return null

        val resolvedNote = note.ifBlank {
            extractFallbackNote(rawTrimmed, normalized, bankName, accountNumber, amountRange)
        }

        return ParsedBankTransfer(
            bankName = bankName,
            bankBin = "",
            accountNumber = accountNumber,
            amount = amount,
            note = resolvedNote,
            rawQrPayload = "",
        )
    }

    private fun detectBankName(normalizedText: String): String? {
        for ((aliases, brandName) in VietnamBankDetector.BANK_BRANDS) {
            for (alias in aliases) {
                val normalizedAlias = normalizeVietnamese(alias)
                val regex = Regex("""(?i)(?:\b|^|_)${Regex.escape(normalizedAlias)}(?:\b|$|_)""")
                if (regex.containsMatchIn(normalizedText)) {
                    return brandName
                }
            }
        }
        return null
    }

    private fun extractAmount(normalizedText: String): Pair<String, IntRange>? {
        AMOUNT_TR_REGEX.find(normalizedText)?.let { match ->
            val majorStr = match.groupValues[1].replace(',', '.')
            val minorStr = match.groupValues[2]
            val value = if (minorStr.isNotEmpty() && !majorStr.contains('.')) {
                val major = majorStr.toLongOrNull() ?: return@let
                val minor = minorStr.toLongOrNull() ?: 0L
                val multiplier = when (minorStr.length) {
                    1 -> 100_000L
                    2 -> 10_000L
                    else -> 1_000L
                }
                major * 1_000_000L + minor * multiplier
            } else {
                val decimal = majorStr.toDoubleOrNull() ?: return@let
                Math.round(decimal * 1_000_000.0)
            }
            if (value > 0L) return value.toString() to match.range
        }

        AMOUNT_K_REGEX.find(normalizedText)?.let { match ->
            val num = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@let
            val value = Math.round(num * 1_000.0)
            if (value > 0L) return value.toString() to match.range
        }

        AMOUNT_VND_REGEX.find(normalizedText)?.let { match ->
            val digits = match.groupValues[1].replace(".", "").replace(",", "")
            if (digits.isNotEmpty() && (digits.toLongOrNull() ?: 0L) > 0L) {
                return digits to match.range
            }
        }

        AMOUNT_GROUPED_REGEX.find(normalizedText)?.let { match ->
            val digits = match.groupValues[1].replace(".", "").replace(",", "")
            if (digits.isNotEmpty() && (digits.toLongOrNull() ?: 0L) > 0L) {
                return digits to match.range
            }
        }

        return null
    }

    private fun extractLabeledNote(rawText: String, normalizedText: String): Pair<String, IntRange?> {
        val match = NOTE_LABEL_REGEX.find(normalizedText) ?: return "" to null
        val valueRange = match.groups[1]?.range ?: return "" to null
        val endIdx = rawText.indexOf('\n', startIndex = valueRange.first.coerceAtMost(rawText.length))
            .let { if (it == -1) rawText.length else it }
        val extracted = rawText.substring(
            valueRange.first.coerceAtMost(rawText.length),
            endIdx.coerceAtMost(rawText.length)
        ).trim().trimEnd(',', ';', '.')
        return extracted to (match.range.first until endIdx)
    }

    private fun buildMaskedText(
        normalizedText: String,
        amountRange: IntRange?,
        noteRange: IntRange?,
    ): String {
        val chars = normalizedText.toCharArray()
        maskRange(chars, amountRange)
        maskRange(chars, noteRange)
        return String(chars)
    }

    private fun maskRange(chars: CharArray, range: IntRange?) {
        if (range == null) return
        val start = range.first.coerceAtLeast(0)
        val end = range.last.coerceAtMost(chars.lastIndex)
        for (i in start..end) {
            chars[i] = ' '
        }
    }

    private fun extractAccountNumber(maskedText: String, fullNormalizedText: String): String? {
        LABELED_ACCOUNT_REGEX.find(maskedText)?.let { return it.groupValues[1] }
        LABELED_ACCOUNT_REGEX.find(fullNormalizedText)?.let { return it.groupValues[1] }

        for (match in STANDALONE_ACCOUNT_REGEX.findAll(maskedText)) {
            val start = match.range.first
            val end = match.range.last
            val prevChar = maskedText.getOrNull(start - 1)
            val nextChar = maskedText.getOrNull(end + 1)
            // Skip numbers attached to thousand-separator dots/commas (e.g. 250.000)
            if (prevChar == '.' || prevChar == ',' || nextChar == '.' || nextChar == ',') {
                continue
            }
            return match.groupValues[1]
        }
        return null
    }

    private fun extractFallbackNote(
        rawText: String,
        normalizedText: String,
        bankName: String,
        accountNumber: String,
        amountRange: IntRange?,
    ): String {
        val chars = rawText.toCharArray()
        maskRange(chars, amountRange)
        var remainder = String(chars)

        remainder = remainder.replace(accountNumber, " ")
        remainder = remainder.replace(
            Regex("""(?i)\b(?:stk|so\s*tk|số\s*tk|số\s*tài\s*khoản|tài\s*khoản|tk|chuyển\s*khoản|chuyen\s*khoan|ck|bank|ngân\s*hàng|ngan\s*hang)\b"""),
            " "
        )

        if (bankName.isNotBlank()) {
            val aliases = VietnamBankDetector.BANK_BRANDS
                .firstOrNull { it.second == bankName }
                ?.first
                .orEmpty()
            for (alias in aliases) {
                remainder = remainder.replace(
                    Regex("""(?i)(?:\b|^)${Regex.escape(alias)}(?:\b|$)"""),
                    " "
                )
            }
        }

        val cleaned = remainder
            .replace(Regex("""[\s\-:,;|]+"""), " ")
            .trim()
        return if (cleaned.length >= 3 && cleaned.any { it.isLetter() }) cleaned else ""
    }

    internal fun normalizeVietnamese(input: String): String {
        val replacedD = input.replace('đ', 'd').replace('Đ', 'D')
        val decomposed = Normalizer.normalize(replacedD, Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("""\p{InCombiningDiacriticalMarks}+"""), "")
            .lowercase(Locale.ROOT)
    }
}
