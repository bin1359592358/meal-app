package com.meals.app.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.meals.app.data.dto.OrderDto
import com.meals.app.data.dto.OrderItemDto
import com.meals.app.data.local.Preferences

private val OrangePrimary = Color(0xFFFF6B35)
private val PriceRed = Color(0xFFE53935)
private val StatusGreen = Color(0xFF4CAF50)
private val StatusGray = Color(0xFF9E9E9E)
private val GrayDescription = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    navController: NavHostController,
    orderId: String,
    viewModel: OrderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val roomId = Preferences.activeRoomId
    val orderIdInt = orderId.toIntOrNull() ?: -1

    LaunchedEffect(orderId) {
        if (orderIdInt > 0) {
            viewModel.loadOrderDetail(roomId, orderIdInt)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.statusUpdated) {
        if (uiState.statusUpdated) {
            snackbarHostState.showSnackbar("状态更新成功")
            viewModel.clearStatusUpdated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "订单详情 #${orderId}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color(0xFF212121)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAFAFA)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.orderDetail == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = OrangePrimary)
                    }
                }

                uiState.orderDetail != null -> {
                    OrderDetailContent(
                        order = uiState.orderDetail!!,
                        isChef = uiState.isChef,
                        onUpdateStatus = { status ->
                            viewModel.updateStatus(roomId, orderIdInt, status)
                        }
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "订单加载失败",
                            color = StatusGray,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailContent(
    order: OrderDto,
    isChef: Boolean,
    onUpdateStatus: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Order info header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            OrderInfoHeader(order)
        }

        // Items list
        if (!order.items.isNullOrEmpty()) {
            item {
                Text(
                    text = "订单明细",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            itemsIndexed(order.items!!) { index, item ->
                OrderItemRow(item = item)
                if (index < order.items!!.size - 1) {
                    Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        // Note display
        if (order.note.isNotBlank()) {
            item {
                NoteSection(note = order.note)
            }
        }

        // Summary
        item {
            OrderSummary(order)
        }

        // Chef action buttons
        if (isChef && order.status.lowercase() == "pending") {
            item {
                StatusActionButtons(onUpdateStatus = onUpdateStatus)
            }
        }

        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OrderInfoHeader(order: OrderDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "订单号: #${order.id}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )

                StatusBadgeLarge(status = order.status)
            }

            Text(
                text = order.created_at,
                fontSize = 14.sp,
                color = GrayDescription
            )

            if (order.user_nickname.isNotBlank()) {
                Text(
                    text = "下单人: ${order.user_nickname}",
                    fontSize = 14.sp,
                    color = Color(0xFF616161)
                )
            }
        }
    }
}

@Composable
private fun StatusBadgeLarge(status: String) {
    val (backgroundColor, textColor, label) = when (status.lowercase()) {
        "pending" -> Triple(OrangePrimary.copy(alpha = 0.1f), OrangePrimary, "待处理")
        "confirmed" -> Triple(Color(0xFF2196F3).copy(alpha = 0.1f), Color(0xFF2196F3), "已确认")
        "completed" -> Triple(StatusGreen.copy(alpha = 0.1f), StatusGreen, "已完成")
        "cancelled" -> Triple(StatusGray.copy(alpha = 0.1f), StatusGray, "已取消")
        else -> Triple(StatusGray.copy(alpha = 0.1f), StatusGray, status)
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun OrderItemRow(item: OrderItemDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${item.dish_name} x ${item.quantity}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121),
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "¥${String.format("%.2f", item.subtotal)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = PriceRed
            )
        }

        if (item.seasoning_text.isNotBlank()) {
            Text(
                text = item.seasoning_text,
                fontSize = 12.sp,
                color = GrayDescription
            )
        }
    }
}

@Composable
private fun NoteSection(note: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "备注",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF57C00)
            )
            Text(
                text = note,
                fontSize = 14.sp,
                color = Color(0xFF795548)
            )
        }
    }
}

@Composable
private fun OrderSummary(order: OrderDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "总价",
                    fontSize = 16.sp,
                    color = Color(0xFF616161)
                )
                Text(
                    text = "¥${String.format("%.2f", order.total_price)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PriceRed
                )
            }

            if (order.people_count > 1) {
                Divider(color = Color(0xFFEEEEEE))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "人均 (${order.people_count}人)",
                        fontSize = 14.sp,
                        color = GrayDescription
                    )
                    Text(
                        text = "¥${String.format("%.2f", order.total_price / order.people_count)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OrangePrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusActionButtons(onUpdateStatus: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "操作",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onUpdateStatus("confirmed") },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "确认订单",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            OutlinedButton(
                onClick = { onUpdateStatus("cancelled") },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PriceRed)
            ) {
                Text(
                    text = "取消订单",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
