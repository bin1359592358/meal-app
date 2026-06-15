package com.meals.app.ui.enhancement.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meals.app.data.dto.OrderSummaryItemDto
import com.meals.app.data.local.Preferences
import com.meals.app.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OverviewUiState(
    val totalOrders: Int = 0,
    val totalPrice: Double = 0.0,
    val dishSummary: List<DishSummaryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortBy: SortType = SortType.QUANTITY
)

data class DishSummaryItem(
    val dishName: String,
    val totalQuantity: Int,
    val orderCount: Int
)

enum class SortType {
    QUANTITY,
    NAME
}

class OverviewViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    init {
        loadOverview()
    }

    fun loadOverview() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val roomId = Preferences.activeRoomId
            OrderRepository.getSummary(roomId).fold(
                onSuccess = { summaryDto ->
                    val summaryList = summaryDto.summary.map { item ->
                        DishSummaryItem(
                            dishName = item.dish_name,
                            totalQuantity = item.total_quantity,
                            orderCount = item.order_count
                        )
                    }

                    _uiState.update {
                        it.copy(
                            totalOrders = summaryDto.total_orders,
                            totalPrice = summaryDto.total_price,
                            dishSummary = sortSummary(summaryList, it.sortBy),
                            isLoading = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "加载概览失败") }
                }
            )
        }
    }

    fun setSortBy(sortType: SortType) {
        _uiState.update { state ->
            state.copy(
                sortBy = sortType,
                dishSummary = sortSummary(state.dishSummary, sortType)
            )
        }
    }

    private fun sortSummary(items: List<DishSummaryItem>, sortType: SortType): List<DishSummaryItem> {
        return when (sortType) {
            SortType.QUANTITY -> items.sortedByDescending { it.totalQuantity }
            SortType.NAME -> items.sortedBy { it.dishName }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
