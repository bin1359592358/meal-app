package com.meals.app.data.dto

data class UserDto(
    val id: Int,
    val username: String,
    val nickname: String,
    val created_at: String = ""
)

data class NicknameUpdateRequest(
    val nickname: String
)
