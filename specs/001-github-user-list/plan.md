# Implementation Plan: GitHub User List

**Branch**: `001-github-user-list` | **Date**: 2026-05-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/001-github-user-list/spec.md`

## Summary

Xây dựng tính năng demo KMP: gọi GitHub API (`/users?since=100&per_page=50`), parse JSON
trong `commonMain`, quản lý UI state theo MVI, và expose `StateFlow<UserListState>` để
Android/iOS native UI subscribe và render bằng UI component riêng của từng nền tảng.

## Technical Context

**Language/Version**: Kotlin 2.x (KMP), Swift 5.9+ (iOS UI), Android minSdk 24+
**Primary Dependencies**:
- `shared/commonMain`: Ktor Client Core, Koin Core, kotlinx.serialization, Kotlin Coroutines
- `shared/androidMain`: Ktor OkHttp engine, Koin Android
- `shared/iosMain`: Ktor Darwin engine
- `androidApp`: Jetpack Compose, Coil (image loading), Koin Android
- `iosApp`: SwiftUI, Kingfisher hoặc AsyncImage (image loading)

**Storage**: N/A — không có persistence, chỉ in-memory state
**Testing**: `kotlin.test` trong `commonTest`; Android Instrumentation Test (tùy chọn)
**Target Platform**: Android 7.0+ (API 24), iOS 14+
**Project Type**: Kotlin Multiplatform mobile demo app (Android + iOS native UI)
**Performance Goals**: Danh sách 50 user hiển thị trong ≤ 5 giây với kết nối mạng bình thường
**Constraints**: Không cache, không pagination, không auth — chỉ 1 GET request duy nhất
**Scale/Scope**: Demo nhỏ — 1 màn hình danh sách + 1 màn hình chi tiết, 50 item tối đa

## Constitution Check

*GATE: Phải pass trước Phase 0. Kiểm tra lại sau Phase 1.*

| Nguyên tắc | Trạng thái | Ghi chú |
|---|---|---|
| I. commonMain-First | ✅ PASS | Toàn bộ Ktor call, JSON parse, ViewModel ở commonMain |
| II. Clean Architecture | ✅ PASS | Domain → Data → Presentation; UI chỉ phụ thuộc Presentation |
| III. MVI | ✅ PASS | `UserListState` sealed class; `StateFlow` expose từ ViewModel |
| IV. Koin DI | ✅ PASS | Module khai báo ở commonMain; expect/actual engine per platform |
| V. Ktor Only | ✅ PASS | Chỉ Ktor client; không dùng Retrofit hay URLSession trực tiếp |
| VI. commonTest coverage | ✅ PASS | UseCase + Repository + ViewModel có unit test trong commonTest |
| VII. Context7 docs | ✅ PASS | Đã tra Ktor + Koin trước khi lập kế hoạch |

**Kết luận**: Tất cả gates pass. Tiến hành Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/001-github-user-list/
├── plan.md              # File này
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── github-api.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
shared/                          # KMP shared module
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/
    │   └── com/demo/kmp/
    │       ├── domain/
    │       │   ├── model/
    │       │   │   └── GitHubUser.kt          # Domain entity
    │       │   ├── repository/
    │       │   │   └── UserRepository.kt      # Repository interface
    │       │   └── usecase/
    │       │       └── GetUsersUseCase.kt     # Use case interface + impl
    │       ├── data/
    │       │   ├── remote/
    │       │   │   ├── dto/
    │       │   │   │   └── GitHubUserDto.kt   # JSON DTO (@Serializable)
    │       │   │   └── GitHubApiService.kt    # Ktor HTTP client calls
    │       │   └── repository/
    │       │       └── UserRepositoryImpl.kt  # Repository implementation
    │       ├── presentation/
    │       │   ├── UserListState.kt           # MVI State + Effect sealed classes
    │       │   └── UserListViewModel.kt       # ViewModel (StateFlow)
    │       └── di/
    │           ├── SharedModule.kt            # Koin module (commonMain)
    │           └── HttpClientModule.kt        # Ktor client factory
    ├── androidMain/kotlin/
    │   └── com/demo/kmp/di/
    │       └── PlatformModule.android.kt      # OkHttp engine binding
    ├── iosMain/kotlin/
    │   └── com/demo/kmp/di/
    │       └── PlatformModule.ios.kt          # Darwin engine binding
    └── commonTest/kotlin/
        └── com/demo/kmp/
            ├── data/
            │   └── UserRepositoryImplTest.kt
            ├── domain/
            │   └── GetUsersUseCaseTest.kt
            └── presentation/
                └── UserListViewModelTest.kt

androidApp/                      # Android native module
├── build.gradle.kts
└── src/main/
    ├── java/com/demo/kmp/android/
    │   ├── MainActivity.kt
    │   └── ui/
    │       ├── userlist/
    │       │   ├── UserListScreen.kt          # Jetpack Compose UI
    │       │   └── UserListViewModel.kt       # Android ViewModel wrapper
    │       └── userdetail/
    │           └── UserDetailScreen.kt
    └── res/

iosApp/                          # iOS native module
└── iosApp/
    ├── ContentView.swift
    └── Features/
        ├── UserList/
        │   ├── UserListView.swift             # SwiftUI View
        │   └── UserListObservable.swift       # ObservableObject wrapper
        └── UserDetail/
            └── UserDetailView.swift
```

**Structure Decision**: KMP multi-module với `shared` (commonMain/androidMain/iosMain),
`androidApp` (Compose UI), `iosApp` (SwiftUI). Shared module expose ViewModel qua
`StateFlow<UserListState>` — Android dùng `collectAsStateWithLifecycle`, iOS dùng
`ObservableObject` wrapper với `StateFlow.toNSPublisher()` hoặc polling qua `collect`.

## Complexity Tracking

> Không có vi phạm constitution — bảng này để trống.
