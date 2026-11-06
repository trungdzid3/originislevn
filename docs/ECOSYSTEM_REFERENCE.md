# TỔNG HỢP HỆ SINH THÁI TÙY BIẾN & MỞ RỘNG ORIGIN ISLAND (ĐẢO NGUYÊN TỬ) TRÊN ORIGINOS VÀ ANDROID

Tài liệu tham chiếu toàn diện về các dự án mã nguồn mở, module root, giải pháp kỹ thuật và thư viện dịch ngược liên quan đến Origin Island của vivo / iQOO cũng như các giải pháp đảo động tương đương trên hệ sinh thái Android.

---

## PHẦN 1: MỞ RỘNG & VƯỢT GIỚI HẠN ORIGIN ISLAND GỐC (VIVO / IQOO)
Nhóm này can thiệp trực tiếp vào giao diện Origin Island nguyên bản của hệ điều hành OriginOS thông qua cấu trúc extras ẩn `notification.superx.*`, giữ nguyên 100% độ mượt mà, chuyển động vật lý đàn hồi và tính đồng bộ phần cứng của vivo.

### A. Nhóm Không Cần Root (No-Root / Shizuku / Notification Listener)

1. **Origin Isle (fvhde/origin-isle)**
   - **Nền tảng**: GitHub, cộng đồng Reddit (`r/Vivo`).
   - **Cơ chế**: Giả lập định danh gói (`package applicationId spoofing`) của ứng dụng bản đồ AMap (`com.autonavi.minimap`) để vượt qua danh sách trắng (whitelist) cứng của vivo SystemUI.
   - **Tính năng**: Bắt thông báo hệ thống qua `NotificationListenerService` và chuyển tiếp thành thẻ SuperX: điều hướng Google Maps, tiến trình tải xuống, cuộc gọi VoIP, mã OTP, biến động ngân hàng, trạng thái thanh toán (Google Wallet, PayPal), tỷ số thể thao trực tiếp.
   - **Dự án hiện tại (origin-isle-vn)**: Bản fork tối ưu hóa riêng cho người dùng Việt Nam (hỗ trợ toàn bộ ngân hàng VN, OTP tiếng Việt, Grab/Be/ShopeeFood, Quick Share/EasyShare, Galaxy Fit 3 và wearables BLE, thẻ âm lượng/chuông/rung, giao diện OriginOS 6 chuẩn mực).

2. **Origin Ghost Player (fvhde/origin-ghost-player)**
   - **Nền tảng**: GitHub.
   - **Cơ chế**: Giả lập applicationId của các ứng dụng phát nhạc nội địa nằm trong whitelist của vivo (như `com.tencent.ibg.joox`, `com.kugou.android`, hoặc `Luna`) và đóng vai trò cầu nối chuyển tiếp `MediaSessionManager`.
   - **Tính năng**: Cho phép Spotify, YouTube, YouTube Music, Apple Music, Tidal, SoundCloud, và các trình duyệt web (Firefox, Chrome, Edge) hiển thị đầy đủ bìa album chất lượng cao, sóng nhạc động và các nút điều khiển trực tiếp trên Origin Island cũng như widget OriginPlayer ngoài màn hình chính.

3. **OriginOS Toolkit (cameleonnbss/originos-toolkit)**
   - **Nền tảng**: GitHub.
   - **Cơ chế**: Khai thác đặc quyền Shizuku (giao tiếp ADB không cần máy tính) kết hợp `Notification API`.
   - **Tính năng**: Cung cấp giao diện "OriginIsland Playground" cho phép người dùng tự thử nghiệm 6 mẫu template gốc của vivo (`TEMPLATE_BASE`, `TEMPLATE_PRIORITY_INFO`, `TEMPLATE_PROGRESS`, `TEMPLATE_WAVE`, `TEMPLATE_BUTTONS`, `TEMPLATE_CAPSULE`), đồng thời cưỡng chế các app điều hướng và phát nhạc bất kỳ hiển thị thanh tiến trình. Kèm theo 39 tinh chỉnh hệ thống khác (khóa tần số quét 120/144Hz từng app, gỡ bỏ ứng dụng rác hệ thống bloatware).

4. **MeloX-Android / 弥乐 (lladlam/MeloX-Android)**
   - **Nền tảng**: GitHub, Appteka, cộng đồng LinuxDo / Coolapk.
   - **Cơ chế**: Trình phát nhạc bên thứ ba tích hợp sẵn bộ chuyển đổi (adapter) cục bộ, gọi thẳng API `notification.superx.*` mà không cần đẩy thông qua máy chủ đám mây Aliyun EMAS.
   - **Tính năng**: Phát nhạc trực tiếp từ nguồn Spotify (thông qua lõi `librespot`), nhập nguồn nhạc chuẩn LX Music (Lạc Tuyết) và tự động kích hoạt Origin Island gốc trên thiết bị vivo/iQOO mà không cần cài thêm công cụ giả lập gói trung gian.

5. **CustomizeLiveUpdate (imlupp/CustomizeLiveUpdate)**
   - **Nền tảng**: GitHub, Coolapk (酷安).
   - **Cơ chế**: Khai thác trực tiếp chuẩn `Live Updates` (Hoạt động trực tiếp) được Google tích hợp trên nền tảng Android 16 (tương thích OriginOS 6 và OriginOS 7).
   - **Tính năng**: Cho phép người dùng tự tạo và ghim cố định các thẻ thông tin tùy biến (mã lấy hàng chuyển phát nhanh, mã nhận đồ ăn, ghi chú công việc nhanh, đồng hồ đếm ngược) kèm bộ icon phân loại lên thẳng đảo động của hệ thống.

### B. Nhóm Yêu Cầu Root (LSPosed / Xposed / KernelSU)

1. **vivo-islandfix**
   - **Nền tảng**: GitHub, LSPosed Repo.
   - **Cơ chế**: Sử dụng Xposed Framework để hook trực tiếp vào tiến trình `com.android.systemui` và `com.vivo.upslide` của OriginOS.
   - **Tính năng**: Vô hiệu hóa hoàn toàn cơ chế kiểm tra whitelist gói ứng dụng (package signature & name check) trong mã nguồn SystemUI của vivo. Giúp bất kỳ ứng dụng nào tự phát hành thông báo SuperX hoặc MediaSession đều hiển thị lên Origin Island mà không cần đổi tên gói thành AMap hoặc JOOX.

---

## PHẦN 2: THƯ VIỆN API & TÀI LIỆU DỊCH NGƯỢC (REVERSE ENGINEERING)

1. **CunnyPlayground (theVakhovskeIsTaken/CunnyPlayground - nhánh originos-experimental)**
   - **Nền tảng**: GitHub.
   - **Giá trị kỹ thuật**: Cung cấp toàn bộ cấu trúc Bundle và JSON đã dịch ngược của `notification.superx` trên OriginOS, `HyperIsland` trên Xiaomi HyperOS và `Live Updates` trên Android 16. Tài liệu tham chiếu cốt lõi cho các nhà phát triển muốn tích hợp SuperX độc lập.

2. **Tài liệu SDK Đảo Nguyên Tử chính thức từ Aliyun (Aliyun EMAS - vivo原子岛推送指南)**
   - **Nền tảng**: Aliyun Help Docs (Mã tài liệu 3030718).
   - **Quy tắc kỹ thuật cốt lõi**:
     - *Tần suất làm mới (Throttle limit)*: Tối đa 10 giây/lần đối với các thông báo không phải luồng trực tiếp (live streaming/navigation).
     - *Thời gian tồn tại tối đa*: 8 tiếng liên tục, sau đó hệ thống tự hủy để tránh rò rỉ bộ nhớ và hao pin.
     - *Quy tắc ưu tiên*: Thẻ có trạng thái `is_ongoing = true` và `is_high_priority = true` sẽ chiếm quyền hiển thị của các thẻ thông thường; tự động hạ cấp thẻ có nút thao tác khi màn hình khóa bật AOD.

3. **Awesome OriginOS (cameleonnbss/originos-toolkit/docs/AWESOME-ORIGINOS.md)**
   - **Nền tảng**: GitHub.
   - **Giá trị kỹ thuật**: Danh mục tổng hợp toàn bộ các lệnh ADB ẩn, bảng mã thuộc tính hệ thống `persist.vivo.*`, các API nội bộ chưa công bố của vivo.

---

## PHẦN 3: CÁC DỰ ÁN ĐẢO ĐỘNG ĐỘC LẬP (LỚP PHỦ CHO MỌI THIẾT BỊ ANDROID)
Nhóm giải pháp tạo cửa sổ lớp phủ (Window Overlay / Accessibility Service), phù hợp cho các thiết bị không chạy OriginOS hoặc các phiên bản OriginOS cũ (OriginOS 3/4/5) chưa có API SuperX.

1. **Dynamic Island for Android (ajitpal/dynamic-island-android)**
   - **Nền tảng**: GitHub (Giấy phép mã nguồn mở MIT).
   - **Đặc điểm**: Bảo mật cao (hoàn toàn không xin quyền Internet, không quảng cáo). Cho phép căn chỉnh vị trí đảo khớp từng pixel với cụm camera (lỗ bấm giữa, đục lỗ góc trái, camera kép hình viên thuốc). Hỗ trợ trả lời tin nhắn trực tiếp, thanh tiến trình gọi xe, 7 bảng màu và trình mở nhanh tiện ích (đèn pin, chụp màn hình).

2. **Smart Island (agupta07505/SmartIsland)**
   - **Nền tảng**: GitHub, Web (`smartisland.vercel.app`).
   - **Đặc điểm**: Tích hợp tính năng *Idle Widgets* (hiển thị tốc độ mạng thời gian thực, thời tiết, đồng hồ mini ngay trên đảo khi không có thông báo) và bắt hơn 30 sự kiện phần cứng (cắm sạc, tai nghe, VPN, trạm phát Wi-Fi).

3. **Material You Dynamic Island (Anto426/Dynamic-Island)**
   - **Nền tảng**: GitHub.
   - **Đặc điểm**: Viết bằng Jetpack Compose chuẩn Material 3, tự động trích xuất màu sắc từ hình nền (Monet Palette), kiến trúc phân tách dạng Plugin độc lập (Pin, Thông báo, Media).

4. **dynamicSpot (bởi Jawomo)**
   - **Nền tảng**: XDA Developers Forums, Google Play.
   - **Đặc điểm**: Dự án lâu đời và tối ưu sâu nhất trên XDA (hơn 230 trang thảo luận). Có khả năng can thiệp chặn thông báo nổi (Heads-up notification) mặc định của Android để chuyển hướng 100% hiển thị qua đảo động.

---

## PHẦN 4: HỆ SINH THÁI TƯƠNG ĐƯƠNG TRÊN XIAOMI HYPEROS

1. **HyperIsland Enhancer (1812z/HyperIsland)**
   - **Nền tảng**: GitHub, Coolapk.
   - **Đặc điểm**: Module và ứng dụng mở rộng đảo động lớn nhất cho Xiaomi HyperOS, chuyên bóc tách thông báo ứng dụng bên thứ ba để đưa vào thanh trạng thái HyperIsland của Xiaomi.

2. **HyperIsland-ToolKit (D4vidDf/HyperIsland-ToolKit)**
   - **Nền tảng**: GitHub.
   - **Đặc điểm**: Bộ thư viện Kotlin DSL kèm ứng dụng Real-Time Inspector (cho phép trích xuất icon, xem trước cấu trúc JSON thông báo đảo). Đây chính là nguồn cảm hứng trực tiếp để cộng đồng vivo phát triển bộ OriginOS Toolkit.

---

## BẢNG SO SÁNH VÀ ĐỊNH VỊ CHO DỰ ÁN ORIGIN ISLE VIỆT NAM

| Dự án / Công cụ | Nền tảng | Cơ chế chính | Cần Root? | Điểm mạnh đặc thù | Định hướng tích hợp vào Origin Isle VN |
|---|---|---|---|---|---|
| **Origin Isle VN (Dự án này)** | GitHub | Giả lập AMap + SuperX cục bộ | Không | Bản địa hóa hoàn toàn cho VN: Ngân hàng, OTP, Grab/Be, Fit 3/BLE, Chuông/Rung, QuickShare, Logo OriginOS 6 | Trụ cột phát triển chính; tiếp tục duy trì và mở rộng thêm các card viễn trắc thiết bị. |
| **Origin Ghost Player** | GitHub | Giả lập JOOX/Kugou | Không | Hiển thị sóng nhạc, album art chuẩn trên Origin Island & Widget | Tiềm năng: Tích hợp module MediaSession bridge của Ghost Player thẳng vào Origin Isle VN để người dùng không cần cài song song 2 app. |
| **MeloX (弥乐)** | GitHub / Appteka | Local SuperX API | Không | Phát Spotify/LX Music hỗ trợ đảo gốc | Tham khảo giải thuật decode packet và cấu trúc âm nhạc không phụ thuộc EMAS. |
| **CustomizeLiveUpdate** | GitHub / Coolapk | Android 16 Live Updates | Không | Ghim ghi chú, mã nhận hàng lên đảo | Tiềm năng: Bổ sung tính năng Ghim Ghi Chú / Lời Nhắc / Mã OTP nhanh lên đảo theo yêu cầu người dùng. |
| **SmartIsland** | GitHub | Overlay | Không | Idle Widgets (Tốc độ mạng, thời tiết thường trực) | Tham khảo ý tưởng: Thẻ hiển thị tốc độ mạng hoặc đồng hồ khi đảo ở trạng thái rảnh rỗi. |
| **vivo-islandfix** | LSPosed | Xposed Hook | Có | Xóa bỏ hoàn toàn whitelist AMap/JOOX | Cung cấp tài liệu cho người dùng máy đã root muốn chạy trực tiếp không cần spoof package. |
