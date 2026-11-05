# KIẾN TRÚC HỆ THỐNG ORIGIN ISLE VIỆT NAM

Tài liệu đặc tả kiến trúc kỹ thuật và luồng dữ liệu của giải pháp Origin Isle VN, tích hợp thông báo từ các ứng dụng dịch vụ tại Việt Nam vào Origin Island (Đảo Nguyên Bản) trên giao diện Vivo OriginOS.

---

## 1. TỔNG QUAN KIẾN TRÚC (SYSTEM OVERVIEW)

Origin Isle VN hoạt động như một tầng middleware trên hệ điều hành Android, thực hiện thu nhận thông báo thời gian thực từ các ứng dụng phổ biến tại Việt Nam (giao đồ ăn, gọi xe, ngân hàng, thương mại điện tử, tin nhắn), phân tích trích xuất dữ liệu có cấu trúc, chuyển đổi thành định dạng thẻ chuẩn và kích hoạt hiển thị trực tiếp lên cụm đảo động của OriginOS thông qua giao thức SuperX API / Vivo PlaygroundService.

```mermaid
flowchart TD
    subgraph Senders ["Ứng dụng Nguồn (VN Apps)"]
        App1["Shopee / Lazada / TikTok Shop"]
        App2["Grab / Be / ShopeeFood"]
        App3["Vietcombank / MB / Techcombank / MoMo"]
        App4["Zalo / Telegram"]
    end

    subgraph Core ["Origin Isle VN Core Engine"]
        NLS["NotificationListenerService\n(Bắt thông báo Android)"]
        Detector["App Detector & Filter Engine\n(Định danh gói tin & Loại hình)"]
        Parser["Parser Pipeline\n(Trích xuất Regex / Payload JSON)"]
        Builder["Card Builder & Data Mapper\n(Tạo cấu trúc Origin Card)"]
        Manager["Island State Manager\n(Xử lý ưu tiên, cập nhật & vòng đời)"]
    end

    subgraph VivoBridge ["Vivo OriginOS Bridge Layer"]
        Playground["PlaygroundService Client\n(Kết nối IPC / AIDL)"]
        SuperX["SuperX API Adapter\n(Đóng gói Bundle & Key tương thích)"]
    end

    subgraph OS ["OriginOS SystemUI"]
        Island["Origin Island Service\n(Hệ thống Đảo Nguyên Bản Vivo)"]
        Display["Hiển thị UI Viên thuốc / Mở rộng Thẻ"]
    end

    Senders -->|Android StatusBarNotification| NLS
    NLS --> Detector
    Detector --> Parser
    Parser --> Builder
    Builder --> Manager
    Manager --> Playground
    Playground --> SuperX
    SuperX -->|IPC / Intent / AIDL| Island
    Island --> Display
```

---

## 2. LUỒNG TUẦN TỰ XỬ LÝ THÔNG BÁO (SEQUENCE DIAGRAM)

Quy trình xử lý hoàn chỉnh từ lúc một thông báo phát sinh cho đến khi hiển thị và cập nhật trên đảo trạng thái.

```mermaid
sequenceDiagram
    autonumber
    participant TargetApp as App Nguồn (Grab/Bank/Shopee)
    participant AndroidOS as Android Notification Subsystem
    participant NLS as NotificationListenerService
    participant Parser as Detector & Parser Engine
    participant CardBuilder as Card Builder
    participant StateMgr as Island State Manager
    participant Bridge as PlaygroundService / SuperX
    participant OriginOS as Vivo Origin Island UI

    TargetApp->>AndroidOS: Phát thông báo (notify)
    AndroidOS->>NLS: onNotificationPosted(StatusBarNotification)
    NLS->>Parser: Chuyển giao gói sbn
    alt Không thuộc danh sách hỗ trợ hoặc thông báo rác
        Parser-->>NLS: Bỏ qua (Ignore / Drop)
    else Hợp lệ
        Parser->>Parser: Regex phân tích mã đơn, trạng thái, số tiền
        Parser->>CardBuilder: Dữ liệu chuẩn hóa (Entity Data)
        CardBuilder->>CardBuilder: Đóng gói thẻ hiển thị (IslandCardModel)
        CardBuilder->>StateMgr: Cập nhật hoặc tạo mới phiên thẻ
        StateMgr->>Bridge: Gửi lệnh hiển thị (Push Island Request)
        Bridge->>OriginOS: Giao tiếp AIDL / SuperX IPC
        OriginOS->>OriginOS: Render viên thuốc đảo động & trạng thái
    end

    Note over TargetApp,OriginOS: Khi ứng dụng cập nhật tiến độ (Tài xế đến / Đã giao hàng)
    TargetApp->>AndroidOS: Cập nhật thông báo
    AndroidOS->>NLS: onNotificationPosted (cùng tag/id)
    NLS->>Parser: Phân tích trạng thái mới
    Parser->>StateMgr: Cập nhật tiến độ phiên thẻ hiện có
    StateMgr->>Bridge: Gửi cập nhật tiến độ
    Bridge->>OriginOS: Render lại animation / thông số đảo
```

---

## 3. CÁC PHÂN HỆ THÀNH PHẦN (CORE MODULES)

### 3.1. NotificationListenerService (Tầng Tiếp nhận)
* Đăng ký quyền lắng nghe thông báo cấp hệ thống `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`.
* Thu nhận các sự kiện `onNotificationPosted` và `onNotificationRemoved`.
* Bộ lọc sơ cấp: loại trừ thông báo nội bộ, thông báo đang tải tệp hoặc các thông báo không chứa thông tin biến động.

### 3.2. Detector & Parser Engine (Tầng Định danh & Phân tích)
* **App Detector**: Xác định chính xác ứng dụng nguồn thông qua `packageName` (ví dụ: `com.grabtaxi.passenger`, `com.zing.zalo`, `com.vng.zalopay`, `com.mbmobile`).
* **Category Resolver**: Phân chia thông báo vào các nhóm nghiệp vụ chính:
  * Ride-Hailing / Delivery (Chuyến đi, lộ trình tài xế, mã đơn giao nhận).
  * Banking / Fintech (Biến động số dư, mã OTP bảo mật).
  * E-Commerce (Trạng thái đơn hàng, đóng gói, giao thành công).
  * Chat / Messaging (Tin nhắn mới, người gửi).
* **Regex / Heuristic Extractors**: Tách chuỗi ký tự bằng mẫu biểu thức chính quy tối ưu, chống nghẽn CPU và bộ nhớ.

### 3.3. Card Builder & Model Mapper (Tầng Chuẩn hóa Dữ liệu)
* Chuyển đổi dữ liệu đã bóc tách sang cấu trúc mô hình `IslandCard`:
  * `title`: Tiêu đề hiển thị ngắn gọn.
  * `subtitle`: Nội dung chi tiết hoặc tiến độ.
  * `icon`: Biểu tượng ứng dụng hoặc vector biểu thị trạng thái.
  * `progress`: Chỉ số phần trăm tiến độ (0 - 100) nếu có.
  * `actions`: Các nút phản hồi hoặc chuyển hướng ứng dụng gốc qua PendingIntent.

### 3.4. Island State Manager (Tầng Quản lý Vòng đời Đảo)
* Quản lý hàng đợi hiển thị và mức độ ưu tiên giữa các ứng dụng khác nhau.
* Tự động dọn dẹp hoặc hủy thẻ khi nhận sự kiện `onNotificationRemoved` hoặc hết thời hạn hiệu lực (timeout).

### 3.5. PlaygroundService & SuperX Client (Tầng Tương thích Hệ thống Vivo)
* Đóng gói dữ liệu phù hợp với giao diện lập trình riêng của Vivo OriginOS (`SuperX API`).
* Thực hiện kết nối dịch vụ IPC thông qua `PlaygroundService` để cấp phát kênh hiển thị trên thanh trạng thái Đảo Nguyên Bản mà không cần root thiết bị.

---

## 4. NGUYÊN TẮC BẢO MẬT VÀ HIỆU NĂNG

* **Bảo mật Dữ liệu Riêng tư**: Thông báo tài chính/ngân hàng và OTP chỉ được trích xuất cục bộ trong RAM của ứng dụng, không ghi vào disk log và không truyền tải ra mạng ngoài.
* **Tối ưu Tài nguyên (Resource Efficiency)**: Tránh khởi chạy luồng thừa; sử dụng Coroutines và Dispatchers nhẹ để đảm bảo thời gian phân tích gói tin dưới 10ms, giữ mượt mà 60 FPS cho giao diện hệ thống.
