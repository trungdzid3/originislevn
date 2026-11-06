# KẾ HOẠCH TRIỂN KHAI SỬA CÁC VẤN ĐỀ TRÊN ĐẢO ORIGIN ISLE

[CONTRACT LOCKED]
- Task 1: Loại trừ điều hướng / Google Maps khỏi DownloadDetector; siết chặt điều kiện progress bar.
- Task 2: Hỗ trợ 4 chế độ Chuông / Rung / Im lặng / Không làm phiền (DND) trong RingerModeCard.
- Task 3: Thay thế icon mảnh ghép Google Play Services bằng biểu tượng Quick Share chính thức trong QuickShareCard và IconCache.
- Task 4: Sửa lỗi trích xuất text "\n", tối ưu OtpDetector và ưu tiên OTP trong NotificationCastListener.
- Task 5: Loại bỏ triệt để tin nhắn rác/quảng cáo bị nhận nhầm thành ngân hàng với số tiền +0đ (bắt buộc số tiền > 0, phát hiện từ khóa quảng cáo/khuyến mãi/gói cước, phân biệt nhà mạng viễn thông).
- Task 6: Khắc phục hiện tượng Galaxy Fit 3 báo kết nối/ngắt kết nối ảo liên tục (chặn lặp từ thông báo nền ongoing của Samsung, lưu trạng thái kết nối để không spam pop-up); đổi huy hiệu ngắt kết nối sang dạng chữ ("Ngắt") đồng bộ với khi kết nối; khi kết nối ưu tiên hiển thị % pin trực tiếp trên viên con nhộng và vòng tròn camera; trang bị tính năng thông báo Đảo Origin khi thiết bị Samsung sạc đầy pin 100%.

[SUBAGENT TASKS]
- [x] Task 1: Sửa DownloadDetector loại trừ Maps, Navigation, Ride và siết chặt progress bar | [Agent: Subagent-DownloadNavFix] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/download/DownloadDetector.kt]
- [x] Task 2: Nâng cấp RingerModeCard bắt Chuông / Rung / Im lặng / Không làm phiền (DND) | [Agent: Subagent-RingerDndFix] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/cards/RingerModeCard.kt]
- [x] Task 3: Tạo biểu tượng Quick Share chuẩn và loại bỏ icon mảnh ghép Google Play Services | [Agent: Subagent-QuickShareIconFix] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/cards/QuickShareCard.kt, app/src/main/java/com/originisle/android/service/IconCache.kt]
- [x] Task 4: Sửa trích xuất text "\n", tối ưu OtpDetector và ưu tiên OTP trong NotificationCastListener | [Agent: Subagent-OtpAndListenerFix] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/otp/OtpDetector.kt, app/src/main/java/com/originisle/android/cards/OtpCard.kt, app/src/main/java/com/originisle/android/service/NotificationCastListener.kt]
- [x] Task 5: Loại bỏ tin nhắn rác quảng cáo 0đ và siết chặt lọc ngân hàng trong VietnamBankDetector | [Agent: Subagent-BankSpamFilter] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/bank/VietnamBankDetector.kt]
- [x] Task 6: Khắc phục thông báo ảo Galaxy Fit 3, đổi mất kết nối thành chữ "Ngắt", báo % pin khi kết nối, và thông báo sạc đầy pin | [Agent: Subagent-Fit3ConnectionFix] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/cards/EarbudsCard.kt, app/src/main/java/com/originisle/android/service/NotificationCastListener.kt]
- [x] Task 7: QA, biên dịch kiểm thử Gradle, xuất APK OriginIsle-VN-v1.0.6-debug.apk và đồng bộ Git | [Agent: Lead-Orchestrator] | [Skill: android-cli] | [Locked Files: PROJECT_CONTEXT.md, task.md]
