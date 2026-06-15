package com.meals.app.data.dto

data class DishCreateRequest(
    val category_id: Int,
    val name: String,
    val description: String = "",
    val price: Double,
    val image_url: String? = null,
    val is_available: Boolean = true,
    val tags: List<String> = emptyList(),
    val seasonings: List<Map<String, Any>>? = null,
    val sort_order: Int = 0
)

data class DishUpdateRequest(
    val category_id: Int? = null,
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val image_url: String? = null,
    val tags: List<String>? = null,
    val seasonings: List<Map<String, Any>>? = null,
    val sort_order: Int? = null
)

data class DishDto(
    val id: Int,
    val category_id: Int,
    val room_id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val image_url: String? = null,
    val is_available: Boolean = true,
    val tags: List<String> = emptyList(),
    val seasonings: List<Map<String, Any>> = emptyList(),
    val sort_order: Int = 0
)
