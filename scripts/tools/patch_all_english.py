# -*- coding: utf-8 -*-
import os, re

BASE_DIR = r"C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\app\src\main\java\com\originisle\android"

# ==========================================
# 1. Update VietnamRideDetector.kt
# ==========================================
ride_detector_path = os.path.join(BASE_DIR, "ride", "VietnamRideDetector.kt")
with open(ride_detector_path, "r", encoding="utf-8") as f:
    text = f.read()

# Make labels concise as requested: "Chuyến đi", "Giao đồ ăn", "Giao hàng" instead of "Grab", "ShopeeFood"...
text = text.replace('"Grab"', '"Chuyến đi"')
text = text.replace('"GrabFood"', '"Giao đồ ăn"')
text = text.replace('"beFood"', '"Giao đồ ăn"')
text = text.replace('"Be"', '"Chuyến đi"')
text = text.replace('"Xanh SM"', '"Chuyến đi"')
text = text.replace('"ShopeeFood"', '"Giao đồ ăn"')

with open(ride_detector_path, "w", encoding="utf-8") as f:
    f.write(text)
print("1. Updated VietnamRideDetector.kt")

# ==========================================
# 2. Update ChargingCard.kt
# ==========================================
charging_path = os.path.join(BASE_DIR, "cards", "ChargingCard.kt")
with open(charging_path, "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace('"$h h $m min"', '"$h giờ $m phút"')
text = text.replace('"$m min"', '"$m phút"')
text = text.replace('"Fully charged"', '"Đã sạc đầy"')
text = text.replace('"Charging"', '"Đang sạc pin"')
text = text.replace('"$level% · ${formatRemaining(remaining)} until full"', '"$level% · Còn ${formatRemaining(remaining)} đến khi đầy"')

with open(charging_path, "w", encoding="utf-8") as f:
    f.write(text)
print("2. Updated ChargingCard.kt")

# ==========================================
# 3. Update EarbudsCard.kt
# ==========================================
earbuds_path = os.path.join(BASE_DIR, "cards", "EarbudsCard.kt")
with open(earbuds_path, "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace('"Connected"', '"Đã kết nối"')
text = text.replace('"Battery low"', '"Pin yếu"')
text = text.replace('"Earbuds"', '"Tai nghe"')
text = text.replace('"Headphones"', '"Tai nghe"')

with open(earbuds_path, "w", encoding="utf-8") as f:
    f.write(text)
print("3. Updated EarbudsCard.kt")

# ==========================================
# 4. Update FlashlightCard.kt
# ==========================================
flashlight_path = os.path.join(BASE_DIR, "cards", "FlashlightCard.kt")
with open(flashlight_path, "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace('"Flashlight on"', '"Đèn pin đang bật"')
text = text.replace('"Tap to turn it off"', '"Chạm để tắt đèn pin"')
text = text.replace('"Turn off"', '"Tắt đèn pin"')

with open(flashlight_path, "w", encoding="utf-8") as f:
    f.write(text)
print("4. Updated FlashlightCard.kt")

# ==========================================
# 5. Update MediaCard.kt
# ==========================================
media_path = os.path.join(BASE_DIR, "cards", "MediaCard.kt")
with open(media_path, "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace('"Playing"', '"Đang phát"')
text = text.replace('"Paused"', '"Tạm dừng"')
text = text.replace('"Now playing"', '"Đang phát nhạc"')

with open(media_path, "w", encoding="utf-8") as f:
    f.write(text)
print("5. Updated MediaCard.kt")

# ==========================================
# 6. Update MessageCard.kt
# ==========================================
message_path = os.path.join(BASE_DIR, "cards", "MessageCard.kt")
with open(message_path, "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace('"New message"', '"Tin nhắn mới"')
text = text.replace('"New notification"', '"Thông báo mới"')
text = text.replace('"$appLabel · ${msg.count} new messages"', '"$appLabel · ${msg.count} tin nhắn mới"')

with open(message_path, "w", encoding="utf-8") as f:
    f.write(text)
print("6. Updated MessageCard.kt")

# ==========================================
# 7. Update OtpCard.kt
# ==========================================
otp_path = os.path.join(BASE_DIR, "cards", "OtpCard.kt")
with open(otp_path, "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace('"Login code · $appLabel"', '"Mã OTP · $appLabel"')
text = text.replace('"Copy"', '"Sao chép"')

with open(otp_path, "w", encoding="utf-8") as f:
    f.write(text)
print("7. Updated OtpCard.kt")

# ==========================================
# 8. Update PaymentCard.kt
# ==========================================
payment_path = os.path.join(BASE_DIR, "cards", "PaymentCard.kt")
with open(payment_path, "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace('"Processing…"', '"Đang xử lý…"')
text = text.replace('"Paid"', '"Thành công"')
text = text.replace('"Paid with $appLabel"', '"Thanh toán qua $appLabel"')
text = text.replace('"Done"', '"Hoàn tất"')

with open(payment_path, "w", encoding="utf-8") as f:
    f.write(text)
print("8. Updated PaymentCard.kt")

# ==========================================
# 9. Update PrivacyCard.kt
# ==========================================
privacy_path = os.path.join(BASE_DIR, "cards", "PrivacyCard.kt")
with open(privacy_path, "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace('"Camera in use"', '"Đang dùng Camera"')
text = text.replace('"Microphone in use"', '"Đang dùng Micro"')
text = text.replace('"Camera and microphone in use"', '"Đang dùng Camera & Micro"')
text = text.replace('"An app is using your camera"', '"Một ứng dụng đang sử dụng Camera"')
text = text.replace('"An app is using your microphone"', '"Một ứng dụng đang sử dụng Micro"')
text = text.replace('"An app is using your camera and microphone"', '"Một ứng dụng đang sử dụng Camera & Micro"')

with open(privacy_path, "w", encoding="utf-8") as f:
    f.write(text)
print("9. Updated PrivacyCard.kt")

# ==========================================
# 10. Update GenericCard.kt
# ==========================================
generic_path = os.path.join(BASE_DIR, "cards", "GenericCard.kt")
with open(generic_path, "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace('"Incoming call"', '"Cuộc gọi đến"')
text = text.replace('"Notification"', '"Thông báo"')
text = text.replace('"live"', '"trực tiếp"')
text = text.replace('"arriving in"', '"đến nơi sau"')

with open(generic_path, "w", encoding="utf-8") as f:
    f.write(text)
print("10. Updated GenericCard.kt")

# ==========================================
# 11. Update CopyCodeReceiver & TorchOffReceiver
# ==========================================
copy_path = os.path.join(BASE_DIR, "service", "CopyCodeReceiver.kt")
with open(copy_path, "r", encoding="utf-8") as f:
    text = f.read()
text = text.replace('"Code copied"', '"Đã sao chép mã OTP"')
text = text.replace('"Login code"', '"Mã xác thực"')
with open(copy_path, "w", encoding="utf-8") as f:
    f.write(text)

torch_path = os.path.join(BASE_DIR, "service", "TorchOffReceiver.kt")
with open(torch_path, "r", encoding="utf-8") as f:
    text = f.read()
text = text.replace('"Turn off"', '"Tắt đèn pin"')
with open(torch_path, "w", encoding="utf-8") as f:
    f.write(text)
print("11. Updated CopyCodeReceiver & TorchOffReceiver")

# ==========================================
# 12. Update NotificationCastListener.kt log strings
# ==========================================
listener_path = os.path.join(BASE_DIR, "service", "NotificationCastListener.kt")
with open(listener_path, "r", encoding="utf-8") as f:
    text = f.read()

notif_replacements = [
    ('"skipped — silent notification"', '"bỏ qua — thông báo im lặng"'),
    ('"skipped — quiet hours"', '"bỏ qua — giờ yên tĩnh"'),
    ('"skipped — app turned off in Apps tab"', '"bỏ qua — ứng dụng đã bị tắt"'),
    ('"skipped — duplicate delivery, unchanged"', '"bỏ qua — thông tin chưa thay đổi"'),
    ('"skipped — media, media casting off"', '"bỏ qua — trình phát nhạc đang tắt"'),
    ('"skipped — plain message (not a live card)"', '"bỏ qua — tin nhắn thông thường"'),
    ('"skipped — group summary"', '"bỏ qua — tóm tắt nhóm"'),
    ('"cast — vietnam ride/delivery"', '"hiển thị — chuyến đi / giao hàng"'),
    ('"cast — payment"', '"hiển thị — biến động số dư"'),
    ('"cast — login code"', '"hiển thị — mã OTP / đăng nhập"'),
    ('"cast — navigation"', '"hiển thị — điều hướng"'),
    ('"cast — media player"', '"hiển thị — trình phát nhạc"'),
    ('"cast — live score"', '"hiển thị — tỷ số thể thao"'),
    ('"cast — $kind"', '"hiển thị — $kind"'),
    ('"Running. Tap to open."', '"Đang hoạt động. Chạm để mở."'),
    ('"Switch off"', '"Tắt"'),
]

for old, new in notif_replacements:
    text = text.replace(old, new)

with open(listener_path, "w", encoding="utf-8") as f:
    f.write(text)
print("12. Updated NotificationCastListener.kt")

# ==========================================
# 13. Update UpdateChecker.kt
# ==========================================
update_path = os.path.join(BASE_DIR, "ui", "UpdateChecker.kt")
with open(update_path, "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace('"Couldn\'t check for updates"', '"Không thể kiểm tra bản cập nhật"')
text = text.replace('"Unknown installed version"', '"Không rõ phiên bản đã cài đặt"')
text = text.replace('"update check failed: ${e.message}"', '"Kiểm tra cập nhật thất bại: ${e.message}"')

with open(update_path, "w", encoding="utf-8") as f:
    f.write(text)
print("13. Updated UpdateChecker.kt")

# ==========================================
# 14. Update PlaygroundService.kt
# ==========================================
playground_path = os.path.join(BASE_DIR, "island", "PlaygroundService.kt")
with open(playground_path, "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace('"Origin Isle: Starting navigation"', '"Origin Isle: Bắt đầu điều hướng"')
text = text.replace('"Casting to OriginIsland"', '"Đang hiển thị trên Đảo Origin"')
text = text.replace('"Background keep-alive"', '"Duy trì chạy ngầm"')
text = text.replace('"Stop all cards"', '"Dừng tất cả thẻ"')

with open(playground_path, "w", encoding="utf-8") as f:
    f.write(text)
print("14. Updated PlaygroundService.kt")
