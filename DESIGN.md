# HỢP ĐỒNG THIẾT KẾ ORIGIN DESIGN (SSOT) - ORIGIN ISLE VIỆT NAM
Chuẩn thiết kế: Vivo OriginOS 6 (Hệ điều hành Android 16 - Origin Design Specification)

---

## 1. TRIẾT LÝ THẨM MỸ KHÔNG GIAN (SPATIAL UI)
- **Tư tưởng cốt lõi**: Xóa nhòa ranh giới giữa giao diện số và thế giới vật lý thông qua ánh sáng, bóng đổ và chiều sâu quang học hữu cơ.
- **Chất liệu chủ đạo**: 
  - **Kính chất lỏng (Liquid Glass)**: Bề mặt khúc xạ bán trong suốt có độ sâu trường ảnh, bắt sáng tự nhiên từ viền cạnh.
  - **Động thái thấu thái (Dynamic Translucent Color)**: Ứng dụng nguyên lý tô màu ba lớp (Three-Layer Coloring) nhằm triệt tiêu hoàn toàn hiện tượng vẩn đục, bợt màu khi các lớp giao diện xếp chồng lên nhau dưới nền làm mờ Gauss.

---

## 2. BẢNG MÀU DỰ ÁN (PROJECT PALETTE)

### A. Màu Nền & Chiều Sâu (Obsidian Depth)
- **Background Primary**: `#0A0D14` (Không gian nền đen sâu thẳm hữu cơ)
- **Surface Dark Translucent**: `#161B26` (Độ trong suốt 85% - 92%)
- **Surface Card**: `#1C2230` (Lớp thẻ Atomic nổi)
- **Surface Capsule**: `#0D111A` (Lớp viên nang Đảo Nguyên Tử)

### B. Màu Nhấn Thương Hiệu (Accent & Glow)
- **Vivo Origin Electric Blue**: `#2B66FF` (Màu xanh điện lực đặc trưng OriginOS)
- **Glow Accent Light**: `#4D82FF` (Hào quang động phản chiếu mép kính)
- **Teal Cyan Mint (Chuyến đi / Hoàn tất)**: `#00D1A0`
- **Amber Orange (Đang tải / Chuẩn bị)**: `#FF9500`
- **Crimson Coral (Cảnh báo / Hủy)**: `#FF453A`

### C. Viền Phản Quang & Phân Tách (Hairline Borders)
- **Border Hairline Light**: `rgba(255, 255, 255, 0.12)` (Độ dày 0.8dp)
- **Border Focused Glow**: `rgba(43, 102, 255, 0.40)` (Viền sáng khi tương tác)
- **Divider Subtle**: `rgba(255, 255, 255, 0.06)`

---

## 3. HÌNH HỌC KHUNG VIỀN: ĐƯỜNG CONG G2 (G2-CONTINUOUS CURVATURE)
- **Nguyên lý**: Loại bỏ hoàn toàn điểm gãy góc quang học tại điểm tiếp giáp viền, mô phỏng cảm giác mềm mại của viên đá cuội (pebble-smooth) lấy cảm hứng từ cấu trúc vivo X Fold5.
- **Bán kính quy chuẩn**:
  - **Atomic Large Card**: Bo góc 28dp (chuẩn Squircle G2)
  - **Medium Card / Control Group**: Bo góc 20dp
  - **Ergonomic Capsule / Pill (Viên nang)**: Bo góc 999dp (Full Squircle Pill)
  - **Small Badge / Chip**: Bo góc 12dp

---

## 4. NGHỆ THUẬT KIỂU CHỮ (TYPOGRAPHY - VIVO SANS HEURISTICS)
- **Phông chữ mục tiêu**: vivo Sans (hoặc hệ thống Sans biến thiên có độ tương phản cao).
- **Hệ thống phân cấp hiển thị**:
  - **Tiêu đề phân mục**: 20sp - 22sp, Semibold, khoảng cách ký tự -0.4sp.
  - **Nhãn thẻ Atomic**: 15sp - 16sp, Medium.
  - **Nội dung phụ / Chú thích**: 12sp - 13sp, Regular, màu xám khói `rgba(255, 255, 255, 0.65)`.
  - **Số liệu / Tiến trình**: 13sp - 14sp, SemiBold / Monospace-width numbers.

---

## 5. ĐỘNG HỌC KHÔNG GIAN (ORIGIN ANIMATION SUITE)
- **Spring Animation (Đàn hồi)**: Áp dụng lò xo vật lý với độ nảy nhẹ có quán tính dựa trên vận tốc vuốt chạm thay vì dừng đột ngột (Damping Ratio: 0.78, Stiffness: 380).
- **Morphing Animation (Biến dạng chất lỏng)**: Viên nang mở rộng thành thẻ bảng điều khiển liên tục, không xuất hiện hiện tượng đứt gãy hình học.
- **Interruptible Animations**: Toàn bộ chu trình hoạt ảnh cho phép tương tác đảo chiều ngay lập tức mà không phải chờ hoàn tất hiệu ứng.

---

## 6. HỢP ĐỒNG GIAO DIỆN ĐẢO NGUYÊN TỬ (ATOMIC ISLAND SUPERX CONTRACT)
- **Viên nang bên phải (Capsule Right)**: Sử dụng `TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT` (Template 6) hiển thị phần trăm hoặc trạng thái ngắn gọn.
- **Bảng điều khiển mở rộng (Base Expanded)**: Sử dụng `TEMPLATE_BASE` (Template 4) kết hợp icon ngữ cảnh, tiêu đề rút gọn ("Gọi xe", "Chuyến đi", "Giao hàng", "Giao đồ ăn", "Tải xuống") và thanh tiến trình `oi_progress`.
- **Ràng buộc tương tác**: Mọi thẻ phát xuất từ Đảo bắt buộc có `click_resp` (PendingIntent hợp lệ) để đảm bảo không bị hệ thống OriginOS reject.
