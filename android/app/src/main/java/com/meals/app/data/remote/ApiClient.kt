package com.meals.app.data.remote

import com.meals.app.data.local.Preferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var apiService: ApiService? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = Preferences.token
        val request = if (!token.isNullOrEmpty()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()
        } else {
            original.newBuilder()
                .header("Content-Type", "application/json")
                .build()
        }
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    fun create(baseUrl: String): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)
        apiService = service
        return service
    }

    fun getApiService(): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: create(Preferences.serverUrl).also { apiService = it }
        }
    }

    fun reset() {
        apiService = null
    }
}
