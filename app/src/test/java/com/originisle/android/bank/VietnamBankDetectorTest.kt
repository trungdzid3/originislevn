package com.originisle.android.bank

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class VietnamBankDetectorTest {

    // -------------------------------------------------------------
    // CHỐNG NHẬN DIỆN NHẦM TIN NHẮN QUẢNG CÁO / HẠN MỨC / SPAM (PHẢI TRẢ VỀ NULL)
    // -------------------------------------------------------------

    @Test
    fun ignoreMbBankVayTieuDungLenToi() {
        val result = VietnamBankDetector.parse(
            packageName = "com.mbmobile",
            title = "MBBank",
            text = "Ưu đãi gói vay tiêu dùng lên tới 70.000.000đ dành cho khách hàng thân thiết"
        )
        assertNull("Quảng cáo gói vay lên tới không được nhận diện thành giao dịch", result)
    }

    @Test
    fun ignoreHanMucChiTieuTheTinDungLenToi() {
        val result = VietnamBankDetector.parse(
            packageName = "com.mbmobile",
            title = "MBBank",
            text = "Nhận ngay hạn mức chi tiêu thẻ tín dụng lên tới 70.000.000đ"
        )
        assertNull("Hạn mức thẻ tín dụng lên tới không được nhận diện thành trừ tiền", result)
    }

    @Test
    fun ignoreZaloPayUuDaiToi() {
        val result = VietnamBankDetector.parse(
            packageName = "vn.com.vng.zalopay",
            title = "ZaloPay",
            text = "Ưu đãi tới 50.000đ khi thanh toán điện nước"
        )
        assertNull("Ưu đãi tới số tiền không được nhận diện thành giao dịch trừ tiền", result)
    }

    @Test
    fun ignoreMoMoQuaTangToi() {
        val result = VietnamBankDetector.parse(
            packageName = "com.mservice.momopay",
            title = "MoMo",
            text = "Quà tặng tới 50.000đ"
        )
        assertNull("Quà tặng tới không được nhận diện thành giao dịch", result)
    }

    @Test
    fun ignoreVpBankMoTheTinDungHoanTienLenDen() {
        val result = VietnamBankDetector.parse(
            packageName = "vn.com.vpbank.neo",
            title = "VPBank NEO",
            text = "Mở thẻ tín dụng nhận hoàn tiền lên đến 2.000.000đ ngay hôm nay"
        )
        assertNull("Quảng cáo mở thẻ hoàn tiền lên đến không được nhận diện", result)
    }

    @Test
    fun ignoreMbHanMucVayToiDa() {
        val result = VietnamBankDetector.parse(
            packageName = "com.mbmobile",
            title = "MB",
            text = "Hạn mức vay tiêu dùng tối đa 100.000.000 VND dành riêng cho bạn"
        )
        assertNull("Hạn mức vay tối đa không được nhận diện", result)
    }

    @Test
    fun ignoreTechcombankCoHoiNhanQuaTangToi() {
        val result = VietnamBankDetector.parse(
            packageName = "vn.com.techcombank.bb.app",
            title = "Techcombank",
            text = "Cơ hội nhận quà tặng tới 100.000đ khi mở tài khoản"
        )
        assertNull("Cơ hội quà tặng tới không được nhận diện", result)
    }

    @Test
    fun ignorePureBalanceWithoutTransaction() {
        val result = VietnamBankDetector.parse(
            packageName = "com.VCB",
            title = "Vietcombank",
            text = "Số dư tài khoản: 5.000.000 VND"
        )
        assertNull("Thông báo chỉ có số dư mà không có giao dịch phải trả về null", result)
    }

    // -------------------------------------------------------------
    // NHẬN DIỆN CHÍNH XÁC CÁC BIẾN ĐỘNG SỐ DƯ THẬT
    // -------------------------------------------------------------

    @Test
    fun detectMbRealIncome() {
        val result = VietnamBankDetector.parse(
            packageName = "com.mbmobile",
            title = "MB",
            text = "TK 1234 +50.000VND vào 12:00 ngày 01/01/2026. Số dư: 1.050.000VND. ND: NGUYEN VAN A chuyen tien"
        )
        assertNotNull("Giao dịch cộng tiền MB thật phải nhận diện được", result)
        assertEquals("MB Bank", result!!.bankName)
        assertTrue("isIncome phải là true cho giao dịch +", result.isIncome)
        assertTrue("formattedAmount phải chứa +50.000", result.formattedAmount.contains("+50.000"))
    }

    @Test
    fun detectZaloPayRealExpense() {
        val result = VietnamBankDetector.parse(
            packageName = "vn.com.vng.zalopay",
            title = "ZaloPay",
            text = "Thanh toán thành công 35.000đ cho Circle K"
        )
        assertNotNull("Giao dịch thanh toán ZaloPay phải nhận diện được", result)
        assertEquals("ZaloPay", result!!.bankName)
        assertFalse("isIncome phải là false cho giao dịch thanh toán", result.isIncome)
        assertTrue("formattedAmount phải chứa -35.000", result.formattedAmount.contains("-35.000"))
    }

    @Test
    fun detectVcbRealExpense() {
        val result = VietnamBankDetector.parse(
            packageName = "com.VCB",
            title = "VCB",
            text = "-150.000 VND lúc 10:30 ngày 01/01/2026. Số dư 2.000.000 VND. Ref: Mua hang"
        )
        assertNotNull("Giao dịch trừ tiền VCB phải nhận diện được", result)
        assertEquals("Vietcombank", result!!.bankName)
        assertFalse("isIncome phải là false cho giao dịch -", result.isIncome)
        assertTrue("formattedAmount phải chứa -150.000", result.formattedAmount.contains("-150.000"))
    }

    @Test
    fun detectVietcombankFullNotification() {
        val result = VietnamBankDetector.parse(
            packageName = "com.VCB",
            title = "Vietcombank",
            text = "Biến động số dư tài khoản 0123456789: -200.000 VND vào lúc 14:20"
        )
        assertNotNull("Biến động số dư Vietcombank phải nhận diện được", result)
        assertEquals("Vietcombank", result!!.bankName)
        assertFalse(result.isIncome)
        assertTrue(result.formattedAmount.contains("-200.000"))
    }

    @Test
    fun detectTechcombankIncome() {
        val result = VietnamBankDetector.parse(
            packageName = "vn.com.techcombank.bb.app",
            title = "Techcombank",
            text = "TK 1903... GD: +1.000.000VND lúc 09:30"
        )
        assertNotNull("Cộng tiền Techcombank phải nhận diện được", result)
        assertEquals("Techcombank", result!!.bankName)
        assertTrue(result.isIncome)
        assertTrue(result.formattedAmount.contains("+1.000.000"))
    }

    @Test
    fun detectMoMoRealIncome() {
        val result = VietnamBankDetector.parse(
            packageName = "com.mservice.momopay",
            title = "MoMo",
            text = "Bạn đã nhận được 100.000đ từ NGUYEN VAN A"
        )
        assertNotNull("Nhận tiền MoMo phải nhận diện được", result)
        assertEquals("MoMo", result!!.bankName)
        assertTrue(result.isIncome)
        assertTrue(result.formattedAmount.contains("+100.000"))
    }

    @Test
    fun detectMoMoRealExpense() {
        val result = VietnamBankDetector.parse(
            packageName = "com.mservice.momopay",
            title = "MoMo",
            text = "Chuyển tiền thành công: 50.000đ cho TRAN VAN B"
        )
        assertNotNull("Chuyển tiền MoMo phải nhận diện được", result)
        assertEquals("MoMo", result!!.bankName)
        assertFalse(result.isIncome)
        assertTrue(result.formattedAmount.contains("-50.000"))
    }

    @Test
    fun detectBankTransactionWithAppendedPromo() {
        // Một số ngân hàng đính kèm câu quảng cáo ở cuối tin nhắn giao dịch thật
        val result = VietnamBankDetector.parse(
            packageName = "com.VCB",
            title = "Vietcombank",
            text = "VCB: 0123456789 -50,000 VND lúc 10:00. Số dư 1,000,000 VND. Ưu đãi mở thẻ tín dụng hạn mức tới 50.000.000đ"
        )
        assertNotNull("Giao dịch thật có câu quảng cáo ở đuôi vẫn phải nhận diện đúng giao dịch chính", result)
        assertEquals("Vietcombank", result!!.bankName)
        assertFalse(result.isIncome)
        assertTrue("Số tiền giao dịch chính là 50.000 VND", result.formattedAmount.contains("-50.000") || result.formattedAmount.contains("-50,000"))
    }
}
