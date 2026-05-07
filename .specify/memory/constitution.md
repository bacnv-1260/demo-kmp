<!--
BÁO CÁO ĐỒNG BỘ
================
Thay đổi phiên bản: 1.0.0 → 1.1.0 (MINOR — thêm nguyên tắc mới VII)
Các mục đã thêm:
  - Nguyên tắc VII. Tra cứu tài liệu bắt buộc qua Context7
Nguyên tắc đã thay đổi: Không có
Các mục đã xoá: Không có
Templates đã kiểm tra:
  - .specify/templates/plan-template.md  ✅ phù hợp
  - .specify/templates/spec-template.md  ✅ phù hợp
  - .specify/templates/tasks-template.md ✅ phù hợp
TODO còn treo: không có
-->

# demo-kmp Constitution

## Nguyên Tắc Cốt Lõi

### I. commonMain là ưu tiên hàng đầu (BẮT BUỘC TUYỆT ĐỐI)

Toàn bộ business logic, domain model, use case và contract tầng data PHẢI nằm trong
`shared/commonMain`. Các module nền tảng (`androidMain`, `iosMain`) CHỈ ĐƯỢC chứa
các implementation đặc thù nền tảng (ví dụ: Ktor engine, platform I/O) và không được
chứa bất kỳ business rule nào. Mọi logic có thể dùng chung ĐỀU PHẢI được đưa vào shared.

**Lý do**: Giá trị cốt lõi của Kotlin Multiplatform là đảm bảo tính nhất quán của logic
trên mọi nền tảng. Đặt business logic ngoài `commonMain` phá vỡ cam kết này và khiến
việc kiểm thử đồng nhất đa nền tảng trở nên bất khả thi.

### II. Kỷ luật phân tầng Clean Architecture

Codebase PHẢI được tổ chức thành ba tầng với chiều phụ thuộc một chiều:

- **Domain** (`commonMain/domain/`): entity, interface use-case, contract repository —
  không phụ thuộc bất kỳ framework nào.
- **Data** (`commonMain/data/`): implementation repository, Ktor client, serialization —
  chỉ phụ thuộc vào tầng Domain.
- **Presentation** (`commonMain/presentation/`): MVI ViewModel / state machine —
  chỉ phụ thuộc vào use case của tầng Domain.

Mã UI nền tảng (Android `Activity`/`Fragment`/`Composable`, iOS `UIViewController`/
`SwiftUI`) nằm trong source set tương ứng và chỉ được phụ thuộc vào tầng Presentation
của shared module. Mọi lối tắt vi phạm tầng (ví dụ: UI gọi thẳng repository) đều
BỊ CẤM.

**Lý do**: Kỷ luật phân tầng đảm bảo từng tầng có thể kiểm thử và thay thế độc lập,
đồng thời làm rõ ranh giới giữa shared code và native code.

### III. Luồng dữ liệu một chiều theo MVI

Mọi màn hình PHẢI tuân theo pattern MVI:

- Một `State` data class duy nhất (bất biến) đại diện cho toàn bộ trạng thái UI.
- Hành động người dùng được biểu diễn bằng sealed class `Intent`/`Action`.
- Side effect được biểu diễn bằng sealed class `Effect`/`SideEffect`.
- ViewModel/Presenter PHẢI expose `StateFlow<State>` và `Flow<Effect>`.
- Biến đổi state PHẢI thuần túy theo kiểu reducer; không được ghi state trực tiếp theo
  kiểu imperative.

**Lý do**: Luồng một chiều loại bỏ toàn bộ nhóm lỗi quản lý trạng thái, giúp mọi thay
đổi state có thể truy vết và kiểm thử snapshot dễ dàng.

### IV. Dependency Injection bằng Koin

Mọi dependency PHẢI được khai báo và wire qua Koin module trong `commonMain`.
Các binding đặc thù nền tảng được cung cấp qua expect/actual Koin module.
Constructor injection là bắt buộc; lời gọi service-locator (`get()` ngoài ngữ cảnh
injection) BỊ CẤM trong production code.

**Lý do**: DI tập trung trong `commonMain` giúp dependency graph minh bạch, dễ kiểm thử
(hoán đổi module trong test) và không có coupling ẩn.

### V. Networking chỉ dùng Ktor

Toàn bộ giao tiếp HTTP PHẢI dùng Ktor với engine phù hợp nền tảng (`OkHttp` trên Android,
`Darwin` trên iOS) được wire qua Koin. Không được dùng bất kỳ HTTP client nào khác
(Retrofit, URLSession raw, v.v.). Contract mạng (model request/response) PHẢI được định
nghĩa trong `commonMain` sử dụng `kotlinx.serialization`.

**Lý do**: Một abstraction HTTP duy nhất trong shared code đồng nghĩa với việc logic
mạng chỉ cần kiểm thử một lần và hoạt động giống nhau trên mọi nền tảng.

### VI. Kiểm thử coverage trong commonMain

Mọi use case, implementation repository và ViewModel PHẢI có unit test viết trong
`commonTest` source set. Test PHẢI dùng `kotlin.test` và KHÔNG ĐƯỢC phụ thuộc vào
Android hay iOS API. Code đặc thù nền tảng CÓ THỂ có thêm platform test nhưng KHÔNG
ĐƯỢC trùng lặp coverage đã có trong shared test.

**Lý do**: Test trong `commonTest` chạy trên mọi target, cung cấp độ tin cậy cao nhất
rằng shared logic đúng đắn trên mọi nền tảng.

### VII. Tra cứu tài liệu bắt buộc qua Context7

Trước và trong quá trình implement bất kỳ tính năng nào liên quan đến thư viện hoặc
framework (Ktor, Koin, KMP, Coroutines, kotlinx.serialization, Jetpack Compose, SwiftUI,
v.v.), developer (và AI agent) PHẢI tra cứu tài liệu chính thức và cập nhật nhất thông
qua **Context7** trước khi viết code. Không được dựa vào kiến thức được train sẵn hoặc
tài liệu lưu cache nếu Context7 có thể cung cấp phiên bản mới hơn.

Quy trình bắt buộc:
1. Gọi `mcp_context7_resolve-library-id` để xác định ID thư viện.
2. Gọi `mcp_context7_query-docs` để lấy tài liệu, API mới nhất trước khi implement.
3. Implement dựa trên tài liệu vừa tra cứu — không dựa trên giả định.

**Lý do**: Các thư viện KMP còn đang phát triển nhanh; API có thể thay đổi giữa các
phiên bản. Tra cứu tài liệu thực tế qua Context7 đảm bảo code luôn dùng API đúng,
tránh lỗi do thông tin lỗi thời và giảm thời gian debug.

## Công Nghệ Sử Dụng

| Mối quan tâm | Công nghệ | Chính sách phiên bản |
|---|---|---|
| Logic dùng chung | Kotlin Multiplatform (`commonMain`) | Theo KMP stable mới nhất |
| UI Android | Native Android (ưu tiên Jetpack Compose) | minSdk theo dự án |
| UI iOS | Native iOS (ưu tiên SwiftUI) | iOS deployment target theo dự án |
| Kiến trúc | Clean Architecture + MVI | — |
| DI | Koin | Bản KMP-compatible mới nhất |
| HTTP | Ktor Client | Bản KMP-compatible mới nhất |
| Serialization | kotlinx.serialization | Đi kèm Ktor |
| Bất đồng bộ | Kotlin Coroutines + Flow | Đi kèm KMP |

Không được thêm dependency vào `commonMain` nếu nó yêu cầu Android-only hay iOS-only API.
Mọi dependency trong `commonMain` PHẢI tương thích với Kotlin Multiplatform.

## Quy Trình Phát Triển

- Nhánh tính năng đặt tên theo quy ước `###-mo-ta-ngan` (ví dụ: `001-login-flow`).
- Một tính năng PHẢI có spec (`spec.md`), plan (`plan.md`) và task (`tasks.md`) trước khi
  bắt đầu implementation.
- Mọi PR PHẢI có bằng chứng `commonTest` pass trên cả JVM và (khi CI cho phép) iOS simulator.
- Vi phạm kiến trúc (import sai tầng, business logic ngoài `commonMain`) được xử lý như
  blocker của PR — không phải cảnh báo.
- Constitution này có quyền ưu tiên cao nhất. Mọi mâu thuẫn giữa file này và tài liệu
  khác đều lấy file này làm chuẩn.

## Quản Trị

- Constitution này có quyền ưu tiên cao hơn mọi hướng dẫn phát triển và tài liệu README khác.
- Sửa đổi yêu cầu: (1) lý do được ghi rõ, (2) cập nhật file này kèm tăng version,
  (3) kiểm tra đồng bộ toàn bộ file `.specify/templates/*.md`.
- Chính sách version: MAJOR khi xoá/định nghĩa lại nguyên tắc; MINOR khi thêm nguyên tắc
  hoặc mục mới; PATCH khi làm rõ câu chữ.
- Kiểm tra tuân thủ PHẢI được thực hiện ở đầu mỗi giai đoạn lập kế hoạch (mục
  Constitution Check trong `plan-template.md`).

**Phiên bản**: 1.1.0 | **Ban hành**: 2026-05-06 | **Sửa đổi lần cuối**: 2026-05-06
