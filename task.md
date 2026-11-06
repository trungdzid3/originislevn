# KẾ HOẠCH TRIỂN KHAI SỬA LỖI YOUTUBE MUSIC & KIẾN TRÚC ĐA NHIỆM TRÊN ĐẢO ORIGIN ISLE

[CONTRACT LOCKED]
- Sửa lỗi YouTube Music nhận nhầm tải xuống:
  - Trong `DownloadDetector.kt`: Loại trừ triệt để các thông báo mang `NotificationCompat.EXTRA_MEDIA_SESSION`, `android.mediaSession`, `MediaStyle` hoặc thuộc các gói ứng dụng nghe nhạc (YouTube Music, Spotify, Zing MP3, Apple Music,...) nếu không có kênh/từ khóa tải xuống ngoại tuyến rõ ràng.
- Đa nhiệm trên Đảo Nguyên Tử (Multitasking & Multi-Scene Switching):
  - Phân tách Scene chuẩn xác theo danh mục SuperX được cấp phép:
    - Nhạc / Media: Dùng `SCENE = "MOVIE"` trong `MediaCard.kt`.
    - Tải xuống / Download: Dùng `SCENE = "DELIEVERY"` trong `DownloadCard.kt`.
    - Gọi xe & Giao đồ: Dùng `SCENE = "TAXI"` / `"TAKEOUT"` trong `RideCard.kt`.
    - Dẫn đường: Dùng `SCENE = "NAVIGATION"` trong `NavigationCard.kt`.
  - Cơ chế đa nhiệm hệ thống: Khi các tác vụ sở hữu Scene độc lập, Vivo SystemUI sẽ không bị xóa chéo mà tự động kích hoạt ngăn xếp đa nhiệm: hiển thị đảo chính kết hợp capsule đảo phụ nhỏ bên cạnh, đồng thời hỗ trợ cử chỉ lướt/vuốt chuyển đổi hoặc bung thẻ chuyển đổi đa nhiệm.

[SUBAGENT TASKS]
- [x] Task 1: Sửa triệt để lỗi YouTube Music bị nhận nhầm thành tải xuống trong `DownloadDetector.kt` | [Agent: Subagent-DownloadDetector] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/download/DownloadDetector.kt]
- [x] Task 2: Triển khai phân tách Scene độc lập (`MOVIE`, `DELIEVERY`, `TAXI`, `NAVIGATION`) và đa nhiệm trong `MediaCard.kt`, `DownloadCard.kt`, `PlaygroundService.kt` | [Agent: Subagent-MultiSceneArchitect] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/island/PlaygroundService.kt, app/src/main/java/com/originisle/android/cards/MediaCard.kt, app/src/main/java/com/originisle/android/cards/DownloadCard.kt]
- [x] Task 3: Biên dịch kiểm thử unit test & assembleDebug, xuất APK `OriginIsle-VN-v1.0.6-debug.apk`, cập nhật `PROJECT_CONTEXT.md` và đồng bộ Git | [Agent: Lead-Orchestrator] | [Skill: android-cli] | [Locked Files: PROJECT_CONTEXT.md, task.md]
