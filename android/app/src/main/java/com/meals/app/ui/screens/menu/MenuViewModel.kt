package com.meals.app.ui.screens.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meals.app.data.dto.CategoryDto
import com.meals.app.data.dto.DishDto
import com.meals.app.data.local.Preferences
import com.meals.app.data.repository.MealRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartItem(
    val dish: DishDto,
    val quantity: Int,
    val seasoningSelections: Map<String, List<String>> = emptyMap()
)

data class MenuUiState(
    val categories: List<CategoryDto> = emptyList(),
    val dishes: List<DishDto> = emptyList(),
    val allDishes: List<DishDto> = emptyList(),
    val selectedCategoryId: Int? = null,
    val cartItems: Map<Int, CartItem> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showSeasoningPanel: Boolean = false,
    val selectedDishForSeasoning: DishDto? = null,
    val searchQuery: String = ""
)

/**
 * Simple singleton to share cart state between MenuViewModel and CartViewModel.
 * MenuViewModel writes to this store; CartViewModel reads from it on initialization.
 */
object CartStore {
    val items = MutableStateFlow<Map<Int, CartItem>>(emptyMap())

    fun updateItems(newItems: Map<Int, CartItem>) {
        items.value = newItems
    }

    fun clear() {
        items.value = emptyMap()
    }
}

class MenuViewModel : ViewModel() {

    private val _state = MutableStateFlow(MenuUiState())
    val state: StateFlow<MenuUiState> = _state.asStateFlow()

    init {
        // Restore cart from CartStore if available (e.g. returning from cart screen)
        _state.update { it.copy(cartItems = CartStore.items.value) }
        loadCategories()
        loadAllDishes()
    }

    // ──────────────────────────────────────────────
    //  Data loading
    // ──────────────────────────────────────────────

    fun loadCategories() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val categories = MealRepository.getCategories(Preferences.activeRoomId).getOrThrow()
                _state.update { current ->
                    val newState = current.copy(
                        categories = categories,
                        isLoading = false
                    )
                    // Auto-select first category if none selected
                    if (current.selectedCategoryId == null && categories.isNotEmpty()) {
                        newState.copy(selectedCategoryId = categories.first().id)
                    } else {
                        newState
                    }
                }
                // After categories loaded, load dishes for the selected category
                val catId = _state.value.selectedCategoryId
                if (catId != null) {
                    loadDishes(catId)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "加载分类失败") }
            }
        }
    }

    fun loadAllDishes() {
        viewModelScope.launch {
            try {
                val allDishes = MealRepository.getDishes(Preferences.activeRoomId).getOrThrow()
                _state.update { it.copy(allDishes = allDishes) }
            } catch (_: Exception) {
                // Silently fail; we still have per-category loading
            }
        }
    }

    fun loadDishes(categoryId: Int? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val dishes = MealRepository.getDishes(Preferences.activeRoomId, categoryId).getOrThrow()
                _state.update { it.copy(dishes = dishes, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "加载菜品失败") }
            }
        }
    }

    fun selectCategory(categoryId: Int) {
        _state.update { it.copy(selectedCategoryId = categoryId) }
        loadDishes(categoryId)
    }

    fun searchDishes(query: String) {
        _state.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            // Reset to category-based loading
            val catId = _state.value.selectedCategoryId
            loadDishes(catId)
        } else {
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true) }
                try {
                    val roomId = Preferences.activeRoomId
                    val dishes = MealRepository.getDishes(roomId, searchQuery = query).getOrThrow()
                    _state.update { it.copy(dishes = dishes, isLoading = false) }
                } catch (e: Exception) {
                    _state.update { it.copy(isLoading = false, error = e.message ?: "搜索失败") }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            try {
                val roomId = Preferences.activeRoomId
                val categories = MealRepository.getCategories(roomId).getOrThrow()
                val catId = _state.value.selectedCategoryId
                val dishes = MealRepository.getDishes(roomId, catId).getOrThrow()
                val allDishes = MealRepository.getDishes(roomId).getOrThrow()
                _state.update {
                    it.copy(
                        categories = categories,
                        dishes = dishes,
                        allDishes = allDishes,
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isRefreshing = false, error = e.message ?: "刷新失败") }
            }
        }
    }

    // ──────────────────────────────────────────────
    //  Cart operations
    // ──────────────────────────────────────────────

    fun addToCart(
        dish: DishDto,
        quantity: Int = 1,
        seasoningSelections: Map<String, List<String>> = emptyMap()
    ) {
        _state.update { current ->
            val existing = current.cartItems[dish.id]
            val newQuantity = (existing?.quantity ?: 0) + quantity
            val updatedCart = current.cartItems.toMutableMap().apply {
                this[dish.id] = CartItem(
                    dish = dish,
                    quantity = newQuantity,
                    seasoningSelections = if (seasoningSelections.isNotEmpty()) {
                        seasoningSelections
                    } else {
                        existing?.seasoningSelections ?: emptyMap()
                    }
                )
            }
            CartStore.updateItems(updatedCart)
            current.copy(cartItems = updatedCart)
        }
    }

    fun removeFromCart(dishId: Int) {
        _state.update { current ->
            val updatedCart = current.cartItems.toMutableMap().apply { remove(dishId) }
            CartStore.updateItems(updatedCart)
            current.copy(cartItems = updatedCart)
        }
    }

    fun updateCartQuantity(dishId: Int, quantity: Int) {
        _state.update { current ->
            val existing = current.cartItems[dishId] ?: return@update current
            val updatedCart = if (quantity <= 0) {
                current.cartItems.toMutableMap().apply { remove(dishId) }
            } else {
                current.cartItems.toMutableMap().apply {
                    this[dishId] = existing.copy(quantity = quantity)
                }
            }
            CartStore.updateItems(updatedCart)
            current.copy(cartItems = updatedCart)
        }
    }

    fun clearCart() {
        _state.update { it.copy(cartItems = emptyMap()) }
        CartStore.clear()
    }

    // ──────────────────────────────────────────────
    //  Computed cart values
    // ──────────────────────────────────────────────

    val cartTotalCount: Int
        get() = _state.value.cartItems.values.sumOf { it.quantity }

    val cartTotalPrice: Double
        get() = _state.value.cartItems.values.sumOf { it.dish.price * it.quantity }

    // ──────────────────────────────────────────────
    //  Seasoning panel
    // ──────────────────────────────────────────────

    fun showSeasoningPanel(dish: DishDto) {
        _state.update { it.copy(showSeasoningPanel = true, selectedDishForSeasoning = dish) }
    }

    fun hideSeasoningPanel() {
        _state.update { it.copy(showSeasoningPanel = false, selectedDishForSeasoning = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
