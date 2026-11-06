# DANH SÁCH CÔNG VIỆC (TASK MATRIX) - BẢN VÁ LỖI v1.2.3

[CONTRACT LOCKED]
- Phiên bản mục tiêu: `v1.2.3` (`versionCode = 10203`, `versionName = "1.2.3"`).
- Khóa phạm vi file độc lập, cấm 2 subagent sửa chung file.

## Ma trận Phân quyền Thực thi

- [x] **Task 1: Sửa triệt để lỗi không bắt được mã OTP (MessagingStyle EXTRA_MESSAGES, Action Title, regex OTP) & Tối ưu UI hiển thị OTP bên trái, nút/chữ sao chép bên phải**
  - `[Agent: Subagent-OTP-Specialist]`
  - `[Skill: android-system-integration]`
  - `[Locked Files: app/src/main/java/com/originisle/android/otp/OtpDetector.kt, app/src/main/java/com/originisle/android/cards/OtpCard.kt]`

- [x] **Task 2: Cơ chế đọc % pin chuẩn cho Samsung Galaxy Fit 3 (Quét BluetoothDevice bondedDevices, sửa GATT API 33+ ByteArray, bóc tách sâu notification extras Samsung Wearable)**
  - `[Agent: Subagent-Wearable-Battery-Specialist]`
  - `[Skill: reverse-engineer-anything]`
  - `[Locked Files: app/src/main/java/com/originisle/android/cards/EarbudsCard.kt, app/src/main/java/com/originisle/android/service/NotificationCastListener.kt]`

- [x] **Task 3: Nâng cấp phiên bản v1.2.3, Build APK, Kiểm thử & Đồng bộ Git**
  - `[Agent: Lead-Orchestrator]`
  - `[Skill: pr-feedback-quality-gate]`
  - `[Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, task.md]`
