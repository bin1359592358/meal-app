package com.meals.app.data.repository

import com.meals.app.data.dto.*
import com.meals.app.data.remote.ApiClient

object OrderRepository {

    private val api get() = ApiClient.getApiService()

    suspend fun getOrders(
        roomId: Int,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<List<OrderDto>> = safeCall {
        val response = api.getOrders(roomId, page, pageSize)
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            Result.success(body.data)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to get orders"))
        }
    }

    suspend fun getOrderDetail(roomId: Int, orderId: Int): Result<OrderDto> = safeCall {
        val response = api.getOrderDetail(roomId, orderId)
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            Result.success(body.data)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to get order detail"))
        }
    }

    suspend fun createOrder(
        roomId: Int,
        items: List<OrderItemRequest>,
        note: String = "",
        peopleCount: Int = 1
    ): Result<OrderDto> = safeCall {
        val response = api.createOrder(
            roomId,
            OrderCreateRequest(items, note, peopleCount)
        )
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            Result.success(body.data)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to create order"))
        }
    }

    suspend fun updateOrderStatus(
        roomId: Int,
        orderId: Int,
        status: String
    ): Result<OrderDto> = safeCall {
        val response = api.updateOrderStatus(roomId, orderId, StatusUpdateRequest(status))
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            Result.success(body.data)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to update order status"))
        }
    }

    suspend fun getSummary(roomId: Int): Result<OrderSummaryDto> = safeCall {
        val response = api.getOrderSummary(roomId)
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            Result.success(body.data)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to get order summary"))
        }
    }

    private inline fun <T> safeCall(block: () -> Result<T>): Result<T> {
        return try {
            block()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
