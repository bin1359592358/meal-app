package com.meals.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.meals.app.data.dto.RoomDto

object Preferences {

    private const val PREFS_NAME = "meals_prefs"

    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_TOKEN = "token"
    private const val KEY_ACTIVE_ROOM_ID = "active_room_id"
    private const val KEY_ACTIVE_ROOM_NAME = "active_room_name"
    private const val KEY_ACTIVE_ROOM_CODE = "active_room_code"
    private const val KEY_ROLE = "role"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_ROOMS_JSON = "rooms_json"

    private const val DEFAULT_SERVER_URL = "http://10.0.2.2:8000"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- User Info ---

    var userId: Int
        get() = prefs.getInt(KEY_USER_ID, -1)
        set(value) = prefs.edit().putInt(KEY_USER_ID, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var nickname: String?
        get() = prefs.getString(KEY_NICKNAME, null)
        set(value) = prefs.edit().putString(KEY_NICKNAME, value).apply()

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    // --- Active Room ---

    var activeRoomId: Int
        get() = prefs.getInt(KEY_ACTIVE_ROOM_ID, -1)
        set(value) = prefs.edit().putInt(KEY_ACTIVE_ROOM_ID, value).apply()

    var activeRoomName: String?
        get() = prefs.getString(KEY_ACTIVE_ROOM_NAME, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_ROOM_NAME, value).apply()

    var activeRoomCode: String?
        get() = prefs.getString(KEY_ACTIVE_ROOM_CODE, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_ROOM_CODE, value).apply()

    var role: String?
        get() = prefs.getString(KEY_ROLE, null)
        set(value) = prefs.edit().putString(KEY_ROLE, value).apply()

    // --- Server URL ---

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    // --- Rooms List (cached as JSON) ---

    var rooms: List<RoomDto>
        get() {
            val json = prefs.getString(KEY_ROOMS_JSON, null) ?: return emptyList()
            return try {
                val type = object : TypeToken<List<RoomDto>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        }
        set(value) {
            val json = gson.toJson(value)
            prefs.edit().putString(KEY_ROOMS_JSON, json).apply()
        }

    // --- Helpers ---

    val isLoggedIn: Boolean
        get() = !token.isNullOrEmpty()

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun clearActiveRoom() {
        prefs.edit()
            .remove(KEY_ACTIVE_ROOM_ID)
            .remove(KEY_ACTIVE_ROOM_NAME)
            .remove(KEY_ACTIVE_ROOM_CODE)
            .remove(KEY_ROLE)
            .apply()
    }
}
