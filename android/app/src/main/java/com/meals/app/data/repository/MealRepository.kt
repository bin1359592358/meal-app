package com.meals.app.data.repository

import com.meals.app.data.dto.*
import com.meals.app.data.remote.ApiClient

object MealRepository {

    private val api get() = ApiClient.getApiService()

    // ==================== Categories ====================

    suspend fun getCategories(roomId: Int): Result<List<CategoryDto>> = safeCall {
        val response = api.getCategories(roomId)
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            Result.success(body.data)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to get categories"))
        }
    }

    suspend fun createCategory(
        roomId: Int,
        name: String,
        icon: String = "",
        sortOrder: Int = 0
    ): Result<CategoryDto> = safeCall {
        val response = api.createCategory(
            roomId,
            CategoryCreateRequest(name, icon, sortOrder)
        )
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            Result.success(body.data)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to create category"))
        }
    }

    suspend fun updateCategory(
        roomId: Int,
        categoryId: Int,
        name: String? = null,
        icon: String? = null,
        sortOrder: Int? = null
    ): Result<CategoryDto> = safeCall {
        val response = api.updateCategory(
            roomId,
            categoryId,
            CategoryUpdateRequest(name, icon, sortOrder)
        )
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            Result.success(body.data)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to update category"))
        }
    }

    suspend fun deleteCategory(roomId: Int, categoryId: Int): Result<Unit> = safeCall {
        val response = api.deleteCategory(roomId, categoryId)
        val body = response.body()
        if (response.isSuccessful && body?.code == 0) {
            Result.success(Unit)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to delete category"))
        }
    }

    // ==================== Dishes ====================

    suspend fun getDishes(
        roomId: Int,
        categoryId: Int? = null,
        searchQuery: String? = null
    ): Result<List<DishDto>> = safeCall {
        val response = api.getDishes(roomId, categoryId, searchQuery)
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            Result.success(body.data)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to get dishes"))
        }
    }

    suspend fun getDish(roomId: Int, dishId: Int): Result<DishDto> = safeCall {
        val response = api.getDish(roomId, dishId)
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            Result.success(body.data)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to get dish"))
        }
    }

    suspend fun createDish(
        roomId: Int,
        request: DishCreateRequest
    ): Result<DishDto> = safeCall {
        val response = api.createDish(roomId, request)
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            Result.success(body.data)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to create dish"))
        }
    }

    suspend fun updateDish(
        roomId: Int,
        dishId: Int,
        request: DishUpdateRequest
    ): Result<DishDto> = safeCall {
        val response = api.updateDish(roomId, dishId, request)
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            Result.success(body.data)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to update dish"))
        }
    }

    suspend fun deleteDish(roomId: Int, dishId: Int): Result<Unit> = safeCall {
        val response = api.deleteDish(roomId, dishId)
        val body = response.body()
        if (response.isSuccessful && body?.code == 0) {
            Result.success(Unit)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to delete dish"))
        }
    }

    suspend fun toggleDish(roomId: Int, dishId: Int): Result<DishDto> = safeCall {
        val response = api.toggleDish(roomId, dishId)
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            Result.success(body.data)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to toggle dish"))
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
