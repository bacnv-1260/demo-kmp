package com.demo.kmp.presentation.user

import androidx.lifecycle.viewModelScope
import com.demo.kmp.platform.UserLocalDataSource
import com.demo.kmp.domain.model.GitHubUser
import com.demo.kmp.domain.usecase.GetUsersUseCase
import com.demo.kmp.domain.usecase.UseCase
import com.demo.kmp.presentation.BaseViewModel
import com.demo.kmp.presentation.ViewModelState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý màn hình danh sách GitHub users.
 *
 * Triển khai mô hình MVI:
 * - UI gửi [UserListIntent] vào [processIntent]
 * - ViewModel cập nhật [state] (StateFlow) khi dữ liệu thay đổi
 * - Side-effect một lần (navigate) đi qua [effect] (Channel)
 *
 * Khởi tạo: tự động load users từ local data source ngay khi khởi tạo.
 *
 * Được expose sang iOS qua SKIE — [state] và [effect] tự động
 * được bridge thành `AsyncSequence` để dùng `for await in` bên Swift.
 *
 * @param getUsersUseCase UseCase gọi API remote.
 * @param userLocalDataSource Data source local theo platform (Android Room / iOS in-memory).
 */
class UserListViewModel(
    private val getUsersUseCase: GetUsersUseCase,
    private val userLocalDataSource: UserLocalDataSource
) : BaseViewModel<UserDataState>() {

    /**
     * Backing field duy nhất cho state của ViewModel.
     *
     * Khai báo là **backing field** (`= MutableStateFlow(...)`) thay vì computed property
     * (`get() = MutableStateFlow(...)`) để đảm bảo chỉ tạo một instance duy nhất.
     * Nếu dùng computed property, mỗi lần truy cập `_state` sẽ tạo object mới — khiến
     * `BaseViewModel.state` (observe instance cũ) không bao giờ nhận được update từ UI.
     */
    override val _state = MutableStateFlow(ViewModelState(dataState = UserDataState()))

    private val _effect = Channel<UserListEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        processIntent(UserListIntent.GetLocalUsers)
    }

    /**
     * Điểm vào duy nhất để xử lý intent từ UI (MVI entry point).
     *
     * Phân phối intent đến đúng hàm xử lý nội bộ:
     * - [UserListIntent.LoadUsers] → gọi API remote
     * - [UserListIntent.GetLocalUsers] → đọc từ local data source
     * - [UserListIntent.SelectUser] → phát [UserListEffect.NavigateToDetail]
     *
     * @param intent Hành động từ UI.
     */
    fun processIntent(intent: UserListIntent) {
        when (intent) {
            is UserListIntent.LoadUsers -> loadUsers()
            is UserListIntent.GetLocalUsers -> loadLocalUsers()
            is UserListIntent.SelectUser -> {
                viewModelScope.launch {
                    _effect.send(UserListEffect.NavigateToDetail(intent.user))
                }
            }
        }
    }

    /**
     * Đọc danh sách users từ local data source (platform-specific).
     *
     * Chạy trên [Dispatchers.IO] qua [executeTask].
     * Không phát Loading state (needBlock = true theo default nhưng
     * được gọi ngay lúc init nên UI thấy Loading ngay từ đầu).
     * Kết quả: cập nhật [state] → [List<GitHubUser>].
     */
    private fun loadLocalUsers() {
        executeTask(action = {
            userLocalDataSource.getAllUser()
        }) { data ->
            _state.update { current ->
                current.copy(
                    dataState = current.dataState.copy(users = data)
                )
            }
        }
    }

    /**
     * Gọi GitHub API để lấy danh sách users thông qua [GetUsersUseCase].
     *
     * Flow:
     * Gọi [GetUsersUseCase.invoke]
     * Kết quả: cập nhật [state] → [List<GitHubUser>].
     */
    private fun loadUsers() {
        executeNetworkTask(action = {
            getUsersUseCase.invoke(UseCase.None())
        }) { data ->
            _state.update { current ->
                current.copy(
                    dataState = current.dataState.copy(users = data.orEmpty())
                )
            }
        }
    }
}

/**
 * Trạng thái UI của màn hình danh sách users.
 *
 * Được SKIE bridge thành Swift sealed class với `onEnum(of:)` helper.
 */
data class UserDataState(
    var users: List<GitHubUser> = emptyList()
)

/**
 * Intent được gửi từ UI vào [UserListViewModel.processIntent].
 *
 * - [LoadUsers]: Lấy danh sách từ GitHub API (remote)
 * - [GetLocalUsers]: Lấy danh sách từ local data source (cache)
 * - [SelectUser]: Người dùng tap vào một user — trigger navigate
 */
sealed class UserListIntent {
    data object LoadUsers : UserListIntent()
    data object GetLocalUsers: UserListIntent()
    data class SelectUser(val user: GitHubUser) : UserListIntent()
}

/**
 * Side-effect một lần từ [UserListViewModel].
 *
 * Đi qua [Channel.BUFFERED] nên không bị bỏ dù UI chưa collect kịp.
 * Được SKIE bridge thành `AsyncSequence` để dùng `for await in` bên Swift.
 */
sealed class UserListEffect {
    data class NavigateToDetail(val user: GitHubUser) : UserListEffect()
}
