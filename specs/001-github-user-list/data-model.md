# Data Model: GitHub User List

**Phase 1 Output** | **Feature**: 001-github-user-list | **Date**: 2026-05-06

---

## Domain Entities (`commonMain/domain/model/`)

### GitHubUser

Entity thuần domain — không có annotation framework, không phụ thuộc Ktor hay serialization.

```kotlin
data class GitHubUser(
    val id: Int,
    val login: String,
    val avatarUrl: String,
    val htmlUrl: String,
    val type: String        // "User" | "Organization"
)
```

---

## Data Transfer Objects (`commonMain/data/remote/dto/`)

### GitHubUserDto

DTO map 1:1 với JSON response từ GitHub API. Dùng `@Serializable` và `@SerialName`
để xử lý snake_case → camelCase.

```kotlin
@Serializable
data class GitHubUserDto(
    val id: Int,
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String,
    @SerialName("html_url") val htmlUrl: String,
    val type: String
)

fun GitHubUserDto.toDomain(): GitHubUser = GitHubUser(
    id = id,
    login = login,
    avatarUrl = avatarUrl,
    htmlUrl = htmlUrl,
    type = type
)
```

---

## MVI State & Effect (`commonMain/presentation/`)

### UserListState

```kotlin
sealed class UserListState {
    data object Loading : UserListState()
    data class Success(val users: List<GitHubUser>) : UserListState()
    data class Error(val message: String) : UserListState()
}
```

### UserListIntent

```kotlin
sealed class UserListIntent {
    data object LoadUsers : UserListIntent()
    data class SelectUser(val user: GitHubUser) : UserListIntent()
}
```

### UserListEffect

```kotlin
sealed class UserListEffect {
    data class NavigateToDetail(val user: GitHubUser) : UserListEffect()
}
```

---

## Repository Contract (`commonMain/domain/repository/`)

```kotlin
interface UserRepository {
    suspend fun getUsers(): Result<List<GitHubUser>>
}
```

---

## Use Case (`commonMain/domain/usecase/`)

```kotlin
class GetUsersUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(): Result<List<GitHubUser>> = repository.getUsers()
}
```

---

## Entity Relationships

```
GitHub API Response (JSON)
        │
        ▼
  GitHubUserDto  ──toDomain()──►  GitHubUser
                                       │
                         ┌─────────────┤
                         │             │
                  UserListState    UserListEffect
                  (Loading /      (NavigateToDetail)
                   Success /
                   Error)
                         │
                    ViewModel
                    StateFlow
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
      Android UI               iOS UI
   (Compose/collect)      (SwiftUI/.task{})
```

---

## State Transitions

```
Initial ──LoadUsers──► Loading ──API OK──► Success(users)
                           │
                           └──API Error──► Error(message)
Error ──LoadUsers──► Loading (retry)
```
