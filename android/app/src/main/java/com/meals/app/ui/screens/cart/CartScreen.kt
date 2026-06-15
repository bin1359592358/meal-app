package com.meals.app.ui.screens.cart

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.meals.app.ui.screens.menu.CartItem

// ─────────────────────────────────────────────────────
//  Theme Colors (consistent with other screens)
// ─────────────────────────────────────────────────────

private val OrangePrimary = Color(0xFFFF6B35)
private val OrangeLight = Color(0xFFFF8A65)
private val PriceColor = Color(0xFFE53935)
private val GrayDesc = Color(0xFF9E9E9E)
private val BgColor = Color(0xFFF8F6F3)
private val CardBg = Color(0xFFFFFFFF)
private val DarkText = Color(0xFF212121)
private val LightGrayBG = Color(0xFFF5F5F5)
private val ErrorColor = Color(0xFFE53935)

private val CartColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    background = BgColor,
    onBackground = DarkText,
    surface = CardBg,
    onSurface = DarkText,
    error = ErrorColor
)

// ─────────────────────────────────────────────────────
//  Cart Screen
// ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavHostController,
    onOrderPlaced: (String) -> Unit,
    viewModel: CartViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    // Navigate to order detail on successful order
    LaunchedEffect(state.orderResult) {
        state.orderResult?.let { order ->
            onOrderPlaced(order.id.toString())
        }
    }

    MaterialTheme(colorScheme = CartColorScheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "确认订单",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = DarkText
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CardBg
                    )
                )
            },
            bottomBar = {
                // ── Bottom: Total + Confirm button ──
                OrderBottomBar(
                    totalPrice = viewModel.totalPrice,
                    itemCount = viewModel.totalCount,
                    isSubmitting = state.isSubmitting,
                    isEmpty = state.items.isEmpty(),
                    onConfirm = { viewModel.submitOrder() }
                )
            },
            containerColor = BgColor
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (state.items.isEmpty() && state.orderResult == null) {
                    // Empty cart
                    EmptyCartView()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ── Cart Items ──
                        items(
                            items = state.items,
                            key = { it.dish.id }
                        ) { cartItem ->
                            CartItemCard(
                                cartItem = cartItem,
                                onIncrement = {
                                    viewModel.updateItemQuantity(
                                        cartItem.dish.id,
                                        cartItem.quantity + 1
                                    )
                                },
                                onDecrement = {
                                    viewModel.updateItemQuantity(
                                        cartItem.dish.id,
                                        cartItem.quantity - 1
                                    )
                                },
                                onRemove = {
                                    viewModel.removeItem(cartItem.dish.id)
                                }
                            )
                        }

                        // ── People Count ──
                        item {
                            PeopleCountSelector(
                                count = state.peopleCount,
                                onCountChange = { viewModel.updatePeopleCount(it) }
                            )
                        }

                        // ── Note ──
                        item {
                            NoteSection(
                                note = state.note,
                                onNoteChange = { viewModel.updateNote(it) }
                            )
                        }

                        // ── Order Summary ──
                        item {
                            OrderSummaryCard(
                                items = state.items,
                                peopleCount = state.peopleCount,
                                totalPrice = viewModel.totalPrice
                            )
                        }

                        // Spacer for bottom bar
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // Error display
                state.error?.let { error ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "关闭",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Loading overlay
                AnimatedVisibility(
                    visible = state.isSubmitting,
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
                            colors = CardDefaults.cardColors(containerColor = CardBg)
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
                                    text = "正在下单...",
                                    color = DarkText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────
//  Empty Cart View
// ─────────────────────────────────────────────────────

@Composable
private fun EmptyCartView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Restaurant,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = GrayDesc.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "购物车是空的",
                fontSize = 16.sp,
                color = GrayDesc,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "返回菜单选择你喜欢的菜品吧",
                fontSize = 13.sp,
                color = GrayDesc.copy(alpha = 0.7f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────
//  Cart Item Card
// ─────────────────────────────────────────────────────

@Composable
private fun CartItemCard(
    cartItem: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    val subtotal = cartItem.dish.price * cartItem.quantity

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top row: name + remove button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cartItem.dish.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "移除",
                        tint = GrayDesc.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Seasoning summary
            if (cartItem.seasoningSelections.isNotEmpty()) {
                val summaryText = cartItem.seasoningSelections
                    .filter { it.value.isNotEmpty() }
                    .map { (key, values) ->
                        "$key: ${values.joinToString("、")}"
                    }
                    .joinToString("  |  ")

                if (summaryText.isNotBlank()) {
                    Text(
                        text = summaryText,
                        fontSize = 12.sp,
                        color = OrangeLight,
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: price + quantity controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subtotal price
                Text(
                    text = "¥${String.format("%.1f", subtotal)}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PriceColor
                )

                // Quantity controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Decrement
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(LightGrayBG)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "减少",
                            tint = DarkText,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Quantity with animation
                    AnimatedContent(
                        targetState = cartItem.quantity,
                        transitionSpec = {
                            fadeIn(tween(150)) + scaleIn(initialScale = 0.7f) togetherWith
                                fadeOut(tween(150)) + scaleOut(targetScale = 0.7f)
                        },
                        label = "cartQty"
                    ) { qty ->
                        Text(
                            text = qty.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(24.dp)
                        )
                    }

                    // Increment
                    IconButton(
                        onClick = onIncrement,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(OrangePrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "增加",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────
//  People Count Selector
// ─────────────────────────────────────────────────────

@Composable
private fun PeopleCountSelector(
    count: Int,
    onCountChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "用餐人数",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkText
                    )
                    Text(
                        text = "方便后厨备餐",
                        fontSize = 12.sp,
                        color = GrayDesc
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { if (count > 1) onCountChange(count - 1) },
                    enabled = count > 1,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (count > 1) LightGrayBG else LightGrayBG.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "减少人数",
                        tint = if (count > 1) DarkText else GrayDesc,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = count.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(28.dp)
                )

                IconButton(
                    onClick = { onCountChange(count + 1) },
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(OrangePrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "增加人数",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────
//  Note Section
// ─────────────────────────────────────────────────────

@Composable
private fun NoteSection(
    note: String,
    onNoteChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = "备注",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                placeholder = {
                    Text(
                        text = "例如：少放辣、不要香菜、多加蒜...",
                        color = GrayDesc.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    focusedLabelColor = OrangePrimary,
                    cursorColor = OrangePrimary,
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    color = DarkText
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────
//  Order Summary Card
// ─────────────────────────────────────────────────────

@Composable
private fun OrderSummaryCard(
    items: List<CartItem>,
    peopleCount: Int,
    totalPrice: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = "订单概览",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Item count row
            SummaryRow(label = "菜品数量", value = "${items.sumOf { it.quantity }} 道")

            Spacer(modifier = Modifier.height(6.dp))

            // People count row
            SummaryRow(label = "用餐人数", value = "$peopleCount 人")

            Spacer(modifier = Modifier.height(6.dp))

            // Variety count
            SummaryRow(label = "菜品种类", value = "${items.size} 种")

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = Color(0xFFEEEEEE),
                thickness = 1.dp
            )

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "合计",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Text(
                    text = "¥${String.format("%.1f", totalPrice)}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PriceColor
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = GrayDesc
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = DarkText,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─────────────────────────────────────────────────────
//  Order Bottom Bar
// ─────────────────────────────────────────────────────

@Composable
private fun OrderBottomBar(
    totalPrice: Double,
    itemCount: Int,
    isSubmitting: Boolean,
    isEmpty: Boolean,
    onConfirm: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBg,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price section
                Column {
                    Text(
                        text = "共 $itemCount 件",
                        fontSize = 12.sp,
                        color = GrayDesc
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "¥",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PriceColor
                        )
                        Text(
                            text = String.format("%.1f", totalPrice),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PriceColor
                        )
                    }
                }

                // Confirm button
                Button(
                    onClick = onConfirm,
                    enabled = !isSubmitting && !isEmpty,
                    modifier = Modifier
                        .width(160.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
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
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isSubmitting) "下单中" else "确认下单",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
