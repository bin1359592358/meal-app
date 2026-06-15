package com.meals.app.data.dto

data class ApiResponse<T>(
    val code: Int = 0,
    val data: T? = null,
    val message: String = "ok"
)
