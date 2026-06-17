package com.meals.app.ui.screens.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meals.app.data.dto.OrderItemRequest
import com.meals.app.data.dto.OrderDto
import com.meals.app.data.local.Preferences
import com.meals.app.data.repository.OrderRepository
import com.meals.app.ui.screens.menu.CartItem
import com.meals.app.ui.screens.menu.CartStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val note: String = "",
    val peopleCount: Int = 1,
    val isSubmitting: Boolean = false,
    val orderResult: OrderDto? = null,
    val error: String? = null
)

class CartViewModel : ViewModel() {

    private val _state = MutableStateFlow(CartUiState())
    val state: StateFlow<CartUiState> = _state.asStateFlow()

    init {
        // Load items from CartStore on initialization
        val storeItems = CartStore.items.value.values.toList()
        _state.update { it.copy(items = storeItems) }
    }

    /**
     * Set cart items explicitly (e.g. from navigation arguments).
     */
    fun setItems(items: List<CartItem>) {
        _state.update { it.copy(items = items) }
    }

    // ──────────────────────────────────────────────
    //  Note & people count
    // ──────────────────────────────────────────────

    fun updateNote(note: String) {
        _state.update { it.copy(note = note) }
    }

    fun updatePeopleCount(count: Int) {
        if (count in 1..50) {
            _state.update { it.copy(peopleCount = count) }
        }
    }

    // ──────────────────────────────────────────────
    //  Item management
    // ──────────────────────────────────────────────

    fun updateItemQuantity(dishId: Int, quantity: Int) {
        _state.update { current ->
            val updatedItems = if (quantity <= 0) {
                current.items.filter { it.dish.id != dishId }
            } else {
                current.items.map { item ->
                    if (item.dish.id == dishId) item.copy(quantity = quantity) else item
                }
            }
            current.copy(items = updatedItems)
        }
        // Sync back to CartStore
        syncToCartStore()
    }

    fun removeItem(dishId: Int) {
        _state.update { current ->
            current.copy(items = current.items.filter { it.dish.id != dishId })
        }
        syncToCartStore()
    }

    private fun syncToCartStore() {
        val currentItems = _state.value.items
        val map = currentItems.associateBy { it.dish.id }
        CartStore.updateItems(map)
    }

    // ──────────────────────────────────────────────
    //  Computed values
    // ──────────────────────────────────────────────

    val totalPrice: Double
        get() = _state.value.items.sumOf { it.dish.price * it.quantity }

    val totalCount: Int
        get() = _state.value.items.sumOf { it.quantity }

    // ──────────────────────────────────────────────
    //  Submit order
    // ──────────────────────────────────────────────

    fun submitOrder() {
        val currentState = _state.value
        if (currentState.items.isEmpty()) {
            _state.update { it.copy(error = "购物车为空") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            try {
                // Build order request from cart items
                val orderItems = currentState.items.map { cartItem ->
                    OrderItemRequest(
                        dish_id = cartItem.dish.id,
                        quantity = cartItem.quantity,
                        seasonings = if (cartItem.seasoningSelections.isNotEmpty()) {
                            cartItem.seasoningSelections.mapValues { it.value as Any }
                        } else null
                    )
                }

                val order = OrderRepository.createOrder(
                    roomId = Preferences.activeRoomId,
                    items = orderItems,
                    note = currentState.note,
                    peopleCount = currentState.peopleCount
                ).getOrThrow()

                // Clear cart on success
                CartStore.clear()

                _state.update {
                    it.copy(
                        isSubmitting = false,
                        orderResult = order,
                        items = emptyList()
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        error = e.message ?: "下单失败，请重试"
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
