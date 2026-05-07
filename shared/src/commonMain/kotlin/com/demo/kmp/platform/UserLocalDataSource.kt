package com.demo.kmp.platform

import com.demo.kmp.domain.model.GitHubUser

/**
 * Interface định nghĩa contract truy cập dữ liệu user cục bộ (local).
 *
 * Mỗi platform cung cấp implementation riêng:
 * - **Android**: [UserDaoLocalDataSource][com.demo.kmp.android.dao.UserDaoLocalDataSource]
 *   wrap Room DAO (hoặc hardcoded list hiện tại)
 * - **iOS**: `UserDaoLocalDataSource` (Swift) wrap native `UserDao`
 *
 * Registered vào Koin:
 * - Android: trong `androidAppModule` tại `MainApplication`
 * - iOS: trong [initKoinIos][com.demo.kmp.di.initKoinIos] khi khởi động app
 */
interface UserLocalDataSource {
    /**
     * Lấy toàn bộ danh sách users từ local storage.
     *
     * Hàm blocking (không suspend) vì được gọi trong [executeTask] trên IO dispatcher.
     *
     * @return Danh sách [GitHubUser], rỗng nếu không có dữ liệu.
     */
    fun getAllUser(): List<GitHubUser>
}