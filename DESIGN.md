# HỢP ĐỒNG THIẾT KẾ ORIGIN DESIGN (SSOT) - ORIGIN ISLE VIỆT NAM
Chuẩn thiết kế: Vivo OriginOS 6 (Hệ điều hành Android 16 - Origin Design Specification)

---

## 1. TRIẾT LÝ THẨM MỸ & KHÔNG GIAN (SPATIAL MINIMALIST UI)
- **Tư tưởng cốt lõi**: Tối giản hiện đại (Minimalist Modern) kết hợp chiều sâu không gian quang học của Vivo OriginOS 6, đồng bộ tuyệt đối với ngôn ngữ thiết kế gốc của máy.
- **Bề mặt & Chiều sâu**:
  - **Nền đen OLED tuyệt đối**: `#000000` cho Dark Mode, triệt tiêu viền đen quanh vết cắt camera đục lỗ (Pinhole punch-hole).
  - **Bầu không khí sáng thanh lịch**: `#F2F4F7` cho Light Mode, dịu mắt, độ tương phản vừa vặn.
  - **Bề mặt thẻ Atomic Squircle G2**: `#17181C` (Dark) / `#FFFFFF` (Light) bo góc cong liên tục G2 mượt mà 24dp.
  - **Viền Hairline tinh tế 0.8dp**: `rgba(255, 255, 255, 0.12)` (Dark) / `rgba(0, 0, 0, 0.06)` (Light) giúp phân định rõ ràng các khối thẻ mà không gây nặng nề thị giác.

---

## 2. BẢNG MÀU DỰ ÁN (PROJECT PALETTE)

### A. Màu Nền & Bề Mặt Thẻ (Surfaces & Backgrounds)
- **Background Primary**:
  - Dark Mode: `#000000` (OLED Pure Black)
  - Light Mode: `#F2F4F7` (OriginOS Soft Gray)
- **Surface (Thẻ nhóm GroupCard)**:
  - Dark Mode: `#17181C`
  - Light Mode: `#FFFFFF`
- **SurfaceHigh (Thẻ lồng nhau NestedSettingContainer / Thanh tìm kiếm)**:
  - Dark Mode: `#22242A`
  - Light Mode: `#E8EAEF`
- **SurfaceHigher (Tab kích hoạt / Chip lựa chọn)**:
  - Dark Mode: `#2C2E35`
  - Light Mode: `#DFE2E8`
- **Menu Dropdown Container**:
  - Dark Mode: `#1C1E24`
  - Light Mode: `#FFFFFF`

### B. Màu Nhấn Thương Hiệu (Accent & Origin Cyan Mint)
- **Vivo Origin Teal Cyan**: `#00BFA5` (Màu nhấn đặc trưng của OriginOS 6 cho nút bấm, switch bật và chỉ dẫn)
- **Glow Accent**: `#33DFCB` (Hào quang động mép kính khi phát sáng)
- **Accent Text**: `#00D2B8` (Dark) / `#00897B` (Light)
- **Hairline Border**: `0.8dp` siêu mảnh
- **Divider**: `#24262E` (Dark) / `#E5E7EB` (Light)

### C. Trạng Thái & Tiện Ích Chức Năng (Semantic Status)
- **Thành công (Good)**: `#00D084` (Dark) / `#00A86B` (Light)
- **Cảnh báo (Warn)**: `#FF9F2E` (Dark) / `#F57C00` (Light)
- **Lỗi / Nguy hiểm (Bad / Danger)**: `#FF5247` (Dark) / `#E53935` (Light)
- **Chấm bảo mật (Privacy Dot)**: Camera Đỏ `#FF3B30`, Micro Xanh lá `#34C759`

---

## 3. NGHỆ THUẬT KIỂU CHỮ HỆ THỐNG (VIVO SANS HEURISTICS)
- **Họ phông chữ hệ thống**: Ưu tiên nạp font hệ thống Vivo (`vivo-sans` / `sans-serif-medium`) trên các thiết bị Vivo/iQOO OriginOS, đồng thời tự động điều hướng tương thích chuẩn `FontFamily.SansSerif`.
- **Bảo toàn tỷ lệ hiển thị**: Tôn trọng 100% tỷ lệ co giãn phông chữ người dùng thiết lập (`fontScale`) và kích thước hiển thị màn hình hệ thống (`Display Size`).
- **Hệ thống phân cấp kiểu chữ (Typography)**:
  - **Tiêu đề màn hình (Screen Title)**: `26sp`, `FontWeight.Bold`, letter spacing `-0.5sp`.
  - **Nhãn phân nhóm (SectionLabel)**: `13sp`, `FontWeight.SemiBold`, màu `TextSecondary`, lề chuẩn `16dp`.
  - **Tiêu đề mục cài đặt (SettingRow Title)**: `16sp`, `FontWeight.SemiBold`, màu `Text`.
  - **Mô tả phụ (Subtitle)**: `13sp`, `FontWeight.Normal`, `lineHeight = 18sp`, màu `TextSecondary`.
  - **Nội dung Tab / Chip**: `13sp - 14sp`, `FontWeight.Medium / Bold`.

---

## 4. HÌNH HỌC KHUNG VIỀN & THÀNH PHẦN GIAO DIỆN CHUẨN ORIGIN

### A. Bán Kính Bo Góc (Squircle G2 Continuous Curvature)
- **Thẻ nhóm lớn (GroupCard)**: `24dp` (Squircle viền hairline 0.8dp).
- **Thẻ con lồng nhau (NestedSettingContainer)**: `16dp` (SurfaceHigh, lề thụt 16dp).
- **Nút bấm & Thanh tìm kiếm dạng viên nang (Pill Shape)**: `21dp - 26dp` (Full capsule).
- **Chip lựa chọn (ChoiceChip) & SegmentedControl**: `12dp - 14dp`.

### B. Thành Phần Cài Đặt Lồng Nhau (NestedSettingContainer)
- **Cơ chế**: Khi mục cài đặt cha được bật (ví dụ: *Pin & Trạng thái sạc*, *Lọc thông báo*, *Giờ yên tĩnh*), toàn bộ nhóm tùy chọn con sẽ mở ra mượt mà qua `AnimatedVisibility` (`expandVertically` + `fadeIn`).
- **Hình thức**: Nằm gọn trong card con `SurfaceHigh` bo góc `16dp`, có dải chỉ dẫn dọc mảnh (`3dp x 30dp`, bo góc `1.5dp`) mang sắc thái màu `Accent` ở mép trái, ngăn chặn hoàn toàn hiện tượng xô lệch bố cục chính.

### C. Công Tắc Đảo Tự Thiết Kế (OriginOS 6 Physical Switch)
- **Kích thước rãnh (Track)**: `48dp x 24dp`, bo góc `12dp` capsule.
- **Kích thước nút gạt (Thumb)**: Vòng tròn đường kính `32dp`, viền dày `4.8dp`, nhô vượt rãnh đều đặn trên/dưới `4dp`.
- **Khu vực chạm (Touch Target)**: Đảm bảo tối thiểu `48dp x 48dp` đạt chuẩn tiếp cận di động.

---

## 5. BỐ CỤC PHÂN TẦNG 5 NHÓM CHUẨN VIVO SETTINGS (HOMESCREEN ARCHITECTURE)

1. **Nhóm 1: Trạng thái & Dịch vụ cốt lõi**:
   - *Banner quyền thông minh*: Chỉ xuất hiện khi thiếu quyền hoặc dịch vụ bị OriginOS hạn chế pin/truy cập thông báo; tự ẩn hoàn toàn khi máy đã sẵn sàng.
   - *Công tắc Đảo tổng*: Bật/tắt toàn bộ dịch vụ kèm avatar đảo Origin và báo trạng thái kết nối.
   - *Giao diện ứng dụng & Đảo*: SegmentedControl 3 nấc ("Theo hệ thống", "Sáng", "Tối") tinh gọn, thay thế hoàn toàn mockup cồng kềnh cũ.
   - *Vị trí Đảo trên màn hình*: SegmentedControl 3 vị trí ("Bên trái", "Ở giữa", "Bên phải").

2. **Nhóm 2: Dịch vụ Trực tiếp & Tiện ích Việt Nam**:
   - Gọi xe & Giao đồ ăn Việt Nam (Grab, Be, Xanh SM, ShopeeFood).
   - Biến động số dư Ngân hàng (Vietcombank, MB, Techcombank, VPBank,...).
   - Mã xác thực OTP SMS (Tự động nhận diện và sao chép 1 chạm).
   - Tiến trình Tải xuống & Quick Share (Trình duyệt và tệp tải về).
   - Trạng thái Cuộc gọi & Đàm thoại trực tiếp trên Đảo.

3. **Nhóm 3: Tiện ích Hệ thống & Phần cứng**:
   - Trình phát nhạc Đảo gốc OriginOS 6 (Spotify, Zing MP3, YouTube Music).
   - Tai nghe & Thiết bị đeo (Samsung Galaxy Fit 3, Galaxy Watch, Buds... pin chi tiết).
   - Pin & Trạng thái sạc (kèm `NestedSettingContainer` chọn "Thoáng qua 5s" hoặc "Suốt thời gian sạc").
   - Chế độ Âm thanh Chuông / Rung / Im lặng & DND (phản hồi tức thì khi gạt nút âm thanh).
   - Đèn pin (thẻ tắt đèn pin trực tiếp nhanh từ Đảo).
   - Chỉ báo bảo mật Camera & Micro (chấm riêng tư đỏ / xanh lá).

4. **Nhóm 4: Quy tắc Hiển thị & Giờ yên tĩnh**:
   - Chế độ lọc thông báo: "Chỉ thẻ động trực tiếp" vs "Tất cả thông báo".
     - Khi chọn "Tất cả": Mở `NestedSettingContainer` chọn thời gian tự ẩn ("Không ẩn", "5s", "10s", "15s", "30s").
   - Bỏ qua thông báo im lặng.
   - Hiển thị trên màn hình khóa.
   - Theo chế độ Không làm phiền (DND).
   - Lịch đặt giờ yên tĩnh (kèm `NestedSettingContainer` chọn giờ Bắt đầu và giờ Kết thúc).
   - Nút thao tác trên thẻ.

5. **Nhóm 5: Quản lý & Trải nghiệm Đảo**:
   - Ứng dụng được phép (Hiển thị số ứng dụng đã tắt / bật).
   - Quyền hệ thống (Dẫn tới bảng kiểm soát quyền).
   - Nhật ký hoạt động (Xem lại lịch sử thông báo đã phát lên Đảo).
   - Kiểm tra cập nhật (Kiểm tra phiên bản mới từ GitHub Release).
   - Phòng thử nghiệm mẫu Đảo (`SampleTester`) và cụm nút "Hiện lại tất cả" / "Dừng tất cả".
