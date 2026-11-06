# DANH SÁCH CÔNG VIỆC (TASK LIST) - ORIGIN ISLE VN v1.2.12

## Trạng thái: ĐÃ HOÀN TẤT BẢN VÁ v1.2.12 (100% UNIT TESTS & APK BUILD PASS)

- [x] Task 1: Tối ưu lại biểu tượng chế độ Rung (`createVibrateIcon`) trong `RingerModeCard.kt` — Xóa bỏ quả chuông nghiêng `-12f` và các nét zigzag sắc nhọn; thay bằng hình tượng điện thoại thông minh thẳng đứng cân đối ở trung tâm (`phoneRect = RectF(7.6f, 3.8f, 16.4f, 20.2f)`), rãnh loa thoại và vạch đáy tinh tế, hai bên là 2 cặp dải sóng rung hình cánh cung cong mượt mà đối xứng chuẩn Material Symbols / OriginOS, hiển thị sắc nét trên nền trong suốt không khung mờ | [Agent: Lead-Orchestrator] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/cards/RingerModeCard.kt]
- [x] Task 2: Nâng phiên bản lên `1.2.12` (`versionCode = 10212`), chạy kiểm thử `testDebugUnitTest assembleDebug`, xuất bản `OriginIsle-VN-v1.2.12.apk`, cập nhật `PROJECT_CONTEXT.md` và đồng bộ Git an toàn | [Agent: Lead-Orchestrator] | [Skill: android-cli] | [Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, OriginIsle-VN-v1.2.12.apk]
