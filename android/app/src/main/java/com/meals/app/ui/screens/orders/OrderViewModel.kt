package com.meals.app.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meals.app.data.dto.OrderDto
import com.meals.app.data.local.Preferences
import com.meals.app.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderUiState(
    val orders: List<OrderDto> = emptyList(),
    val orderDetail: OrderDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isChef: Boolean = false,
    val statusUpdated: Boolean = false
)

class OrderViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(isChef = Preferences.role == "chef") }
    }

    fun loadOrders(roomId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            OrderRepository.getOrders(roomId).fold(
                onSuccess = { orders ->
                    val sorted = orders.sortedByDescending { it.created_at }
                    _uiState.update { it.copy(orders = sorted, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "加载订单失败") }
                }
            )
        }
    }

    fun loadOrderDetail(roomId: Int, orderId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, orderDetail = null) }
            OrderRepository.getOrderDetail(roomId, orderId).fold(
                onSuccess = { order ->
                    _uiState.update { it.copy(orderDetail = order, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "加载订单详情失败") }
                }
            )
        }
    }

    fun updateStatus(roomId: Int, orderId: Int, status: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, statusUpdated = false) }
            OrderRepository.updateOrderStatus(roomId, orderId, status).fold(
                onSuccess = { updatedOrder ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusUpdated = true,
                            orderDetail = updatedOrder
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "更新状态失败") }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearStatusUpdated() {
        _uiState.update { it.copy(statusUpdated = false) }
    }
}
