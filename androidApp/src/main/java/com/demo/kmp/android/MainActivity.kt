package com.demo.kmp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.demo.kmp.android.ui.userdetail.UserDetailScreen
import com.demo.kmp.android.ui.userlist.UserListScreen
import com.demo.kmp.domain.model.GitHubUser
import com.demo.kmp.presentation.user.UserListViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "userList") {
        composable("userList") {
            val viewModel: UserListViewModel = koinViewModel()
            UserListScreen(
                viewModel = viewModel,
                onNavigateToDetail = { user ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("user", user)
                    navController.navigate("userDetail")
                }
            )
        }
        composable("userDetail") {
            val user = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<GitHubUser>("user")
            if (user != null) {
                UserDetailScreen(
                    user = user,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
