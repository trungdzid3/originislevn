# -*- coding: utf-8 -*-
import os

BASE_DIR = r"C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\app\src\main"

# 1. Update strings.xml
strings_xml_path = os.path.join(BASE_DIR, "res", "values", "strings.xml")
strings_xml_content = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Origin Isle</string>
    <string name="channel_name">Origin Island</string>
    <string name="channel_desc">Kênh thông báo trực tiếp của Origin Isle</string>
    <string name="keepalive_accessibility_desc">Giúp Origin Isle duy trì chạy ngầm để chuyển tiếp thông báo lên đảo linh hoạt — hoàn toàn không hiển thị biểu tượng trên thanh trạng thái. Ứng dụng không đọc nội dung màn hình, không lưu văn bản hay theo dõi bất kỳ thao tác nào của bạn.</string>
</resources>
"""
with open(strings_xml_path, "w", encoding="utf-8") as f:
    f.write(strings_xml_content)
print("1. Updated strings.xml")

# 2. Update OnboardingScreen.kt
onboarding_path = os.path.join(BASE_DIR, "java", "com", "originisle", "android", "ui", "OnboardingScreen.kt")
with open(onboarding_path, "r", encoding="utf-8") as f:
    text = f.read()

replacements_onboarding = [
    ('"Welcome to Origin Isle"', '"Chào mừng đến với Origin Isle"'),
    ('"A few permissions are needed before notifications can be cast to the island."', '"Cần cấp một số quyền để có thể chuyển tiếp thông báo lên đảo linh hoạt."'),
    ('"Notification access"', '"Truy cập thông báo"'),
    ('"Required — lets Origin Isle read notifications so it can re-cast them."', '"Bắt buộc — cho phép Origin Isle đọc thông báo để hiển thị lên đảo."'),
    ('"Allow notifications"', '"Cho phép thông báo"'),
    ('"Lets Origin Isle show its own status notification."', '"Cho phép hiển thị thông báo trạng thái của ứng dụng."'),
    ('"Keep-alive"', '"Duy trì chạy ngầm (Trợ năng)"'),
    ('"Reconnects casting when OriginOS restarts the app, instead of " +\n                        "waiting for you to open it."', '"Tự động kết nối lại khi OriginOS khởi động lại ứng dụng thay vì chờ bạn mở thủ công."'),
    ('"Reconnects casting when OriginOS restarts the app, instead of waiting for you to open it."', '"Tự động kết nối lại khi OriginOS khởi động lại ứng dụng thay vì chờ bạn mở thủ công."'),
    ('"Battery unrestricted"', '"Không hạn chế pin"'),
    ('"Stops OriginOS from killing the background caster to save power."', '"Ngăn OriginOS tự ý tắt ứng dụng chạy ngầm để tiết kiệm pin."'),
    ('"Autostart + Associated startup"', '"Tự khởi chạy & Khởi chạy liên kết"'),
    ('"Turn BOTH on. Without them, closing the app from recents kills " +\n                        "casting until you reboot."', '"Bật CẢ HAI mục. Nếu không, khi đóng app từ đa nhiệm đảo sẽ ngừng hoạt động cho đến khi khởi động lại máy."'),
    ('"Turn BOTH on. Without them, closing the app from recents kills casting until you reboot."', '"Bật CẢ HAI mục. Nếu không, khi đóng app từ đa nhiệm đảo sẽ ngừng hoạt động cho đến khi khởi động lại máy."'),
    ('if (notifGranted) "Continue" else "Grant notification access to continue"', 'if (notifGranted) "Tiếp tục" else "Vui lòng cấp quyền thông báo để tiếp tục"'),
    ('"Required"', '"Bắt buộc"'),
    ('"Open settings"', '"Mở cài đặt"'),
    ('"Granted"', '"Đã cấp"'),
]

for old, new in replacements_onboarding:
    text = text.replace(old, new)

with open(onboarding_path, "w", encoding="utf-8") as f:
    f.write(text)
print("2. Updated OnboardingScreen.kt")

# 3. Update PermissionsSheet.kt
permissions_path = os.path.join(BASE_DIR, "java", "com", "originisle", "android", "ui", "PermissionsSheet.kt")
with open(permissions_path, "r", encoding="utf-8") as f:
    text = f.read()

replacements_permissions = [
    ('"Permissions"', '"Quyền ứng dụng"'),
    ('"Notification access"', '"Truy cập thông báo"'),
    ('if (listenerOk) "Granted" else "Not granted — required"', 'if (listenerOk) "Đã cấp" else "Chưa cấp — bắt buộc"'),
    ('"Origin Isle notifications"', '"Thông báo Origin Isle"'),
    ('if (postGranted) "Allowed" else "Not allowed"', 'if (postGranted) "Đã cho phép" else "Chưa cho phép"'),
    ('"Battery"', '"Tối ưu hóa pin"'),
    ('if (batteryOk) "Unrestricted" else "Restricted — OriginOS may kill it"', 'if (batteryOk) "Không giới hạn" else "Bị giới hạn — OriginOS có thể đóng app"'),
    ('"Keep-alive"', '"Duy trì chạy ngầm (Trợ năng)"'),
    ('status = if (keepAliveOk) "On — reconnects casting after a kill" else "Off — slower to recover after a kill"', 'status = if (keepAliveOk) "Đã bật — tự phục hồi sau khi bị đóng" else "Đang tắt — khôi phục chậm hơn"'),
    ('"Enable \\"Origin Isle keep-alive\\" under Accessibility. No status-bar icon; it reads nothing."', '"Bật \\"Origin Isle keep-alive\\" trong mục Trợ năng. Không hiện icon trạng thái, không đọc dữ liệu cá nhân."'),
    ('"Autostart + Associated startup"', '"Tự khởi chạy & Khởi chạy liên kết"'),
    ('status = if (autoStartAck) "Opened — can\'t be verified, check it\'s still on" else "Not confirmed"', 'status = if (autoStartAck) "Đã mở cài đặt — hãy đảm bảo đã bật cả hai" else "Chưa xác nhận"'),
    ('"Turn BOTH on, or closing the app from recents kills casting until you reboot."', '"Bật CẢ HAI mục, nếu không khi vuốt đóng đa nhiệm đảo sẽ dừng hoạt động cho đến khi khởi động lại máy."'),
    ('"Redo first-run setup"', '"Thiết lập lại từ đầu"'),
    ('"Setup needs attention"', '"Cài đặt cần lưu ý"'),
    ('"Casting may not work"', '"Đảo có thể không hoạt động ổn định"'),
]

for old, new in replacements_permissions:
    text = text.replace(old, new)

with open(permissions_path, "w", encoding="utf-8") as f:
    f.write(text)
print("3. Updated PermissionsSheet.kt")

# 4. Update AppsScreen.kt
apps_path = os.path.join(BASE_DIR, "java", "com", "originisle", "android", "ui", "AppsScreen.kt")
with open(apps_path, "r", encoding="utf-8") as f:
    text = f.read()

replacements_apps = [
    ('ScreenHeader("Apps", onBack)', 'ScreenHeader("Ứng dụng", onBack)'),
    ('if (anyOff) "All on" else "All off"', 'if (anyOff) "Bật tất cả" else "Tắt tất cả"'),
    ('listOf("All", "On", "Off")', 'listOf("Tất cả", "Đang bật", "Đã tắt")'),
    ('"Turn OFF the apps you don\'t want cast. Plain chat texts are already filtered out, " +\n                        "so a messenger\'s calls still show even while its messages don\'t."', '"Tắt những ứng dụng bạn không muốn hiển thị trên đảo. Tin nhắn văn bản thông thường đã được lọc tự động, nên cuộc gọi vẫn sẽ hiển thị ngay cả khi tắt tin nhắn."'),
    ('if (query.isNotBlank()) "No apps match \\"$query\\"." else "No apps"', 'if (query.isNotBlank()) "Không có ứng dụng nào khớp với \\"$query\\"." else "Không có ứng dụng nào"'),
    ('"Search apps"', '"Tìm kiếm ứng dụng"'),
    ('"Clear search"', '"Xóa tìm kiếm"'),
]

for old, new in replacements_apps:
    text = text.replace(old, new)

with open(apps_path, "w", encoding="utf-8") as f:
    f.write(text)
print("4. Updated AppsScreen.kt")

# 5. Update ActivityScreen.kt
activity_path = os.path.join(BASE_DIR, "java", "com", "originisle", "android", "ui", "ActivityScreen.kt")
with open(activity_path, "r", encoding="utf-8") as f:
    text = f.read()

replacements_activity = [
    ('ScreenHeader("Activity", onBack)', 'ScreenHeader("Nhật ký hoạt động", onBack)'),
    ('"Clear log"', '"Xóa nhật ký"'),
    ('listOf("All", "Shown", "Skipped")', 'listOf("Tất cả", "Đã hiện", "Bỏ qua")'),
    ('"Nothing yet. Trigger a notification (or tap Recast all) and it\'ll show here " +\n                                "with whether it was cast or skipped, and why."', '"Chưa có hoạt động nào. Khi có thông báo mới (hoặc bấm \\"Hiện lại tất cả\\"), dữ liệu sẽ xuất hiện tại đây kèm lý do được phát hay bị bỏ qua."'),
    ('"Nothing here."', '"Không có dữ liệu."'),
    ('if (connected) "Listening" else "Not listening"', 'if (connected) "Đang hoạt động" else "Chưa kết nối"'),
    ('"Connected ${agoText(NotificationCastListener.connectedAt, now)}"', '"Đã kết nối ${agoText(NotificationCastListener.connectedAt, now)}"'),
    ('"Last event ${agoText(NotificationCastListener.lastEventAt, now)}"', '"Sự kiện gần nhất: ${agoText(NotificationCastListener.lastEventAt, now)}"'),
    ('"Reconnect"', '"Kết nối lại"'),
    ('if (e.cast) "Cast" else "Skipped"', 'if (e.cast) "Đã hiện" else "Bỏ qua"'),
    ('"Rebinding listener… give it a few seconds."', '"Đang kết nối lại bộ lắng nghe… vui lòng chờ vài giây."'),
    ('"Listener is already connected."', '"Bộ lắng nghe thông báo đã được kết nối."'),
    ('"Notification access isn\'t granted — turn it on in Settings first."', '"Chưa cấp quyền truy cập thông báo — vui lòng bật trong Cài đặt."'),
    ('"Couldn\'t rebind. Reboot, or toggle notification access off and on."', '"Không thể kết nối lại. Hãy khởi động lại máy hoặc tắt/bật lại quyền thông báo."'),
    ('"never"', '"chưa từng"'),
    ('"just now"', '"vừa xong"'),
    ('s < 60 -> "${s}s ago"', 's < 60 -> "${s} giây trước"'),
    ('s < 3600 -> "${s / 60}m ago"', 's < 3600 -> "${s / 60} phút trước"'),
    ('else -> "${s / 3600}h ago"', 'else -> "${s / 3600} giờ trước"'),
]

for old, new in replacements_activity:
    text = text.replace(old, new)

with open(activity_path, "w", encoding="utf-8") as f:
    f.write(text)
print("5. Updated ActivityScreen.kt")

# 6. Update HomeScreen.kt
home_path = os.path.join(BASE_DIR, "java", "com", "originisle", "android", "ui", "HomeScreen.kt")
with open(home_path, "r", encoding="utf-8") as f:
    text = f.read()

replacements_home = [
    ('if (connected) "Listening" else "Not listening"', 'if (connected) "Đang hoạt động" else "Chưa kết nối"'),
    ('"Checking…"', '"Đang kiểm tra…"'),
    ('"You\'re on the latest version (v${result.current})."', '"Bạn đang dùng phiên bản mới nhất (v${result.current})."'),
    ('"Update available: v${result.latest} — opening GitHub…"', '"Có bản cập nhật mới: v${result.latest} — Đang mở GitHub…"'),
    ('listOf("Live only", "Everything")', 'listOf("Chỉ thẻ động", "Tất cả")'),
    ('"Every notification, plain messages included."', '"Tất cả thông báo, bao gồm tin nhắn văn bản thông thường."'),
    ('"Only live cards — score, navigation, payment, etc."', '"Chỉ thẻ động trực tiếp — gọi xe, giao hàng, thanh toán, tỷ số, v.v."'),
    ('SettingRow(IsleIcons.Clock, IsleColors.TileTeal, "Hide after", subtitle = "Non live cards")', 'SettingRow(IsleIcons.Clock, IsleColors.TileTeal, "Tự ẩn sau", subtitle = "Dành cho thẻ thông báo thường")'),
    ('listOf(0 to "Never", 5 to "5s", 10 to "10s", 15 to "15s", 30 to "30s")', 'listOf(0 to "Không ẩn", 5 to "5 giây", 10 to "10 giây", 15 to "15 giây", 30 to "30 giây")'),
    ('ToggleRow(\n                        IsleIcons.Headphones, IsleColors.TilePurple, "Earbuds", earbudsOn,\n                        subtitle = "Battery when they connect",\n                    )', 'ToggleRow(\n                        IsleIcons.Headphones, IsleColors.TilePurple, "Tai nghe Bluetooth", earbudsOn,\n                        subtitle = "Hiện phần trăm pin khi kết nối",\n                    )'),
    ('ToggleRow(\n                        IsleIcons.Moon, IsleColors.TilePurple, "Skip silent", ignoreSilent,\n                        subtitle = "Ignore silenced notifications",\n                    )', 'ToggleRow(\n                        IsleIcons.Moon, IsleColors.TilePurple, "Bỏ qua thông báo im lặng", ignoreSilent,\n                        subtitle = "Không hiển thị thông báo không có chuông/rung",\n                    )'),
    ('ToggleRow(\n                        IsleIcons.Lock, IsleColors.TileGreen, "Lock screen", lockscreenLiveCard,\n                        subtitle = "Live card on lockscreen",\n                    )', 'ToggleRow(\n                        IsleIcons.Lock, IsleColors.TileGreen, "Màn hình khóa", lockscreenLiveCard,\n                        subtitle = "Hiển thị thẻ động trên màn hình khóa",\n                    )'),
    ('SectionLabel("Cards")', 'SectionLabel("Các loại thẻ trên đảo")'),
    ('ToggleRow(\n                        IsleIcons.Key, IsleColors.TileBlue, "Login codes", otpOn,\n                        subtitle = "Show the code, tap to copy",\n                    )', 'ToggleRow(\n                        IsleIcons.Key, IsleColors.TileBlue, "Mã OTP / Đăng nhập", otpOn,\n                        subtitle = "Hiển thị mã xác thực, chạm để sao chép",\n                    )'),
    ('ToggleRow(\n                        IsleIcons.Buttons, IsleColors.TileTeal, "Action buttons", actionsOn,\n                        subtitle = "The app\'s own buttons on the card",\n                    )', 'ToggleRow(\n                        IsleIcons.Buttons, IsleColors.TileTeal, "Nút hành động", actionsOn,\n                        subtitle = "Hiển thị các nút thao tác của thông báo trên thẻ",\n                    )'),
    ('ToggleRow(\n                        IsleIcons.Bolt, IsleColors.TileGreen, "Charging", chargingOn,\n                        subtitle = "Battery card when you plug in",\n                    )', 'ToggleRow(\n                        IsleIcons.Bolt, IsleColors.TileGreen, "Trạng thái sạc pin", chargingOn,\n                        subtitle = "Hiển thị thẻ pin khi cắm sạc",\n                    )'),
    ('listOf(false to "Briefly", true to "While charging")', 'listOf(false to "Thoáng qua", true to "Suốt thời gian sạc")'),
    ('ToggleRow(\n                        IsleIcons.Camera, IsleColors.TileAmber, "Camera & mic", privacyDotOn,\n                        subtitle = "Dot while they\'re in use",\n                    )', 'ToggleRow(\n                        IsleIcons.Camera, IsleColors.TileAmber, "Chỉ báo Camera & Mic", privacyDotOn,\n                        subtitle = "Chấm màu khi đang dùng quyền riêng tư",\n                    )'),
    ('SectionLabel("Quiet hours")', 'SectionLabel("Giờ yên tĩnh")'),
    ('ToggleRow(\n                        IsleIcons.Moon, IsleColors.TilePurple, "Follow Do Not Disturb", quietDnd,\n                        subtitle = "Quiet while DND is on",\n                    )', 'ToggleRow(\n                        IsleIcons.Moon, IsleColors.TilePurple, "Theo chế độ Không làm phiền", quietDnd,\n                        subtitle = "Tự động tắt đảo khi bật Không làm phiền",\n                    )'),
    ('ToggleRow(\n                        IsleIcons.Clock, IsleColors.TileGray, "Schedule", quietSchedule,\n                        subtitle = "${QuietHours.format(quietStart)} – ${QuietHours.format(quietEnd)} every day",\n                    )', 'ToggleRow(\n                        IsleIcons.Clock, IsleColors.TileGray, "Lịch đặt giờ", quietSchedule,\n                        subtitle = "${QuietHours.format(quietStart)} – ${QuietHours.format(quietEnd)} hàng ngày",\n                    )'),
    ('"From ${QuietHours.format(quietStart)}"', '"Từ ${QuietHours.format(quietStart)}"'),
    ('"To ${QuietHours.format(quietEnd)}"', '"Đến ${QuietHours.format(quietEnd)}"'),
    ('"While quiet, only calls, navigation, timers, login codes, media and charging show."', '"Trong giờ yên tĩnh, chỉ có cuộc gọi, điều hướng, hẹn giờ, mã OTP, âm nhạc và sạc pin mới được hiển thị."'),
    ('NavRow(\n                    IsleIcons.Grid, IsleColors.TileAmber, "Apps",\n                    value = if (appsOff == 0) "All on" else "$appsOff off",\n                    onClick = onOpenApps,\n                )', 'NavRow(\n                    IsleIcons.Grid, IsleColors.TileAmber, "Ứng dụng",\n                    value = if (appsOff == 0) "Đang bật tất cả" else "Đã tắt $appsOff",\n                    onClick = onOpenApps,\n                )'),
    ('NavRow(IsleIcons.Pulse, IsleColors.TileGray, "Activity", onClick = onOpenActivity)', 'NavRow(IsleIcons.Pulse, IsleColors.TileGray, "Nhật ký hoạt động", onClick = onOpenActivity)'),
    ('if (sentAt != 0L) "Sent" else "Hiện trên đảo"', 'if (sentAt != 0L) "Đã gửi" else "Thử trên đảo"'),
    ('"Notification access isn\'t granted — turn it on in Settings, then tap again."', '"Chưa cấp quyền truy cập thông báo — vui lòng bật trong Cài đặt rồi thử lại."'),
    ('"Listener not connected — reconnecting now. Give it a few seconds, then tap again."', '"Bộ lắng nghe chưa kết nối — đang kết nối lại. Vui lòng chờ vài giây rồi thử lại."'),
    ('"Recast swept $count notifications."', '"Đã phát lại $count thông báo lên đảo."'),
]

for old, new in replacements_home:
    text = text.replace(old, new)

with open(home_path, "w", encoding="utf-8") as f:
    f.write(text)
print("6. Updated HomeScreen.kt")

# 7. Update IslandSamples.kt
samples_path = os.path.join(BASE_DIR, "java", "com", "originisle", "android", "ui", "samples", "IslandSamples.kt")
with open(samples_path, "r", encoding="utf-8") as f:
    text = f.read()

replacements_samples = [
    ('"Football score", "Scoreboard · ARS 1 – 1 MAN · 65\' (generic badges)"', '"Tỷ số bóng đá", "Bảng tỷ số trực tiếp · ARS 1 – 1 MAN · 65\'"'),
    ('"App download", "Progress card + island ring · 75%"', '"Tải ứng dụng", "Thanh tiến trình + vòng tròn đảo · 75%"'),
    ('"Payment", "Apple-style: Processing… → Paid ✓ (green tick + glow)"', '"Biến động số dư", "Thông báo biến động số dư ngân hàng Việt Nam"'),
    ('"€42.00"', '"+500.000 đ"'),
    ('"Café de Flore"', '"Vietcombank"'),
    ('"Google Wallet"', '"VCB Digibank"'),
    ('"Food delivery", "TAKEOUT · base card + capsule ETA"', '"Giao đồ ăn (ShopeeFood / Grab)", "Trạng thái tài xế giao món đến nơi"'),
    ('"Delivery"', '"Giao hàng"'),
    ('"Courier on the way"', '"Tài xế đang giao hàng"'),
    ('"Your order arrives soon"', '"Đơn hàng sắp đến nơi"'),
    ('"12 min"', '"12 phút"'),
    ('"Ride hailing", "TAXI · driver arriving"', '"Gọi xe (Grab / Be / Xanh SM)", "Tài xế đang đến điểm đón"'),
    ('"Ride"', '"Chuyến đi"'),
    ('"Driver arriving"', '"Tài xế đang đến"'),
    ('"Toyota Prius · 白 · AB-123-CD"', '"VinFast VF8 · Trắng · 29A-888.88"'),
    ('"3 min"', '"3 phút"'),
    ('"Flight boarding", "FLIGHT · symmetric route card"', '"Chuyến bay", "Thẻ lộ trình bay đối xứng"'),
    ('"Boarding"', '"Đang lên máy bay"'),
    ('"CDG → FCO"', '"HAN → SGN"'),
    ('"Gate K21"', '"Cửa 12"'),
]

for old, new in replacements_samples:
    text = text.replace(old, new)

with open(samples_path, "w", encoding="utf-8") as f:
    f.write(text)
print("7. Updated IslandSamples.kt")
