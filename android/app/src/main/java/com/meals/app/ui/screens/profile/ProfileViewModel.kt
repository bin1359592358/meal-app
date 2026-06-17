package com.meals.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meals.app.data.dto.MemberDto
import com.meals.app.data.dto.RoomDto
import com.meals.app.data.dto.RoomRenameRequest
import com.meals.app.data.dto.UserDto
import com.meals.app.data.local.Preferences
import com.meals.app.data.remote.ApiClient
import com.meals.app.data.repository.AuthRepository
import com.meals.app.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: UserDto? = null,
    val room: RoomDto? = null,
    val members: List<MemberDto> = emptyList(),
    val role: String = "",
    val inviteCode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val nicknameSaved: Boolean = false,
    val pinChanged: Boolean = false,
    val loggedOut: Boolean = false,
    val roomClosed: Boolean = false,
    val serverUrl: String = ""
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val role = Preferences.role ?: ""
            val roomId = Preferences.activeRoomId
            val serverUrl = Preferences.serverUrl

            // Load user info
            AuthRepository.getMe().fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(user = user) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message ?: "加载用户信息失败") }
                }
            )

            // Load room info via API
            try {
                val response = ApiClient.getApiService().getRoom(roomId)
                val body = response.body()
                if (response.isSuccessful && body?.code == 0 && body.data != null) {
                    val room = body.data
                    _uiState.update {
                        it.copy(
                            room = room,
                            inviteCode = room.code,
                            members = room.members ?: emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                // Room loading is non-critical, continue
            }

            _uiState.update {
                it.copy(
                    role = role,
                    isLoading = false,
                    serverUrl = serverUrl
                )
            }
        }
    }

    fun changeNickname(newNickname: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            AuthRepository.updateNickname(newNickname).fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            user = user,
                            isLoading = false,
                            nicknameSaved = true
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "修改昵称失败") }
                }
            )
        }
    }

    fun changePin(oldPin: String, newPin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            AuthRepository.changePin(oldPin, newPin).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, pinChanged = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "修改PIN失败") }
                }
            )
        }
    }

    fun updateServerUrl(url: String) {
        Preferences.serverUrl = url
        ApiClient.reset()
        _uiState.update { it.copy(serverUrl = url) }
    }

    fun logout() {
        viewModelScope.launch {
            AuthRepository.logout()
            _uiState.update { it.copy(loggedOut = true) }
        }
    }

    fun clearNicknameSaved() {
        _uiState.update { it.copy(nicknameSaved = false) }
    }

    fun clearPinChanged() {
        _uiState.update { it.copy(pinChanged = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun renameRoom(newName: String) {
        val roomId = Preferences.activeRoomId
        if (roomId < 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = ApiClient.getApiService().renameRoom(roomId, RoomRenameRequest(newName))
                val body = response.body()
                if (response.isSuccessful && body?.code == 0 && body.data != null) {
                    val room = body.data
                    Preferences.activeRoomName = room.name
                    _uiState.update {
                        it.copy(
                            room = room,
                            isLoading = false,
                            successMessage = "房间已改名为「${room.name}」"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = body?.message ?: "改名失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "网络错误: ${e.message}") }
            }
        }
    }

    fun refreshInviteCode() {
        val roomId = Preferences.activeRoomId
        if (roomId < 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = ApiClient.getApiService().refreshRoomCode(roomId)
                val body = response.body()
                if (response.isSuccessful && body?.code == 0 && body.data != null) {
                    val room = body.data
                    Preferences.activeRoomCode = room.code
                    _uiState.update {
                        it.copy(
                            room = room,
                            inviteCode = room.code,
                            isLoading = false,
                            successMessage = "邀请码已刷新"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = body?.message ?: "刷新邀请码失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "网络错误: ${e.message}") }
            }
        }
    }

    fun closeRoom() {
        val roomId = Preferences.activeRoomId
        if (roomId < 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = ApiClient.getApiService().closeRoom(roomId)
                val body = response.body()
                if (response.isSuccessful && body?.code == 0) {
                    // Remove closed room from cached rooms list
                    Preferences.rooms = Preferences.rooms.filter { it.id != roomId }
                    Preferences.clearActiveRoom()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            roomClosed = true,
                            successMessage = "餐桌已关闭"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = body?.message ?: "关闭餐桌失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "网络错误: ${e.message}") }
            }
        }
    }

    fun leaveRoom() {
        val roomId = Preferences.activeRoomId
        if (roomId < 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = ApiClient.getApiService().leaveRoom(roomId)
                val body = response.body()
                if (response.isSuccessful && body?.code == 0) {
                    // Remove left room from cached rooms list
                    Preferences.rooms = Preferences.rooms.filter { it.id != roomId }
                    Preferences.clearActiveRoom()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            roomClosed = true,
                            successMessage = "已退出餐桌"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = body?.message ?: "退出餐桌失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "网络错误: ${e.message}") }
            }
        }
    }

    fun removeMember(userId: Int) {
        val roomId = Preferences.activeRoomId
        if (roomId < 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val response = ApiClient.getApiService().removeMember(roomId, userId)
                val body = response.body()
                if (response.isSuccessful && body?.code == 0) {
                    // Reload members
                    loadProfile()
                    _uiState.update { it.copy(successMessage = "成员已移除") }
                } else {
                    _uiState.update { it.copy(error = body?.message ?: "移除成员失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "网络错误: ${e.message}") }
            }
        }
    }
}
