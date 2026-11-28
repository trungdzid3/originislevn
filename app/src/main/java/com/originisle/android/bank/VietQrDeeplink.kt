package com.originisle.android.bank

import android.net.Uri
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

/**
 * Builds official VietQR Pay deeplinks (`https://dl.vietqr.io/pay`) so compatible
 * Vietnamese banking apps open directly on the transfer screen with pre-filled
 * account number, bank, amount, and note.
 */
object VietQrDeeplink {

    private val PACKAGE_TO_APP_ID = mapOf(
        "com.VCB" to "vcb",
        "vn.com.techcombank.bb.app" to "tcb",
        "com.mbmobile" to "mb",
        "com.vnpay.bidv" to "bidv",
        "com.vietinbank.ipay" to "icb",
        "com.vnpay.agribank" to "vba",
        "com.acb.mobile" to "acb",
        "vn.com.vpbank.neo" to "vpb",
        "vn.tpb.ebank" to "tpb",
        "com.tpb.mb.gprsandroid" to "tpb",
        "com.vn.vib.mobileapp" to "vib",
        "com.vib.myvib2" to "vib-2",
        "com.ocb.omniextra" to "ocb",
        "vn.com.ocb.omni" to "ocb",
        "vn.com.seabank.mb1" to "seab",
        "com.vnpay.eximbank" to "eib",
        "com.eximbank.edigi" to "eib",
        "com.vnpay.namabank" to "nab",
        "ops.namabank.openbanking" to "nab",
        "com.vnpay.pvcombank" to "pvcb",
        "com.vnpay.vietabank" to "vab",
        "com.baovietbank.smart" to "bvb",
        "com.kienlongbank.mbanking" to "klb",
        "com.lienviet24h.mobile" to "lpb",
        "com.vnpay.abbank" to "abb",
        "vn.abbank.abditizen" to "abb",
        "com.vnpay.vietbank" to "vab",
        "com.ncb.izimobile" to "ncb",
        "com.vnpay.sgbank" to "sgicb",
        "com.hdbank.mbanking" to "hdb",
        "com.sacombank.mbank" to "scb",
        "com.vnpay.shb" to "shb",
        "com.shinhan.global.vn.bank" to "shbvn",
        "com.wooribank.smart.vn" to "wvn",
        "com.cimb.octo.vn" to "cimb",
        "io.lifestyle.plus" to "timo",
        "com.vpb.cake" to "cake",
    )

    private val BIN_TO_BANK_CODE = mapOf(
        "970436" to "vcb",
        "970407" to "tcb",
        "970422" to "mb",
        "970418" to "bidv",
        "970415" to "icb",
        "970405" to "vba",
        "970416" to "acb",
        "970432" to "vpb",
        "970423" to "tpb",
        "970441" to "vib",
        "970426" to "msb",
        "970448" to "ocb",
        "970440" to "seab",
        "970431" to "eib",
        "970428" to "nab",
        "970412" to "pvcb",
        "970409" to "bab",
        "970427" to "vab",
        "970438" to "bvb",
        "970452" to "klb",
        "970449" to "lpb",
        "970425" to "abb",
        "970433" to "vietbank",
        "970419" to "ncb",
        "970400" to "sgicb",
        "970430" to "pgb",
        "970437" to "hdb",
        "970403" to "stb",
        "970443" to "shb",
        "970424" to "shbvn",
        "970457" to "wvn",
        "970458" to "uob",
        "970410" to "scvn",
        "458761" to "hsbc",
        "422589" to "cimb",
        "970454" to "timo",
        "963388" to "timo",
        "970414" to "mbv",
        "970439" to "pbvn",
        "970446" to "coopbank",
        "546034" to "cake",
        "546035" to "ubank",
    )

    private val NAME_KEYWORD_TO_BANK_CODE = listOf(
        "vietcombank" to "vcb",
        "vcb" to "vcb",
        "techcombank" to "tcb",
        "tcb" to "tcb",
        "mbbank" to "mb",
        "mb bank" to "mb",
        "bidv" to "bidv",
        "vietinbank" to "icb",
        "agribank" to "vba",
        "acb" to "acb",
        "vpbank" to "vpb",
        "tpbank" to "tpb",
        "vib" to "vib",
        "msb" to "msb",
        "ocb" to "ocb",
        "seabank" to "seab",
        "eximbank" to "eib",
        "nam a" to "nab",
        "pvcombank" to "pvcb",
        "bac a" to "bab",
        "vietabank" to "vab",
        "baoviet" to "bvb",
        "kienlong" to "klb",
        "lpbank" to "lpb",
        "lienviet" to "lpb",
        "abbank" to "abb",
        "ncb" to "ncb",
        "saigonbank" to "sgicb",
        "pgbank" to "pgb",
        "hdbank" to "hdb",
        "sacombank" to "stb",
        "shb" to "shb",
        "shinhan" to "shbvn",
        "woori" to "wvn",
        "cimb" to "cimb",
        "timo" to "timo",
        "cake" to "cake",
    )

    fun appIdForPackage(packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        return PACKAGE_TO_APP_ID[packageName]
    }

    fun bankCodeForTransfer(transfer: ParsedBankTransfer): String? {
        val cleanBin = transfer.bankBin.trim()
        if (cleanBin.isNotEmpty()) {
            return BIN_TO_BANK_CODE[cleanBin] ?: cleanBin
        }
        val normalizedName = normalize(transfer.bankName)
        if (normalizedName.isEmpty()) return null
        return NAME_KEYWORD_TO_BANK_CODE.firstOrNull { (keyword, _) ->
            normalizedName.contains(keyword)
        }?.second
    }

    fun buildPayUrl(transfer: ParsedBankTransfer, targetBankPkg: String?): String? {
        val account = transfer.accountNumber.trim()
        if (account.isEmpty()) return null
        val appId = appIdForPackage(targetBankPkg) ?: return null
        val bankCode = bankCodeForTransfer(transfer) ?: return null

        val params = mutableListOf(
            "app=" + encodeParam(appId),
            "ba=" + encodeParam("$account@$bankCode"),
        )
        val numericAmount = transfer.amount.replace(Regex("[^0-9]"), "")
        if (numericAmount.isNotEmpty()) {
            params.add("am=" + encodeParam(numericAmount))
        }
        val note = transfer.note.trim()
        if (note.isNotEmpty()) {
            params.add("tn=" + encodeParam(note))
        }
        return "https://dl.vietqr.io/pay?" + params.joinToString("&")
    }

    fun buildPayUri(transfer: ParsedBankTransfer, targetBankPkg: String?): Uri? {
        val url = buildPayUrl(transfer, targetBankPkg) ?: return null
        return Uri.parse(url)
    }

    private fun encodeParam(value: String): String {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }

    private fun normalize(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .lowercase(Locale.ROOT)
            .trim()
    }
}
