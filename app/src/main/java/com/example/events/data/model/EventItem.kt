package com.example.events.data.model

data class EventItem(
    val id: Int,
    val name: String,
    val responsible: User,
    val status: String,
    val addedAt: String,
    val updatedAt: String
)