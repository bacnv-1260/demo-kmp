package com.demo.kmp.data.remote.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * KMP HTTP client wrapper dùng Ktor.
 *
 * Cấu hình sẵn:
 * - Base URL: `https://api.github.com/`
 * - Default headers: `Accept: application/vnd.github+json`, `User-Agent: demo-kmp-app`
 * - Timeout: 20 giây
 * - JSON deserialization với `ignoreUnknownKeys = true`
 * - Logging toàn bộ request/response ra console qua `[Ktor]` prefix
 *
 * Được inject bởi Koin dưới dạng singleton trong [sharedModule].
 */
class NetworkClient {

    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Resources)
        install(HttpTimeout) {
            requestTimeoutMillis = 1000 * 20
        }
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    println("[Ktor] $message")
                }
            }
        }
        defaultRequest {
            url("https://api.github.com/")
            headers {
                append("Accept", "application/vnd.github+json")
                append("User-Agent", "demo-kmp-app")
            }
        }
    }

    /**
     * Thực hiện HTTP request và deserialize response body thành kiểu [T].
     *
     * Là hàm `suspend` + `inline reified` nên có thể gọi trực tiếp với type parameter
     * mà không cần truyền class thủ công.
     *
     * @param T Kiểu dữ liệu mong muốn nhận về (được deserialize tự động qua Ktor ContentNegotiation).
     * @param method HTTP method (GET, POST, PUT, DELETE, PATCH…).
     * @param path Đường dẫn API tương đối, ví dụ `"users"` → `https://api.github.com/users`.
     * @param block Lambda tùy chọn để cấu hình thêm request (headers, body, query params…).
     * @return Đối tượng kiểu [T] được parse từ JSON response.
     * @throws io.ktor.client.plugins.ResponseException Khi server trả về HTTP error.
     * @throws kotlinx.serialization.SerializationException Khi JSON không khớp với kiểu [T].
     */
    suspend inline fun <reified T> request(
        method: HttpMethod,
        path: String,
        crossinline block: HttpRequestBuilder.() -> Unit = {},
    ): T = client.request {
        this.method = method
        url(path)
        block()
    }.body()
}
