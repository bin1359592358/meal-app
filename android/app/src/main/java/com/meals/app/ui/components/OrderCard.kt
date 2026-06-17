package com.meals.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meals.app.data.dto.OrderDto

private val OrangePrimary = Color(0xFFFF6B35)
private val PriceRed = Color(0xFFE53935)
private val StatusGreen = Color(0xFF4CAF50)
private val StatusGray = Color(0xFF9E9E9E)

@Composable
fun OrderCard(
    order: OrderDto,
    isChefView: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top row: Order ID + Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${order.id}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )

                StatusBadge(status = order.status)
            }

            // Middle: Dish count and total price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalDishes = order.items?.sumOf { it.quantity } ?: 0
                Text(
                    text = "${totalDishes}道菜 · ¥${String.format("%.2f", order.total_price)}",
                    fontSize = 14.sp,
                    color = Color(0xFF616161)
                )
            }

            // Bottom: Date/time + user nickname
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.created_at,
                    fontSize = 12.sp,
                    color = StatusGray
                )

                if (isChefView && order.user_nickname.isNotBlank()) {
                    Text(
                        text = order.user_nickname,
                        fontSize = 12.sp,
                        color = StatusGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (backgroundColor, textColor, label) = when (status.lowercase()) {
        "pending" -> Triple(OrangePrimary.copy(alpha = 0.1f), OrangePrimary, "待处理")
        "preparing" -> Triple(Color(0xFF2196F3).copy(alpha = 0.1f), Color(0xFF2196F3), "制作中")
        "served" -> Triple(Color(0xFF9C27B0).copy(alpha = 0.1f), Color(0xFF9C27B0), "已上桌")
        "completed" -> Triple(StatusGreen.copy(alpha = 0.1f), StatusGreen, "已完成")
        "cancelled" -> Triple(StatusGray.copy(alpha = 0.1f), StatusGray, "已取消")
        else -> Triple(StatusGray.copy(alpha = 0.1f), StatusGray, status)
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
