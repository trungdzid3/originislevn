package com.originisle.android.bank

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VietQrAndBankTextParserTest {

    // -------------------------------------------------------------
    // VietQrParser Tests
    // -------------------------------------------------------------

    @Test
    fun parseVietQrStandardWithAmountAndNote() {
        val payload = buildVietQr(
            bin = "970436",
            account = "1012345678",
            amount = "250000",
            note = "Thanh toan tien com"
        )

        val parsed = VietQrParser.parse(payload)
        assertNotNull(parsed)
        assertEquals("Vietcombank", parsed!!.bankName)
        assertEquals("970436", parsed.bankBin)
        assertEquals("1012345678", parsed.accountNumber)
        assertEquals("250000", parsed.amount)
        assertEquals("Thanh toan tien com", parsed.note)
        assertEquals(payload, parsed.rawQrPayload)
    }

    @Test
    fun parseVietQrStaticWithoutAmountOrNote() {
        val payload = buildVietQr(
            bin = "970422",
            account = "0987654321"
        )

        val parsed = VietQrParser.parse(payload)
        assertNotNull(parsed)
        assertEquals("MB Bank", parsed!!.bankName)
        assertEquals("970422", parsed.bankBin)
        assertEquals("0987654321", parsed.accountNumber)
        assertEquals("", parsed.amount)
        assertEquals("", parsed.note)
    }

    @Test
    fun parseVietQrWithDecimalAmountAndReferenceLabel() {
        val sub01 = tlv("00", "970407") + tlv("01", "19033445566011")
        val tag38 = tlv("00", "A000000727") + tlv("01", sub01) + tlv("02", "QRIBFTTA")
        val tag62 = tlv("05", "INV202601")
        val payload = tlv("00", "01") +
            tlv("01", "12") +
            tlv("38", tag38) +
            tlv("53", "704") +
            tlv("54", "150000.50") +
            tlv("58", "VN") +
            tlv("62", tag62)

        val parsed = VietQrParser.parse(payload)
        assertNotNull(parsed)
        assertEquals("Techcombank", parsed!!.bankName)
        assertEquals("970407", parsed.bankBin)
        assertEquals("19033445566011", parsed.accountNumber)
        assertEquals("150000.50", parsed.amount)
        assertEquals("INV202601", parsed.note)
    }

    @Test
    fun parseVietQrWithUnnestedSubTag01() {
        // Value of sub-tag 01 has length >= 6 without nested TLV (6-digit BIN + account number)
        val tag38 = tlv("00", "A000000727") + tlv("01", "970415113001234567")
        val payload = tlv("00", "01") + tlv("38", tag38) + tlv("53", "704") + tlv("58", "VN")

        val parsed = VietQrParser.parse(payload)
        assertNotNull(parsed)
        assertEquals("VietinBank", parsed!!.bankName)
        assertEquals("970415", parsed.bankBin)
        assertEquals("113001234567", parsed.accountNumber)
    }

    @Test
    fun parseVietQrInvalidPayloadsReturnNull() {
        assertNull(VietQrParser.parse(""))
        assertNull(VietQrParser.parse("https://example.com/pay"))
        assertNull(VietQrParser.parse("00020101021153037045802VN"))
        assertNull(VietQrParser.parse("0002013899invalid"))
    }

    @Test
    fun bankNameForBinCoversAllMajorAndDigitalBanks() {
        assertEquals("Vietcombank", VietQrParser.bankNameForBin("970436"))
        assertEquals("Techcombank", VietQrParser.bankNameForBin("970407"))
        assertEquals("MB Bank", VietQrParser.bankNameForBin("970422"))
        assertEquals("BIDV", VietQrParser.bankNameForBin("970418"))
        assertEquals("VietinBank", VietQrParser.bankNameForBin("970415"))
        assertEquals("Agribank", VietQrParser.bankNameForBin("970405"))
        assertEquals("ACB", VietQrParser.bankNameForBin("970416"))
        assertEquals("VPBank", VietQrParser.bankNameForBin("970432"))
        assertEquals("TPBank", VietQrParser.bankNameForBin("970423"))
        assertEquals("VIB", VietQrParser.bankNameForBin("970441"))
        assertEquals("MSB", VietQrParser.bankNameForBin("970426"))
        assertEquals("OCB", VietQrParser.bankNameForBin("970448"))
        assertEquals("SeABank", VietQrParser.bankNameForBin("970440"))
        assertEquals("Eximbank", VietQrParser.bankNameForBin("970431"))
        assertEquals("Nam A Bank", VietQrParser.bankNameForBin("970428"))
        assertEquals("PVcomBank", VietQrParser.bankNameForBin("970412"))
        assertEquals("Bac A Bank", VietQrParser.bankNameForBin("970409"))
        assertEquals("VietABank", VietQrParser.bankNameForBin("970427"))
        assertEquals("BaoViet Bank", VietQrParser.bankNameForBin("970438"))
        assertEquals("KienlongBank", VietQrParser.bankNameForBin("970452"))
        assertEquals("LPBank", VietQrParser.bankNameForBin("970449"))
        assertEquals("ABBank", VietQrParser.bankNameForBin("970425"))
        assertEquals("VietBank", VietQrParser.bankNameForBin("970433"))
        assertEquals("NCB", VietQrParser.bankNameForBin("970419"))
        assertEquals("Saigonbank", VietQrParser.bankNameForBin("970400"))
        assertEquals("PGBank", VietQrParser.bankNameForBin("970430"))
        assertEquals("HDBank", VietQrParser.bankNameForBin("970437"))
        assertEquals("Sacombank", VietQrParser.bankNameForBin("970403"))
        assertEquals("SHB", VietQrParser.bankNameForBin("970443"))
        assertEquals("Shinhan Bank", VietQrParser.bankNameForBin("970424"))
        assertEquals("Woori Bank", VietQrParser.bankNameForBin("970457"))
        assertEquals("UOB", VietQrParser.bankNameForBin("970458"))
        assertEquals("Standard Chartered", VietQrParser.bankNameForBin("970410"))
        assertEquals("HSBC", VietQrParser.bankNameForBin("458761"))
        assertEquals("CIMB", VietQrParser.bankNameForBin("422589"))
        assertEquals("BVBank (Timo)", VietQrParser.bankNameForBin("970454"))
        assertEquals("Viettel Money", VietQrParser.bankNameForBin("971005"))
        assertEquals("VNPT Money", VietQrParser.bankNameForBin("971011"))
        assertEquals("MoMo", VietQrParser.bankNameForBin("971025"))
        assertNull(VietQrParser.bankNameForBin("000000"))
    }

    // -------------------------------------------------------------
    // BankTextParser Tests
    // -------------------------------------------------------------

    @Test
    fun parseTextWithKAmountAndLabeledNote() {
        val text = "VCB 1012345678 250k ND: Tra tien an trua"
        val parsed = BankTextParser.parse(text)

        assertNotNull(parsed)
        assertEquals("Vietcombank", parsed!!.bankName)
        assertEquals("1012345678", parsed.accountNumber)
        assertEquals("250000", parsed.amount)
        assertEquals("Tra tien an trua", parsed.note)
    }

    @Test
    fun parseTextWithMillionShorthandAndAccentedLabels() {
        val text = "Ngân hàng Quân đội STK: 0912345678 chuyển 1tr5 Nội dung: Tiền nhà tháng 11"
        val parsed = BankTextParser.parse(text)

        assertNotNull(parsed)
        assertEquals("MB Bank", parsed!!.bankName)
        assertEquals("0912345678", parsed.accountNumber)
        assertEquals("1500000", parsed.amount)
        assertEquals("Tiền nhà tháng 11", parsed.note)
    }

    @Test
    fun parseTextWithDotAndCommaFormattedVndAmount() {
        val text1 = "Techcombank 19033445566011 - 500.000đ - Content: Mua do"
        val parsed1 = BankTextParser.parse(text1)
        assertNotNull(parsed1)
        assertEquals("Techcombank", parsed1!!.bankName)
        assertEquals("19033445566011", parsed1.accountNumber)
        assertEquals("500000", parsed1.amount)
        assertEquals("Mua do", parsed1.note)

        val text2 = "VIB STK 601704060123456 so tien 1,200,000 VND"
        val parsed2 = BankTextParser.parse(text2)
        assertNotNull(parsed2)
        assertEquals("VIB", parsed2!!.bankName)
        assertEquals("601704060123456", parsed2.accountNumber)
        assertEquals("1200000", parsed2.amount)
    }

    @Test
    fun parseTextDoesNotConfuseGroupedAmountWithAccountNumber() {
        val text = "ACB 250.000đ STK 98765432"
        val parsed = BankTextParser.parse(text)

        assertNotNull(parsed)
        assertEquals("ACB", parsed!!.bankName)
        assertEquals("98765432", parsed.accountNumber)
        assertEquals("250000", parsed.amount)
    }

    @Test
    fun parseTextWithoutBankNameButWithStkKeywordAndAmount() {
        val text = "STK: 0381000123456 300k"
        val parsed = BankTextParser.parse(text)

        assertNotNull(parsed)
        assertEquals("", parsed!!.bankName)
        assertEquals("0381000123456", parsed.accountNumber)
        assertEquals("300000", parsed.amount)
    }

    @Test
    fun parseTextRejectsPlainNumbersWithoutSignalOrInvalidLength() {
        assertNull(BankTextParser.parse("12345"))
        assertNull(BankTextParser.parse("Ma don hang 12345678"))
        assertNull(BankTextParser.parse("Vietcombank khuyen mai 500k"))
    }

    // -------------------------------------------------------------
    // VietnamBankDetector Expanded Coverage Tests
    // -------------------------------------------------------------

    @Test
    fun expandedBankPackagesContainNewBanksAndEwallets() {
        assertEquals("TPBank", VietnamBankDetector.BANK_PACKAGES["com.tpb.mb.gprsandroid"])
        assertEquals("VIB", VietnamBankDetector.BANK_PACKAGES["com.vib.myvib2"])
        assertEquals("MSB", VietnamBankDetector.BANK_PACKAGES["vn.com.msb.smartBanking"])
        assertEquals("OCB", VietnamBankDetector.BANK_PACKAGES["vn.com.ocb.omni"])
        assertEquals("Timo", VietnamBankDetector.BANK_PACKAGES["io.lifestyle.plus"])
        assertEquals("TNEX", VietnamBankDetector.BANK_PACKAGES["vn.tnex.consumer"])
        assertEquals("Liobank", VietnamBankDetector.BANK_PACKAGES["vn.io.liobank"])
        assertEquals("Vikki", VietnamBankDetector.BANK_PACKAGES["com.hdbank.vikki"])
        assertEquals("ShopeePay", VietnamBankDetector.BANK_PACKAGES["com.shopeepay.vn"])
        assertEquals("VNPT Money", VietnamBankDetector.BANK_PACKAGES["com.vnpt.vnptpay"])
    }

    private fun buildVietQr(
        bin: String,
        account: String,
        amount: String = "",
        note: String = "",
    ): String {
        val sub01 = tlv("00", bin) + tlv("01", account)
        val tag38 = tlv("00", "A000000727") + tlv("01", sub01) + tlv("02", "QRIBFTTA")
        val sb = StringBuilder()
        sb.append(tlv("00", "01"))
        sb.append(tlv("01", if (amount.isNotEmpty()) "12" else "11"))
        sb.append(tlv("38", tag38))
        sb.append(tlv("53", "704"))
        if (amount.isNotEmpty()) {
            sb.append(tlv("54", amount))
        }
        sb.append(tlv("58", "VN"))
        if (note.isNotEmpty()) {
            sb.append(tlv("62", tlv("08", note)))
        }
        return sb.toString()
    }

    private fun tlv(tag: String, value: String): String {
        val len = value.length.toString().padStart(2, '0')
        return "$tag$len$value"
    }
}
