package com.demo.kmp.presentation

import com.demo.kmp.domain.model.GitHubUser
import com.demo.kmp.domain.repository.UserRepository
import com.demo.kmp.domain.usecase.GetUsersUseCase
import com.demo.kmp.presentation.user.UserListEffect
import com.demo.kmp.presentation.user.UserListIntent
import com.demo.kmp.presentation.user.UserListState
import com.demo.kmp.presentation.user.UserListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class UserListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleUsers = listOf(
        GitHubUser(1, "octocat", "https://avatars.githubusercontent.com/u/1", "https://github.com/octocat", "User"),
        GitHubUser(2, "defunkt", "https://avatars.githubusercontent.com/u/2", "https://github.com/defunkt", "User")
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading then transitions to Success`() = runTest {
        val useCase = GetUsersUseCase(FakeUserRepository(Result.success(sampleUsers)))
        val viewModel = UserListViewModel(useCase)

        assertEquals(UserListState.Loading, viewModel.state.value)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<UserListState.Success>(state)
        assertEquals(2, state.users.size)
        assertEquals("octocat", state.users[0].login)
    }

    @Test
    fun `transitions to Error when use case fails`() = runTest {
        val useCase = GetUsersUseCase(FakeUserRepository(Result.failure(RuntimeException("No network"))))
        val viewModel = UserListViewModel(useCase)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<UserListState.Error>(state)
        assertEquals("No network", state.message)
    }

    @Test
    fun `retry reloads users after error`() = runTest {
        var callCount = 0
        val repo = object : UserRepository {
            override suspend fun getUsers(): Result<List<GitHubUser>> {
                callCount++
                return if (callCount == 1) Result.failure(RuntimeException("First fail"))
                else Result.success(sampleUsers)
            }
        }
        val useCase = GetUsersUseCase(repo)
        val viewModel = UserListViewModel(useCase)

        advanceUntilIdle()
        assertIs<UserListState.Error>(viewModel.state.value)

        viewModel.processIntent(UserListIntent.LoadUsers)
        advanceUntilIdle()

        assertIs<UserListState.Success>(viewModel.state.value)
        assertEquals(2, callCount)
    }

    @Test
    fun `SelectUser intent emits NavigateToDetail effect`() = runTest {
        val useCase = GetUsersUseCase(FakeUserRepository(Result.success(sampleUsers)))
        val viewModel = UserListViewModel(useCase)
        advanceUntilIdle()

        val user = sampleUsers[0]
        var receivedEffect: UserListEffect? = null

        val job = kotlinx.coroutines.launch {
            viewModel.effect.collect { receivedEffect = it }
        }

        viewModel.processIntent(UserListIntent.SelectUser(user))
        advanceUntilIdle()

        assertIs<UserListEffect.NavigateToDetail>(receivedEffect)
        assertEquals(user.login, (receivedEffect as UserListEffect.NavigateToDetail).user.login)

        job.cancel()
    }
}

private class FakeUserRepository(
    private val response: Result<List<GitHubUser>>
) : UserRepository {
    override suspend fun getUsers(): Result<List<GitHubUser>> = response
}
