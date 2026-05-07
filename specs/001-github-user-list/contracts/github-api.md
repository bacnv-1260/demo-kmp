# API Contract: GitHub Users

**Phase 1 Output** | **Feature**: 001-github-user-list | **Date**: 2026-05-06

---

## Endpoint

| Field | Value |
|---|---|
| Method | `GET` |
| URL | `https://api.github.com/users` |
| Query params | `since=100`, `per_page=50` |
| Authentication | None (public endpoint) |
| Rate limit | 60 req/giờ (unauthenticated) |
| Content-Type | `application/json` |

---

## Request

```
GET https://api.github.com/users?since=100&per_page=50
Accept: application/json
```

Không có request body, không có header bắt buộc.

---

## Response — 200 OK

```json
[
  {
    "login": "pjhyett",
    "id": 101,
    "node_id": "MDQ6VXNlcjEwMQ==",
    "avatar_url": "https://avatars.githubusercontent.com/u/101?v=4",
    "gravatar_id": "",
    "url": "https://api.github.com/users/pjhyett",
    "html_url": "https://github.com/pjhyett",
    "type": "User",
    "site_admin": false
  }
]
```

**Fields được sử dụng**:

| JSON field | Kotlin DTO field | Type | Mô tả |
|---|---|---|---|
| `id` | `id` | `Int` | ID duy nhất của user |
| `login` | `login` | `String` | Tên đăng nhập GitHub |
| `avatar_url` | `avatarUrl` | `String` | URL ảnh đại diện |
| `html_url` | `htmlUrl` | `String` | URL trang cá nhân GitHub |
| `type` | `type` | `String` | `"User"` hoặc `"Organization"` |

**Fields bỏ qua**: `node_id`, `gravatar_id`, `url`, `site_admin`, v.v.

---

## Response — Lỗi

| HTTP Status | Tình huống | Xử lý |
|---|---|---|
| 403 Forbidden | Rate limit vượt quá | Hiển thị thông báo lỗi |
| 404 Not Found | Endpoint sai | Hiển thị thông báo lỗi |
| 5xx Server Error | GitHub gặp sự cố | Hiển thị thông báo lỗi |
| Network error | Không có mạng | Hiển thị thông báo lỗi |

---

## Ktor Client Implementation Contract

Shared code trong `commonMain` PHẢI:
1. Khai báo `HttpClient` với `ContentNegotiation` plugin và `kotlinx.serialization`
2. Gọi `client.get(url).body<List<GitHubUserDto>>()`
3. Bắt `Exception` và wrap vào `Result.failure`
4. Map `List<GitHubUserDto>` → `List<GitHubUser>` qua `.toDomain()`
5. Trả về `Result<List<GitHubUser>>` cho use case
