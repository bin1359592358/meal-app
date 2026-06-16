package com.meals.app.ui.enhancement.roomswitch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meals.app.data.dto.RoomDto
import com.meals.app.data.local.Preferences

private val OrangePrimary = Color(0xFFFF6B35)
private val GrayDescription = Color(0xFF9E9E9E)
private val BackgroundColor = Color(0xFFFAFAFA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    onRoomSelected: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: RoomSwitchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val userId = Preferences.userId
    var roomToDelete by remember { mutableStateOf<RoomDto?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.roomSwitched) {
        if (uiState.roomSwitched) {
            snackbarHostState.showSnackbar("已切换房间")
            viewModel.clearRoomSwitched()
            onRoomSelected()
        }
    }

    LaunchedEffect(uiState.roomLeft) {
        if (uiState.roomLeft) {
            snackbarHostState.showSnackbar("已退出餐桌")
            viewModel.clearRoomLeft()
        }
    }

    // Confirmation dialog for leaving/closing a room
    if (roomToDelete != null) {
        val room = roomToDelete!!
        val isChef = room.chef_id == userId
        AlertDialog(
            onDismissRequest = { roomToDelete = null },
            title = { Text(if (isChef) "关闭餐桌" else "退出餐桌") },
            text = {
                Text(
                    if (isChef) "确定要关闭「${room.name}」吗？关闭后成员将无法继续点餐。"
                    else "确定要退出「${room.name}」吗？退出后需要重新输入邀请码才能加入。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.leaveOrCloseRoom(room)
                    roomToDelete = null
                }) {
                    Text(if (isChef) "确认关闭" else "确认退出", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { roomToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "切换房间",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundColor
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundColor
    ) { paddingValues ->
        if (uiState.isLoading && uiState.rooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (uiState.rooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MeetingRoom,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = GrayDescription.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "还没有加入任何餐桌",
                        fontSize = 16.sp,
                        color = GrayDescription
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = uiState.rooms,
                    key = { it.id }
                ) { room ->
                    val isChef = room.chef_id == userId
                    val role = if (isChef) "chef" else "guest"

                    RoomListItem(
                        roomName = room.name,
                        roomCode = room.code,
                        role = role,
                        isCurrent = room.id == uiState.currentRoomId,
                        isActive = room.is_active,
                        onClick = { viewModel.switchRoom(room) },
                        onDelete = { roomToDelete = room }
                    )
                }

                // Bottom spacer
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RoomListItem(
    roomName: String,
    roomCode: String,
    role: String,
    isCurrent: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val PriceRed = Color(0xFFE53935)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCurrent && isActive) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isActive -> Color(0xFFF5F5F5)
                isCurrent -> OrangePrimary.copy(alpha = 0.08f)
                else -> Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrent) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Room icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !isActive -> Color(0xFFE0E0E0)
                            isCurrent -> OrangePrimary.copy(alpha = 0.15f)
                            else -> Color(0xFFF5F5F5)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = when {
                        !isActive -> GrayDescription.copy(alpha = 0.5f)
                        isCurrent -> OrangePrimary
                        else -> GrayDescription
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            // Room info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = roomName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) Color(0xFF212121) else GrayDescription
                    )
                    if (!isActive) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE0E0E0), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "已关闭",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = GrayDescription
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = roomCode,
                        fontSize = 13.sp,
                        color = GrayDescription,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                if (role == "chef") OrangePrimary.copy(alpha = 0.1f) else Color(0xFFE3F2FD),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (role == "chef") "主厨" else "食客",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (role == "chef") OrangePrimary else Color(0xFF1976D2)
                        )
                    }
                }
            }

            // Right side: current indicator + delete button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(OrangePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "当前",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (role == "chef") Icons.Default.Delete else Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = if (role == "chef") "关闭" else "退出",
                        tint = PriceRed.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
