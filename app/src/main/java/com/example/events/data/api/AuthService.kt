package com.example.events.data.api

import com.example.events.data.model.LoginRequest
import com.example.events.data.model.LoginResponse
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
}