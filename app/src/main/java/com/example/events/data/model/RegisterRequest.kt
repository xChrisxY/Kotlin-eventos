package com.example.events.data.model

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val password2: String,
    val firstName: String,
    val lastName: String
)