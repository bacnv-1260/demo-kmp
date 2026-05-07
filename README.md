# Demo KMP

Project base Kotlin Multiplatform (KMP) minh hoạ cách chia sẻ business logic giữa **Android** và **iOS** native, trong khi mỗi platform vẫn giữ UI và data layer riêng.

---

## Tech Stack

| Thành phần | Thư viện | Version |
|---|---|---|
| KMP | Kotlin Multiplatform | 2.1.0 |
| HTTP Client | Ktor (OkHttp/Darwin) | 3.1.3 |
| DI | Koin | 4.1.0 |
| Swift Interop | SKIE | 0.10.11 |
| Coroutines | kotlinx-coroutines | 1.10.2 |
| Serialization | kotlinx-serialization | 1.8.1 |
| Android UI | Jetpack Compose | BOM 2025.04.01 |
| iOS UI | SwiftUI | — |
| Build | Gradle | 8.14.3 |

---

## Cấu trúc project

```
demo-kmp/
├── shared/                          # KMP module — business logic dùng chung
│   └── src/
│       ├── commonMain/              # Code chạy được trên cả Android lẫn iOS
│       │   └── kotlin/com/demo/kmp/
│       │       ├── data/remote/network/
│       │       │   └── NetworkClient.kt       # Ktor HTTP client wrapper
│       │       ├── domain/
│       │       │   ├── model/GitHubUser.kt    # KMP model dùng chung
│       │       │   ├── repository/            # Interface + Impl
│       │       │   └── usecase/               # GetUsersUseCase
│       │       ├── platform/
│       │       │   └── UserLocalDataSource.kt # Interface local data (expect)
│       │       ├── presentation/
│       │       │   ├── BaseViewModel.kt       # Base ViewModel với executeTask / executeNetworkTask
│       │       │   └── user/UserListViewModel.kt  # MVI ViewModel
│       │       └── di/
│       │           ├── SharedModule.kt        # Koin module chung
│       │           ├── PlatformModule.kt      # expect platformModule
│       │           └── KoinHelper.kt          # initKoin() + KoinHelper
│       ├── androidMain/                       # Android-specific implementation
│       │   └── kotlin/com/demo/kmp/di/
│       │       └── PlatformModule.android.kt  # actual platformModule (OkHttp engine)
│       └── iosMain/                           # iOS-specific implementation
│           └── kotlin/com/demo/kmp/di/
│               ├── PlatformModule.ios.kt      # actual platformModule (Darwin engine)
│               └── KoinIosHelper.kt           # initKoinIos(userLocalDataSource:)
│
├── androidApp/                      # Android app
│   └── src/main/java/com/demo/kmp/android/
│       ├── MainApplication.kt       # Khởi động Koin + inject Android module
│       ├── dao/
│       │   ├── UserDao.kt           # Native Android data source (hardcoded / Room-ready)
│       │   └── UserDaoLocalDataSource.kt  # Bridge: UserDao → UserLocalDataSource (KMP)
│       └── ui/                      # Jetpack Compose screens
│
└── iosApp/                          # iOS app
    └── iosApp/
        ├── iOSApp.swift             # Entry point — khởi động Koin
        ├── UserDao.swift            # Native iOS data source (hardcoded / CoreData-ready)
        ├── UserDaoLocalDataSource.swift  # Bridge: UserDao → UserLocalDataSource (KMP)
        └── Features/
            └── UserList/
                ├── UserListView.swift
                ├── UserListObservable.swift   # ObservableObject observe KMP ViewModel
                └── UserRowView.swift
```

---

## Kiến trúc

### Tổng quan

```
┌─────────────────────────────────────────────────────────┐
│                     shared (KMP)                        │
│                                                         │
│  Presentation      Domain           Data (remote)       │
│  ────────────      ──────           ──────────────      │
│  UserListViewModel  GetUsersUseCase  UserRepositoryImpl  │
│  BaseViewModel      UserRepository   NetworkClient       │
│       │                                    │            │
│       └── UserLocalDataSource (interface) ◄┘            │
│               platform/                                 │
└─────────────────────────────────────────────────────────┘
         ↑                          ↑
         │                          │
┌────────────────┐        ┌─────────────────────┐
│  Android       │        │  iOS                │
│  ──────────    │        │  ────────────────   │
│  UserDao       │        │  UserDao.swift      │
│  UserDaoLocal  │        │  UserDaoLocal       │
│  DataSource.kt │        │  DataSource.swift   │
│  (implements   │        │  (implements        │
│  UserLocal     │        │  UserLocal          │
│  DataSource)   │        │  DataSource)        │
└────────────────┘        └─────────────────────┘
```

### Luồng dữ liệu (MVI)

```
UI
 │  dispatch intent
 ▼
UserListViewModel.processIntent()
 │
 ├── LoadUsers ──────────► GetUsersUseCase ──► UserRepositoryImpl ──► NetworkClient (Ktor)
 │                                                                          │ GitHub API
 │                                                                          ▼
 ├── GetLocalUsers ──────► UserLocalDataSource (platform impl)         Result<List<GitHubUser>>
 │                              │
 │                         Android: UserDaoLocalDataSource
 │                         iOS:     UserDaoLocalDataSource.swift
 │
 └── SelectUser ──────────► Channel<UserListEffect> ──► NavigateToDetail
                                        │
                                 observe bằng SKIE
                                 for await in effect (iOS)
                                 collectLatest (Android)

StateFlow<ViewModelState<UserDataState>>
 └── statusState: Loading / None / Error(message)
 └── dataState: UserDataState(users: List<GitHubUser>)
```

---

## Pattern chính: Dependency Inversion cho local data

Đây là pattern cốt lõi cho phép platform inject data source native vào KMP mà không tạo dependency ngược.

### Bước 1 — Định nghĩa interface trong `commonMain`

```kotlin
// shared/commonMain/.../platform/UserLocalDataSource.kt
interface UserLocalDataSource {
    fun getAllUser(): List<GitHubUser>
}
```

### Bước 2 — Android implementation

```kotlin
// androidApp/.../dao/UserDaoLocalDataSource.kt
class UserDaoLocalDataSource(private val userDao: UserDao) : UserLocalDataSource {
    override fun getAllUser(): List<GitHubUser> =
        userDao.getAllUser().map { GitHubUser(id = it.id, login = it.login, ...) }
}
```

Register vào Koin trong `MainApplication`:
```kotlin
private val androidAppModule = module {
    single { UserDao() }
    single<UserLocalDataSource> { UserDaoLocalDataSource(get()) }
}

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MainApplication)
            androidLogger()
            modules(androidAppModule)
        }
    }
}
```

### Bước 3 — iOS implementation (Swift)

```swift
// iosApp/iosApp/UserDaoLocalDataSource.swift
class UserDaoLocalDataSource: UserLocalDataSource {
    private let userDao = UserDao()

    func getAllUser() -> [GitHubUser] {
        return userDao.getAllUser().map { user in
            GitHubUser(id: Int32(user.id), login: user.login,
                       avatarUrl: user.avatarUrl, htmlUrl: user.htmlUrl, type: user.type)
        }
    }
}
```

Inject vào Koin trong `iOSApp.swift`:
```swift
@main
struct iOSApp: App {
    init() {
        let dataSource = UserDaoLocalDataSource(userDao: UserDao())
        KoinIosHelperKt.doInitKoinIos(userLocalDataSource: dataSource)
    }
}
```

`initKoinIos` nhận instance và register vào Koin:
```kotlin
// shared/iosMain/.../di/KoinIosHelper.kt
fun initKoinIos(userLocalDataSource: UserLocalDataSource): KoinApplication {
    return initKoin {
        modules(module {
            single<UserLocalDataSource> { userLocalDataSource }
        })
    }
}
```

---

## SKIE — Swift Interop

SKIE là Kotlin compiler plugin tự động bridge các Kotlin types sang Swift-native equivalents.

### Vì sao cần SKIE?

Không có SKIE, Swift không thể dùng `StateFlow` hay sealed class tự nhiên:
```swift
// ❌ Không có SKIE — callback thủ công, type casting
stateJob = viewModel.observeState { newState in
    if let success = newState as? UserListState.Success { ... }
}
```

Với SKIE:
```swift
// ✅ Có SKIE — native Swift concurrency + exhaustive switch
Task {
    for await state in viewModel.state {       // StateFlow → AsyncSequence
        self.state = state
    }
}

Task {
    for await effect in viewModel.effect {     // Flow → AsyncSequence
        switch onEnum(of: effect) {            // sealed class → exhaustive switch
        case .navigateToDetail(let nav):
            self.selectedUser = nav.user
        }
    }
}
```

### Cài đặt SKIE

Thêm vào `gradle/libs.versions.toml`:
```toml
[versions]
skie = "0.10.11"

[plugins]
skie = { id = "co.touchlab.skie", version.ref = "skie" }
```

Apply trong `shared/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.skie)  // thêm dòng này
}
```

> **Lưu ý**: SKIE chỉ cần apply ở `shared` module, không cần ở `androidApp`.

---

## Koin DI

### Dependency graph

```
NetworkClient (singleton)
  └── UserRepositoryImpl (singleton) implements UserRepository
        └── GetUsersUseCase (factory)
              └── UserListViewModel (viewModel)
                    └── UserLocalDataSource (singleton, inject từ platform)
```

### Modules

| Module | File | Scope |
|--------|------|-------|
| `sharedModule` | `SharedModule.kt` | Network, Repository, UseCase, ViewModel |
| `platformModule` | `PlatformModule.*.kt` | Ktor engine (OkHttp / Darwin) |
| `androidAppModule` | `MainApplication.kt` | UserDao, UserDaoLocalDataSource |
| iOS module (inline) | `KoinIosHelper.kt` | UserLocalDataSource (từ Swift) |

### Lấy ViewModel bên iOS

iOS không có `koinViewModel()` như Android Compose, nên dùng `KoinHelper`:
```kotlin
// shared/commonMain/.../di/KoinHelper.kt
class KoinHelper : KoinComponent {
    fun getUserListViewModel(): UserListViewModel {
        val viewModel: UserListViewModel by inject()
        return viewModel
    }
}
```

```swift
// UserListObservable.swift
viewModel = KoinHelper().getUserListViewModel()
```

---

## Thêm một feature mới

### 1. Thêm model vào `commonMain`

```kotlin
// shared/commonMain/.../domain/model/MyModel.kt
@Serializable
data class MyModel(val id: Int, val name: String)
```

### 2. Thêm repository

```kotlin
// Interface
interface MyRepository {
    suspend fun getData(): Result<List<MyModel>>
}

// Implementation
class MyRepositoryImpl(private val networkClient: NetworkClient) : MyRepository {
    override suspend fun getData() = runCatching {
        networkClient.request<List<MyModel>>(HttpMethod.Get, "endpoint")
    }
}
```

### 3. Thêm UseCase

```kotlin
class GetMyDataUseCase(private val repository: MyRepository) {
    suspend operator fun invoke() = repository.getData()
}
```

### 4. Thêm ViewModel (MVI pattern)

Extend `BaseViewModel<MyDataState>` — được cung cấp `executeTask`, `executeNetworkTask`,
và `state: StateFlow<ViewModelState<MyDataState>>` tự động.

```kotlin
data class MyDataState(val items: List<MyModel> = emptyList())

sealed class MyIntent {
    data object Load : MyIntent()
    data class Select(val item: MyModel) : MyIntent()
}

sealed class MyEffect {
    data class NavigateToDetail(val item: MyModel) : MyEffect()
}

class MyViewModel(
    private val useCase: GetMyDataUseCase
) : BaseViewModel<MyDataState>() {

    // Phải là backing field (không dùng get()) để tránh tạo instance mới mỗi lần truy cập
    override val _state = MutableStateFlow(ViewModelState(dataState = MyDataState()))

    private val _effect = Channel<MyEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init { processIntent(MyIntent.Load) }

    fun processIntent(intent: MyIntent) {
        when (intent) {
            is MyIntent.Load -> loadData()
            is MyIntent.Select -> viewModelScope.launch {
                _effect.send(MyEffect.NavigateToDetail(intent.item))
            }
        }
    }

    private fun loadData() {
        executeNetworkTask(action = { useCase() }) { data ->
            _state.update { it.copy(dataState = it.dataState.copy(items = data.orEmpty())) }
        }
    }
}
```

### 5. Register vào Koin

```kotlin
// SharedModule.kt
val sharedModule = module {
    // ... existing
    singleOf(::MyRepositoryImpl) bind MyRepository::class
    factoryOf(::GetMyDataUseCase)
    viewModelOf(::MyViewModel)
}
```

### 6. Android UI — Jetpack Compose

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // render state
}
```

### 7. iOS UI — SwiftUI + SKIE

```swift
@MainActor
class MyObservable: ObservableObject {
    // ViewModelState không có type argument vì SKIE erase generic type parameter
    @Published var state = ViewModelState(statusState: StatusState.None(), dataState: MyDataState(items: []))
    @Published var selectedItem: MyModel? = nil
    private var viewModel = KoinHelper().getMyViewModel()
    private var tasks: [Task<Void, Never>] = []

    init() {
        tasks.append(Task {
            for await s in viewModel.state { self.state = s }  // StateFlow → AsyncSequence
        })
        tasks.append(Task {
            for await effect in viewModel.effect {
                switch onEnum(of: effect) {                    // sealed class → exhaustive switch
                case .navigateToDetail(let nav): self.selectedItem = nav.item
                }
            }
        })
    }

    deinit { tasks.forEach { $0.cancel() } }
}
```
    }
}
```

---

## Thêm platform-specific dependency

Khi cần inject một dependency native (DB, sensor, camera…) từ platform vào KMP:

1. **Định nghĩa interface** trong `commonMain/platform/`
2. **Implement** trong `androidApp` (Kotlin) và `iosApp` (Swift)
3. **Register** vào Koin:
   - Android: trong `androidAppModule` ở `MainApplication`
   - iOS: truyền vào `initKoinIos()` tại `iOSApp.swift`

---

## Build & Run

### Yêu cầu

- Android Studio Hedgehog+
- Xcode 15.4+
- JDK 17
- macOS (để build iOS)

### Android

```bash
./gradlew :androidApp:assembleDebug
# hoặc mở Android Studio và Run
```

### iOS framework (build KMP trước khi mở Xcode)

```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

### iOS app

```bash
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination "platform=iOS Simulator,name=iPhone 16" \
  -configuration Debug \
  build
```

Hoặc mở `iosApp/iosApp.xcodeproj` trong Xcode và Run (Xcode tự gọi Gradle build phase).

### Chạy test

```bash
./gradlew :shared:allTests
```

---

## Lưu ý quan trọng

| Vấn đề | Giải pháp |
|--------|-----------|
| `KoinIosHelperKt` not found trong Swift | Build lại Kotlin framework: `./gradlew :shared:iosSimulatorArm64Binaries` |
| SKIE không hỗ trợ Kotlin version | Kiểm tra compatibility tại [skie.touchlab.co](https://skie.touchlab.co/intro#compatibility-with-kotlin) |
| `UserLocalDataSource` chưa có iOS impl | Implement interface trong Swift, truyền vào `initKoinIos()` |
| StateFlow không update trên main thread iOS | `UserListObservable` đã mark `@MainActor` — đảm bảo init từ main thread |
| Xcode project không tìm được framework | Kiểm tra `FRAMEWORK_SEARCH_PATHS` trỏ đúng `shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)` |
