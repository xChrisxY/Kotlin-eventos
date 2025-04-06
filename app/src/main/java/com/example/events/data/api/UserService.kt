package com.example.events.data.api

import android.util.Log
import com.example.events.data.model.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class UserService(private val token: String) {
    private val client = OkHttpClient()
    private val baseUrl = "http://192.168.1.93:8000/api/v1"

    suspend fun fetchAllUsers(): List<com.example.events.data.model.User> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/users/") // Reemplaza con tu endpoint de usuarios
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        responseBody?.let { parseUsers(it) } ?: emptyList()
                    } else {
                        Log.e("EventsService", "Error fetching users: ${response.code}")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("EventsService", "Exception fetching users", e)
                emptyList()
            }
        }
    }

    private fun parseUsers(jsonString: String): List<com.example.events.data.model.User> {
        val jsonArray = JSONArray(jsonString)
        return (0 until jsonArray.length()).map { index ->
            val userJson = jsonArray.getJSONObject(index)
            com.example.events.data.model.User(
                id = userJson.getInt("id"),
                username = userJson.getString("username"),
                email = userJson.getString("email"),
                firstName = userJson.getString("first_name"),
                lastName = userJson.getString("last_name")
            )
        }
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