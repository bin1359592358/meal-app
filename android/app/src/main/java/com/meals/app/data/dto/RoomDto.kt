package com.meals.app.data.dto

data class RoomCreateRequest(
    val name: String
)

data class RoomJoinRequest(
    val code: String
)

data class RoomDto(
    val id: Int,
    val name: String,
    val code: String,
    val chef_id: Int,
    val is_active: Boolean = true,
    val created_at: String = "",
    val members: List<MemberDto>? = null
)

data class MemberDto(
    val id: Int,
    val user_id: Int,
    val username: String,
    val nickname: String,
    val role: String,
    val joined_at: String = ""
)
