package com.demo.kmp.domain.repository

import com.demo.kmp.domain.model.GitHubUser

/**
 * Repository interface định nghĩa contract lấy dữ liệu user từ remote.
 *
 * Nằm ở domain layer nên hoàn toàn độc lập với Ktor hay bất kỳ HTTP client nào.
 * Implementation duy nhất hiện tại: [UserRepositoryImpl].
 */
interface UserRepository {
    /**
     * Lấy danh sách GitHub users từ remote.
     *
     * @return [Result.success] với list users, hoặc [Result.failure] nếu có lỗi mạng/parse.
     */
    suspend fun getUsers(): Result<List<GitHubUser>>
}
