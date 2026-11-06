# DANH SÁCH CÔNG VIỆC (TASK MATRIX) - BẢN NÂNG CẤP v1.2.5

[CONTRACT LOCKED]
- Phiên bản mục tiêu: `v1.2.5` (`versionCode = 10205`, `versionName = "1.2.5"`).
- Khóa phạm vi file độc lập, cấm race condition giữa các subagent.

## Ma trận Phân quyền Thực thi

- [x] **Task 1: Tối giản hóa thông báo ngắt kết nối tai nghe (chỉ hiện 'Đã ngắt', loại bỏ icon X đỏ)**
  - `[Agent: Subagent-Earbuds-Disconnect]`
  - `[Skill: android-cli]`
  - `[Locked Files: app/src/main/java/com/originisle/android/cards/EarbudsCard.kt]`

- [x] **Task 2: Khắc phục triệt để lỗi nhận diện tin nhắn quảng cáo/hạn mức vay (ZaloPay, MBBank...) thành giao dịch trừ tiền**
  - `[Agent: Subagent-Bank-Spam-Filter]`
  - `[Skill: android-cli]`
  - `[Locked Files: app/src/main/java/com/originisle/android/bank/VietnamBankDetector.kt, app/src/test/java/com/originisle/android/bank/VietnamBankDetectorTest.kt]`

- [x] **Task 3: Nâng cấp build.gradle.kts lên v1.2.5 (10205), Unit Test QA, build APK & Đồng bộ Git**
  - `[Agent: Lead-Orchestrator]`
  - `[Skill: pr-feedback-quality-gate]`
  - `[Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, task.md]`
