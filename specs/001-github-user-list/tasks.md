---
description: "Task list for GitHub User List feature"
---

# Tasks: GitHub User List

**Input**: Design documents từ `specs/001-github-user-list/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/github-api.md ✅

## Format: `[ID] [P?] [Story?] Mô tả`

- **[P]**: Có thể chạy song song (file khác nhau, không phụ thuộc)
- **[Story]**: User story tương ứng (US1, US2)
- Đường dẫn file đầy đủ trong mỗi mô tả

---

## Phase 1: Setup (Khởi tạo project)

**Mục đích**: Tạo cấu trúc project KMP và cấu hình dependencies.

- [x] T001 Tạo cấu trúc KMP multi-module project: `shared/`, `androidApp/`, `iosApp/` tại root
- [x] T002 Cấu hình `settings.gradle.kts` để include các module `shared`, `androidApp`
- [x] T003 [P] Cấu hình `shared/build.gradle.kts` với KMP targets (androidTarget, iosArm64, iosX64, iosSimulatorArm64), thêm dependencies: `ktor-client-core`, `ktor-client-okhttp` (androidMain), `ktor-client-darwin` (iosMain), `koin-core` (commonMain), `koin-android` (androidMain), `kotlinx-serialization-json`, `kotlinx-coroutines-core`
- [x] T004 [P] Cấu hình `androidApp/build.gradle.kts` với Jetpack Compose, Coil, Koin Android, dependency vào module `shared`
- [x] T005 [P] Cấu hình `gradle/libs.versions.toml` với version catalog: ktor `2.3.x`, koin `4.1.0`, kotlinx-serialization, coil `2.x`
- [x] T006 Cấu hình `shared/src/androidMain/AndroidManifest.xml` (permission INTERNET)

**Checkpoint**: Project sync thành công, không có compile error.

---

## Phase 2: Foundational (Nền tảng dùng chung — PHẢI xong trước khi bắt đầu User Story)

**Mục đích**: Tạo domain model, DI framework, Ktor client — tất cả các User Story đều phụ thuộc.

⚠️ **CRITICAL**: Không bắt đầu Phase 3 hoặc 4 cho đến khi Phase 2 hoàn thành.

- [x] T007 Tạo domain entity `GitHubUser` tại `shared/src/commonMain/kotlin/com/demo/kmp/domain/model/GitHubUser.kt` (data class: id, login, avatarUrl, htmlUrl, type)
- [x] T008 Tạo repository interface `UserRepository` tại `shared/src/commonMain/kotlin/com/demo/kmp/domain/repository/UserRepository.kt` (suspend fun getUsers(): Result<List<GitHubUser>>)
- [x] T009 Tạo DTO `GitHubUserDto` tại `shared/src/commonMain/kotlin/com/demo/kmp/data/remote/dto/GitHubUserDto.kt` (@Serializable, @SerialName cho avatar_url, html_url; extension fun toDomain())
- [x] T010 Tạo `GitHubApiService` tại `shared/src/commonMain/kotlin/com/demo/kmp/data/remote/GitHubApiService.kt` (Ktor HttpClient, GET https://api.github.com/users?since=100&per_page=50, trả List<GitHubUserDto>)
- [x] T011 Tạo `UserRepositoryImpl` tại `shared/src/commonMain/kotlin/com/demo/kmp/data/repository/UserRepositoryImpl.kt` (gọi GitHubApiService, map DTO → domain, wrap Result)
- [x] T012 [P] Tạo Koin module `SharedModule` tại `shared/src/commonMain/kotlin/com/demo/kmp/di/SharedModule.kt` (bind GitHubApiService, UserRepositoryImpl, GetUsersUseCase, UserListViewModel)
- [x] T013 [P] Tạo `expect val platformModule: Module` tại `shared/src/commonMain/kotlin/com/demo/kmp/di/PlatformModule.kt`
- [x] T014 Tạo `actual val platformModule` cho Android tại `shared/src/androidMain/kotlin/com/demo/kmp/di/PlatformModule.android.kt` (bind OkHttp HttpClientEngine)
- [x] T015 Tạo `actual val platformModule` cho iOS tại `shared/src/iosMain/kotlin/com/demo/kmp/di/PlatformModule.ios.kt` (bind Darwin HttpClientEngine)
- [x] T016 Tạo hàm `initKoin(config: KoinAppDeclaration? = null)` tại `shared/src/commonMain/kotlin/com/demo/kmp/di/KoinHelper.kt` (startKoin với sharedModule + platformModule)

**Checkpoint**: `./gradlew :shared:compileKotlinMetadata` pass. Koin module không có lỗi circular dependency.

---

## Phase 3: User Story 1 — Xem danh sách GitHub User (Priority: P1) 🎯 MVP

**Goal**: Người dùng mở app, thấy trạng thái loading, sau đó danh sách 50 GitHub user hiển thị bằng UI native của từng nền tảng. Lỗi mạng được xử lý graceful.

**Independent Test**: Chạy Android app trên emulator hoặc iOS app trên simulator — danh sách phải hiển thị mà không cần thao tác thêm.

### Shared — Presentation layer (US1)

- [x] T017 [P] Tạo MVI state/intent/effect tại `shared/src/commonMain/kotlin/com/demo/kmp/presentation/UserListState.kt` (sealed class UserListState: Loading, Success(users), Error(message); sealed class UserListIntent; sealed class UserListEffect)
- [x] T018 Tạo `GetUsersUseCase` tại `shared/src/commonMain/kotlin/com/demo/kmp/domain/usecase/GetUsersUseCase.kt` (operator fun invoke(): Result<List<GitHubUser>>)
- [x] T019 Tạo `UserListViewModel` tại `shared/src/commonMain/kotlin/com/demo/kmp/presentation/UserListViewModel.kt` (StateFlow<UserListState>, Channel<UserListEffect>, fun processIntent(intent: UserListIntent), gọi GetUsersUseCase trong coroutineScope)

### Android — User Story 1 (US1)

- [x] T020 Tạo `MainApplication` tại `androidApp/src/main/java/com/demo/kmp/android/MainApplication.kt` (Application class, gọi initKoin với androidContext + androidLogger)
- [x] T021 [P] Tạo Android ViewModel wrapper `UserListAndroidViewModel` tại `androidApp/src/main/java/com/demo/kmp/android/ui/userlist/UserListAndroidViewModel.kt` (extends AndroidViewModel, delegate sang shared UserListViewModel qua Koin)
- [x] T022 Tạo `UserListScreen` Composable tại `androidApp/src/main/java/com/demo/kmp/android/ui/userlist/UserListScreen.kt` (collectAsStateWithLifecycle từ StateFlow; khi Loading → CircularProgressIndicator; khi Success → LazyColumn với UserItem; khi Error → Text lỗi + nút Retry)
- [x] T023 [P] Tạo `UserItem` Composable tại `androidApp/src/main/java/com/demo/kmp/android/ui/userlist/UserItem.kt` (Row với AsyncImage Coil cho avatar, Text login)
- [x] T024 Cập nhật `MainActivity` tại `androidApp/src/main/java/com/demo/kmp/android/MainActivity.kt` (setContent với Compose theme, hiển thị UserListScreen)

### iOS — User Story 1 (US1)

- [x] T025 Cập nhật `iOSApp.swift` tại `iosApp/iosApp/iOSApp.swift` (gọi KoinHelperKt.doInitKoin() trong init)
- [x] T026 [P] Tạo `UserListObservable` tại `iosApp/iosApp/Features/UserList/UserListObservable.swift` (ObservableObject, collect StateFlow từ shared UserListViewModel trong Task{})
- [x] T027 Tạo `UserListView` tại `iosApp/iosApp/Features/UserList/UserListView.swift` (SwiftUI View, switch trên state: ProgressView / List với UserRowView / Text lỗi + Button retry)
- [x] T028 [P] Tạo `UserRowView` tại `iosApp/iosApp/Features/UserList/UserRowView.swift` (HStack với AsyncImage cho avatar, Text login)
- [x] T029 Cập nhật `ContentView.swift` tại `iosApp/iosApp/ContentView.swift` (hiển thị UserListView)

**Checkpoint**: App chạy được trên Android emulator VÀ iOS simulator, hiển thị danh sách 50 GitHub user. Tắt mạng → thấy thông báo lỗi, không crash.

---

## Phase 4: User Story 2 — Xem chi tiết User (Priority: P2)

**Goal**: Người dùng nhấn vào một user trong danh sách, màn hình chi tiết hiển thị đầy đủ: login, avatar, profile URL, loại tài khoản. Nút back về danh sách.

**Independent Test**: Nhấn vào bất kỳ user nào trong danh sách — màn hình chi tiết phải hiển thị.

### Shared — Navigation effect (US2)

- [x] T030 Cập nhật `UserListEffect` trong `shared/src/commonMain/kotlin/com/demo/kmp/presentation/UserListState.kt` thêm `NavigateToDetail(user: GitHubUser)`
- [x] T031 Cập nhật `UserListViewModel` tại `shared/src/commonMain/kotlin/com/demo/kmp/presentation/UserListViewModel.kt` xử lý `UserListIntent.SelectUser` → emit `UserListEffect.NavigateToDetail`

### Android — User Story 2 (US2)

- [x] T032 [P] Tạo `UserDetailScreen` Composable tại `androidApp/src/main/java/com/demo/kmp/android/ui/userdetail/UserDetailScreen.kt` (nhận GitHubUser, hiển thị avatar lớn + login + html_url clickable + type, TopAppBar với nút back)
- [x] T033 Cập nhật `MainActivity` tại `androidApp/src/main/java/com/demo/kmp/android/MainActivity.kt` thêm NavHost với 2 destinations: userList và userDetail, collect Effect để navigate

### iOS — User Story 2 (US2)

- [x] T034 [P] Tạo `UserDetailView` tại `iosApp/iosApp/Features/UserDetail/UserDetailView.swift` (nhận GitHubUser, hiển thị AsyncImage avatar lớn, Text login, Link html_url, Text type)
- [x] T035 Cập nhật `UserListView` tại `iosApp/iosApp/Features/UserList/UserListView.swift` thêm NavigationLink tới UserDetailView khi tap vào UserRowView

**Checkpoint**: Nhấn vào user → màn hình chi tiết đầy đủ thông tin. Nút back hoạt động, danh sách giữ nguyên vị trí cuộn.

---

## Phase 5: Unit Tests trong commonTest

**Mục đích**: Đảm bảo 100% business logic trong commonMain được cover test.

- [x] T036 [P] Tạo `UserRepositoryImplTest` tại `shared/src/commonTest/kotlin/com/demo/kmp/data/UserRepositoryImplTest.kt` (mock GitHubApiService, kiểm tra mapping DTO→domain, kiểm tra error wrapping)
- [x] T037 [P] Tạo `GetUsersUseCaseTest` tại `shared/src/commonTest/kotlin/com/demo/kmp/domain/GetUsersUseCaseTest.kt` (mock UserRepository, kiểm tra success/failure forwarding)
- [x] T038 [P] Tạo `UserListViewModelTest` tại `shared/src/commonTest/kotlin/com/demo/kmp/presentation/UserListViewModelTest.kt` (kiểm tra state transitions: Initial→Loading→Success, Initial→Loading→Error, retry logic)

**Checkpoint**: `./gradlew :shared:allTests` pass. Tất cả 3 test file GREEN.

---

## Phase 6: Polish & Cross-Cutting

- [x] T039 [P] Thêm empty state UI cho danh sách rỗng trong `UserListScreen.kt` (Android) và `UserListView.swift` (iOS)
- [x] T040 [P] Thêm `User-Agent` header vào Ktor client trong `shared/src/commonMain/kotlin/com/demo/kmp/di/SharedModule.kt` để tránh GitHub API reject
- [x] T041 Kiểm tra Constitution compliance: xác nhận không có HTTP call hay business logic nào nằm ngoài `commonMain`

---

## Dependency Graph

```
T001-T006 (Setup)
    │
T007-T016 (Foundation)
    │
    ├── T017-T029 (US1 — MVP) ◄── có thể bắt đầu sau khi T007-T016 xong
    │       │
    │       └── T030-T035 (US2) ◄── sau khi T017-T029 xong
    │
    └── T036-T038 (Tests) ◄── song song với US1/US2
```

## Parallel Execution (trong cùng phase)

**Phase 2 (có thể song song)**:
- T007 + T008 + T009 (models độc lập nhau)
- T012 + T013 (DI modules độc lập nhau)
- T014 + T015 (platform actual độc lập nhau)

**Phase 3 (có thể song song)**:
- T017 + T018 (sau T007-T008)
- T023 + T024 và T028 + T029 (Android và iOS UI độc lập nhau)

**Phase 4 (có thể song song)**:
- T032 + T033 (Android) song song với T034 + T035 (iOS)

**Phase 5 (hoàn toàn song song)**:
- T036 + T037 + T038

## Implementation Strategy

| Scope | Tasks | Deliverable |
|---|---|---|
| MVP (US1 only) | T001–T029 | App chạy được, hiển thị danh sách, xử lý lỗi |
| Full | T001–T038 | MVP + màn hình chi tiết + unit tests |
| Production-ready | T001–T041 | Full + polish + compliance check |
