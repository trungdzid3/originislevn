package com.originisle.android.tiktok

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TikTokStreakDetectorTest {

    @Test
    fun isTikTokPackage_recognizesSupportedAndVariantPackages() {
        for (pkg in TikTokStreakDetector.TIKTOK_PACKAGES) {
            assertTrue("Phải nhận diện package chính thức: $pkg", TikTokStreakDetector.isTikTokPackage(pkg))
        }
        assertTrue(TikTokStreakDetector.isTikTokPackage("COM.ZHILIAOAPP.MUSICALLY"))
        assertTrue(TikTokStreakDetector.isTikTokPackage("com.tiktok.lite.example"))
        assertTrue(TikTokStreakDetector.isTikTokPackage("com.ss.android.ugc.trill.vn"))

        assertFalse(TikTokStreakDetector.isTikTokPackage("com.zing.zalo"))
        assertFalse(TikTokStreakDetector.isTikTokPackage("com.facebook.orca"))
        assertFalse(TikTokStreakDetector.isTikTokPackage("com.android.chrome"))
        assertFalse(TikTokStreakDetector.isTikTokPackage(""))
    }

    @Test
    fun extractStreakDays_parsesVietnameseAndEnglishFormats() {
        assertEquals(15, TikTokStreakDetector.extractStreakDays("Chuỗi 15 ngày với Minh"))
        assertEquals(7, TikTokStreakDetector.extractStreakDays("Keep your 7 days streak going!"))
        assertEquals(1, TikTokStreakDetector.extractStreakDays("1 day left to keep streak"))
        assertEquals(42, TikTokStreakDetector.extractStreakDays("🔥 42 cùng Lan"))
        assertEquals(100, TikTokStreakDetector.extractStreakDays("100🔥 với Hùng"))
        assertEquals(30, TikTokStreakDetector.extractStreakDays("30 lửa đang cháy"))
        assertEquals(12, TikTokStreakDetector.extractStreakDays("Chuỗi 12 cùng bạn"))
        assertEquals(9, TikTokStreakDetector.extractStreakDays("Streak 9 with Alex"))
    }

    @Test
    fun extractStreakDays_returnsZeroWhenMissingOrNonPositive() {
        assertEquals(0, TikTokStreakDetector.extractStreakDays(""))
        assertEquals(0, TikTokStreakDetector.extractStreakDays("Gửi tin nhắn để giữ chuỗi hôm nay"))
        assertEquals(0, TikTokStreakDetector.extractStreakDays("0 ngày"))
    }

    @Test
    fun withFriendRegex_extractsFriendPhraseInVietnameseAndEnglish() {
        val vnMatch = TikTokStreakDetector.WITH_FRIEND_REGEX.find("Giữ chuỗi với Minh Anh!")
        assertNotNull(vnMatch)
        assertEquals("Minh Anh", vnMatch!!.groupValues[1].trim())

        val enMatch = TikTokStreakDetector.WITH_FRIEND_REGEX.find("Keep streak with Alex, hurry up")
        assertNotNull(enMatch)
        assertEquals("Alex", enMatch!!.groupValues[1].trim())

        val fromMatch = TikTokStreakDetector.WITH_FRIEND_REGEX.find("Tin nhắn từ Linh")
        assertNotNull(fromMatch)
        assertEquals("Linh", fromMatch!!.groupValues[1].trim())

        assertNull(TikTokStreakDetector.WITH_FRIEND_REGEX.find("Nhắc nhở chuỗi TikTok"))
    }

    @Test
    fun streakInfo_defaultsStreakDaysToZeroAndNullIntent() {
        val info = TikTokStreakDetector.TikTokStreakInfo(
            friendName = "Minh",
            message = "Đừng để mất chuỗi hôm nay!",
            sourcePkg = "com.ss.android.ugc.trill",
        )
        assertEquals(0, info.streakDays)
        assertNull(info.contentIntent)
        assertEquals("Minh", info.friendName)
    }
}
