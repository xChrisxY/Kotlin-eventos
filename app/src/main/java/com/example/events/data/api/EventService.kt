package com.example.events.data.api

import android.util.Log
import com.example.events.data.model.Event
import com.example.events.data.model.EventImage
import com.example.events.data.model.EventItem
import com.example.events.data.model.ItemList
import com.example.events.data.model.AudioNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class EventsService(private val token: String) {
    private val client = OkHttpClient()
    private val baseUrl = "http://10.0.2.2:8000/api/v1" // Ajusta a tu URL base

    suspend fun fetchEvents(): List<Event> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/events/")
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        responseBody?.let { parseEvents(it) } ?: emptyList()
                    } else {
                        Log.e("EventsService", "Error fetching events: ${response.code}")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("EventsService", "Exception fetching events", e)
                emptyList()
            }
        }
    }

    private fun parseEvents(jsonString: String): List<Event> {
        val jsonArray = JSONArray(jsonString)
        return (0 until jsonArray.length()).map { index ->
            val eventJson = jsonArray.getJSONObject(index)
            parseEvent(eventJson)
        }
    }

    private fun parseEvent(json: JSONObject): Event {
        return Event(
            id = json.getInt("id"),
            name = json.getString("name"),
            description = json.getString("description"),
            location = json.getString("location"),
            date = json.getString("date"),
            time = json.getString("time"),
            createdAt = json.getString("created_at"),
            updatedAt = json.getString("updated_at"),
            organizer = parseUser(json.getJSONObject("organizer")),
            participants = parseUserList(json.getJSONArray("participants")),
            images = parseImageList(json.getJSONArray("images")),
            audioNotes = parseAudioNoteList(json.getJSONArray("audio_notes")),
            itemList = parseItemList(json.getJSONObject("item_list"))
        )
    }

    // Métodos auxiliares para parsear objetos anidados (implementa según tu modelo)
    private fun parseUser(json: JSONObject) = com.example.events.data.model.User(
        id = json.getInt("id"),
        username = json.getString("username"),
        email = json.getString("email"),
        firstName = json.getString("first_name"),
        lastName = json.getString("last_name")
    )

    private fun parseUserList(jsonArray: JSONArray): List<com.example.events.data.model.User> {
        return (0 until jsonArray.length()).map { index ->
            parseUser(jsonArray.getJSONObject(index))
        }
    }

    private fun parseImageList(jsonArray: JSONArray): List<EventImage> {
        return (0 until jsonArray.length()).map { index ->
            val imageJson = jsonArray.getJSONObject(index)
            EventImage(
                id = imageJson.getInt("id"),
                image = imageJson.getString("image"),
                caption = imageJson.optString("caption", ""),
                uploadedAt = imageJson.getString("uploaded_at")
            )
        }
    }

    private fun parseAudioNoteList(jsonArray: JSONArray): List<AudioNote> {
        println("A continucación los audios de los eventos")
        println(jsonArray)
        return (0 until jsonArray.length()).map { index ->
            val audioJson = jsonArray.getJSONObject(index)
            AudioNote(
                id = audioJson.getInt("id"),
                audioFile = audioJson.getString("audio_file"),
                title = audioJson.optString("title", ""),
                recordedAt = audioJson.getString("recorded_at")
            )
        }
    }

    private fun parseItemList(json: JSONObject): ItemList {
        println("A continuación el json de los items")
        println(json)
        return ItemList(
            id = json.getInt("id"),
            title = json.getString("title"),
            description = json.optString("description", ""),
            items = parseEventItemList(json.getJSONArray("items"))
        )
    }

    private fun parseEventItemList(jsonArray: JSONArray): List<EventItem> {
        println("A continuación cada item")
        println(jsonArray)
        return (0 until jsonArray.length()).map { index ->
            val itemJson = jsonArray.getJSONObject(index)
            EventItem(
                id = itemJson.getInt("id"),
                name = itemJson.getString("name"),
                responsible = parseUser(itemJson.getJSONObject("responsible")),
                status = itemJson.getString("status"),
                addedAt = itemJson.getString("added_at"),
                updatedAt = itemJson.getString("updated_at")
            )
        }
    }
}