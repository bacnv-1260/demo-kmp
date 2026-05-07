package com.demo.kmp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

/**
 * Base ViewModel cho toàn bộ KMP shared module.
 *
 * Cung cấp hai hàm tiện ích để thực thi coroutine task:
 * - [executeTask]: cho mọi tác vụ bất đồng bộ (local DB, tính toán…)
 * - [executeNetworkTask]: cho các tác vụ mạng, tự xử lý exception và kiểm tra kết nối
 *
 * Quản lý [StatusState] để báo hiệu trạng thái loading/error toàn cục cho UI.
 * Extend `androidx.lifecycle.ViewModel` nên được share giữa Android và iOS qua Koin.
 */
abstract class BaseViewModel<T>: ViewModel() {
    protected abstract val _state: MutableStateFlow<ViewModelState<T>>

    /**
     * StateFlow expose ra ngoài cho UI observe.
     *
     * Dùng `by lazy` thay vì khởi tạo trực tiếp (`= _state.asStateFlow()`) để tránh
     * `NullPointerException`: constructor của class cha chạy trước subclass, nên
     * `_state` (abstract, được khởi tạo ở subclass) vẫn là `null` tại thời điểm
     * parent constructor thực thi. `by lazy` trì hoãn đến lần đầu tiên `state` được
     * đọc — lúc đó subclass đã init xong và `_state` đã có giá trị.
     */
    val state: StateFlow<ViewModelState<T>> by lazy { _state.asStateFlow() }

    // exception handler for coroutine
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        viewModelScope.launch {
            throwable.printStackTrace()
            _state.update { it.copy(statusState = StatusState.Error(throwable.message)) }
        }
    }

    private val viewModelScopeExceptionHandler = viewModelScope + exceptionHandler

    /**
     * Thực thi một suspend action trên [Dispatchers.IO] và trả kết quả về qua callback.
     *
     * Nếu [needBlock] = true, tự động phát [StatusState.Loading] trước khi chạy
     * và reset về [StatusState.None] sau khi xong.
     *
     * @param T Kiểu dữ liệu trả về từ [action].
     * @param needBlock Nếu `true`, phát Loading state trong suốt quá trình thực thi.
     * @param action Suspend lambda chứa logic cần chạy nền (IO-bound).
     * @param callback Lambda nhận kết quả từ [action], chạy trong [viewModelScope].
     */
    fun <T> executeTask(
        needBlock: Boolean = true,
        action: suspend () -> T,
        callback: CoroutineScope.(T) -> Unit = {},
    ) {
        viewModelScopeExceptionHandler.launch {
            if (needBlock) {
                _state.update { it.copy(statusState = StatusState.Loading) }
            }
            withContext(Dispatchers.IO) {
                callback(action())
            }
            if (needBlock) _state.update { it.copy(statusState = StatusState.None) }
        }
    }

    /**
     * Thực thi một network request bọc trong try-catch, tự xử lý exception.
     *
     * Trước khi chạy, gọi [checkNetworkAvailable] để kiểm tra kết nối.
     * Nếu [action] ném exception, in stack trace và reset loading state về None
     * thay vì crash app; [onSuccess] nhận `null` trong trường hợp lỗi.
     *
     * @param T Kiểu dữ liệu trả về từ [action].
     * @param needBlock Nếu `true`, phát Loading state trong suốt quá trình thực thi.
     * @param action Suspend lambda gọi network (thường là UseCase).
     * @param showDialog Điều khiển có hiển thị dialog lỗi mạng hay không (chưa implement).
     * @param onSuccess Lambda nhận `T?` — `null` nếu có exception.
     */
    protected fun <T> executeNetworkTask(
        needBlock: Boolean = true,
        action: suspend () -> Result<T>,
        showDialog: Boolean = true,
        onSuccess: (T?) -> Unit = {},
    ) {
        checkNetworkAvailable(
            showDialog = showDialog
        ) {
            executeTask(needBlock = needBlock, {
                return@executeTask try {
                    action()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    if (needBlock) _state.update { it.copy(statusState = StatusState.None) }
                    null
                }
            }, { result ->
                result?.onSuccess {
                    onSuccess(it)
                }?.onFailure { ex ->
                    _state.update { it.copy(statusState = (StatusState.Error(ex.message ?: "Đã có lỗi xảy ra. Vui lòng thử lại."))) }
                }
            })
        }
    }

    /**
     * Kiểm tra kết nối mạng trước khi thực thi [callback].
     *
     * Hiện tại luôn gọi [callback] ngay (network check chưa implement).
     * Khi tích hợp `networkConnectionUtil`, sẽ phát [CommonState.ErrorNetwork]
     * thay vì gọi callback nếu không có mạng.
     *
     * @param showDialog Nếu `true`, hiển thị dialog báo lỗi mạng khi không có kết nối.
     * @param callback Logic cần chạy khi mạng available.
     */
    fun checkNetworkAvailable(
        showDialog: Boolean = true,
        callback: () -> Unit = {}
    ) {
//        if (networkConnectionUtil.isConnected) {
//            if (showDialog) _commonState.value = CommonState.None
//            callback()
//        } else {
//            if (showDialog) {
//                _commonState.value = CommonState.ErrorNetwork
//            }
//        }
        callback()
    }
}

/**
 * Trạng thái chung dùng để báo hiệu loading/lỗi toàn cục từ [BaseViewModel].
 *
 * - [None]: Idle — không có tác vụ nào đang chạy.
 * - [Loading]: Đang thực thi tác vụ nền.
 * - [Error]: Lỗi khi execute task.
 */
open class StatusState {
    data object None: StatusState()
    data object Loading : StatusState()
    data class Error(val message: String?): StatusState()
}

data class ViewModelState<T>(var statusState: StatusState = StatusState.None, var dataState: T)