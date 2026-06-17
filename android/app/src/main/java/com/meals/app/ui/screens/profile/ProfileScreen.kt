package com.meals.app.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.meals.app.ui.enhancement.empty.MembersEmpty

private val OrangePrimary = Color(0xFFFF6B35)
private val PriceRed = Color(0xFFE53935)
private val GrayDescription = Color(0xFF9E9E9E)
private val BackgroundColor = Color(0xFFFAFAFA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    onNavigateToAdmin: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showNicknameDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCloseConfirm by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.nicknameSaved) {
        if (uiState.nicknameSaved) {
            snackbarHostState.showSnackbar("昵称已更新")
            viewModel.clearNicknameSaved()
        }
    }

    LaunchedEffect(uiState.pinChanged) {
        if (uiState.pinChanged) {
            snackbarHostState.showSnackbar("PIN已更新")
            viewModel.clearPinChanged()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(uiState.roomClosed) {
        if (uiState.roomClosed) {
            // Navigate to join/create room screen (not login)
            navController.navigate(com.meals.app.ui.navigation.Routes.JOIN_ROOM) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(uiState.loggedOut) {
        if (uiState.loggedOut) {
            navController.navigate(com.meals.app.ui.navigation.Routes.WELCOME) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Nickname dialog
    if (showNicknameDialog) {
        EditNicknameDialog(
            currentNickname = uiState.user?.nickname ?: "",
            onConfirm = { newNickname ->
                viewModel.changeNickname(newNickname)
                showNicknameDialog = false
            },
            onDismiss = { showNicknameDialog = false }
        )
    }

    // PIN dialog
    if (showPinDialog) {
        ChangePinDialog(
            onConfirm = { oldPin, newPin ->
                viewModel.changePin(oldPin, newPin)
                showPinDialog = false
            },
            onDismiss = { showPinDialog = false }
        )
    }

    // Logout confirmation
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("确认退出") },
            text = { Text("退出后需要重新登录") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    viewModel.logout()
                }) {
                    Text("退出", color = PriceRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Rename room dialog
    if (showRenameDialog) {
        RenameRoomDialog(
            currentName = uiState.room?.name ?: "",
            onConfirm = { newName ->
                viewModel.renameRoom(newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    // Close room confirmation (chef)
    if (showCloseConfirm) {
        AlertDialog(
            onDismissRequest = { showCloseConfirm = false },
            title = { Text("关闭餐桌") },
            text = { Text("关闭后，成员将无法继续在此餐桌点餐。确定关闭吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showCloseConfirm = false
                    viewModel.closeRoom()
                }) {
                    Text("确认关闭", color = PriceRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Leave room confirmation (guest)
    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("退出餐桌") },
            text = { Text("退出后将不再属于此餐桌，需要重新输入邀请码才能加入。") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveConfirm = false
                    viewModel.leaveRoom()
                }) {
                    Text("确认退出", color = PriceRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
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
                        text = "个人中心",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundColor
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundColor
    ) { paddingValues ->
        if (uiState.isLoading && uiState.user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // User profile header
                item {
                    UserSection(
                        username = uiState.user?.username ?: "",
                        nickname = uiState.user?.nickname ?: "",
                        role = uiState.role,
                        onEditNickname = { showNicknameDialog = true }
                    )
                }

                // Current room section header
                item {
                    Text(
                        text = "当前餐桌",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                // Room section
                item {
                    RoomSection(
                        roomName = uiState.room?.name ?: "未加入房间",
                        inviteCode = uiState.inviteCode,
                        memberCount = uiState.members.size,
                        role = uiState.role,
                        onCopyCode = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("邀请码", uiState.inviteCode)
                            clipboard.setPrimaryClip(clip)
                        },
                        onShareCode = {
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "加入我的点餐房间，邀请码: ${uiState.inviteCode}")
                                type = "text/plain"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        onRenameRoom = { showRenameDialog = true },
                        onRefreshCode = { viewModel.refreshInviteCode() },
                        onCloseRoom = { showCloseConfirm = true },
                        onLeaveRoom = { showLeaveConfirm = true }
                    )
                }

                // Members section header
                item {
                    Text(
                        text = "成员 (${uiState.members.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                // Members list or empty
                if (uiState.members.isNotEmpty()) {
                    items(uiState.members) { member ->
                        MemberRow(
                            nickname = member.nickname,
                            role = member.role,
                            isCurrentUserChef = uiState.role == "chef",
                            isSelf = member.user_id == uiState.user?.id,
                            onRemoveMember = { viewModel.removeMember(member.user_id) }
                        )
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            MembersEmpty()
                        }
                    }
                }

                // Room management section header
                item {
                    Text(
                        text = "餐桌管理",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                // Chef: Admin button
                if (uiState.role == "chef") {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToAdmin() },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = OrangePrimary.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "菜单管理",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OrangePrimary
                                    )
                                    Text(
                                        text = "管理分类和菜品",
                                        fontSize = 12.sp,
                                        color = OrangePrimary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Switch rooms entry
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(com.meals.app.ui.navigation.Routes.ROOM_LIST) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = OrangePrimary.copy(alpha = 0.06f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(OrangePrimary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "切换餐桌",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangePrimary
                                )
                                Text(
                                    text = "查看已加入的所有餐桌并切换",
                                    fontSize = 12.sp,
                                    color = OrangePrimary.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = OrangePrimary.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Join / Create room entry
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(com.meals.app.ui.navigation.Routes.JOIN_ROOM) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF1976D2).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "加入 / 创建餐桌",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                                Text(
                                    text = "输入邀请码加入或新建一个餐桌",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1976D2).copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF90CAF9),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Account settings section header
                item {
                    Text(
                        text = "账号设置",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                // Server URL setting
                item {
                    ServerUrlSection(
                        currentUrl = uiState.serverUrl,
                        onUrlChanged = { viewModel.updateServerUrl(it) }
                    )
                }

                // Settings card (PIN + Logout)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            SettingsActionRow(
                                icon = Icons.Default.Key,
                                label = "修改PIN码",
                                subtitle = "保护你的账户安全",
                                iconTint = Color(0xFF616161),
                                backgroundColor = Color(0xFFF5F5F5),
                                onClick = { showPinDialog = true }
                            )
                            HorizontalDivider(
                                color = Color(0xFFEEEEEE),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            SettingsActionRow(
                                icon = Icons.AutoMirrored.Filled.ExitToApp,
                                label = "退出登录",
                                subtitle = "退出当前账户",
                                iconTint = PriceRed,
                                backgroundColor = PriceRed.copy(alpha = 0.06f),
                                onClick = { showLogoutConfirm = true }
                            )
                        }
                    }
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
private fun UserSection(
    username: String,
    nickname: String,
    role: String,
    onEditNickname: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Gradient header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFF6B35),
                                Color(0xFFFF8F65)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            )

            // Avatar + info overlapping the gradient
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar - overlapping the gradient
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .offset(y = (-32).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFF6B35).copy(alpha = 0.2f),
                                        Color(0xFFFF8F65).copy(alpha = 0.2f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Name + role (offset to compensate for avatar overlap)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = (-20).dp)
                ) {
                    Text(
                        text = nickname.ifBlank { username },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "@$username",
                            fontSize = 13.sp,
                            color = GrayDescription
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (role == "chef") OrangePrimary.copy(alpha = 0.1f) else Color(0xFFE3F2FD),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (role == "chef") "主厨" else "食客",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (role == "chef") OrangePrimary else Color(0xFF1976D2)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Edit nickname chip
                    androidx.compose.material3.OutlinedButton(
                        onClick = onEditNickname,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = GrayDescription
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("修改昵称", fontSize = 12.sp, color = Color(0xFF616161))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RoomSection(
    roomName: String,
    inviteCode: String,
    memberCount: Int,
    role: String,
    onCopyCode: () -> Unit,
    onShareCode: () -> Unit,
    onRenameRoom: () -> Unit,
    onRefreshCode: () -> Unit,
    onCloseRoom: () -> Unit,
    onLeaveRoom: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Room name + rename
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(OrangePrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = roomName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121),
                    modifier = Modifier.weight(1f)
                )
                if (role == "chef") {
                    IconButton(
                        onClick = onRenameRoom,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "改名",
                            tint = GrayDescription,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Invite code with background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF8F4), RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "邀请码",
                            fontSize = 11.sp,
                            color = GrayDescription
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = inviteCode,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary,
                            letterSpacing = 3.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (role == "chef") {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9))
                                    .clickable { onRefreshCode() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "刷新",
                                    tint = Color(0xFF43A047),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF5F5F5))
                                .clickable { onCopyCode() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "复制",
                                tint = Color(0xFF616161),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(OrangePrimary.copy(alpha = 0.1f))
                                .clickable { onShareCode() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "分享",
                                tint = OrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Member count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = GrayDescription,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${memberCount}位成员",
                    fontSize = 13.sp,
                    color = GrayDescription
                )
            }

            // Divider
            HorizontalDivider(color = Color(0xFFEEEEEE))

            // Management actions
            if (role == "chef") {
                SettingsActionRow(
                    icon = Icons.Default.StopCircle,
                    label = "关闭餐桌",
                    subtitle = "关闭后成员将无法点餐",
                    iconTint = PriceRed,
                    backgroundColor = PriceRed.copy(alpha = 0.06f),
                    onClick = onCloseRoom
                )
            }
            if (role == "guest") {
                SettingsActionRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    label = "退出餐桌",
                    subtitle = "需要新邀请码才能重新加入",
                    iconTint = Color(0xFF757575),
                    backgroundColor = Color(0xFFF5F5F5),
                    onClick = onLeaveRoom
                )
            }
        }
    }
}

@Composable
private fun MemberRow(
    nickname: String,
    role: String,
    isCurrentUserChef: Boolean,
    isSelf: Boolean,
    onRemoveMember: () -> Unit
) {
    var showRemoveConfirm by remember { mutableStateOf(false) }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("移除成员") },
            text = { Text("确定要将「$nickname」从此餐桌移除吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveConfirm = false
                    onRemoveMember()
                }) {
                    Text("确认移除", color = PriceRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (role == "chef") OrangePrimary.copy(alpha = 0.1f) else Color(0xFFF5F5F5)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = if (role == "chef") OrangePrimary else GrayDescription,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = nickname,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121),
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .background(
                        if (role == "chef") OrangePrimary.copy(alpha = 0.1f) else Color(0xFFF5F5F5),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (role == "chef") "主厨" else "食客",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (role == "chef") OrangePrimary else GrayDescription
                )
            }

            // Kick button: only visible to chef, for non-self, non-chef members
            if (isCurrentUserChef && !isSelf && role != "chef") {
                IconButton(
                    onClick = { showRemoveConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonRemove,
                        contentDescription = "移除",
                        tint = PriceRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerUrlSection(
    currentUrl: String,
    onUrlChanged: (String) -> Unit
) {
    var urlText by remember(currentUrl) { mutableStateOf(currentUrl) }
    var isEditing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    text = "服务器地址",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF616161)
                )
                if (!isEditing) {
                    TextButton(onClick = { isEditing = true }) {
                        Text("修改", fontSize = 13.sp, color = OrangePrimary)
                    }
                }
            }

            if (isEditing) {
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("服务器URL", fontSize = 12.sp) },
                    placeholder = { Text("http://192.168.1.100:8080", fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedLabelColor = OrangePrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { isEditing = false }) {
                        Text("取消", color = GrayDescription)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onUrlChanged(urlText)
                            isEditing = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("保存", color = Color.White)
                    }
                }
            } else {
                Text(
                    text = currentUrl.ifBlank { "未设置" },
                    fontSize = 13.sp,
                    color = GrayDescription
                )
            }
        }
    }
}

@Composable
private fun EditNicknameDialog(
    currentNickname: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nickname by remember { mutableStateOf(currentNickname) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改昵称") },
        text = {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("昵称") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    focusedLabelColor = OrangePrimary
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nickname) },
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                enabled = nickname.isNotBlank()
            ) {
                Text("确认", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ChangePinDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改PIN码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { if (it.length <= 6) oldPin = it },
                    label = { Text("原PIN码") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedLabelColor = OrangePrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6) newPin = it },
                    label = { Text("新PIN码 (4-6位)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedLabelColor = OrangePrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(oldPin, newPin) },
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                enabled = oldPin.length in 4..6 && newPin.length in 4..6
            ) {
                Text("确认", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun RenameRoomDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改餐桌名称") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("餐桌名称") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    focusedLabelColor = OrangePrimary
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                enabled = name.isNotBlank()
            ) {
                Text("确认", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun SettingsActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    iconTint: Color,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(backgroundColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = GrayDescription
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(20.dp)
        )
    }
}
