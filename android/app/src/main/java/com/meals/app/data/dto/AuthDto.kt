package com.meals.app.data.dto

data class RegisterRequest(
    val username: String,
    val nickname: String,
    val pin: String
)

data class LoginRequest(
    val username: String,
    val pin: String
)

data class PinChangeRequest(
    val old_pin: String,
    val new_pin: String
)

data class AuthResponse(
    val user: UserDto,
    val token: String
)
