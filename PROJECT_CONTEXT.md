# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: **`1.0.4.1`** (mã bản dựng `versionCode = 10005`, tuân thủ chuẩn SemVer 2.0.0).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (v2.2.1 / commit `3fe129d`).
- **Cải tiến v1.0.4.1 (Hotfix chống crash OTP Demo & Fix Quick Share Real)**:
  1. **Sửa triệt để crash demo OTP Ngân hàng**:
     - Thay `R.mipmap.ic_launcher_round` (adaptive-icon) trong `IslandPreview` thành `R.drawable.ic_chat` (VectorDrawable an toàn 100% trong Compose `painterResource`).
     - Gỡ bỏ hoàn toàn việc phát notification nội bộ trong demo OTP, loại bỏ triệt để nguy cơ ngoại lệ runtime.
  2. **Khắc phục Quick Share thực tế không lên Đảo**:
     - Đưa `QuickShareDetector` lên trước `OtpDetector` trong `NotificationCastListener.kt` (tránh việc OTP bắt nhầm các thông báo Quick Share có số dung lượng/số lượng file).
     - Mở rộng nhận diện Quick Share: quét cả package (`com.google.android.gms`, `com.vivo.easyshare`, `com.vivo.share`, `com.bbk.share`, `com.android.bluetooth`, `com.google.android.apps.sharing`), tag (`sharing`, `nearby`, `quick`), Action buttons (Chấp nhận/Từ chối), Channel ID và Text.
     - Đồng bộ cấu hình SuperX của `QuickShareCard.kt` theo đúng chuẩn mẫu demo (`TEMPLATE_BASE (4)` + Capsule "Nhận tệp" hoặc Vòng tiến độ % quanh camera).

## 2. Quy Định Xuất Bản Cố Định (Strict Policy)
File APK thành phẩm xuất bản trực tiếp tại thư mục nguồn dự án:
`C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\OriginIsle-VN-v1.0.4.1-debug.apk` (và `OriginIsle-VN-v1.0.4-debug.apk`)

## 3. Tình Trạng Hiện Tại
- Biên dịch: `BUILD SUCCESSFUL in 2m 20s`.
- File APK: `OriginIsle-VN-v1.0.4.1-debug.apk` (76.6 MB).
