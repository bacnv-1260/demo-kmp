# Feature Specification: GitHub User List

**Feature Branch**: `001-github-user-list`
**Created**: 2026-05-06
**Status**: Draft
**Input**: User description: "demo nhỏ để cho thấy việc hoạt động của KMP với android và iOS native, xây dựng chức năng lấy danh sách user từ api: https://api.github.com/users?since=100&per_page=50 và trả ra data để hiển thị UI native của từng nền tảng"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Xem danh sách GitHub User (Priority: P1)

Người dùng mở ứng dụng trên Android hoặc iOS, ứng dụng tự động tải danh sách 50 GitHub
user (bắt đầu từ ID 100) từ GitHub API và hiển thị danh sách bằng UI native của từng nền tảng.

**Why this priority**: Đây là tính năng duy nhất và cốt lõi của demo. Không có tính năng
này thì ứng dụng không có giá trị.

**Independent Test**: Có thể kiểm thử độc lập bằng cách chạy app trên Android emulator
hoặc iOS simulator — danh sách user phải hiển thị đầy đủ mà không cần thao tác thêm.

**Acceptance Scenarios**:

1. **Given** ứng dụng vừa khởi động, **When** màn hình danh sách được mở, **Then** hiển
   thị trạng thái loading trong khi đang tải dữ liệu.
2. **Given** API trả về thành công, **When** dữ liệu được nhận, **Then** danh sách hiển
   thị tối thiểu 1 user với login name và avatar.
3. **Given** thiết bị không có kết nối mạng, **When** màn hình danh sách được mở,
   **Then** hiển thị thông báo lỗi thân thiện (không crash).

---

### User Story 2 - Xem chi tiết một User (Priority: P2)

Người dùng nhấn vào một user trong danh sách để xem thêm thông tin chi tiết của user đó
(profile URL, loại tài khoản).

**Why this priority**: Tăng giá trị demo bằng cách cho thấy khả năng điều hướng và hiển
thị data phong phú hơn, nhưng không cần thiết cho MVP.

**Independent Test**: Có thể kiểm thử bằng cách nhấn vào bất kỳ user nào trong danh sách
— màn hình chi tiết phải hiển thị đầy đủ thông tin của user đó.

**Acceptance Scenarios**:

1. **Given** danh sách đã tải xong, **When** người dùng nhấn vào một user, **Then** hiển
   thị màn hình/view chi tiết với login, avatar, profile URL và loại tài khoản.
2. **Given** đang ở màn hình chi tiết, **When** người dùng nhấn nút back, **Then** quay
   về danh sách và giữ nguyên trạng thái cuộn.

---

### Edge Cases

- Nếu API GitHub trả về lỗi HTTP (4xx/5xx), ứng dụng hiển thị thông báo lỗi, không crash.
- Nếu danh sách trả về rỗng, hiển thị trạng thái empty state thay vì danh sách trống.
- Ứng dụng không được block luồng chính (UI thread) khi đang tải dữ liệu.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống PHẢI gọi API `https://api.github.com/users?since=100&per_page=50`
  khi màn hình danh sách được khởi tạo.
- **FR-002**: Hệ thống PHẢI hiển thị trạng thái loading trong khi đang chờ phản hồi API.
- **FR-003**: Hệ thống PHẢI hiển thị danh sách user với tối thiểu: login name và avatar URL.
- **FR-004**: Hệ thống PHẢI hiển thị thông báo lỗi thân thiện khi API thất bại hoặc mạng
  không khả dụng.
- **FR-005**: Người dùng PHẢI có thể nhấn vào một user trong danh sách để xem thông tin
  chi tiết (login, avatar URL, html_url, type).
- **FR-006**: Toàn bộ business logic (gọi API, parse JSON, quản lý state) PHẢI nằm trong
  shared KMP module, không được lặp lại trên từng nền tảng.
- **FR-007**: UI PHẢI được implement bằng native của từng nền tảng (Android native,
  iOS native) — không dùng UI cross-platform.

### Key Entities

- **GitHubUser**: Đại diện một GitHub user với các thuộc tính: `id` (số nguyên), `login`
  (tên đăng nhập), `avatarUrl` (URL ảnh đại diện), `htmlUrl` (URL trang cá nhân),
  `type` (loại tài khoản: User / Organization).
- **UserListState**: Trạng thái màn hình danh sách — một trong ba trạng thái: Loading,
  Success (kèm danh sách user), Error (kèm thông báo lỗi).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Danh sách 50 user hiển thị thành công trên cả Android và iOS trong vòng 5 giây
  kể từ khi mở màn hình (với kết nối mạng bình thường).
- **SC-002**: 100% business logic (gọi API, xử lý dữ liệu, quản lý state) nằm trong
  `commonMain` — bằng chứng: không có HTTP call hay JSON parse nào trong Android/iOS
  source set.
- **SC-003**: Ứng dụng không crash trong mọi trường hợp lỗi mạng — bằng chứng: tắt mạng
  và mở app, chỉ thấy thông báo lỗi, không thấy crash report.
- **SC-004**: Code UI Android và iOS hoàn toàn độc lập với nhau, chỉ nhận `State` từ shared
  ViewModel — bằng chứng: không có import cross-platform trong UI layer.

## Assumptions

- Không cần authentication với GitHub API (public endpoint, rate limit 60 req/giờ là đủ
  cho demo).
- Không cần phân trang (pagination) — chỉ tải một lần 50 user.
- Không cần lưu cache hay offline support.
- Avatar được hiển thị dưới dạng URL (load ảnh từ mạng) — thư viện load ảnh được chọn
  riêng cho từng nền tảng (Coil cho Android, AsyncImage/SDWebImage cho iOS).
- Mục tiêu là demo kỹ thuật KMP, không phải production app — không cần thiết kế UI phức tạp.
