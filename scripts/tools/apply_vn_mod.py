import os
import re

BASE_DIR = r"C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn"

# 1. Create RideInfo.kt
ride_dir = os.path.join(BASE_DIR, "app", "src", "main", "java", "com", "originisle", "android", "ride")
os.makedirs(ride_dir, exist_ok=True)

ride_info_code = '''package com.originisle.android.ride

enum class RideType {
    TAXI,
    DELIVERY
}

data class RideInfo(
    val appName: String,
    val packageName: String,
    val title: String,
    val driverName: String? = null,
    val licensePlate: String? = null,
    val etaMinutes: Int? = null,
    val statusText: String,
    val type: RideType = RideType.TAXI
)
'''
with open(os.path.join(ride_dir, "RideInfo.kt"), "w", encoding="utf-8") as f:
    f.write(ride_info_code)
print("Created RideInfo.kt")

# 2. Create VietnamRideDetector.kt
detector_code = '''package com.originisle.android.ride

import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import java.util.regex.Pattern

object VietnamRideDetector {

    const val PKG_GRAB = "com.grabtaxi.passenger"
    const val PKG_BE = "xyz.be.customer"
    const val PKG_XANH_SM = "com.gsm.customer"
    const val PKG_SHOPEE = "com.shopee.vn"

    private val SUPPORTED_PACKAGES = setOf(PKG_GRAB, PKG_BE, PKG_XANH_SM, PKG_SHOPEE)

    // Regex bien so xe Viet Nam: 29A-123.45, 59X3-12345, 43B1-999.99
    private val PLATE_REGEX = Pattern.compile("(?i)\\\\b(\\\\d{2}[A-Z0-9]{1,2}[-. ]?\\\\d{3,5}(?:\\\\.\\\\d{2})?)\\\\b")

    // Regex thoi gian: 3 phut, 5 min
    private val ETA_REGEX = Pattern.compile("(?i)(\\\\d{1,2})\\\\s*(?:phút|min|mins)")

    fun isSupported(packageName: String): Boolean {
        return SUPPORTED_PACKAGES.contains(packageName)
    }

    fun parse(sbn: StatusBarNotification): RideInfo? {
        val pkg = sbn.packageName
        if (!isSupported(pkg)) return null

        val extras = sbn.notification.extras ?: return null
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString().orEmpty()

        val fullContent = "$title $text $bigText".trim()
        if (fullContent.isBlank()) return null

        val plateMatcher = PLATE_REGEX.matcher(fullContent)
        val licensePlate = if (plateMatcher.find()) plateMatcher.group(1)?.uppercase() else null

        val etaMatcher = ETA_REGEX.matcher(fullContent)
        val eta = if (etaMatcher.find()) etaMatcher.group(1)?.toIntOrNull() else null

        return when (pkg) {
            PKG_GRAB -> parseGrab(title, text, licensePlate, eta)
            PKG_BE -> parseBe(title, text, licensePlate, eta)
            PKG_XANH_SM -> parseXanhSM(title, text, licensePlate, eta)
            PKG_SHOPEE -> parseShopeeFood(title, text, eta)
            else -> null
        }
    }

    private fun parseGrab(title: String, text: String, plate: String?, eta: Int?): RideInfo {
        val isFood = title.contains("GrabFood", ignoreCase = true) || text.contains("món", ignoreCase = true)
        return RideInfo(
            appName = if (isFood) "GrabFood" else "Grab",
            packageName = PKG_GRAB,
            title = title.ifEmpty { "Chuyến đi Grab" },
            licensePlate = plate,
            etaMinutes = eta,
            statusText = text.ifEmpty { "Đang cập nhật lộ trình" },
            type = if (isFood) RideType.DELIVERY else RideType.TAXI
        )
    }

    private fun parseBe(title: String, text: String, plate: String?, eta: Int?): RideInfo {
        val isFood = title.contains("beFood", ignoreCase = true) || text.contains("đơn hàng", ignoreCase = true)
        return RideInfo(
            appName = if (isFood) "beFood" else "Be",
            packageName = PKG_BE,
            title = title.ifEmpty { "Chuyến đi Be" },
            licensePlate = plate,
            etaMinutes = eta,
            statusText = text.ifEmpty { "Tài xế đang di chuyển" },
            type = if (isFood) RideType.DELIVERY else RideType.TAXI
        )
    }

    private fun parseXanhSM(title: String, text: String, plate: String?, eta: Int?): RideInfo {
        return RideInfo(
            appName = "Xanh SM",
            packageName = PKG_XANH_SM,
            title = title.ifEmpty { "Taxi điện Xanh SM" },
            licensePlate = plate,
            etaMinutes = eta,
            statusText = text.ifEmpty { "Đang điều phối xe" },
            type = RideType.TAXI
        )
    }

    private fun parseShopeeFood(title: String, text: String, eta: Int?): RideInfo {
        return RideInfo(
            appName = "ShopeeFood",
            packageName = PKG_SHOPEE,
            title = title.ifEmpty { "ShopeeFood" },
            etaMinutes = eta,
            statusText = text.ifEmpty { "Đang giao đồ ăn" },
            type = RideType.DELIVERY
        )
    }
}
'''
with open(os.path.join(ride_dir, "VietnamRideDetector.kt"), "w", encoding="utf-8") as f:
    f.write(detector_code)
print("Created VietnamRideDetector.kt")

# 3. Create RideCard.kt
cards_dir = os.path.join(BASE_DIR, "app", "src", "main", "java", "com", "originisle", "android", "cards")
ride_card_code = '''package com.originisle.android.cards

import android.content.Context
import android.content.Intent
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.ride.RideInfo
import com.originisle.android.ride.RideType

object RideCard {

    fun post(context: Context, info: RideInfo, notificationId: Int) {
        val scene = if (info.type == RideType.TAXI) "TAXI" else "DELIEVERY"

        val capsuleContent = buildString {
            if (!info.licensePlate.isNullOrBlank()) {
                append(info.licensePlate)
            } else {
                append(info.appName)
            }
            if (info.etaMinutes != null) {
                append(" • ${info.etaMinutes}p")
            }
        }

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", notificationId)
            putExtra("oi_scene", scene)
            putExtra("title", info.title)
            putExtra("text", info.statusText)
            putExtra("source_pkg", info.packageName)
            putExtra("is_ongoing", true)

            // SuperX configuration
            putExtra("oi_left_content", info.appName)
            putExtra("oi_bg_color", "#FF1C1C1E")
            putExtra("oi_fg_color", "#FFFFFFFF")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_NAVIGATION)
            putExtra("status_chip_text", capsuleContent)
        }

        context.startService(intent)
    }
}
'''
with open(os.path.join(cards_dir, "RideCard.kt"), "w", encoding="utf-8") as f:
    f.write(ride_card_code)
print("Created RideCard.kt")

# 4. Patch NotificationCastListener.kt
listener_path = os.path.join(BASE_DIR, "app", "src", "main", "java", "com", "originisle", "android", "service", "NotificationCastListener.kt")
with open(listener_path, "r", encoding="utf-8") as f:
    listener_code = f.read()

# Add imports
imports_anchor = "import com.originisle.android.cards.PrivacyCard\n"
new_imports = "import com.originisle.android.cards.PrivacyCard\nimport com.originisle.android.cards.RideCard\nimport com.originisle.android.ride.VietnamRideDetector\n"
listener_code = listener_code.replace(imports_anchor, new_imports)

# Replace PAYMENT_APPS: remove foreign apps, add VN banks/wallets
old_payment_apps = """        private val PAYMENT_APPS = setOf(
            "com.google.android.apps.walletnfcrel",     // Google Wallet
            "com.google.android.apps.nbu.paisa.user",   // Google Pay
            "com.revolut.revolut",                      // Revolut
            "com.paypal.android.p2pmobile",             // PayPal
            "com.wise.android", "com.transferwise.android", // Wise
            "de.number26.android", "com.n26.n26",       // N26
            "com.monzo.emma",                           // Monzo
            "com.starlingbank.android",                 // Starling
            "fr.lydia.android",                         // Lydia
            "com.getcurve.mobile",                      // Curve
            "com.klarna.mobile",                        // Klarna
            "co.mona.android",                          // Crypto.com
        )"""

new_payment_apps = """        private val PAYMENT_APPS = setOf(
            "com.google.android.apps.walletnfcrel",     // Google Wallet
            "com.paypal.android.p2pmobile",             // PayPal
            // Ngan hang & Vi dien tu Viet Nam
            "com.VCB",                                  // Vietcombank
            "vn.com.techcombank.bb.app",                // Techcombank
            "com.mbmobile",                             // MB Bank
            "com.vnpay.bidv",                           // BIDV
            "com.mservice.momopay",                     // MoMo
            "vn.com.vng.zalopay",                       // ZaloPay
            "com.zing.zalo",                            // Zalo
            "vn.com.vpbank.neo",                        // VPBank NEO
            "com.acb.mobile",                           // ACB ONE
        )"""
listener_code = listener_code.replace(old_payment_apps, new_payment_apps)

# Update PAYMENT_WORDS: add VN words
old_payment_words = """        private val PAYMENT_WORDS = listOf(
            "paid", "payment", "you sent", "you received", "charged", "purchase",
            "transaction", "debited", "credited", "receipt", "spent", "approved",
            "bezahlt", "payé", "reçu", "pago", "pagamento", "betaling",
        )"""

new_payment_words = """        private val PAYMENT_WORDS = listOf(
            "paid", "payment", "you sent", "you received", "charged", "purchase",
            "transaction", "debited", "credited", "receipt", "spent", "approved",
            "thanh toán", "chuyển tiền", "chuyển khoản", "nhận được", "biến động số dư",
            "giao dịch", "số dư", "thẻ", "tiền vào", "tiền ra",
        )"""
listener_code = listener_code.replace(old_payment_words, new_payment_words)

# Update AMOUNT regex for VND
old_amount = """        private val AMOUNT = Regex(
            \"\"\"(?:[€$£¥]|\\b(?:EUR|USD|GBP|CHF)\\b)\\s?\\d[\\d.,]*|\\d[\\d.,]*\\s?(?:[€$£¥]|\\b(?:EUR|USD|GBP|CHF)\\b)\"\"\",
            RegexOption.IGNORE_CASE,
        )"""

new_amount = """        private val AMOUNT = Regex(
            \"\"\"(?:[€$£¥đ]|\\b(?:EUR|USD|GBP|CHF|VND|VNĐ)\\b)\\s?\\d[\\d.,]*|\\d[\\d.,]*\\s?(?:[€$£¥đ]|\\b(?:EUR|USD|GBP|CHF|VND|VNĐ)\\b)\"\"\",
            RegexOption.IGNORE_CASE,
        )"""
listener_code = listener_code.replace(old_amount, new_amount)

# Insert VietnamRideDetector in onNotificationPosted before detectPayment
detect_payment_anchor = """        // Payments (Wallet, Revolut, PayPal, banks, …) -> Apple-Pay-style success card."""
ride_call_code = """        // Vietnam Ride & Delivery (Grab, Be, Xanh SM, ShopeeFood)
        if (VietnamRideDetector.isSupported(sbn.packageName)) {
            val rideInfo = VietnamRideDetector.parse(sbn)
            if (rideInfo != null) {
                log(sbn, "cast — vietnam ride/delivery", true)
                RideCard.post(applicationContext, rideInfo, sbn.id)
                return
            }
        }

        // Payments (Wallet, Revolut, PayPal, banks, …) -> Apple-Pay-style success card."""
listener_code = listener_code.replace(detect_payment_anchor, ride_call_code)

with open(listener_path, "w", encoding="utf-8") as f:
    f.write(listener_code)
print("Patched NotificationCastListener.kt")

# 5. Patch GenericCard.kt styleFor: remove foreign apps, add VN ride styling
generic_path = os.path.join(cards_dir, "GenericCard.kt")
with open(generic_path, "r", encoding="utf-8") as f:
    generic_code = f.read()

old_generic_paisa = """            // Google Wallet (payments + passes) and standalone Google Pay.
            "com.google.android.apps.walletnfcrel",
            "com.google.android.apps.nbu.paisa.user",
            -> when {
                listOf("boarding", "flight", "gate", "depart", "terminal", "airline")
                    .any { t.contains(it) } -> CastStyle(base, chip = "✈")
                listOf("train", "platform", "rail", "track", "sncf", "coach", "wagon")
                    .any { t.contains(it) } -> CastStyle(base, chip = "🚆")
            }"""

new_generic_vn = """            // Google Wallet (payments + passes)
            "com.google.android.apps.walletnfcrel" -> when {
                listOf("boarding", "flight", "gate", "depart", "terminal", "airline")
                    .any { t.contains(it) } -> CastStyle(base, chip = "✈")
                listOf("train", "platform", "rail", "track", "sncf", "coach", "wagon")
                    .any { t.contains(it) } -> CastStyle(base, chip = "🚆")
                else -> null
            }
            // Xe cong nghe & Giao hang Viet Nam
            "com.grabtaxi.passenger" -> CastStyle(base, chip = if (t.contains("food") || t.contains("món")) "🍔" else "🚗")
            "xyz.be.customer" -> CastStyle(base, chip = if (t.contains("food") || t.contains("đơn")) "🍜" else "🛵")
            "com.gsm.customer" -> CastStyle(base, chip = "⚡")
            "com.shopee.vn" -> if (t.contains("food") || t.contains("giao") || t.contains("đơn")) CastStyle(base, chip = "📦") else null"""
generic_code = generic_code.replace(old_generic_paisa, new_generic_vn)

with open(generic_path, "w", encoding="utf-8") as f:
    f.write(generic_code)
print("Patched GenericCard.kt")

# 6. Vietnamese localization for HomeScreen.kt
home_path = os.path.join(BASE_DIR, "app", "src", "main", "java", "com", "originisle", "android", "ui", "HomeScreen.kt")
with open(home_path, "r", encoding="utf-8") as f:
    home_code = f.read()

home_translations = [
    ('"Check for updates"', '"Kiểm tra cập nhật"'),
    ('"Permissions"', '"Quyền ứng dụng"'),
    ('"Recast all"', '"Hiện lại tất cả"'),
    ('"Stop all"', '"Dừng tất cả"'),
    ('"Send to island"', '"Hiện trên đảo"'),
    ('ToggleRow(IsleIcons.Bell, IsleColors.TileBlue, "Notifications", castOn)', 'ToggleRow(IsleIcons.Bell, IsleColors.TileBlue, "Thông báo", castOn)'),
    ('ToggleRow(\n                        IsleIcons.Music, IsleColors.TileOrange, "Media", mediaOn,\n                        subtitle = "Mimic OriginPlayer for media apps",\n                    )', 'ToggleRow(\n                        IsleIcons.Music, IsleColors.TileOrange, "Âm nhạc", mediaOn,\n                        subtitle = "Trình phát nhạc trên đảo",\n                    )'),
    ('SectionLabel("Notifications")', 'SectionLabel("Tùy chọn thông báo")'),
    ('listOf("Live only", "All")', 'listOf("Chỉ đang diễn ra", "Tất cả")'),
    ('ToggleRow(IsleIcons.Earbuds, IsleColors.TilePurple, "Earbuds", earbudsOn, "Shows connection & battery level")', 'ToggleRow(IsleIcons.Earbuds, IsleColors.TilePurple, "Tai nghe", earbudsOn, "Hiện kết nối và mức pin tai nghe")'),
    ('ToggleRow(IsleIcons.Moon, IsleColors.TileIndigo, "Ignore silent", ignoreSilent, "Skip notifications delivered without sound")', 'ToggleRow(IsleIcons.Moon, IsleColors.TileIndigo, "Bỏ qua thông báo im lặng", ignoreSilent, "Không hiện thông báo đã tắt âm")'),
    ('ToggleRow(IsleIcons.Lock, IsleColors.TileGreen, "Lock screen", lockscreenLiveCard, "Show island card while screen is locked")', 'ToggleRow(IsleIcons.Lock, IsleColors.TileGreen, "Màn hình khóa", lockscreenLiveCard, "Hiện trên màn hình khóa")'),
    ('ToggleRow(IsleIcons.Bolt, IsleColors.TileGreen, "Charging", chargingOn, "Pill when plugged in, watt & full estimate")', 'ToggleRow(IsleIcons.Bolt, IsleColors.TileGreen, "Sạc pin", chargingOn, "Hiện công suất Watt và ước tính đầy pin")'),
]

for old, new in home_translations:
    home_code = home_code.replace(old, new)

with open(home_path, "w", encoding="utf-8") as f:
    f.write(home_code)
print("Patched HomeScreen.kt (Vietnamese localization)")

print("All modifications completed successfully!")
