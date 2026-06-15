package com.meals.app.data.dto

data class CategoryCreateRequest(
    val name: String,
    val icon: String = "",
    val sort_order: Int = 0
)

data class CategoryUpdateRequest(
    val name: String? = null,
    val icon: String? = null,
    val sort_order: Int? = null
)

data class CategoryDto(
    val id: Int,
    val room_id: Int,
    val name: String,
    val icon: String,
    val sort_order: Int,
    val dish_count: Int = 0
)
