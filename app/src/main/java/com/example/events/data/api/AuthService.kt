package com.example.events.data.api

import com.example.events.data.model.LoginRequest
import com.example.events.data.model.LoginResponse
import com.example.events.data.model.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AuthService {
    private val client = OkHttpClient()
    private val baseUrl = "http://192.168.1.93:8000/api/v1"

    suspend fun login(username: String, password: String): LoginResponse? {
        val loginRequest = LoginRequest(username, password)
        val jsonBody = JSONObject().apply {
            put("username", loginRequest.username)
            put("password", loginRequest.password)
        }.toString()

        println("Pasando por aquí.")
        val request = Request.Builder()
            .url("$baseUrl/token/")
            .post(jsonBody.toRequestBody())
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    responseBody?.let {
                        val jsonResponse = JSONObject(it)
                        return LoginResponse(
                            accessToken = jsonResponse.getString("access"),
                            refreshToken = jsonResponse.getString("refresh")
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    suspend fun registerUser(authService: AuthService, registerRequest: RegisterRequest): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("username", registerRequest.username)
                    put("email", registerRequest.email)
                    put("password", registerRequest.password)
                    put("password2", registerRequest.password2)
                    put("first_name", registerRequest.firstName)
                    put("last_name", registerRequest.lastName)
                }.toString()

                val request = Request.Builder()
                    .url("http://192.168.1.93:8000/api/v1/users/register/")
                    .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                val client = OkHttpClient()
                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}