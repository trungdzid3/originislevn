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

## QUY CHUẨN ĐIỀU PHỐI THỐNG NHẤT (SUBAGENT & /SWARM)
- **Kế hoạch tổng quát & Thuật toán**: Dùng **Gemini 4 Carbon** (`gemini-4-carbon-medium`; hết quota fallback sang **Gemini 4 Neon** `gemini-4-neon-medium`).
- **Kế hoạch chi tiết**: Dùng **Gemini 4 Argon** (`gemini-4-argon-medium`; hết quota fallback sang **Gemini 4 Neon** `gemini-4-neon-medium`).
- **Mức 1 (`DIRECT`)**: 1 file hoặc hotfix dưới 30 dòng -> sửa trực tiếp.
- **Mức 2 (`Subagent thường`)**: 2–3 file độc lập không cần verify chéo -> dùng **Gemini 4 Hydrogen** (`gemini-4-hydrogen-medium`) để thực thi và tiết kiệm quota.
- **Mức 3 (`/swarm` Mode)**: Việc lớn nhiều tầng (Coder + Verifier + Build/Release) hoặc khi gọi `/swarm` -> **Coordinator (Điều phối)** BẮT BUỘC dùng **Gemini 4 Neon** (`gemini-4-neon-medium`; không fallback ngược về Carbon/Argon), **Workers (Thực thi)** dùng **Gemini 4 Hydrogen** (`gemini-4-hydrogen-medium`).
- **Quy chuẩn chung**: Luôn chốt Contract trước khi chạy song song, khóa `Locked Files` trong `task.md` và báo cáo ngắn <= 10 dòng.
