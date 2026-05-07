# Quickstart: GitHub User List

**Phase 1 Output** | **Feature**: 001-github-user-list | **Date**: 2026-05-06

---

## Yêu cầu

- Android Studio Hedgehog+ với KMP plugin
- Xcode 15+ (cho iOS)
- JDK 17+

---

## Cấu trúc project

```
demo-kmp/
├── shared/                  ← KMP module (business logic)
├── androidApp/              ← Android native app
└── iosApp/                  ← iOS native app
```

---

## Luồng dữ liệu tổng quan

```
GitHub API
    │ HTTP GET (Ktor)
    ▼
GitHubApiService (commonMain/data/remote)
    │ List<GitHubUserDto>
    ▼
UserRepositoryImpl (commonMain/data/repository)
    │ Result<List<GitHubUser>>
    ▼
GetUsersUseCase (commonMain/domain/usecase)
    │ Result<List<GitHubUser>>
    ▼
UserListViewModel (commonMain/presentation)
    │ StateFlow<UserListState>
    ├──────────────────────────────┐
    ▼                              ▼
Android UI (Compose)          iOS UI (SwiftUI)
collectAsStateWithLifecycle   ObservableObject wrapper
```

---

## Khởi tạo Koin

### Android — `MainApplication.kt`

```kotlin
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MainApplication)
            androidLogger()
        }
    }
}
```

### iOS — `iOSApp.swift`

```swift
@main
struct iOSApp: App {
    init() {
        KoinHelperKt.doInitKoin()
    }
    var body: some Scene {
        WindowGroup { ContentView() }
    }
}
```

---

## Android UI — `UserListScreen.kt` (skeleton)

```kotlin
@Composable
fun UserListScreen(viewModel: AndroidUserListViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state) {
        is UserListState.Loading -> CircularProgressIndicator()
        is UserListState.Success -> LazyColumn { /* render items */ }
        is UserListState.Error -> Text((state as UserListState.Error).message)
    }
}
```

---

## iOS UI — `UserListView.swift` (skeleton)

```swift
struct UserListView: View {
    @StateObject var observable = UserListObservable()

    var body: some View {
        switch observable.state {
        case is UserListState.Loading:
            ProgressView()
        case let s as UserListState.Success:
            List(s.users, id: \.id) { user in
                UserRowView(user: user)
            }
        case let e as UserListState.Error:
            Text(e.message)
        default:
            EmptyView()
        }
    }
}
```

---

## Chạy tests

```bash
# Unit tests trong commonTest (chạy trên JVM)
./gradlew :shared:testDebugUnitTest

# Hoặc toàn bộ
./gradlew :shared:allTests
```
