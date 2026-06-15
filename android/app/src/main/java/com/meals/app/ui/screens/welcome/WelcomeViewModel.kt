package com.meals.app.ui.screens.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meals.app.data.local.Preferences
import com.meals.app.data.remote.ApiClient
import com.meals.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WelcomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val isRegisterMode: Boolean = false,
    val username: String = "",
    val nickname: String = "",
    val pin: String = "",
    val roomCode: String = "",
    val showRoomSetup: Boolean = false,
    val createdRoomCode: String? = null
)

class WelcomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(WelcomeUiState())
    val state: StateFlow<WelcomeUiState> = _state

    fun updateUsername(v: String) { _state.value = _state.value.copy(username = v) }
    fun updateNickname(v: String) { _state.value = _state.value.copy(nickname = v) }
    fun updatePin(v: String) { _state.value = _state.value.copy(pin = v) }
    fun updateRoomCode(v: String) { _state.value = _state.value.copy(roomCode = v) }
    fun toggleMode() { _state.value = _state.value.copy(isRegisterMode = !_state.value.isRegisterMode, error = null) }

    fun login() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                AuthRepository.login(_state.value.username, _state.value.pin).getOrThrow()
                // After login, fetch user's rooms
                fetchRooms()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "登录失败")
            }
        }
    }

    private fun fetchRooms() {
        viewModelScope.launch {
            try {
                val api = ApiClient.getApiService()
                val response = api.getMyRooms()
                val rooms = response.body()?.data
                if (!rooms.isNullOrEmpty()) {
                    // Auto-select the first room
                    val room = rooms.first()
                    Preferences.activeRoomId = room.id
                    Preferences.activeRoomName = room.name
                    Preferences.activeRoomCode = room.code
                    // Find current user's role from members list
                    val myUserId = Preferences.userId
                    val myRole = room.members?.find { it.user_id == myUserId }?.role ?: "guest"
                    Preferences.role = myRole
                    Preferences.rooms = rooms
                    _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
                } else {
                    // No rooms — show room creation/join UI
                    _state.value = _state.value.copy(isLoading = false, showRoomSetup = true)
                }
            } catch (e: Exception) {
                // If fetching rooms fails, still show room setup
                _state.value = _state.value.copy(isLoading = false, showRoomSetup = true)
            }
        }
    }

    fun register() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                AuthRepository.register(_state.value.username, _state.value.nickname, _state.value.pin)
                _state.value = _state.value.copy(isLoading = false, showRoomSetup = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "注册失败")
            }
        }
    }

    fun createRoom(name: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val api = ApiClient.getApiService()
                val response = api.createRoom(com.meals.app.data.dto.RoomCreateRequest(name))
                val data = response.body()?.data
                if (data != null) {
                    Preferences.activeRoomId = data.id
                    Preferences.activeRoomName = data.name
                    Preferences.activeRoomCode = data.code
                    Preferences.role = "chef"
                    _state.value = _state.value.copy(isLoading = false, createdRoomCode = data.code, isLoggedIn = true)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "创建失败")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "创建失败")
            }
        }
    }

    fun joinRoom() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val api = ApiClient.getApiService()
                val response = api.joinRoom(com.meals.app.data.dto.RoomJoinRequest(_state.value.roomCode))
                val data = response.body()?.data
                if (data != null) {
                    Preferences.activeRoomId = data.id
                    Preferences.activeRoomName = data.name
                    Preferences.activeRoomCode = data.code
                    Preferences.role = "guest"
                    _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "邀请码无效或餐桌不存在")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "加入失败")
            }
        }
    }
}
