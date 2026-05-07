package com.demo.kmp.di

import com.demo.kmp.data.remote.network.NetworkClient
import com.demo.kmp.domain.repository.UserRepositoryImpl
import com.demo.kmp.domain.repository.UserRepository
import com.demo.kmp.domain.usecase.GetUsersUseCase
import com.demo.kmp.presentation.user.UserListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module chứa toàn bộ dependency dùng chung giữa Android và iOS.
 *
 * Được load tự động trong [initKoin].
 *
 * Dependency graph:
 * ```
 * NetworkClient (singleton)
 *   └── UserRepositoryImpl (singleton) → UserRepository
 *         └── GetUsersUseCase (factory)
 *               └── UserListViewModel (viewModel — Koin quản lý lifecycle)
 *                     └── UserLocalDataSource (platform-specific, inject từ ngoài)
 * ```
 */
val sharedModule = module {
    single {
        NetworkClient()
    }
    singleOf(::UserRepositoryImpl) bind UserRepository::class
    factoryOf(::GetUsersUseCase)
    viewModelOf(::UserListViewModel)
}
