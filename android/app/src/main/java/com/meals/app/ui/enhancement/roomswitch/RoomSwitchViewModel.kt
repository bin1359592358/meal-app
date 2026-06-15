package com.meals.app.ui.enhancement.roomswitch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meals.app.data.dto.RoomDto
import com.meals.app.data.local.Preferences
import com.meals.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoomListUiState(
    val rooms: List<RoomDto> = emptyList(),
    val currentRoomId: Int = -1,
    val isLoading: Boolean = false,
    val error: String? = null,
    val roomSwitched: Boolean = false
)

class RoomSwitchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RoomListUiState())
    val uiState: StateFlow<RoomListUiState> = _uiState.asStateFlow()

    init {
        loadRooms()
    }

    fun loadRooms() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = ApiClient.getApiService().getMyRooms()
                val body = response.body()
                if (response.isSuccessful && body?.code == 0 && body.data != null) {
                    val rooms = body.data
                    _uiState.update {
                        it.copy(
                            rooms = rooms,
                            currentRoomId = Preferences.activeRoomId,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "加载房间列表失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "网络错误") }
            }
        }
    }

    fun switchRoom(room: RoomDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                Preferences.activeRoomId = room.id
                Preferences.activeRoomName = room.name
                Preferences.activeRoomCode = room.code

                // Determine role based on whether user is the chef
                val userId = Preferences.userId
                Preferences.role = if (room.chef_id == userId) "chef" else "guest"

                _uiState.update {
                    it.copy(
                        currentRoomId = room.id,
                        isLoading = false,
                        roomSwitched = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "切换失败") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearRoomSwitched() {
        _uiState.update { it.copy(roomSwitched = false) }
    }
}
