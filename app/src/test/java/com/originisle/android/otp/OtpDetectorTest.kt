package com.originisle.android.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpDetectorTest {

    @Test fun plainCode() = assertEquals("482913", OtpDetector.find("Your verification code is 482913. Don't share it."))
    @Test fun codeBeforeKeyword() = assertEquals("482913", OtpDetector.find("482913 is your login code"))
    @Test fun googlePrefix() = assertEquals("123456", OtpDetector.find("G-123456 is your Google verification code."))
    @Test fun splitWithSpace() = assertEquals("123456", OtpDetector.find("Use code 123 456 to sign in"))
    @Test fun splitWithDash() = assertEquals("123456", OtpDetector.find("Your WhatsApp code: 123-456"))
    @Test fun fourDigits() = assertEquals("4821", OtpDetector.find("Uber code: 4821"))
    @Test fun french() = assertEquals("5531", OtpDetector.find("Code de vérification : 5531"))
    @Test fun spanish() = assertEquals("774120", OtpDetector.find("Tu código es 774120"))
    @Test fun german() = assertEquals("90817", OtpDetector.find("Ihr Bestätigungscode lautet 90817"))
    @Test fun chinese() = assertEquals("605218", OtpDetector.find("您的验证码为605218，5分钟内有效"))
    @Test fun fptLongChau() = assertEquals(
        "532247",
        OtpDetector.find(
            "Nha Thuoc FPT Long Chau: Ma xac thuc OTP cua ban la 532247. Vui long lien he 18006928 de duoc h...",
            listOf("Xóa", "Đánh dấu đã đọc", "Sao chép mã")
        )
    )
    @Test fun fptLongChauNoActions() = assertEquals(
        "532247",
        OtpDetector.find(
            "Nha Thuoc FPT Long Chau: Ma xac thuc OTP cua ban la 532247. Vui long lien he 18006928 de duoc h..."
        )
    )

    // Bank-Style Fast OTP & Vietnamese SMS/Zalo tests
    @Test fun techcombankOtp() = assertEquals(
        "482913",
        OtpDetector.find("Techcombank: 482913 la ma OTP cua ban. Khong chia se ma nay voi bat ky ai.")
    )
    @Test fun vietcombankOtp() = assertEquals(
        "829103",
        OtpDetector.find("VCB: Ma xac thuc (OTP) cua Quy khach la 829103. Hieu luc trong 3 phut.")
    )
    @Test fun mbbankSmartOtp() = assertEquals(
        "918273",
        OtpDetector.find("MBBank: Ma xac thuc SmartOTP la 918273 cho giao dich chuyen tien 2,000,000 VND.")
    )
    @Test fun tpbankOtpWithHotlineAndAmount() = assertEquals(
        "618293",
        OtpDetector.find("TPBank: Ma bao mat OTP 618293. Giao dich chuyen tien 500k. Hotline 1900585885.")
    )
    @Test fun vpbankOneTimePassword() = assertEquals(
        "102938",
        OtpDetector.find("VPBank: Mat khau mot lan (OTP) la 102938 cho gd tai khoan 12345678.")
    )
    @Test fun zaloLoginOtp() = assertEquals(
        "392018",
        OtpDetector.find("Zalo: Ma xac minh dang nhap cua ban la 392018. Tuyet doi khong chia se ma nay cho nguoi khac.")
    )
    @Test fun shopeeOtp() = assertEquals(
        "719283",
        OtpDetector.find("Shopee: 719283 la ma xac nhan cua ban. Ma co hieu luc trong 15 phut.")
    )
    @Test fun vcbDigibankLogin() = assertEquals(
        "491823",
        OtpDetector.find("Vietcombank: Ma dang nhap VCB Digibank cua ban la 491823. Vui long khong cung cap cho bat ky ai.")
    )
    @Test fun otpStartingWithZero() = assertEquals(
        "049281",
        OtpDetector.find("BIDV: Ma OTP cua ban la 049281. Hieu luc trong 2 phut.")
    )
    @Test fun smsWithVnMobilePhone() = assertEquals(
        "582914",
        OtpDetector.find("Tu 0901234567: Ma xac thuc OTP la 582914.")
    )
    @Test fun smsWithAmountAndDong() = assertEquals(
        "381920",
        OtpDetector.find("Techcombank: Ma OTP 381920 xac nhan thanh toan 350000d qua the Visa.")
    )

    @Test fun noKeyword() = assertNull(OtpDetector.find("Meet me at 1830 near gate 4"))
    @Test fun chatNumber() = assertNull(OtpDetector.find("I paid 1500 for the tickets"))
    @Test fun price() = assertNull(OtpDetector.find("Confirm your payment of €1299"))
    @Test fun priceAfter() = assertNull(OtpDetector.find("Confirm your payment of 1299 €"))
    @Test fun decimal() = assertNull(OtpDetector.find("Confirm 1299.50 to verify"))
    @Test fun time() = assertNull(OtpDetector.find("Log in before 12:30"))
    @Test fun year() = assertNull(OtpDetector.find("Code valid until 2026"))
    @Test fun phone() = assertNull(OtpDetector.find("Verify by calling +33 612 345 678"))
    @Test fun keywordTooFar() = assertNull(
        OtpDetector.find("Your code was sent. " + "x".repeat(80) + " Order 88123 shipped"),
    )
}
