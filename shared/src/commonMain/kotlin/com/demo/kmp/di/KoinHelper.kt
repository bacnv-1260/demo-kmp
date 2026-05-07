package com.demo.kmp.di

import com.demo.kmp.presentation.user.UserListViewModel
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

/**
 * Khởi động Koin DI container với các module của shared module.
 *
 * Được gọi một lần duy nhất khi app khởi động:
 * - **Android**: `MainApplication.onCreate()` qua `startKoin { modules(androidAppModule) }`
 * - **iOS**: [initKoinIos] gọi hàm này và inject thêm iOS-specific modules
 *
 * Tự động load [sharedModule] (network, repo, usecase, viewmodel)
 * và [platformModule] (Ktor engine theo platform).
 *
 * @param config Lambda tùy chọn để inject thêm module theo platform.
 * @return [KoinApplication] được cấu hình sẵn sàng.
 */
fun initKoin(config: (KoinApplication.() -> Unit)? = null): KoinApplication {
    return startKoin {
        config?.invoke(this)
        modules(sharedModule, platformModule)
    }
}

/**
 * Bridge để iOS lấy ViewModel từ Koin mà không dùng service locator trực tiếp.
 *
 * iOS không hỗ trợ Koin extension function `koinViewModel()` như Android Compose,
 * nên cần class này để thông qua [KoinComponent] inject an toàn.
 * Được dùng trong [UserListObservable] bên Swift.
 */
class KoinHelper : KoinComponent {
    /**
     * Lấy [UserListViewModel] instance từ Koin container.
     *
     * Koin quản lý lifecycle của ViewModel nhờ `viewModelOf` trong [sharedModule].
     *
     * @return [UserListViewModel] được inject đầy đủ dependency.
     */
    fun getUserListViewModel(): UserListViewModel {
        val viewModel: UserListViewModel by inject()
        return viewModel
    }
}
