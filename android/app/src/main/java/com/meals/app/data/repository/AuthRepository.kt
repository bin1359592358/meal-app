package com.meals.app.data.repository

import com.google.gson.Gson
import com.meals.app.data.dto.*
import com.meals.app.data.local.Preferences
import com.meals.app.data.remote.ApiClient
import okhttp3.ResponseBody

object AuthRepository {

    private val api get() = ApiClient.getApiService()
    private val gson = Gson()

    private fun parseErrorBody(body: ResponseBody?): String? {
        return try {
            val json = body?.string() ?: return null
            val map = gson.fromJson(json, Map::class.java)
            map["detail"] as? String ?: map["message"] as? String
        } catch (_: Exception) {
            null
        }
    }

    suspend fun register(
        username: String,
        nickname: String,
        pin: String
    ): Result<AuthResponse> = safeCall {
        val response = api.register(RegisterRequest(username, nickname, pin))
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            saveAuthData(body.data)
            Result.success(body.data)
        } else {
            val errorDetail = parseErrorBody(response.errorBody())
            Result.failure(ApiException(errorDetail ?: body?.message ?: "注册失败"))
        }
    }

    suspend fun login(username: String, pin: String): Result<AuthResponse> = safeCall {
        val response = api.login(LoginRequest(username, pin))
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            saveAuthData(body.data)
            Result.success(body.data)
        } else {
            val errorDetail = parseErrorBody(response.errorBody())
            Result.failure(ApiException(errorDetail ?: body?.message ?: "登录失败"))
        }
    }

    suspend fun changePin(oldPin: String, newPin: String): Result<Unit> = safeCall {
        val response = api.changePin(PinChangeRequest(oldPin, newPin))
        val body = response.body()
        if (response.isSuccessful && body?.code == 0) {
            // Extract new token from response data
            val data = body.data
            if (data is Map<*, *> && data["token"] is String) {
                Preferences.token = data["token"] as String
            }
            Result.success(Unit)
        } else {
            val errorDetail = parseErrorBody(response.errorBody())
            Result.failure(ApiException(errorDetail ?: body?.message ?: "PIN修改失败"))
        }
    }

    suspend fun getMe(): Result<UserDto> = safeCall {
        val response = api.getMe()
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            Preferences.userId = body.data.id
            Preferences.username = body.data.username
            Preferences.nickname = body.data.nickname
            Result.success(body.data)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to get user info"))
        }
    }

    suspend fun updateNickname(nickname: String): Result<UserDto> = safeCall {
        val response = api.updateNickname(NicknameUpdateRequest(nickname))
        val body = response.body()
        if (response.isSuccessful && body?.code == 0 && body.data != null) {
            Preferences.nickname = body.data.nickname
            Result.success(body.data)
        } else {
            Result.failure(ApiException(body?.message ?: "Failed to update nickname"))
        }
    }

    suspend fun logout() {
        // Best-effort server-side logout
        try {
            val token = Preferences.token
            if (!token.isNullOrBlank()) {
                api.logout("Bearer $token")
            }
        } catch (_: Exception) {
            // Ignore server errors - still clear local state
        }
        Preferences.clear()
        ApiClient.reset()
    }

    private fun saveAuthData(authResponse: AuthResponse) {
        Preferences.token = authResponse.token
        Preferences.userId = authResponse.user.id
        Preferences.username = authResponse.user.username
        Preferences.nickname = authResponse.user.nickname
    }

    private inline fun <T> safeCall(block: () -> Result<T>): Result<T> {
        return try {
            block()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ApiException(message: String) : Exception(message)
