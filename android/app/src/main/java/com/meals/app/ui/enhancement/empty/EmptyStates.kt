package com.meals.app.ui.enhancement.empty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

private val OrangePrimary = Color(0xFFFF6B35)
private val GrayDescription = Color(0xFF9E9E9E)

@Composable
fun MenuEmptyChef(onAddDish: () -> Unit) {
    EmptyStateContainer(
        icon = Icons.Default.AddCircle,
        iconColor = OrangePrimary,
        title = "还没有菜品",
        description = "点击下方按钮添加你的第一道菜品吧",
        actionLabel = "添加菜品",
        onAction = onAddDish
    )
}

@Composable
fun MenuEmptyGuest(onRefresh: () -> Unit) {
    EmptyStateContainer(
        icon = Icons.Default.RestaurantMenu,
        iconColor = GrayDescription,
        title = "主厨正在准备菜单",
        description = "请耐心等待，菜品马上就好",
        actionLabel = "刷新菜单",
        onAction = onRefresh
    )
}

@Composable
fun OrdersEmpty(onGoToMenu: () -> Unit) {
    EmptyStateContainer(
        icon = Icons.Default.Receipt,
        iconColor = OrangePrimary,
        title = "还没有订单",
        description = "快去菜单页面点几道菜吧",
        actionLabel = "去点餐",
        onAction = onGoToMenu
    )
}

@Composable
fun MembersEmpty() {
    EmptyStateContainer(
        icon = Icons.Default.Share,
        iconColor = Color(0xFF2196F3),
        title = "还没有人加入",
        description = "分享邀请码，邀请小伙伴一起来点餐",
        actionLabel = null,
        onAction = null
    )
}

@Composable
private fun EmptyStateContainer(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(iconColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = iconColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        Text(
            text = description,
            fontSize = 14.sp,
            color = GrayDescription,
            textAlign = TextAlign.Center
        )

        // Action button
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAction,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
