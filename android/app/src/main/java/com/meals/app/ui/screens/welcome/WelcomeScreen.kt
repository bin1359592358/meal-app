package com.meals.app.ui.screens.welcome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Theme colors
private val OrangePrimary = Color(0xFFFF6B35)
private val OrangeLight = Color(0xFFFF8A65)
private val OrangeDark = Color(0xFFE55A2B)
private val BgColor = Color(0xFFFAFAFA)
private val CardBgColor = Color(0xFFFFFFFF)
private val ErrorColor = Color(0xFFE53935)
private val GrayText = Color(0xFF9E9E9E)
private val DarkText = Color(0xFF212121)

private val WelcomeLightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = OrangeLight,
    onPrimaryContainer = Color.White,
    secondary = OrangeLight,
    onSecondary = Color.White,
    background = BgColor,
    onBackground = DarkText,
    surface = CardBgColor,
    onSurface = DarkText,
    error = ErrorColor,
    onError = Color.White
)

@Composable
fun WelcomeScreen(
    onNavigateToMain: () -> Unit,
    skipToRoomSetup: Boolean = false,
    joinRoom: Boolean = false,
    viewModel: WelcomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    // If user is already logged in but has no room, skip to room setup
    LaunchedEffect(skipToRoomSetup) {
        if (skipToRoomSetup) {
            viewModel.fetchRoomsForExistingUser()
        }
    }

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) {
            onNavigateToMain()
        }
    }

    MaterialTheme(colorScheme = WelcomeLightColorScheme) {
        Scaffold(
            containerColor = BgColor
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(BgColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(60.dp))

                    // Logo area
                    LogoSection()

                    Spacer(modifier = Modifier.height(40.dp))

                    // Auth card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBgColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (skipToRoomSetup && state.isLoading && !state.showRoomSetup) {
                                // Loading state while fetching rooms for existing user
                                Spacer(modifier = Modifier.height(32.dp))
                                CircularProgressIndicator(color = OrangePrimary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "正在加载餐桌...",
                                    color = GrayText,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                            } else if (!state.showRoomSetup && !joinRoom) {
                                // Tab row for login/register
                                AuthTabs(
                                    isRegisterMode = state.isRegisterMode,
                                    onToggleMode = { viewModel.toggleMode() }
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Error message
                                AnimatedVisibility(
                                    visible = state.error != null,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    state.error?.let { error ->
                                        Text(
                                            text = error,
                                            color = ErrorColor,
                                            fontSize = 14.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp)
                                                .background(
                                                    Color(0xFFFFEBEE),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(12.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                if (!state.isRegisterMode) {
                                    LoginForm(
                                        username = state.username,
                                        pin = state.pin,
                                        isLoading = state.isLoading,
                                        onUsernameChange = viewModel::updateUsername,
                                        onPinChange = viewModel::updatePin,
                                        onLogin = { viewModel.login() }
                                    )
                                } else {
                                    RegisterForm(
                                        username = state.username,
                                        nickname = state.nickname,
                                        pin = state.pin,
                                        isLoading = state.isLoading,
                                        onUsernameChange = viewModel::updateUsername,
                                        onNicknameChange = viewModel::updateNickname,
                                        onPinChange = viewModel::updatePin,
                                        onRegister = { viewModel.register() }
                                    )
                                }
                            } else {
                                // Room setup after registration or from join entry
                                RoomSetupSection(
                                    isLoading = state.isLoading,
                                    error = state.error,
                                    roomCode = state.roomCode,
                                    createdRoomCode = state.createdRoomCode,
                                    joinMode = joinRoom || skipToRoomSetup,
                                    onRoomCodeChange = viewModel::updateRoomCode,
                                    onCreateRoom = { name -> viewModel.createRoom(name) },
                                    onJoinRoom = { viewModel.joinRoom() }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Footer text
                    Text(
                        text = "美味共享，快乐点菜",
                        color = GrayText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Loading overlay
                AnimatedVisibility(
                    visible = state.isLoading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x80000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBgColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = OrangePrimary,
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "请稍候...",
                                    color = DarkText,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(OrangePrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "🍽️ 点菜",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "和朋友一起，轻松点菜",
            fontSize = 15.sp,
            color = GrayText
        )
    }
}

@Composable
private fun AuthTabs(
    isRegisterMode: Boolean,
    onToggleMode: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(if (isRegisterMode) 1 else 0) }

    LaunchedEffect(isRegisterMode) {
        selectedTabIndex = if (isRegisterMode) 1 else 0
    }

    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = Color.Transparent,
        contentColor = OrangePrimary,
        indicator = { tabPositions ->
            if (selectedTabIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    height = 3.dp,
                    color = OrangePrimary
                )
            }
        },
        divider = {}
    ) {
        Tab(
            selected = !isRegisterMode,
            onClick = {
                selectedTabIndex = 0
                if (isRegisterMode) onToggleMode()
            },
            text = {
                Text(
                    text = "登录",
                    fontSize = 16.sp,
                    fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (!isRegisterMode) OrangePrimary else GrayText
                )
            }
        )
        Tab(
            selected = isRegisterMode,
            onClick = {
                selectedTabIndex = 1
                if (!isRegisterMode) onToggleMode()
            },
            text = {
                Text(
                    text = "注册",
                    fontSize = 16.sp,
                    fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (isRegisterMode) OrangePrimary else GrayText
                )
            }
        )
    }
}

@Composable
private fun LoginForm(
    username: String,
    pin: String,
    isLoading: Boolean,
    onUsernameChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onLogin: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("用户名") },
            placeholder = { Text("请输入用户名", color = GrayText) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = GrayText
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangePrimary,
                focusedLabelColor = OrangePrimary,
                cursorColor = OrangePrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6) onPinChange(it) },
            label = { Text("PIN 密码") },
            placeholder = { Text("请输入6位数字密码", color = GrayText) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = GrayText
                )
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangePrimary,
                focusedLabelColor = OrangePrimary,
                cursorColor = OrangePrimary
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onLogin,
            enabled = !isLoading && username.isNotBlank() && pin.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OrangePrimary,
                contentColor = Color.White,
                disabledContainerColor = OrangePrimary.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            )
        ) {
            Text(
                text = "登 录",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RegisterForm(
    username: String,
    nickname: String,
    pin: String,
    isLoading: Boolean,
    onUsernameChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onRegister: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("用户名") },
            placeholder = { Text("请输入用户名", color = GrayText) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = GrayText
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangePrimary,
                focusedLabelColor = OrangePrimary,
                cursorColor = OrangePrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            label = { Text("昵称") },
            placeholder = { Text("请输入昵称", color = GrayText) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = GrayText
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangePrimary,
                focusedLabelColor = OrangePrimary,
                cursorColor = OrangePrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6) onPinChange(it) },
            label = { Text("PIN 密码") },
            placeholder = { Text("请设置6位数字密码", color = GrayText) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = GrayText
                )
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangePrimary,
                focusedLabelColor = OrangePrimary,
                cursorColor = OrangePrimary
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRegister,
            enabled = !isLoading && username.isNotBlank() && nickname.isNotBlank() && pin.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OrangePrimary,
                contentColor = Color.White,
                disabledContainerColor = OrangePrimary.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            )
        ) {
            Text(
                text = "注 册",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RoomSetupSection(
    isLoading: Boolean,
    error: String?,
    roomCode: String,
    createdRoomCode: String?,
    joinMode: Boolean = false,
    onRoomCodeChange: (String) -> Unit,
    onCreateRoom: (String) -> Unit,
    onJoinRoom: () -> Unit
) {
    var roomName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (joinMode) "🍽️ 创建或加入餐桌" else "🎉 注册成功！",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "创建或加入一个餐桌，开始点菜吧",
            fontSize = 14.sp,
            color = GrayText
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error display
        if (error != null) {
            Text(
                text = error,
                color = ErrorColor,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                textAlign = TextAlign.Center
            )
        }

        // Create room section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🍳 创建餐桌",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("餐桌名称") },
                    placeholder = { Text("例如：周末聚餐", color = GrayText) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedLabelColor = OrangePrimary,
                        cursorColor = OrangePrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onCreateRoom(roomName) },
                    enabled = !isLoading && roomName.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangePrimary,
                        contentColor = Color.White,
                        disabledContainerColor = OrangePrimary.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = "创建餐桌",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Show created room code
                if (createdRoomCode != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Text(
                            text = "餐桌已创建！邀请码：$createdRoomCode",
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Divider with "or"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color(0xFFE0E0E0))
            )
            Text(
                text = "  或者  ",
                color = GrayText,
                fontSize = 13.sp
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color(0xFFE0E0E0))
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Join room section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🪑 加入餐桌",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = roomCode,
                    onValueChange = onRoomCodeChange,
                    label = { Text("邀请码") },
                    placeholder = { Text("输入好友分享的邀请码", color = GrayText) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedLabelColor = OrangePrimary,
                        cursorColor = OrangePrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onJoinRoom,
                    enabled = !isLoading && roomCode.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OrangePrimary
                    )
                ) {
                    Text(
                        text = "加入餐桌",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
