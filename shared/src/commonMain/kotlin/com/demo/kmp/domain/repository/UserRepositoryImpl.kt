package com.demo.kmp.domain.repository

import com.demo.kmp.data.remote.network.NetworkClient
import com.demo.kmp.domain.model.GitHubUser
import io.ktor.http.HttpMethod

/**
 * Implements [UserRepository] bằng cách gọi GitHub API qua [NetworkClient].
 *
 * Dùng `runCatching` để bật tất cả exception thành [Result.failure],
 * giúp tầng trên (UseCase/ViewModel) xử lý lỗi thống nhất.
 *
 * @param networkClient HTTP client singleton từ Koin.
 */
class UserRepositoryImpl(
    private val networkClient: NetworkClient
) : UserRepository {

    /**
     * Gọi `GET https://api.github.com/users` và parse kết quả thành `List<GitHubUser>`.
     *
     * @return [Result.success] với list users nếu thành công.
     * @return [Result.failure] nếu mạng lỗi hoặc JSON không parse được.
     */
    override suspend fun getUsers(): Result<List<GitHubUser>> {
        return runCatching {
            networkClient.request(
                method = HttpMethod.Get,
                path = "users"
            )
        }
    }
}