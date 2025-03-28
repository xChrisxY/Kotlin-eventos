package com.example.events.data.model

class Event(
    val id: Int,
    val name: String,
    val description: String,
    val location: String,
    val date: String,
    val time: String,
    val createdAt: String,
    val updatedAt: String,
    val organizer: User,
    val participants: List<User>,
    val images: List<EventImage>,
    val audioNotes: List<AudioNote>,
    val itemList: ItemList
)

