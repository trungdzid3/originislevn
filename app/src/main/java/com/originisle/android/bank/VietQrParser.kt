package com.originisle.android.bank

/**
 * Parsed bank transfer details extracted from a VietQR payload or Vietnamese transfer text.
 */
data class ParsedBankTransfer(
    val bankName: String,
    val bankBin: String = "",
    val accountNumber: String,
    val amount: String = "",
    val note: String = "",
    val rawQrPayload: String = "",
)

/**
 * Parser for EMVCo / NAPAS VietQR TLV payloads.
 */
object VietQrParser {

    private val BIN_TO_BANK_NAME = mapOf(
        "970436" to "Vietcombank",
        "970407" to "Techcombank",
        "970422" to "MB Bank",
        "970418" to "BIDV",
        "970415" to "VietinBank",
        "970405" to "Agribank",
        "970416" to "ACB",
        "970432" to "VPBank",
        "970423" to "TPBank",
        "970441" to "VIB",
        "970426" to "MSB",
        "970448" to "OCB",
        "970440" to "SeABank",
        "970431" to "Eximbank",
        "970428" to "Nam A Bank",
        "970412" to "PVcomBank",
        "970409" to "Bac A Bank",
        "970427" to "VietABank",
        "970438" to "BaoViet Bank",
        "970452" to "KienlongBank",
        "970449" to "LPBank",
        "970425" to "ABBank",
        "970433" to "VietBank",
        "970419" to "NCB",
        "970400" to "Saigonbank",
        "970430" to "PGBank",
        "970437" to "HDBank",
        "970403" to "Sacombank",
        "970443" to "SHB",
        "970424" to "Shinhan Bank",
        "970457" to "Woori Bank",
        "970458" to "UOB",
        "970410" to "Standard Chartered",
        "458761" to "HSBC",
        "422589" to "CIMB",
        "970454" to "BVBank (Timo)",
        "963388" to "Timo",
        "970462" to "KBank",
        "970463" to "Kookmin Bank",
        "970455" to "IBK",
        "970456" to "IBK HCM",
        "970406" to "Vikki (DongA Bank)",
        "970414" to "MBV",
        "970444" to "CBBank",
        "970408" to "GPBank",
        "970439" to "Public Bank",
        "970434" to "Indovina Bank",
        "970421" to "VRB",
        "970446" to "Co-opBank",
        "546034" to "Cake",
        "546035" to "Ubank",
        "971005" to "Viettel Money",
        "971011" to "VNPT Money",
        "971025" to "MoMo",
    )

    /**
     * Resolves a 6-digit NAPAS BIN to its Vietnam bank/e-wallet display name.
     */
    fun bankNameForBin(bin: String): String? {
        val cleanBin = bin.trim()
        if (cleanBin.isEmpty()) return null
        return BIN_TO_BANK_NAME[cleanBin]
    }

    /**
     * Parses an EMVCo / VietQR TLV payload into [ParsedBankTransfer], or returns null if invalid.
     */
    fun parse(payload: String): ParsedBankTransfer? {
        val trimmed = payload.trim()
        if (trimmed.length < 12) return null

        val rootTags = parseTlv(trimmed)
        if (rootTags.isEmpty()) return null

        // Tag 38 holds NAPAS VietQR merchant account info
        val tag38 = rootTags["38"] ?: return null
        val sub38 = parseTlv(tag38)
        if (sub38.isEmpty()) return null

        val (bankBin, accountNumber) = extractBankAndAccount(sub38)
        if (accountNumber.isBlank()) return null

        val rawAmount = rootTags["54"]?.trim().orEmpty()
        val amount = cleanAmount(rawAmount)

        val note = rootTags["62"]
            ?.let { parseTlv(it) }
            ?.let { sub62 -> sub62["08"] ?: sub62["01"] ?: sub62["05"] }
            ?.trim()
            .orEmpty()

        val bankName = bankNameForBin(bankBin)
            ?: if (bankBin.isNotBlank()) "Ngân hàng ($bankBin)" else "Ngân hàng"

        return ParsedBankTransfer(
            bankName = bankName,
            bankBin = bankBin,
            accountNumber = accountNumber,
            amount = amount,
            note = note,
            rawQrPayload = trimmed,
        )
    }

    private fun extractBankAndAccount(sub38: Map<String, String>): Pair<String, String> {
        val raw01 = sub38["01"]?.trim().orEmpty()
        if (raw01.isEmpty()) return "" to ""

        val nested = parseTlv(raw01)
        if (nested.containsKey("00") && nested.containsKey("01")) {
            val bin = nested["00"]?.trim().orEmpty()
            val account = nested["01"]?.trim().orEmpty()
            if (account.isNotEmpty()) return bin to account
        }

        // Fallback when sub-tag 01 is not nested TLV (length >= 6: 6-digit BIN + account)
        if (raw01.length > 6 && raw01.take(6).all { it.isDigit() }) {
            return raw01.take(6) to raw01.drop(6).trim()
        }

        val maybeBin = sub38["00"]?.trim().orEmpty()
        if (maybeBin.length == 6 && maybeBin.all { it.isDigit() }) {
            return maybeBin to raw01
        }

        return "" to raw01
    }

    private fun cleanAmount(raw: String): String {
        if (raw.isBlank()) return ""
        val sanitized = raw.replace(",", "").trim()
        return if (sanitized.matches(Regex("""^\d+(?:\.\d+)?$"""))) sanitized else ""
    }

    /**
     * Parses EMVCo TLV string (2-char Tag + 2-digit Length + Value) into an ordered map.
     */
    internal fun parseTlv(data: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        var index = 0
        while (index + 4 <= data.length) {
            val tag = data.substring(index, index + 2)
            val lengthStr = data.substring(index + 2, index + 4)
            val length = lengthStr.toIntOrNull() ?: return emptyMap()
            if (length < 0 || index + 4 + length > data.length) {
                return emptyMap()
            }
            val value = data.substring(index + 4, index + 4 + length)
            result[tag] = value
            index += 4 + length
        }
        return if (index == data.length) result else emptyMap()
    }
}
