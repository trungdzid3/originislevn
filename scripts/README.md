# Thư Mục Scripts & Tools - Origin Isle VN

Tài liệu hướng dẫn và mô tả mục đích các tệp lệnh tự động hóa trong dự án Origin Isle VN.

## 1. scripts/release.sh
* **Mục đích**: Tự động hóa quy trình tạo bản dựng release và đánh tag Git cho phiên bản mới.
* **Cách sử dụng**: Chạy trên môi trường Bash/WSL/Git Bash:
  ```bash
  bash scripts/release.sh <phiên_bản>
  ```

## 2. scripts/tools/
Thư mục chứa các script Python nội bộ phục vụ bảo trì, đồng bộ hóa và xử lý chuỗi:

* `apply_originos6_style.py`: Đồng bộ các quy chuẩn giao diện và style theo phong cách OriginOS 6.
* `apply_vn_mod.py`: Script áp dụng các bản chỉnh sửa và logic tiếng Việt vào mã nguồn.
* `vietnamese_localization.py`: Bản đồ và công cụ dịch thuật chuỗi tài nguyên tiếng Việt.
* `patch_all_english.py`: Quét và thay thế các chuỗi hardcode tiếng Anh còn sót lại trong Compose UI/String resources.
* `patch_download_hook.py`: Vá các móc xử lý sự kiện tải xuống (download hook) cho dynamic island.
* `patch_notif_system.py`: Vá logic tích hợp thông báo hệ thống và dynamic notification.
* `fix_imports.py`: Tự động sửa và làm sạch các import trong Kotlin files sau khi tái cấu trúc.
* `scan_strings.py`: Quét toàn bộ mã nguồn Kotlin và XML để phát hiện chuỗi chưa được bản địa hóa.
* `scan_results.txt`: Kết quả quét chuỗi phục vụ kiểm tra đối chiếu.
* `update_samples.py`: Cập nhật dữ liệu mẫu và mock data cho previews/tests.
