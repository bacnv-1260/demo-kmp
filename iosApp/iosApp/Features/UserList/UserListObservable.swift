import SwiftUI
import shared

/// ObservableObject bridge giữa KMP `UserListViewModel` và SwiftUI.
///
/// Trách nhiệm:
/// - Lấy `UserListViewModel` từ Koin qua `KoinHelper`
/// - Observe `state` (StateFlow) và `effect` (Flow) từ ViewModel bằng `for await in`
///   (SKIE tự động bridge Kotlin Flow → Swift `AsyncSequence`)
/// - Publish `state` ra SwiftUI qua `@Published` để UI re-render
///
/// `state` là `ViewModelState` — wrapper chứa `statusState` (Loading/None/Error)
/// và `dataState` (`UserDataState` với danh sách users). Do SKIE erase generic type
/// parameter khi bridge sang Swift, kiểu được khai báo là `ViewModelState` (không có
/// type argument) và `dataState` được truy cập qua optional (`dataState?.users`).
///
/// Chạy hoàn toàn trên `@MainActor` để tất cả cập nhật UI an toàn trên main thread.
@MainActor
class UserListObservable: ObservableObject {
    /// Trạng thái UI hiện tại — map trực tiếp từ `ViewModelState` của ViewModel.
    /// `statusState` điều khiển loading/error, `dataState?.users` chứa danh sách users.
    @Published var state = ViewModelState(statusState: StatusState.None(), dataState: UserDataState(users: []))

    /// User được chọn để navigate đến detail screen.
    /// Được set khi nhận `UserListEffect.NavigateToDetail`.
    @Published var selectedUser: GitHubUser? = nil

    private var viewModel: UserListViewModel
    /// Danh sách async tasks đang observe — cancelled khi view bị deallocate.
    private var tasks: [Task<Void, Never>] = []

    init() {
        viewModel = KoinHelper().getUserListViewModel()
        startObserving()
    }

    /// Bắt đầu observe `state` và `effect` từ ViewModel song song.
    ///
    /// Mỗi Flow được observe trong một `Task` riêng:
    /// - Task 1: `for await in viewModel.state` → cập nhật `self.state`
    /// - Task 2: `for await in viewModel.effect` → xử lý side-effect (navigate)
    ///
    /// Dùng `onEnum(of:)` của SKIE để switch exhaustive trên sealed class `UserListEffect`.
    private func startObserving() {
        tasks.append(Task {
            for await newState in viewModel.state {
                self.state = newState
            }
        })

        tasks.append(Task {
            for await effect in viewModel.effect {
                switch onEnum(of: effect) {
                case .navigateToDetail(let nav):
                    self.selectedUser = nav.user
                }
            }
        })
    }

    /// Gửi intent `LoadUsers` để fetch danh sách từ GitHub API (remote).
    ///
    /// Thường được gọi khi người dùng nhấn nút "Thử lại" hoặc pull-to-refresh.
    func loadUsers() {
        viewModel.processIntent(intent: UserListIntent.LoadUsers())
    }

    /// Gửi intent `SelectUser` khi người dùng tap vào một user trong danh sách.
    ///
    /// ViewModel sẽ phát `UserListEffect.NavigateToDetail` qua channel,
    /// `startObserving` nhận effect đó và set `selectedUser` để trigger navigation.
    ///
    /// - Parameter user: User được chọn, sẽ được truyền vào `UserDetailView`.
    func selectUser(user: GitHubUser) {
        viewModel.processIntent(intent: UserListIntent.SelectUser(user: user))
    }

    deinit {
        tasks.forEach { $0.cancel() }
    }
}
