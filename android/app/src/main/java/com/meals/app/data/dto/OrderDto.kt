package com.meals.app.data.dto

data class OrderItemRequest(
    val dish_id: Int,
    val quantity: Int,
    val seasonings: Map<String, Any>? = null
)

data class OrderCreateRequest(
    val items: List<OrderItemRequest>,
    val note: String = "",
    val people_count: Int = 1
)

data class OrderItemDto(
    val id: Int,
    val dish_id: Int,
    val dish_name: String,
    val dish_price: Double,
    val quantity: Int,
    val seasoning_text: String = "",
    val subtotal: Double = 0.0
)

data class OrderDto(
    val id: Int,
    val room_id: Int,
    val user_id: Int,
    val user_nickname: String = "",
    val total_price: Double,
    val status: String,
    val note: String = "",
    val people_count: Int = 1,
    val created_at: String = "",
    val items: List<OrderItemDto>? = null
)

data class OrderSummaryItemDto(
    val dish_name: String,
    val total_quantity: Int,
    val order_count: Int,
    val seasonings_list: List<String> = emptyList()
)

data class OrderSummaryDto(
    val summary: List<OrderSummaryItemDto>,
    val total_orders: Int,
    val total_price: Double
)

data class StatusUpdateRequest(
    val status: String
)
