# DANH SÁCH CÔNG VIỆC (TASK MATRIX) - BẢN VÁ LỖI v1.2.1

[CONTRACT LOCKED]
- Phiên bản mục tiêu: `v1.2.1` (`versionCode = 10201`, `versionName = "1.2.1"`).
- Khóa phạm vi file độc lập, không để 2 subagent cùng sửa chung file.

## Ma trận Phân quyền Thực thi

- [x] **Task 1: Dập tắt triệt để báo ảo ngắt và kết nối đồng hồ/vòng đeo tay Fit 3**
  - `[Agent: Subagent-Wearable-AntiFlap]`
  - `[Skill: android-system-integration]`
  - `[Locked Files: app/src/main/java/com/originisle/android/cards/EarbudsCard.kt, app/src/main/java/com/originisle/android/service/NotificationCastListener.kt]`

- [x] **Task 2: Nâng cấp phiên bản v1.2.1, Build APK, Kiểm thử cú pháp & Đồng bộ Git**
  - `[Agent: Lead-Orchestrator]`
  - `[Skill: pr-feedback-quality-gate]`
  - `[Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, task.md]`
