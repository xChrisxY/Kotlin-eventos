package com.example.events.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

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

}