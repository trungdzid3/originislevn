package com.originisle.android.bank

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VietQrDeeplinkTest {

    @Test
    fun buildPayUrl_fromQrTransfer_encodesAppBankAccountAmountAndNote() {
        val transfer = ParsedBankTransfer(
            bankName = "Vietcombank",
            bankBin = "970436",
            accountNumber = "1029384756",
            amount = "150000",
            note = "Tra tien com",
        )

        val url = VietQrDeeplink.buildPayUrl(transfer, "com.mbmobile")
        assertNotNull(url)
        assertEquals(
            "https://dl.vietqr.io/pay?app=mb&ba=1029384756%40vcb&am=150000&tn=Tra%20tien%20com",
            url,
        )
    }

    @Test
    fun buildPayUrl_fromTextTransfer_resolvesBankNameWithoutBin() {
        val transfer = ParsedBankTransfer(
            bankName = "Techcombank",
            accountNumber = "19033445566",
            amount = "50.000",
            note = "Chuyen khoan",
        )

        val url = VietQrDeeplink.buildPayUrl(transfer, "com.vietinbank.ipay")
        assertEquals(
            "https://dl.vietqr.io/pay?app=icb&ba=19033445566%40tcb&am=50000&tn=Chuyen%20khoan",
            url,
        )
    }

    @Test
    fun buildPayUrl_returnsNull_whenPackageUnsupported() {
        val transfer = ParsedBankTransfer(
            bankName = "Vietcombank",
            bankBin = "970436",
            accountNumber = "1029384756",
        )

        assertNull(VietQrDeeplink.buildPayUrl(transfer, "com.unknown.wallet"))
    }
}
