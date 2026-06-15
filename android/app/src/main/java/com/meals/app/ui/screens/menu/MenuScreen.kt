package com.meals.app.ui.screens.menu

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.meals.app.data.dto.CategoryDto
import com.meals.app.data.dto.DishDto
import com.meals.app.data.local.Preferences

import kotlinx.coroutines.delay

import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────
//  Theme Colors — warm, appetizing palette
// ─────────────────────────────────────────────────────

private val OrangePrimary = Color(0xFFFF6B35)
private val OrangeLight = Color(0xFFFF8A65)
private val OrangeDark = Color(0xFFE55A2B)
private val OrangeSoft = Color(0xFFFFF3E0)
private val PriceColor = Color(0xFFE53935)
private val GrayDesc = Color(0xFF9E9E9E)
private val GrayHint = Color(0xFFBDBDBD)
private val BgColor = Color(0xFFF8F6F3)          // warm off-white background
private val CardBg = Color(0xFFFFFFFF)
private val DarkText = Color(0xFF212121)
private val MediumText = Color(0xFF424242)
private val LightGrayBG = Color(0xFFF5F5F5)
private val CategorySelectedBg = Color(0xFFFFF3E0)
private val CategoryBg = Color(0xFFFAF8F5)       // sidebar warm bg
private val WarmGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFF8A65), Color(0xFFFF6B35))
)
private val CartGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF3D3D3D), Color(0xFF2A2A2A))
)
private val SoldOutOverlay = Color(0xB3FFFFFF)    // semi-transparent white

private val MenuColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = OrangeLight,
    secondary = OrangeLight,
    background = BgColor,
    onBackground = DarkText,
    surface = CardBg,
    onSurface = DarkText,
    error = Color(0xFFE53935)
)

// ─────────────────────────────────────────────────────
//  Main Screen
// ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MenuScreen(
    navController: NavHostController,
    onNavigateToCart: () -> Unit,
    onNavigateToOrderDetail: (Int) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToDishEdit: (String) -> Unit = {},
    viewModel: MenuViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val isChef = Preferences.role == "chef"
    val roomName = Preferences.activeRoomName ?: "点菜"

    // Random dish picker state
    var showRandomPicker by remember { mutableStateOf(false) }
    var randomDish by remember { mutableStateOf<DishDto?>(null) }
    var showDishDetail by remember { mutableStateOf<DishDto?>(null) }
    var showCartPreview by remember { mutableStateOf(false) }

    // Pull-to-refresh state (Material 2 interop for pull refresh)
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    MaterialTheme(colorScheme = MenuColorScheme) {
        Scaffold(
            topBar = {
                // Warm gradient top bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmGradient)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = roomName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Random dish picker button
                        if (state.allDishes.isNotEmpty()) {
                            IconButton(onClick = {
                                val dishes = state.allDishes.filter { it.is_available }
                                if (dishes.isNotEmpty()) {
                                    randomDish = dishes.random()
                                    showRandomPicker = true
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Shuffle,
                                    contentDescription = "随机选菜",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        if (isChef) {
                            IconButton(onClick = onNavigateToAdmin) {
                                Icon(
                                    imageVector = Icons.Outlined.EditNote,
                                    contentDescription = "管理后台",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            },
            containerColor = BgColor
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Main content: two-pane layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(pullRefreshState)
                ) {
                    // ── Left: Category sidebar ──
                    CategorySidebar(
                        categories = state.categories,
                        selectedCategoryId = state.selectedCategoryId,
                        onCategorySelected = { viewModel.selectCategory(it) },
                        modifier = Modifier
                            .width(80.dp)
                            .fillMaxHeight()
                            .background(CategoryBg)
                    )

                    // ── Right: Dish list ──
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFEAE7E2))
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (state.isLoading && state.dishes.isEmpty()) {
                            // Loading state
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = OrangePrimary,
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "加载中...",
                                        color = GrayDesc,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else if (state.dishes.isEmpty()) {
                            // Empty state - warm and friendly
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(OrangeSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🍽️",
                                            fontSize = 36.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "暂无菜品",
                                        color = MediumText,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "让厨师添加新菜品吧",
                                        color = GrayHint,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 8.dp,
                                    end = 8.dp,
                                    top = 6.dp,
                                    bottom = 80.dp // space for cart bar
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                state = rememberLazyListState()
                            ) {
                                items(
                                    items = state.dishes,
                                    key = { it.id }
                                ) { dish ->
                                    DishCard(
                                        dish = dish,
                                        cartItem = state.cartItems[dish.id],
                                        isChef = isChef,
                                        onAddToCart = {
                                            if (dish.seasonings.isNotEmpty()) {
                                                viewModel.showSeasoningPanel(dish)
                                            } else {
                                                viewModel.addToCart(dish, 1)
                                            }
                                        },
                                        onIncrement = {
                                            if (dish.seasonings.isNotEmpty() && state.cartItems[dish.id] == null) {
                                                viewModel.showSeasoningPanel(dish)
                                            } else {
                                                viewModel.updateCartQuantity(
                                                    dish.id,
                                                    (state.cartItems[dish.id]?.quantity ?: 0) + 1
                                                )
                                            }
                                        },
                                        onDecrement = {
                                            val current = state.cartItems[dish.id]?.quantity ?: 0
                                            viewModel.updateCartQuantity(dish.id, current - 1)
                                        },
                                        onShowDetail = {
                                            showDishDetail = dish
                                        }
                                    )
                                }
                            }
                        }

                        // Pull refresh indicator
                        PullRefreshIndicator(
                            refreshing = state.isRefreshing,
                            state = pullRefreshState,
                            modifier = Modifier.align(Alignment.TopCenter),
                            contentColor = OrangePrimary
                        )
                    }
                }

                // ── Bottom: Floating Cart Bar ──
                AnimatedVisibility(
                    visible = viewModel.cartTotalCount > 0,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(200)),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(animationSpec = tween(150)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    CartBar(
                        totalCount = viewModel.cartTotalCount,
                        totalPrice = viewModel.cartTotalPrice,
                        onClick = { showCartPreview = true }
                    )
                }

                // Error snackbar-like display with auto-dismiss
                state.error?.let { error ->
                    LaunchedEffect(error) {
                        delay(3000)
                        viewModel.clearError()
                    }
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text(
                            text = error,
                            color = Color.White,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // ── Seasoning Overlay Panel ──
        if (state.showSeasoningPanel && state.selectedDishForSeasoning != null) {
            SeasoningOverlay(
                dish = state.selectedDishForSeasoning!!,
                onDismiss = { viewModel.hideSeasoningPanel() },
                onConfirm = { selections ->
                    viewModel.addToCart(state.selectedDishForSeasoning!!, 1, selections)
                    viewModel.hideSeasoningPanel()
                }
            )
        }

        // ── Random Dish Picker Dialog ──
        if (showRandomPicker && randomDish != null) {
            RandomDishPickerDialog(
                dish = randomDish!!,
                onDismiss = { showRandomPicker = false },
                onAddToCart = {
                    if (randomDish!!.seasonings.isNotEmpty()) {
                        viewModel.showSeasoningPanel(randomDish!!)
                    } else {
                        viewModel.addToCart(randomDish!!, 1)
                    }
                    showRandomPicker = false
                },
                onReroll = {
                    val dishes = state.allDishes.filter { it.is_available }
                    if (dishes.isNotEmpty()) {
                        randomDish = dishes.random()
                    }
                }
            )
        }

        // ── Dish Detail Bottom Sheet ──
        showDishDetail?.let { dish ->
            DishDetailSheet(
                dish = dish,
                cartItem = state.cartItems[dish.id],
                onDismiss = { showDishDetail = null },
                onAddToCart = {
                    if (dish.seasonings.isNotEmpty()) {
                        viewModel.showSeasoningPanel(dish)
                    } else {
                        viewModel.addToCart(dish, 1)
                    }
                    showDishDetail = null
                }
            )
        }

        // ── Cart Preview Bottom Sheet ──
        if (showCartPreview) {
            CartPreviewSheet(
                cartItems = state.cartItems.values.toList(),
                onDismiss = { showCartPreview = false },
                onNavigateToCheckout = {
                    showCartPreview = false
                    onNavigateToCart()
                },
                onIncrement = { dishId ->
                    val current = state.cartItems[dishId]?.quantity ?: 0
                    viewModel.updateCartQuantity(dishId, current + 1)
                },
                onDecrement = { dishId ->
                    val current = state.cartItems[dishId]?.quantity ?: 0
                    viewModel.updateCartQuantity(dishId, current - 1)
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────
//  Category Sidebar
// ─────────────────────────────────────────────────────

@Composable
private fun CategorySidebar(
    categories: List<CategoryDto>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = categories,
            key = { it.id }
        ) { category ->
            val isSelected = category.id == selectedCategoryId
            CategoryItem(
                category = category,
                isSelected = isSelected,
                onClick = { onCategorySelected(category.id) }
            )
            // Separator between categories
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 0.5.dp,
                color = Color(0xFFE8E5E0)
            )
        }
    }
}

@Composable
private fun CategoryItem(
    category: CategoryDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) CategorySelectedBg else Color.Transparent,
        animationSpec = tween(200),
        label = "catBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) OrangePrimary else MediumText,
        animationSpec = tween(200),
        label = "catTextColor"
    )
    val iconBg by animateColorAsState(
        targetValue = if (isSelected) OrangeSoft else Color.Transparent,
        animationSpec = tween(200),
        label = "catIconBg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bgColor)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(52.dp)
                        .background(OrangePrimary)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 8.dp,
                        horizontal = if (isSelected) 4.dp else 7.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (category.icon.isNotBlank()) category.icon
                               else category.name.take(1),
                        fontSize = if (category.icon.isNotBlank()) 18.sp else 13.sp,
                        color = if (isSelected) OrangePrimary else GrayDesc,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = category.name,
                    fontSize = 11.sp,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )

                if (category.dish_count > 0) {
                    Text(
                        text = "${category.dish_count}道",
                        fontSize = 9.sp,
                        color = if (isSelected) OrangeLight else GrayHint,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────
//  Dish Card
// ─────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DishCard(
    dish: DishDto,
    cartItem: CartItem?,
    isChef: Boolean,
    onAddToCart: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onShowDetail: () -> Unit = {}
) {
    val quantity = cartItem?.quantity ?: 0
    val inCartBorder by animateColorAsState(
        targetValue = if (quantity > 0) OrangePrimary.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(250),
        label = "cartBorder"
    )
    val isSoldOut = !dish.is_available

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .border(
                width = if (quantity > 0) 1.dp else 0.dp,
                color = inCartBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // ── Dish Image ──
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LightGrayBG)
                    .clickable(enabled = !isSoldOut) { onShowDetail() },
                contentAlignment = Alignment.Center
            ) {
                if (!dish.image_url.isNullOrBlank()) {
                    AsyncImage(
                        model = dish.image_url,
                        contentDescription = dish.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(OrangeSoft.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🍜", fontSize = 28.sp)
                    }
                }

                if (isSoldOut) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SoldOutOverlay),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "已售罄",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // ── Dish Info ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !isSoldOut) { onShowDetail() },
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Name
                Text(
                    text = dish.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSoldOut) GrayDesc else DarkText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Price pill
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFF0EB), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "¥${String.format("%.1f", dish.price)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PriceColor
                    )
                }

                // Description
                if (!dish.description.isNullOrBlank()) {
                    Text(
                        text = dish.description,
                        fontSize = 11.sp,
                        color = GrayHint,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }

                // Tags
                if (dish.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        dish.tags.take(3).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(OrangeSoft, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(text = tag, fontSize = 9.sp, color = OrangePrimary)
                            }
                        }
                        if (dish.tags.size > 3) {
                            Text(
                                text = "+${dish.tags.size - 3}",
                                fontSize = 9.sp, color = GrayHint
                            )
                        }
                    }
                }


            }

            // ── Quantity Controls (compact rectangular style) ──
            if (!isSoldOut) {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (quantity > 0) {
                            // Minus — small rounded rectangle
                            val mIS = remember { MutableInteractionSource() }
                            val mPressed by mIS.collectIsPressedAsState()
                            val mScale by animateFloatAsState(
                                targetValue = if (mPressed) 0.9f else 1f,
                                animationSpec = tween(100), label = "ms"
                            )
                            val mBg by animateColorAsState(
                                targetValue = if (mPressed) Color(0xFFD8D8D8) else LightGrayBG,
                                animationSpec = tween(100), label = "mb"
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 26.dp, height = 22.dp)
                                    .scale(mScale)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(mBg)
                                    .clickable(
                                        onClick = onDecrement,
                                        interactionSource = mIS,
                                        indication = null
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "−",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MediumText
                                )
                            }

                            // Quantity display
                            AnimatedContent(
                                targetState = quantity,
                                transitionSpec = {
                                    fadeIn(tween(150)) + scaleIn(initialScale = 0.7f) togetherWith
                                        fadeOut(tween(150)) + scaleOut(targetScale = 0.7f)
                                },
                                label = "quantity"
                            ) { qty ->
                                Text(
                                    text = qty.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        // Plus — small rounded rectangle with brand color
                        val pIS = remember { MutableInteractionSource() }
                        val pPressed by pIS.collectIsPressedAsState()
                        val pScale by animateFloatAsState(
                            targetValue = if (pPressed) 0.9f else 1f,
                            animationSpec = tween(100), label = "ps"
                        )
                        val pBg by animateColorAsState(
                            targetValue = if (pPressed) OrangeDark else OrangePrimary,
                            animationSpec = tween(100), label = "pb"
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 26.dp, height = 22.dp)
                                .scale(pScale)
                                .clip(RoundedCornerShape(5.dp))
                                .background(pBg)
                                .clickable(
                                    onClick = if (quantity > 0) onIncrement else onAddToCart,
                                    interactionSource = pIS,
                                    indication = null
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
// ─────────────────────────────────────────────────────
//  Cart Bar (floating bottom bar)
// ─────────────────────────────────────────────────────

@Composable
private fun CartBar(
    totalCount: Int,
    totalPrice: Double,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(16.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF333333)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CartGradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cart icon with badge
                BadgedBox(
                    badge = {
                        if (totalCount > 0) {
                            Badge(
                                containerColor = OrangePrimary,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = totalCount.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ShoppingBag,
                        contentDescription = "购物车",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Total price
                Column {
                    Text(
                        text = "合计",
                        fontSize = 11.sp,
                        color = Color(0xFFAAAAAA)
                    )
                    AnimatedContent(
                        targetState = String.format("%.1f", totalPrice),
                        transitionSpec = {
                            fadeIn(tween(200)) + scaleIn(initialScale = 0.8f) togetherWith
                                fadeOut(tween(150)) + scaleOut(targetScale = 0.8f)
                        },
                        label = "totalPrice"
                    ) { price ->
                        Text(
                            text = "¥$price",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Checkout button with gradient
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangePrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "去结算",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────
//  Seasoning Overlay Panel (custom, avoids ModalBottomSheet touch issues)
// ─────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeasoningOverlay(
    dish: DishDto,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, List<String>>) -> Unit
) {
    // Track user selections: seasoning name -> list of selected option names
    val selections = remember {
        mutableStateMapOf<String, MutableList<String>>().apply {
            dish.seasonings.forEach { seasoning ->
                val name = seasoning["name"] as? String ?: ""
                val type = seasoning["type"] as? String ?: "single"
                val options = (seasoning["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val defaultSelection = if (type == "multi") {
                    mutableListOf<String>()
                } else {
                    mutableListOf(options.firstOrNull() ?: "")
                }
                put(name, defaultSelection)
            }
        }
    }

    // Full-screen overlay using Column to separate scrim from panel
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Scrim (takes remaining space, catches taps to dismiss)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onDismiss() }
        )

        // Bottom panel
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = CardBg,
            shadowElevation = 8.dp
        ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 32.dp)
                ) {
                    // Drag handle indicator
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFE0E0E0))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "选择调味",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dish.name,
                                fontSize = 13.sp,
                                color = GrayDesc
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = GrayDesc
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Seasoning groups
                    dish.seasonings.forEach { seasoning ->
                        val name = seasoning["name"] as? String ?: ""
                        val type = seasoning["type"] as? String ?: "single"
                        val description = seasoning["description"] as? String
                        val options = (seasoning["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        val isMultiSelect = type == "multi"

                        Text(
                            text = name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkText
                        )

                        if (!description.isNullOrBlank()) {
                            Text(
                                text = description,
                                fontSize = 12.sp,
                                color = GrayDesc
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Options as selectable chips (using Button for reliable touch handling)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val currentSelections = selections[name] ?: mutableListOf()

                            options.forEach { option ->
                                val isSelected = currentSelections.contains(option)

                                Button(
                                    onClick = {
                                        // Create a NEW mutable list to trigger recomposition
                                        val currentList = selections[name]?.toMutableList() ?: mutableListOf()
                                        if (isMultiSelect) {
                                            if (isSelected) {
                                                currentList.remove(option)
                                            } else {
                                                currentList.add(option)
                                            }
                                        } else {
                                            currentList.clear()
                                            if (!isSelected) {
                                                currentList.add(option)
                                            }
                                        }
                                        selections[name] = currentList
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) OrangePrimary.copy(alpha = 0.15f) else LightGrayBG,
                                        contentColor = if (isSelected) OrangePrimary else DarkText
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) OrangePrimary else Color(0xFFE0E0E0)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = option,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Confirm button
                    Button(
                        onClick = {
                            val result: Map<String, List<String>> = selections.mapValues { it.value.toList() }
                            onConfirm(result)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangePrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = "确认并加入购物车",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

// ─────────────────────────────────────────────────────
//  Random Dish Picker Dialog
// ─────────────────────────────────────────────────────

@Composable
private fun RandomDishPickerDialog(
    dish: DishDto,
    onDismiss: () -> Unit,
    onAddToCart: () -> Unit,
    onReroll: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "今天吃什么?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "让命运来决定你的午餐",
                    fontSize = 13.sp,
                    color = GrayHint
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Dish image
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(LightGrayBG),
                    contentAlignment = Alignment.Center
                ) {
                    if (!dish.image_url.isNullOrBlank()) {
                        AsyncImage(
                            model = dish.image_url,
                            contentDescription = dish.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(text = "🍜", fontSize = 48.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dish name
                Text(
                    text = dish.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Price
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFFFF0EB),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "¥${String.format("%.1f", dish.price)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PriceColor
                    )
                }

                // Description
                if (!dish.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = dish.description,
                        fontSize = 13.sp,
                        color = GrayDesc,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Reroll button
                    Button(
                        onClick = onReroll,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightGrayBG,
                            contentColor = MediumText
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "换一个",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Add to cart button
                    Button(
                        onClick = onAddToCart,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangePrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = "就它了",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────
//  Dish Detail Bottom Sheet
// ─────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DishDetailSheet(
    dish: DishDto,
    cartItem: CartItem?,
    onDismiss: () -> Unit,
    onAddToCart: () -> Unit
) {
    val quantity = cartItem?.quantity ?: 0

    // Full-screen overlay
    Column(modifier = Modifier.fillMaxSize()) {
        // Scrim
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable { onDismiss() }
        )

        // Bottom panel
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = CardBg,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 32.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFE0E0E0))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(LightGrayBG),
                    contentAlignment = Alignment.Center
                ) {
                    if (!dish.image_url.isNullOrBlank()) {
                        AsyncImage(
                            model = dish.image_url,
                            contentDescription = dish.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(text = "🍜", fontSize = 64.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name
                Text(
                    text = dish.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Price
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFFFF0EB),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "¥${String.format("%.1f", dish.price)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PriceColor
                    )
                }

                // Description
                if (!dish.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = dish.description,
                        fontSize = 14.sp,
                        color = MediumText,
                        lineHeight = 22.sp
                    )
                }

                // Tags
                if (dish.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "标签",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dish.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = OrangeSoft,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 12.sp,
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Seasoning info
                if (dish.seasonings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "可选调味",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    dish.seasonings.forEach { seasoning ->
                        val name = seasoning["name"] as? String ?: ""
                        val options = (seasoning["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🧂", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$name: ${options.joinToString("、")}",
                                fontSize = 13.sp,
                                color = MediumText
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Add to cart button
                Button(
                    onClick = onAddToCart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangePrimary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (quantity > 0) "再来一份 (已在购物车: $quantity)" else "加入购物车",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────
//  Cart Preview Bottom Sheet
// ─────────────────────────────────────────────────────

@Composable
private fun CartPreviewSheet(
    cartItems: List<CartItem>,
    onDismiss: () -> Unit,
    onNavigateToCheckout: () -> Unit,
    onIncrement: (Int) -> Unit,
    onDecrement: (Int) -> Unit
) {
    val totalPrice = cartItems.sumOf { it.dish.price * it.quantity }
    val totalCount = cartItems.sumOf { it.quantity }

    Column(modifier = Modifier.fillMaxSize()) {
        // Scrim
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onDismiss() }
        )

        // Bottom panel
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = CardBg,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 32.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFE0E0E0))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "购物车",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Text(
                        text = "$totalCount 件商品",
                        fontSize = 13.sp,
                        color = GrayDesc
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cart items list (max height to keep sheet reasonable)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    cartItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Name + seasoning hint
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.dish.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.seasoningSelections.isNotEmpty()) {
                                    val summary = item.seasoningSelections
                                        .filter { it.value.isNotEmpty() }
                                        .map { "${it.key}: ${it.value.joinToString("、")}" }
                                        .joinToString(" | ")
                                    if (summary.isNotBlank()) {
                                        Text(
                                            text = summary,
                                            fontSize = 11.sp,
                                            color = OrangeLight,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Quantity controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Decrement
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(LightGrayBG)
                                        .clickable { onDecrement(item.dish.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "−",
                                        fontSize = 13.sp,
                                        color = MediumText,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = item.quantity.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(18.dp)
                                )

                                // Increment
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(OrangePrimary)
                                        .clickable { onIncrement(item.dish.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+",
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Subtotal
                            Text(
                                text = "¥${String.format("%.1f", item.dish.price * item.quantity)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PriceColor
                            )
                        }

                        // Separator between items
                        if (index < cartItems.lastIndex) {
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                thickness = 0.5.dp,
                                color = Color(0xFFEEEEEE)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom: total + checkout button
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 1.dp,
                    color = Color(0xFFE0E0E0)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total
                    Column {
                        Text(
                            text = "合计",
                            fontSize = 11.sp,
                            color = GrayDesc
                        )
                        AnimatedContent(
                            targetState = String.format("%.1f", totalPrice),
                            transitionSpec = {
                                fadeIn(tween(200)) + scaleIn(initialScale = 0.8f) togetherWith
                                    fadeOut(tween(150)) + scaleOut(targetScale = 0.8f)
                            },
                            label = "previewTotal"
                        ) { price ->
                            Text(
                                text = "¥$price",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PriceColor
                            )
                        }
                    }

                    // Checkout button
                    Button(
                        onClick = onNavigateToCheckout,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangePrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = "去结算",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}