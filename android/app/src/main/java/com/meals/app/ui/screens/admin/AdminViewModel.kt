package com.meals.app.ui.screens.admin

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

data class AdminUiState(
    val categories: List<CategoryDto> = emptyList(),
    val dishes: List<DishDto> = emptyList(),
    val selectedCategoryId: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddCategoryDialog: Boolean = false,
    val categoryAdded: Boolean = false,
    val categoryDeleted: Boolean = false,
    val dishDeleted: Boolean = false,
    val dishToggled: Boolean = false
)

class AdminViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val roomId = Preferences.activeRoomId
            MealRepository.getCategories(roomId).fold(
                onSuccess = { categories ->
                    _uiState.update { it.copy(categories = categories, isLoading = false) }
                    // Auto-select first category
                    if (categories.isNotEmpty() && _uiState.value.selectedCategoryId == null) {
                        selectCategory(categories.first().id)
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "加载分类失败") }
                }
            )
        }
    }

    fun selectCategory(categoryId: Int) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        loadDishes(categoryId)
    }

    fun loadDishes(categoryId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val roomId = Preferences.activeRoomId
            MealRepository.getDishes(roomId, categoryId = categoryId).fold(
                onSuccess = { dishes ->
                    _uiState.update { it.copy(dishes = dishes, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "加载菜品失败") }
                }
            )
        }
    }

    fun addCategory(name: String, icon: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val roomId = Preferences.activeRoomId
            MealRepository.createCategory(roomId, name, icon).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isLoading = false, categoryAdded = true, showAddCategoryDialog = false)
                    }
                    loadCategories()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "添加分类失败") }
                }
            )
        }
    }

    fun deleteCategory(categoryId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val roomId = Preferences.activeRoomId
            MealRepository.deleteCategory(roomId, categoryId).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            categoryDeleted = true,
                            selectedCategoryId = null,
                            dishes = emptyList()
                        )
                    }
                    loadCategories()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "删除分类失败") }
                }
            )
        }
    }

    fun deleteDish(roomId: Int, dishId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            MealRepository.deleteDish(roomId, dishId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, dishDeleted = true) }
                    _uiState.value.selectedCategoryId?.let { catId -> loadDishes(catId) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "删除菜品失败") }
                }
            )
        }
    }

    fun toggleDish(dishId: Int) {
        viewModelScope.launch {
            val roomId = Preferences.activeRoomId
            MealRepository.toggleDish(roomId, dishId).fold(
                onSuccess = { updatedDish ->
                    _uiState.update { state ->
                        state.copy(
                            dishes = state.dishes.map { dish ->
                                if (dish.id == dishId) updatedDish else dish
                            },
                            dishToggled = true
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message ?: "操作失败") }
                }
            )
        }
    }

    fun showAddCategoryDialog() {
        _uiState.update { it.copy(showAddCategoryDialog = true) }
    }

    fun hideAddCategoryDialog() {
        _uiState.update { it.copy(showAddCategoryDialog = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearCategoryAdded() {
        _uiState.update { it.copy(categoryAdded = false) }
    }

    fun clearCategoryDeleted() {
        _uiState.update { it.copy(categoryDeleted = false) }
    }

    fun clearDishDeleted() {
        _uiState.update { it.copy(dishDeleted = false) }
    }

    fun clearDishToggled() {
        _uiState.update { it.copy(dishToggled = false) }
    }
}
