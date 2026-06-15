package com.meals.app.data.remote

import com.meals.app.data.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ==================== Auth ====================

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/pin/change")
    suspend fun changePin(@Body request: PinChangeRequest): Response<ApiResponse<Unit>>

    // ==================== Users ====================

    @GET("api/users/me")
    suspend fun getMe(): Response<ApiResponse<UserDto>>

    @PUT("api/users/me")
    suspend fun updateNickname(@Body request: NicknameUpdateRequest): Response<ApiResponse<UserDto>>

    // ==================== Rooms ====================

    @POST("api/rooms")
    suspend fun createRoom(@Body request: RoomCreateRequest): Response<ApiResponse<RoomDto>>

    @POST("api/rooms/join")
    suspend fun joinRoom(@Body request: RoomJoinRequest): Response<ApiResponse<RoomDto>>

    @GET("api/rooms/{id}")
    suspend fun getRoom(@Path("id") id: Int): Response<ApiResponse<RoomDto>>

    @GET("api/rooms/mine")
    suspend fun getMyRooms(): Response<ApiResponse<List<RoomDto>>>

    @DELETE("api/rooms/{id}/leave")
    suspend fun leaveRoom(@Path("id") id: Int): Response<ApiResponse<Unit>>

    @DELETE("api/rooms/{id}/members/{userId}")
    suspend fun removeMember(
        @Path("id") id: Int,
        @Path("userId") userId: Int
    ): Response<ApiResponse<Unit>>

    // ==================== Categories ====================

    @GET("api/rooms/{roomId}/categories")
    suspend fun getCategories(
        @Path("roomId") roomId: Int
    ): Response<ApiResponse<List<CategoryDto>>>

    @POST("api/rooms/{roomId}/categories")
    suspend fun createCategory(
        @Path("roomId") roomId: Int,
        @Body request: CategoryCreateRequest
    ): Response<ApiResponse<CategoryDto>>

    @PUT("api/rooms/{roomId}/categories/{id}")
    suspend fun updateCategory(
        @Path("roomId") roomId: Int,
        @Path("id") id: Int,
        @Body request: CategoryUpdateRequest
    ): Response<ApiResponse<CategoryDto>>

    @DELETE("api/rooms/{roomId}/categories/{id}")
    suspend fun deleteCategory(
        @Path("roomId") roomId: Int,
        @Path("id") id: Int
    ): Response<ApiResponse<Unit>>

    // ==================== Dishes ====================

    @GET("api/rooms/{roomId}/dishes")
    suspend fun getDishes(
        @Path("roomId") roomId: Int,
        @Query("category_id") categoryId: Int? = null,
        @Query("q") query: String? = null
    ): Response<ApiResponse<List<DishDto>>>

    @GET("api/rooms/{roomId}/dishes/{id}")
    suspend fun getDish(
        @Path("roomId") roomId: Int,
        @Path("id") id: Int
    ): Response<ApiResponse<DishDto>>

    @POST("api/rooms/{roomId}/dishes")
    suspend fun createDish(
        @Path("roomId") roomId: Int,
        @Body request: DishCreateRequest
    ): Response<ApiResponse<DishDto>>

    @PUT("api/rooms/{roomId}/dishes/{id}")
    suspend fun updateDish(
        @Path("roomId") roomId: Int,
        @Path("id") id: Int,
        @Body request: DishUpdateRequest
    ): Response<ApiResponse<DishDto>>

    @DELETE("api/rooms/{roomId}/dishes/{id}")
    suspend fun deleteDish(
        @Path("roomId") roomId: Int,
        @Path("id") id: Int
    ): Response<ApiResponse<Unit>>

    @PATCH("api/rooms/{roomId}/dishes/{id}/toggle")
    suspend fun toggleDish(
        @Path("roomId") roomId: Int,
        @Path("id") id: Int
    ): Response<ApiResponse<DishDto>>

    // ==================== Orders ====================

    @GET("api/rooms/{roomId}/orders")
    suspend fun getOrders(
        @Path("roomId") roomId: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<ApiResponse<List<OrderDto>>>

    @GET("api/rooms/{roomId}/orders/{id}")
    suspend fun getOrderDetail(
        @Path("roomId") roomId: Int,
        @Path("id") id: Int
    ): Response<ApiResponse<OrderDto>>

    @POST("api/rooms/{roomId}/orders")
    suspend fun createOrder(
        @Path("roomId") roomId: Int,
        @Body request: OrderCreateRequest
    ): Response<ApiResponse<OrderDto>>

    @PATCH("api/rooms/{roomId}/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("roomId") roomId: Int,
        @Path("id") id: Int,
        @Body request: StatusUpdateRequest
    ): Response<ApiResponse<OrderDto>>

    @GET("api/rooms/{roomId}/orders/summary")
    suspend fun getOrderSummary(
        @Path("roomId") roomId: Int
    ): Response<ApiResponse<OrderSummaryDto>>

    // ==================== Upload ====================

    @Multipart
    @POST("api/upload/image")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<Map<String, String>>>
}
