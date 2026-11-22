package com.pulsedesk.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Kết quả trả về sau khi xác thực hoặc đăng ký qua Backend API.
 */
data class AuthResult(
    val success: Boolean,
    val email: String? = null,
    val role: String? = null,
    val fullName: String? = null,
    val errorMessage: String? = null
)

/**
 * Client kết nối REST API của Spring Boot Server (kết nối trực tiếp tới MongoDB Atlas).
 */
object PulseAuthApiClient {
    private const val BASE_URL = "http://localhost:8080/api/auth"

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build()

    /**
     * Xác thực đăng nhập với MongoDB Atlas qua Backend Server.
     */
    suspend fun login(identifier: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("identifier", identifier)
                put("password", password)
            }
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$BASE_URL/login"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(6))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val resJson = JSONObject(response.body())

            if (response.statusCode() == 200 && resJson.optString("status") == "SUCCESS") {
                AuthResult(
                    success = true,
                    email = resJson.optString("email", identifier),
                    role = resJson.optString("roleTitle", "Staff Member"),
                    fullName = resJson.optString("fullName")
                )
            } else {
                AuthResult(
                    success = false,
                    errorMessage = resJson.optString("error", "Email hoặc mật khẩu không chính xác!")
                )
            }
        } catch (e: Exception) {
            // Khi Backend Spring Boot chưa bật
            AuthResult(
                success = false,
                errorMessage = "Chưa bật Backend Server (http://localhost:8080). Vui lòng chạy `./gradlew :server:bootRun` để kết nối MongoDB Atlas!"
            )
        }
    }

    /**
     * Đăng ký tài khoản người dùng mới và lưu trực tiếp vào MongoDB Atlas.
     */
    suspend fun register(
        fullName: String,
        email: String,
        staffId: String,
        password: String,
        role: String
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("fullName", fullName)
                put("email", email)
                put("staffId", staffId)
                put("password", password)
                put("role", role)
            }
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$BASE_URL/register"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(6))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val resJson = JSONObject(response.body())

            if (response.statusCode() == 200 && resJson.optString("status") == "SUCCESS") {
                AuthResult(
                    success = true,
                    email = resJson.optString("email", email),
                    role = resJson.optString("roleTitle", role),
                    fullName = resJson.optString("fullName", fullName)
                )
            } else {
                AuthResult(
                    success = false,
                    errorMessage = resJson.optString("error", "Đăng ký thất bại!")
                )
            }
        } catch (e: Exception) {
            AuthResult(
                success = false,
                errorMessage = "Chưa bật Backend Server (http://localhost:8080). Vui lòng chạy `./gradlew :server:bootRun` để lưu người dùng vào MongoDB Atlas!"
            )
        }
    }
}
