package com.meals.app.ui.screens.admin

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meals.app.data.dto.CategoryDto
import com.meals.app.data.dto.DishCreateRequest
import com.meals.app.data.dto.DishDto
import com.meals.app.data.dto.DishUpdateRequest
import com.meals.app.data.local.Preferences
import com.meals.app.data.repository.MealRepository
import com.meals.app.util.ImagePicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DishEditUiState(
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val categoryId: Int = 0,
    val tags: List<String> = emptyList(),
    val seasonings: List<Map<String, Any>> = emptyList(),
    val imageUrl: String = "",
    val isAvailable: Boolean = true,
    val categories: List<CategoryDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val isEditing: Boolean = false,
    val isUploading: Boolean = false
)

class DishEditViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DishEditUiState())
    val uiState: StateFlow<DishEditUiState> = _uiState.asStateFlow()

    private var dishId: Int = -1

    fun init(dishId: Int) {
        this.dishId = dishId
        _uiState.update { it.copy(isEditing = dishId != -1) }
        loadCategories()
        if (dishId != -1) {
            loadDish(dishId)
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val roomId = Preferences.activeRoomId
            MealRepository.getCategories(roomId).fold(
                onSuccess = { categories ->
                    _uiState.update { it.copy(categories = categories) }
                    if (dishId == -1 && categories.isNotEmpty()) {
                        _uiState.update { it.copy(categoryId = categories.first().id) }
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message ?: "加载分类失败") }
                }
            )
        }
    }

    private fun loadDish(dishId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val roomId = Preferences.activeRoomId
            MealRepository.getDish(roomId, dishId).fold(
                onSuccess = { dish ->
                    _uiState.update {
                        it.copy(
                            name = dish.name,
                            description = dish.description,
                            price = dish.price.toString(),
                            categoryId = dish.category_id,
                            tags = dish.tags,
                            seasonings = dish.seasonings,
                            imageUrl = dish.image_url ?: "",
                            isAvailable = dish.is_available,
                            isLoading = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "加载菜品失败") }
                }
            )
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updatePrice(price: String) {
        if (price.isEmpty() || price.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { it.copy(price = price) }
        }
    }

    fun updateCategoryId(categoryId: Int) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun addTag(tag: String) {
        if (tag.isNotBlank()) {
            _uiState.update { it.copy(tags = it.tags + tag) }
        }
    }

    fun removeTag(tag: String) {
        _uiState.update { it.copy(tags = it.tags - tag) }
    }

    fun updateSeasonings(seasonings: List<Map<String, Any>>) {
        _uiState.update { it.copy(seasonings = seasonings) }
    }

    fun updateImageUrl(url: String) {
        _uiState.update { it.copy(imageUrl = url) }
    }

    fun toggleAvailable() {
        _uiState.update { it.copy(isAvailable = !it.isAvailable) }
    }

    fun save() {
        val state = _uiState.value

        // Validation
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "菜品名称不能为空") }
            return
        }
        if (state.price.isBlank() || state.price.toDoubleOrNull() == null) {
            _uiState.update { it.copy(error = "请输入有效的价格") }
            return
        }
        if (state.categoryId == 0 && state.categories.isNotEmpty()) {
            _uiState.update { it.copy(error = "请选择分类") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val roomId = Preferences.activeRoomId

            val result = if (dishId != -1) {
                MealRepository.updateDish(
                    roomId,
                    dishId,
                    DishUpdateRequest(
                        category_id = state.categoryId,
                        name = state.name,
                        description = state.description,
                        price = state.price.toDouble(),
                        image_url = state.imageUrl.ifBlank { null },
                        tags = state.tags,
                        seasonings = state.seasonings
                    )
                )
            } else {
                MealRepository.createDish(
                    roomId,
                    DishCreateRequest(
                        category_id = state.categoryId,
                        name = state.name,
                        description = state.description,
                        price = state.price.toDouble(),
                        image_url = state.imageUrl.ifBlank { null },
                        is_available = state.isAvailable,
                        tags = state.tags,
                        seasonings = state.seasonings
                    )
                )
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, saved = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "保存失败") }
                }
            )
        }
    }

    fun deleteDish() {
        if (dishId == -1) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val roomId = Preferences.activeRoomId
            MealRepository.deleteDish(roomId, dishId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, deleted = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "删除失败") }
                }
            )
        }
    }

    fun uploadImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) }
            ImagePicker.uploadImage(context, uri).fold(
                onSuccess = { url ->
                    _uiState.update { it.copy(imageUrl = url, isUploading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isUploading = false, error = e.message ?: "图片上传失败") }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
