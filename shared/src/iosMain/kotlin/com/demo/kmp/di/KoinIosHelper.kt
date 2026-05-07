package com.demo.kmp.di

import com.demo.kmp.platform.UserLocalDataSource
import org.koin.core.KoinApplication
import org.koin.dsl.module

/**
 * Khởi động Koin cho iOS, inject [UserLocalDataSource] platform-specific.
 *
 * Gọi từ `iOSApp.swift` khi app khởi động:
 * ```swift
 * KoinIosHelperKt.doInitKoinIos(userLocalDataSource: UserDaoLocalDataSource())
 * ```
 *
 * Pattern Dependency Inversion:
 * - KMP interface [UserLocalDataSource] được định nghĩa trong `commonMain`
 * - Swift `UserDaoLocalDataSource` implements interface này bên iOS
 * - Hàm này nhận instance đó và register vào Koin
 * - Nhờ đó [UserListViewModel] không cần biết được chạy trên platform nào
 *
 * @param userLocalDataSource Implementation từ iOS (Swift class implements KMP interface).
 * @return [KoinApplication] đã khởi động với đầy đủ dependency.
 */
fun initKoinIos(userLocalDataSource: UserLocalDataSource): KoinApplication {
    return initKoin {
        modules(
            module {
                single<UserLocalDataSource> { userLocalDataSource }
            }
        )
    }
}
