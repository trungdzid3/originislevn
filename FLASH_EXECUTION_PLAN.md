# KẾ HOẠCH THỰC THI DÀNH CHO FLASH (FLASH EXECUTION PLAN)
**Mục tiêu**: Khắc phục triệt để lỗi không hiển thị Đảo của **OTP**, **Tiến độ tải xuống**, **Nút thử nghiệm Demo**, tích hợp chuẩn **Quick Share**, và ngăn chặn sinh thông báo rác dưới khay thông báo — **KHÔNG dùng Developer Mode, KHÔNG dùng Quyền Trợ Năng**.

---

## I. CHẨN ĐOÁN KỸ THUẬT CHÍNH XÁC (TẠI SAO BẢN 0.0.1 CHẠY ĐƯỢC MÀ CÁC BẢN SAU LỖI)

Qua đối chiếu `git diff 3fe129d HEAD` (so sánh với bản gốc `2.2.1` / `0.0.1`), có **4 nguyên nhân kỹ thuật trực tiếp** làm Vivo OriginOS từ chối (reject) thẻ Đảo và biến chúng thành thông báo thường dưới khay:

1. **Lỗi 1 tại `PlaygroundService.kt` (Nguyên nhân làm hỏng Demo, OTP và Tải xuống)**:
   - Bản gốc `3fe129d` chỉ gọi `endOrigin(prevInScene, scene)` khi chuyển đổi giữa các thẻ mẫu (`if (id >= 40000)`), và **KHÔNG gọi `notificationManager.cancel` ngay lập tức**, cũng **KHÔNG có bộ xếp chồng `activeDeck`**.
   - Các bản sau đã xóa điều kiện `if (id >= 40000)` và thêm `activeDeck`, khiến mọi thẻ thực tế (`id` 20000–29999) hoặc thẻ Demo khi gửi lên đều gọi `endOrigin` (gửi `operation = 2` đóng Scene `NAVIGATION`) + `cancel()` rồi 0ms sau gọi `postSuperX` (`operation = 0` mở Scene `NAVIGATION`). Hệ điều hành Vivo xử lý bất đồng bộ nên lệnh đóng Scene dập tắt luôn thẻ mới vừa gửi và đẩy xuống thành thông báo thường dưới khay.
   - Ngoài ra, ở các bản gần đây đã gán `oi_show_notify = false` cho thẻ Scene `NAVIGATION` của gói `com.autonavi.minimap`. Trên Vivo OriginOS, chỉ gói hệ thống `com.vivo.musicwidgetmix` mới hỗ trợ `showNotify = false`; với `com.autonavi.minimap`, đặt `showNotify = false` khiến Vivo **tắt luôn không hiển thị lên Đảo**! Việc ẩn khỏi khay thông báo thực chất được quyết định bởi `oi_displays = DISPLAY_STATUSBAR or DISPLAY_AOD` (không có bit `DISPLAY_NOTIFICATION = 1`).

2. **Lỗi 2 tại `OtpCard.kt` (Nguyên nhân OTP biến thành thông báo mới dưới khay thay vì lên Đảo)**:
   - Ở bản gốc `3fe129d`: `putExtra("oi_left_content", "")` (để trống hoàn toàn chữ bên trái camera), `putExtra("oi_right_content", code)` (mã số viết liền không dấu cách), không ép `oi_fg_color`, và `showNotify` để mặc định `true`.
   - Ở bản sửa sau này: Truyền `putExtra("oi_left_content", appLabel)` (ví dụ `"Tin nhắn của Google"` dài 19 ký tự) làm tràn giới hạn ký tự bên trái viên thuốc của Vivo -> **Vivo SuperX reject thẻ (`SUPERX_VALUE = "false"`)** -> Hệ điều hành Vivo tước bỏ tag Đảo và hạ cấp thành 1 thông báo mới dưới khay thông báo!

3. **Lỗi 3 tại `DownloadCard.kt` & `GenericCard.kt` (Nguyên nhân Tải xuống bản 0.0.1 chạy được, các bản sau tịt)**:
   - Ở bản `0.0.1` (`3fe129d`): Thông báo tải xuống đi qua `GenericCard.kt`, dùng khung **`TEMPLATE_BASE` (4)** kết hợp vòng tròn tiến độ bên phải camera **`TEMPLATE_RIGHT_ISLAND_PROGRESS` (2)** (`oi_left_content = ""` hoặc ngắn gọn `<= 8` ký tự, `oi_right_content = "${progress}%"`).
   - Ở các bản sau: Tách sang `DownloadCard.kt` ép dùng `TEMPLATE_PROGRESS_VISUAL` (2) (mẫu thẻ có `nodeIcons` 1x1 transparent bitmap thiếu `BUNDLE_KEY_INFO_INDICATOR_ICON`), truyền `icon_res = R.drawable.ic_download` (Vector XML) và `oi_show_notify = false`, khiến Vivo SuperX từ chối hiển thị.

4. **Lỗi 4 tại `IslandSamples.kt` & `ActivityScreen.kt` (Nguyên nhân các nút Demo / Thử nghiệm không chạy)**:
   - Các hàm `start()` trong `IslandSamples.kt` và nút test trong `ActivityScreen.kt` đang truyền `oi_show_notify = false` hoặc dùng `TEMPLATE_PROGRESS_VISUAL` (2) / `icon_res` là Vector XML (`R.drawable.*`), dẫn đến bị Vivo reject.

---

## II. DANH SÁCH NHIỆM VỤ CỤ THỂ CHO FLASH (THEO THỨ TỰ)

### Nhiệm vụ 1: Khôi phục `PlaygroundService.kt` về đúng cơ chế nguyên bản của commit `3fe129d`
- **File khóa**: `app/src/main/java/com/originisle/android/island/PlaygroundService.kt`
- **Thao tác thực hiện**:
  1. Xóa bỏ hoàn toàn `CardSession`, `activeDeck`, `deckIndex`, `ACTION_CYCLE_DECK`, `cycleDeck()`, `isHighPriority()`, `isHighPrioritySession()`, và đoạn code tự động ghép nút `"[ ⟲ Đổi tác vụ ]"` vào `actions`.
  2. Trong `readAndPost(intent: Intent)`:
     - Giữ nguyên cách tính `displays`:
       ```kotlin
       val baseDisplays = intent.getIntExtra(
           "oi_displays",
           OriginIslandConstants.DISPLAY_STATUSBAR or OriginIslandConstants.DISPLAY_AOD,
       )
       val lockscreenOptIn = getSharedPreferences(PREFS_NAME, 0)
           .getBoolean("cast_lockscreen_live_card", false)
       val displays = (baseDisplays and OriginIslandConstants.DISPLAY_LOCKSCREEN.inv()) or
           (if (lockscreenOptIn) OriginIslandConstants.DISPLAY_LOCKSCREEN else 0)
       val showNotify = intent.getBooleanExtra("oi_show_notify", true)
       ```
     - Đảm bảo `baseIcon` luôn an toàn (không crash khi gặp Vector XML):
       ```kotlin
       val iconRes = intent.getIntExtra("icon_res", R.mipmap.ic_launcher_round)
       val baseIcon: Icon = IconCache.activeSmallIcons[id]
           ?: IconCache.safeResourceIcon(this, iconRes)
       ```
     - Khôi phục đúng khối kiểm tra `sceneCurrentId` của commit `3fe129d` (CHỈ `endOrigin` khi chuyển giữa các thẻ sample `id >= 40000`, tuyệt đối không gọi `notificationManager.cancel` ngay lập tức ở đây):
       ```kotlin
       if (id >= 40000) {
           val prevInScene = sceneCurrentId[scene]
           if (prevInScene != null && prevInScene != id && prevInScene >= 40000) {
               endOrigin(prevInScene, scene)
               activeIds.remove(prevInScene)
               originChangeRecord.remove(prevInScene)
               originScenes.remove(prevInScene)
           }
           sceneCurrentId[scene] = id
       }
       activeIds.add(id)
       originScenes[id] = scene
       activeCardIds = activeIds.toSet()
       postSuperX(id, iconRes, title, text, sourceApp, isOngoing, scene, bundle)
       ```
     - Trong `postSuperX(...)`: Luôn dùng `.setSmallIcon(R.mipmap.ic_launcher_round)` để tránh lỗi Vector XML trên Vivo.
     - Trong `cancelNotification(...)`: Khôi phục đúng logic của `3fe129d`:
       ```kotlin
       val scene = originScenes.remove(id) ?: intent.getStringExtra("oi_scene")
       if (scene != null) {
           endOrigin(id, scene)
           mainHandler.postDelayed({
               notificationManager.cancel(OriginIslandConstants.SUPERX_TAG, id)
           }, 350L)
       } else {
           notificationManager.cancel(id)
       }
       activeIds.remove(id)
       originChangeRecord.remove(id)
       activeCardIds = activeIds.toSet()
       ```

---

### Nhiệm vụ 2: Khôi phục `OtpCard.kt` về đúng chuẩn SuperX của commit `3fe129d` (có Việt hóa text)
- **File khóa**: `app/src/main/java/com/originisle/android/cards/OtpCard.kt`
- **Thao tác thực hiện**:
  1. Lấy `appIcon = IconCache.safeAppIcon(app, sbn.packageName)`.
  2. Tạo `copyAction` với icon an toàn `IconCache.safeResourceIcon(app, R.mipmap.ic_launcher_round)` và nhãn `"Sao chép"`.
  3. Cấu hình `Intent` gửi tới `PlaygroundService` giống hệt cấu trúc chuẩn của `3fe129d`:
     - `putExtra("id", id)`
     - `putExtra("oi_scene", "NAVIGATION")`
     - `putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)` (4)
     - `putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)` (6)
     - `putExtra("title", spaced(code))`
     - `putExtra("text", "Mã xác thực · $appLabel")`
     - `putExtra("source_app", appLabel)`
     - `putExtra("source_pkg", sbn.packageName)`
     - `putExtra("oi_left_content", "")` (**BẮT BUỘC ĐỂ CHUỖI RỖNG `""`** như bản gốc để bên trái chỉ hiện icon ứng dụng, không bị tràn viên thuốc làm Vivo reject!).
     - `putExtra("oi_right_content", code)` (viết liền `code` không dấu cách để vừa vặn trong capsule).
     - `putExtra("status_chip_text", code)`
     - `putExtra("icon_res", R.mipmap.ic_launcher_round)`
     - `appIcon?.let { putExtra("oi_left_icon", it); putExtra("oi_large_icon", it) }`
     - `putParcelableArrayListExtra("actions", arrayListOf(copyAction))`
     - `putExtra("source_content_intent", copy)`
     - `putExtra("oi_force_show", true)`
     - **TUYỆT ĐỐI KHÔNG** đặt `oi_show_notify = false`, **KHÔNG** đặt `oi_fg_color`.

---

### Nhiệm vụ 3: Đưa `DownloadCard.kt` và `QuickShareCard.kt` về khung `TEMPLATE_BASE` (4) + `TEMPLATE_RIGHT_ISLAND_PROGRESS` (2) như bản `0.0.1`
- **File khóa**:
  - `app/src/main/java/com/originisle/android/cards/DownloadCard.kt`
  - `app/src/main/java/com/originisle/android/cards/QuickShareCard.kt`
- **Thao tác thực hiện tại `DownloadCard.kt`**:
  1. Đổi `oi_template` từ `TEMPLATE_PROGRESS_VISUAL` (2) sang **`OriginIslandConstants.TEMPLATE_BASE` (4)** (cùng kèm `putExtra("oi_card_ring", info.progress.coerceIn(0, 100))` để hiện vòng tròn phần trăm trên thẻ mở rộng, và `oi_right_template = OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS` (2) để hiện vòng tròn tiến độ bên phải camera trên viên thuốc).
  2. Đặt `putExtra("oi_left_content", "")` (để bên trái camera chỉ hiện icon của Chrome / Play Store / Edge gọn gàng như bản 0.0.1, không bị tràn chữ).
  3. Đặt `putExtra("status_chip_text", "${info.progress}%")` và `putExtra("oi_right_content", "${info.progress}%")`.
  4. Đặt `putExtra("progress", info.progress.coerceIn(0, 100))` và `putExtra("show_progress", true)`, `putExtra("progress_max", 100)`.
  5. Đặt `putExtra("icon_res", R.mipmap.ic_launcher_round)`.
  6. **XÓA BỎ** `putExtra("oi_show_notify", false)` và `putExtra("oi_island_notify", true)` (để mặc định `showNotify = true` cùng `displays = STATUSBAR | AOD`).
- **Thao tác thực hiện tại `QuickShareCard.kt`**:
  1. **Trạng thái 1 (`info.isRequest == true` — Yêu cầu nhận tệp)**:
     - Nếu có `info.actions` (Từ chối / Chấp nhận): dùng `oi_template = OriginIslandConstants.TEMPLATE_BUTTONS` (8) khi `actions.size >= 2`, hoặc `TEMPLATE_BASE` (4) nếu có 1 action.
     - `oi_right_template = OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT` (6).
     - `oi_left_content = ""` (chỉ hiện icon Quick Share bên trái).
     - `oi_right_content = "Nhận tệp"`, `status_chip_text = "Nhận tệp"`.
     - `putParcelableArrayListExtra("actions", ArrayList(info.actions))`.
     - **XÓA BỎ** `putExtra("oi_show_notify", false)`.
  2. **Trạng thái 2 (`info.isRequest == false` — Đang truyền tệp)**:
     - Dùng `oi_template = OriginIslandConstants.TEMPLATE_BASE` (4) + `oi_card_ring = info.progress.coerceIn(0, 100)`.
     - `oi_right_template = OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS` (2).
     - `oi_left_content = ""`.
     - `status_chip_text = chipText`, `oi_right_content = chipText`.
     - `show_progress = true`, `progress = info.progress.coerceIn(0, 100)`, `progress_max = 100`.
     - **XÓA BỎ** `putExtra("oi_show_notify", false)`.

---

### Nhiệm vụ 4: Sửa toàn bộ nút Demo / Thử nghiệm trong `IslandSamples.kt` và `ActivityScreen.kt`
- **File khóa**:
  - `app/src/main/java/com/originisle/android/ui/samples/IslandSamples.kt`
  - `app/src/main/java/com/originisle/android/ui/ActivityScreen.kt`
- **Thao tác thực hiện**:
  1. Trong `IslandSamples.kt`:
     - Tại hàm `start(...)`: **XÓA BỎ** `putExtra("oi_show_notify", false)` (vì cờ này làm Vivo chặn hiển thị thẻ mẫu của `com.autonavi.minimap`). Giữ `putExtra("oi_force_show", true)` và `putExtra("oi_dismiss_when_kill", true)`.
     - Tại mẫu **"Mã OTP"**: Gọi cấu hình y hệt `OtpCard.kt` (`oi_template = TEMPLATE_BASE`, `oi_right_template = TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT`, `oi_left_content = ""`, `oi_right_content = "490603"`, `status_chip_text = "490603"`, `title = "490 603"`, `text = "Mã xác thực · FPT Long Châu"`).
     - Tại các mẫu **"Tải xuống"**, **"Hẹn giờ"**, **"Quick Share (Đang nhận)"**: Đổi `oi_template` từ `TEMPLATE_PROGRESS_VISUAL` (2) sang `OriginIslandConstants.TEMPLATE_BASE` (4) kèm `putExtra("oi_card_ring", progress)` và giữ `oi_right_template = OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS` (2).
     - Xóa mẫu `"Đa tác vụ Carousel"` trong `IslandSamples.kt` (vì đã bỏ `activeDeck`).
  2. Trong `ActivityScreen.kt`:
     - Kiểm tra 3 nút test nhanh ("Thử Số dư", "Thử OTP", "Thử Tải về"): Đảm bảo **KHÔNG** truyền `oi_show_notify = false`, nút "Thử OTP" dùng `TEMPLATE_BASE` + `TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT` với `oi_left_content = ""`, nút "Thử Tải về" dùng `TEMPLATE_BASE` + `TEMPLATE_RIGHT_ISLAND_PROGRESS` với `oi_left_content = ""`.

---

### Nhiệm vụ 5: Bảo toàn tuyệt đối `PaymentCard.kt` và Đóng gói APK `1.0.3`
- **Ràng buộc**: **KHÔNG** chạm vào bố cục của `PaymentCard.kt` (đang hoạt động đúng yêu cầu: bên trái tên ngân hàng, bên phải số tiền). Chỉ kiểm tra nếu trong `PaymentCard.kt` có `oi_show_notify = false` thì xóa dòng đó đi để thẻ ngân hàng luôn hiện lên Đảo.
- **Tăng version trong `app/build.gradle.kts`**:
  - `versionCode = 10003`
  - `versionName = "1.0.3"`
- **Biên dịch & Kiểm tra**:
  - Chạy `.\gradlew.bat assembleRelease`
  - Kiểm tra chữ ký v2 bằng `apksigner verify`
  - Copy file ra gốc: `Dao-Origin-1.0.3-VN.apk` (xóa APK cũ).
  - Cập nhật `PROJECT_CONTEXT.md`, `task.md` và commit Git.
