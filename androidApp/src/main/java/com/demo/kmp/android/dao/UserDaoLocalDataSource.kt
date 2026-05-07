package com.demo.kmp.android.dao

import com.demo.kmp.platform.UserLocalDataSource
import com.demo.kmp.domain.model.GitHubUser

/**
 * Android implementation của [UserLocalDataSource].
 *
 * Đóng vai trò bridge giữa native Android [UserDao] và KMP interface.
 * Map model Android-specific [User][com.demo.kmp.android.dao.User] sang
 * KMP model [GitHubUser] để shared module không phụ thuộc vào Android.
 *
 * Được registered vào Koin trong `MainApplication.androidAppModule`.
 *
 * @param userDao Nguồn dữ liệu local của Android (hiện tại: hardcoded list, sẵn sàng thay bằng Room).
 */
class UserDaoLocalDataSource(private val userDao: UserDao) : UserLocalDataSource {
    /**
     * Lấy danh sách users từ [UserDao] và map sang [GitHubUser].
     *
     * Chỉ map các field cần thiết: id, login, avatarUrl, htmlUrl, type.
     *
     * @return List [GitHubUser] tương ứng với data từ native DAO.
     */
    override fun getAllUser(): List<GitHubUser> {
        return userDao.getAllUser().map { GitHubUser(
            id = it.id,
            login = it.login,
            avatarUrl = it.avatarUrl,
            htmlUrl = it.htmlUrl,
            type = it.type
        ) }
    }
}
