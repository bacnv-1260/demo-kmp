package com.demo.kmp.android.ui.userdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.demo.kmp.domain.model.GitHubUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(user: GitHubUser, onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user.login) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "${user.login} avatar",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            )
            Text(user.login, style = MaterialTheme.typography.headlineMedium)
            AssistChip(
                onClick = {},
                label = { Text(user.type) }
            )
            Button(onClick = { uriHandler.openUri(user.htmlUrl) }) {
                Text("Xem trang GitHub")
            }
        }
    }
}
