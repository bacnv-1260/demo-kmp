# Research: GitHub User List

**Phase 0 Output** | **Feature**: 001-github-user-list | **Date**: 2026-05-06

---

## 1. Ktor Client trong KMP

**Decision**: Dùng `ktor-client-core` ở `commonMain`, `ktor-client-okhttp` ở `androidMain`,
`ktor-client-darwin` ở `iosMain`. Plugin `ContentNegotiation` + `kotlinx.serialization`
để tự động deserialize JSON.

**Rationale**: Đây là cách chính thức được Ktor documentation khuyến nghị cho KMP. OkHttp
engine cho Android đã battle-tested; Darwin engine dùng native iOS networking stack.

**Alternatives considered**:
- Retrofit: chỉ Android, không dùng cho KMP
- URLSession: chỉ iOS, vi phạm nguyên tắc V (Ktor Only)

**Versions (từ Context7)**:
- `ktor-client-core`: 2.3.x (latest stable)
- `ktor-client-okhttp`: 2.3.x
- `ktor-client-darwin`: 2.3.x

---

## 2. Koin DI trong KMP

**Decision**: Khai báo `SharedModule` trong `commonMain` với tất cả binding. Dùng
`expect val platformModule: Module` với `actual` implementation trong `androidMain`
(OkHttp engine) và `iosMain` (Darwin engine). Khởi tạo Koin qua `initKoin()` ở
`commonMain`, gọi từ `Application.onCreate()` (Android) và `main.swift` / AppDelegate (iOS).

**Rationale**: Pattern expect/actual Koin module là cách được Koin documentation khuyến
nghị để inject platform-specific dependency mà không làm lộ platform code lên commonMain.

**Versions (từ Context7)**:
- `koin-core`: 4.1.0
- `koin-android`: 4.1.0

---

## 3. GitHub API

**Decision**: Gọi `GET https://api.github.com/users?since=100&per_page=50`.
Không cần authentication. Response là JSON array.

**Response fields cần thiết**:
```json
[
  {
    "id": 101,
    "login": "pjhyett",
    "avatar_url": "https://avatars.githubusercontent.com/u/101?v=4",
    "html_url": "https://github.com/pjhyett",
    "type": "User"
  }
]
```

**Rate limit**: 60 request/giờ unauthenticated — đủ cho demo.

**Alternatives considered**: Dùng authentication token để tăng rate limit — không cần
thiết cho demo.

---

## 4. MVI State Management

**Decision**: Sealed class `UserListState` với 3 trạng thái: `Loading`, `Success(users)`,
`Error(message)`. ViewModel expose `StateFlow<UserListState>`. Side effect (navigation)
dùng `Channel<UserListEffect>` expose dưới dạng `Flow<UserListEffect>`.

**Rationale**: `StateFlow` đảm bảo Android và iOS đều nhận được state hiện tại ngay khi
subscribe. `Channel` cho effect để tránh re-deliver khi resubscribe.

---

## 5. iOS State Observation

**Decision**: Dùng `ObservableObject` wrapper trong Swift — collect `StateFlow` từ Kotlin
bằng `createPublisher()` (KMP-NativeCoroutines) hoặc polling đơn giản qua
`ViewModel.collect { }` trong `.task {}` của SwiftUI.

**Rationale**: KMP-NativeCoroutines là thư viện phổ biến nhất để bridge Kotlin Flow sang
Swift Combine/async-await. Cho demo đơn giản, có thể dùng helper `collect` trực tiếp
trong SwiftUI `.task {}`.

---

## 6. Kết luận — Tất cả unknowns đã giải quyết

| Unknown | Quyết định |
|---|---|
| HTTP engine per platform | OkHttp (Android) / Darwin (iOS) via Koin expect/actual |
| JSON parsing | kotlinx.serialization + Ktor ContentNegotiation |
| State management | MVI: StateFlow<UserListState> + Channel<Effect> |
| iOS Flow observation | ObservableObject wrapper + collect trong SwiftUI .task{} |
| DI initialization | initKoin() ở commonMain, gọi từ platform entry point |
