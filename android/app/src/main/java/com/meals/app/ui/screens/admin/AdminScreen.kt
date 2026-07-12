package com.meals.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.meals.app.data.dto.DishDto
import com.meals.app.data.local.Preferences
import com.meals.app.ui.navigation.Routes

private val OrangePrimary = Color(0xFFFF6B35)
private val PriceRed = Color(0xFFE53935)
private val GrayDescription = Color(0xFF9E9E9E)
private val BackgroundColor = Color(0xFFFAFAFA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    navController: NavHostController,
    onNavigateToDishEdit: (String) -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteCategoryDialog by remember { mutableStateOf<Int?>(null) }
    var showDeleteDishDialog by remember { mutableStateOf<Int?>(null) }

    // Add category dialog
    if (uiState.showAddCategoryDialog) {
        AddCategoryDialog(
            onConfirm = { name, icon -> viewModel.addCategory(name, icon) },
            onDismiss = { viewModel.hideAddCategoryDialog() }
        )
    }

    // Delete category confirmation
    showDeleteCategoryDialog?.let { categoryId ->
        AlertDialog(
            onDismissRequest = { showDeleteCategoryDialog = null },
            title = { Text("确认删除分类") },
            text = { Text("删除分类将同时删除该分类下的所有菜品，确定要删除吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(categoryId)
                    showDeleteCategoryDialog = null
                }) {
                    Text("删除", color = PriceRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCategoryDialog = null }) {
                    Text("取消")
                }
            }
        )
    }

    // Delete dish confirmation
    showDeleteDishDialog?.let { dishId ->
        AlertDialog(
            onDismissRequest = { showDeleteDishDialog = null },
            title = { Text("确认删除菜品") },
            text = { Text("确定要删除这道菜品吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDish(Preferences.activeRoomId, dishId)
                    showDeleteDishDialog = null
                }) {
                    Text("删除", color = PriceRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDishDialog = null }) {
                    Text("取消")
                }
            }
        )
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.categoryAdded) {
        if (uiState.categoryAdded) {
            snackbarHostState.showSnackbar("分类已添加")
            viewModel.clearCategoryAdded()
        }
    }

    LaunchedEffect(uiState.categoryDeleted) {
        if (uiState.categoryDeleted) {
            snackbarHostState.showSnackbar("分类已删除")
            viewModel.clearCategoryDeleted()
        }
    }

    LaunchedEffect(uiState.dishDeleted) {
        if (uiState.dishDeleted) {
            snackbarHostState.showSnackbar("菜品已删除")
            viewModel.clearDishDeleted()
        }
    }

    // Refresh data when navigating back to this screen (e.g., after returning from DishEditScreen)
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.route == Routes.ADMIN) {
                viewModel.loadCategories()
                val selectedCat = viewModel.uiState.value.selectedCategoryId
                if (selectedCat != null) {
                    viewModel.loadDishes(selectedCat)
                }
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "菜单管理",
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
                    containerColor = BackgroundColor
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundColor
    ) { paddingValues ->
        if (uiState.isLoading && uiState.categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Right: Dish list (rendered first, behind left panel)
                Column(
                    modifier = Modifier
                        .padding(start = 101.dp)
                        .fillMaxSize()
                ) {
                    if (uiState.selectedCategoryId != null) {
                        // Dish list header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "菜品列表",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF212121)
                            )

                            Text(
                                text = "${uiState.dishes.size}道",
                                fontSize = 13.sp,
                                color = GrayDescription
                            )
                        }

                        if (uiState.dishes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestaurantMenu,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = GrayDescription.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "还没有菜品",
                                        fontSize = 14.sp,
                                        color = GrayDescription
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(
                                    items = uiState.dishes,
                                    key = { it.id }
                                ) { dish ->
                                    DishAdminRow(
                                        dish = dish,
                                        onEdit = { onNavigateToDishEdit(dish.id.toString()) },
                                        onToggle = { viewModel.toggleDish(dish.id) },
                                        onDelete = { showDeleteDishDialog = dish.id }
                                    )
                                }
                            }
                        }

                        // Add dish button
                        Button(
                            onClick = { onNavigateToDishEdit("-1") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("添加菜品", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestaurantMenu,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = GrayDescription.copy(alpha = 0.4f)
                                )
                                Text(
                                    text = "请选择或添加分类",
                                    fontSize = 15.sp,
                                    color = GrayDescription
                                )
                            }
                        }
                    }
                }

                // Left: Category list (rendered on top)
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight()
                        .background(Color.White),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = uiState.categories,
                            key = { it.id }
                        ) { category ->
                            CategoryAdminItem(
                                icon = category.icon,
                                name = category.name,
                                dishCount = category.dish_count,
                                isSelected = category.id == uiState.selectedCategoryId,
                                onClick = { viewModel.selectCategory(category.id) },
                                onDelete = { showDeleteCategoryDialog = category.id }
                            )
                        }
                    }

                    // Add category button
                    Button(
                        onClick = { viewModel.showAddCategoryDialog() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加分类", fontSize = 13.sp)
                    }
                }

                // Divider between panels
                Box(
                    modifier = Modifier
                        .padding(start = 100.dp)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFEEEEEE))
                )
            }
        }
    }
}

@Composable
private fun CategoryAdminItem(
    icon: String,
    name: String,
    dishCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (isSelected) OrangePrimary.copy(alpha = 0.1f) else Color.Transparent
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) OrangePrimary else Color(0xFF212121),
                maxLines = 1
            )
            Text(
                text = "${dishCount}道",
                fontSize = 10.sp,
                color = GrayDescription
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                tint = PriceRed.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun DishAdminRow(
    dish: DishDto,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dish info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dish.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )
                Text(
                    text = "¥${String.format("%.1f", dish.price)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = PriceRed
                )
            }

            // Available switch
            Switch(
                checked = dish.is_available,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = OrangePrimary
                ),
                modifier = Modifier.height(24.dp)
            )

            // Edit button
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFF5F5F5), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = OrangePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("\uD83C\uDF7D") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加分类") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedLabelColor = OrangePrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("图标 (emoji)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedLabelColor = OrangePrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                // Quick emoji picker
                Text(
                    text = "快捷选择",
                    fontSize = 12.sp,
                    color = GrayDescription
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("\uD83C\uDF5A", "\uD83C\uDF5C", "\uD83E\uDD57", "\uD83C\uDF56", "\uD83E\uDD64", "\uD83C\uDF70", "\uD83C\uDF55", "\uD83E\uDD58").forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (icon == emoji) OrangePrimary.copy(alpha = 0.1f) else Color(0xFFF5F5F5)
                                )
                                .clickable { icon = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, icon) },
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                enabled = name.isNotBlank()
            ) {
                Text("添加", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
