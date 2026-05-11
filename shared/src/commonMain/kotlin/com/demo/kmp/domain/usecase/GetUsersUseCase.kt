package com.demo.kmp.domain.usecase

import com.demo.kmp.domain.model.GitHubUser
import com.demo.kmp.domain.repository.UserRepository

/**
 * Use case lấy danh sách GitHub users từ remote.
 *
 * Tại thời điểm này chỉ đơn giản delegate tới [UserRepository.getUsers].
 * Vị trí lý tưởng để thêm business logic sau: filter, sort, pagination…
 *
 * @param repository Nguồn dữ liệu remote, inject qua Koin.
 */
class GetUsersUseCase(private val repository: UserRepository) : UseCase<Result<List<GitHubUser>>, UseCase.None>() {
    /**
     * Thực thi use case.
     *
     * @return [Result] chứa `List<GitHubUser>` hoặc error.
     */
    override suspend fun run(params: None): Result<List<GitHubUser>> {
        return repository.getUsers()
    }
}
