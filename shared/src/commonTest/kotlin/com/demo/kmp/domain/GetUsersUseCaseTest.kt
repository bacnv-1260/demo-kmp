package com.demo.kmp.domain

import com.demo.kmp.domain.model.GitHubUser
import com.demo.kmp.domain.repository.UserRepository
import com.demo.kmp.domain.usecase.GetUsersUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetUsersUseCaseTest {

    private val sampleUser = GitHubUser(
        id = 1,
        login = "octocat",
        avatarUrl = "https://avatars.githubusercontent.com/u/1",
        htmlUrl = "https://github.com/octocat",
        type = "User"
    )

    @Test
    fun `invoke returns success from repository`() = runTest {
        val fakeRepo = FakeUserRepository(Result.success(listOf(sampleUser)))
        val useCase = GetUsersUseCase(fakeRepo)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(listOf(sampleUser), result.getOrThrow())
    }

    @Test
    fun `invoke forwards failure from repository`() = runTest {
        val error = RuntimeException("Timeout")
        val fakeRepo = FakeUserRepository(Result.failure(error))
        val useCase = GetUsersUseCase(fakeRepo)

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("Timeout", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke returns empty list when repository returns empty`() = runTest {
        val fakeRepo = FakeUserRepository(Result.success(emptyList()))
        val useCase = GetUsersUseCase(fakeRepo)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrThrow())
    }
}

private class FakeUserRepository(
    private val response: Result<List<GitHubUser>>
) : UserRepository {
    override suspend fun getUsers(): Result<List<GitHubUser>> = response
}
