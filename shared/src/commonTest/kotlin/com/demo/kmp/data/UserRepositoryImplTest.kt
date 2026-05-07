package com.demo.kmp.data

import com.demo.kmp.data.remote.dto.GitHubUserDto
import com.demo.kmp.domain.repository.UserRepositoryImpl
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserRepositoryImplTest {

    private val successDto = GitHubUserDto(
        id = 101,
        login = "testuser",
        avatarUrl = "https://avatars.githubusercontent.com/u/101",
        htmlUrl = "https://github.com/testuser",
        type = "User"
    )

    @Test
    fun `getUsers returns mapped domain models on success`() = runTest {
        val fakeService = FakeGitHubApiService(listOf(successDto))
        val repository = UserRepositoryImpl(fakeService)

        val result = repository.getUsers()

        assertTrue(result.isSuccess)
        val users = result.getOrThrow()
        assertEquals(1, users.size)
        assertEquals("testuser", users[0].login)
        assertEquals(101, users[0].id)
        assertEquals("https://avatars.githubusercontent.com/u/101", users[0].avatarUrl)
        assertEquals("User", users[0].type)
    }

    @Test
    fun `getUsers returns failure when service throws`() = runTest {
        val fakeService = FakeGitHubApiService(error = RuntimeException("Network error"))
        val repository = UserRepositoryImpl(fakeService)

        val result = repository.getUsers()

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getUsers returns empty list when service returns empty`() = runTest {
        val fakeService = FakeGitHubApiService(emptyList())
        val repository = UserRepositoryImpl(fakeService)

        val result = repository.getUsers()

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().size)
    }
}

private class FakeGitHubApiService(
    private val response: List<GitHubUserDto> = emptyList(),
    private val error: Exception? = null
) : GitHubApiServiceInterface {
    override suspend fun getUsers(): List<GitHubUserDto> {
        if (error != null) throw error
        return response
    }
}
