# -*- coding: utf-8 -*-
path = r"C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\app\src\main\java\com\originisle\android\service\NotificationCastListener.kt"

with open(path, "r", encoding="utf-8") as f:
    text = f.read()

# Add imports
old_import = "import com.originisle.android.cards.RideCard"
new_import = "import com.originisle.android.cards.RideCard\nimport com.originisle.android.cards.DownloadCard\nimport com.originisle.android.download.DownloadDetector"
text = text.replace(old_import, new_import)

# Add Download hook before generic handling
target_hook = """        // Payments (Wallet, Revolut, PayPal, banks, …) -> Apple-Pay-style success card. This runs
        // BEFORE the "plain message" filter below, because a payment receipt isn't ongoing and has
        // no progress bar, so it would otherwise be dropped.
        detectPayment(sbn)?.let {
            if (lastCastPostTime.put(sbn.key, sbn.postTime) == sbn.postTime) {
                log(sbn, "bỏ qua — thông tin chưa thay đổi", false); return
            }
            castPayment(sbn, it); log(sbn, "hiển thị — biến động số dư", true); return
        }"""

replacement_hook = """        // Payments (Wallet, Revolut, PayPal, banks, …) -> Apple-Pay-style success card. This runs
        // BEFORE the "plain message" filter below, because a payment receipt isn't ongoing and has
        // no progress bar, so it would otherwise be dropped.
        detectPayment(sbn)?.let {
            if (lastCastPostTime.put(sbn.key, sbn.postTime) == sbn.postTime) {
                log(sbn, "bỏ qua — thông tin chưa thay đổi", false); return
            }
            castPayment(sbn, it); log(sbn, "hiển thị — biến động số dư", true); return
        }

        // Tải xuống ứng dụng & tệp tin (Google Play Store, Chrome, Download Manager, ...)
        if (DownloadDetector.isDownload(sbn)) {
            val dlInfo = DownloadDetector.parse(sbn)
            if (dlInfo != null) {
                log(sbn, "hiển thị — tiến độ tải xuống", true)
                DownloadCard.post(applicationContext, dlInfo, sbn)
                return
            }
        }"""

text = text.replace(target_hook, replacement_hook)

with open(path, "w", encoding="utf-8") as f:
    f.write(text)

print("Updated NotificationCastListener.kt with DownloadDetector hook")
