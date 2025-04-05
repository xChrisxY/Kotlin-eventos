package com.example.events.data.model

data class ItemList(
    val id: Int,
    val title: String,
    val description: String? = null,
    val items: List<EventItem>
)

