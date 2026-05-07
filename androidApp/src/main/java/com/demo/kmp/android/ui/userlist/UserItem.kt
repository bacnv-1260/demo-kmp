package com.demo.kmp.android.ui.userlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.demo.kmp.domain.model.GitHubUser

@Composable
fun UserItem(user: GitHubUser, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(user.login, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(user.type, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "${user.login} avatar",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        }
    )
    HorizontalDivider()
}
