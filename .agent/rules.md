---
auto_github_sync: true
---

# RULES CỤC BỘ - ORIGIN ISLE VIỆT NAM

Dự án đã kết nối GitHub (`origin https://github.com/trungdzid3/originislevn.git`).
Chế độ tự động hóa: Tự động kiểm tra bảo mật (Secret Guard), commit với thông điệp chuẩn mực và push code lên GitHub sau mỗi task hoàn thành.

## QUY CHUẨN ĐÁNH SỐ PHIÊN BẢN (VERSIONING LOGIC)
Định dạng bắt buộc: `X.Y.Z` (`ThếHệ.NângCấp.VáLỗi`):
- `X` (Số đầu - MAJOR): **Thế hệ hiện tại** (Thay đổi kiến trúc lớn, đại tu toàn diện).
- `Y` (Số tiếp theo - MINOR): **Số nâng cấp tính năng mới** nhưng vẫn chung một thế hệ.
- `Z` (Số tiếp nữa - PATCH): **Bản vá lỗi** (Bug fixes, tinh chỉnh, hotfix, sửa logic nhỏ lẻ).
- `versionCode` tăng tương ứng: `X * 10000 + Y * 100 + Z` (ví dụ `1.1.1` -> `10101`).
- Mỗi lần vá lỗi hoặc thêm tính năng, BẮT BUỘC tăng đúng vị trí số tương ứng, build và đổi tên APK thành phẩm ở thư mục gốc thành `OriginIsle-VN-vX.Y.Z.apk`, đồng thời dọn dẹp APK cũ.
