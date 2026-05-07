package com.demo.kmp.android.ui.userlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demo.kmp.domain.model.GitHubUser
import com.demo.kmp.presentation.StatusState
import com.demo.kmp.presentation.user.UserListEffect
import com.demo.kmp.presentation.user.UserListIntent
import com.demo.kmp.presentation.user.UserListViewModel

@Composable
fun UserListScreen(
    viewModel: UserListViewModel,
    onNavigateToDetail: (GitHubUser) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is UserListEffect.NavigateToDetail ->
                    onNavigateToDetail(effect.user)
            }
        }
    }
    when (val currentState = state.statusState) {
        is StatusState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is StatusState.None -> {
            if (state.dataState.users.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không có user nào.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.dataState.users, key = { it.id }) { user ->
                        UserItem(
                            user = user,
                            onClick = { viewModel.processIntent(UserListIntent.SelectUser(user)) }
                        )
                    }
                }
            }
        }

        is StatusState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentState.message.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.processIntent(UserListIntent.LoadUsers) }) {
                    Text("Thử lại")
                }
            }
        }
    }
}
