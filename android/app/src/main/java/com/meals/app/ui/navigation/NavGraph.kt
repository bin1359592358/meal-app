package com.meals.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.meals.app.data.remote.ApiClient
import com.meals.app.data.local.Preferences
import com.meals.app.ui.screens.admin.AdminScreen
import com.meals.app.ui.screens.admin.DishEditScreen
import com.meals.app.ui.screens.cart.CartScreen
import com.meals.app.ui.screens.menu.MenuScreen
import com.meals.app.ui.screens.orders.OrderDetailScreen
import com.meals.app.ui.screens.orders.OrderHistoryScreen
import com.meals.app.ui.enhancement.overview.OverviewScreen
import com.meals.app.ui.enhancement.roomswitch.RoomListScreen
import com.meals.app.ui.screens.profile.ProfileScreen
import com.meals.app.ui.screens.welcome.WelcomeScreen
import kotlinx.coroutines.launch

/**
 * Route constants used for navigation throughout the app.
 */
object Routes {
    const val WELCOME = "welcome"
    const val JOIN_ROOM = "join_room"
    const val MAIN = "main"
    const val MENU = "menu"
    const val CART = "cart"
    const val ORDER_HISTORY = "order_history"
    const val ORDER_DETAIL = "order_detail/{orderId}"
    const val PROFILE = "profile"
    const val ADMIN = "admin"
    const val DISH_EDIT = "dish_edit/{dishId}"
    const val OVERVIEW = "overview"
    const val ROOM_LIST = "room_list"

    // Helper functions for routes with arguments
    fun orderDetail(orderId: String) = "order_detail/$orderId"
    fun dishEdit(dishId: String) = "dish_edit/$dishId"
}

/**
 * Data class representing a bottom navigation bar item.
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

/**
 * Bottom navigation items displayed in the main scaffold.
 */
val bottomNavItems = listOf(
    BottomNavItem(
        route = Routes.MENU,
        label = "菜单",
        icon = Icons.Outlined.Restaurant,
        selectedIcon = Icons.Filled.Restaurant
    ),
    BottomNavItem(
        route = Routes.ORDER_HISTORY,
        label = "订单",
        icon = Icons.Outlined.Receipt,
        selectedIcon = Icons.Filled.Receipt
    ),
    BottomNavItem(
        route = Routes.PROFILE,
        label = "我的",
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Filled.Person
    )
)

/**
 * Routes where the bottom navigation bar should be hidden.
 * Uses prefix matching for parameterized routes.
 */
private val routesWithoutBottomBarPrefixes = setOf(
    Routes.WELCOME,
    Routes.JOIN_ROOM,
    Routes.CART,
    "dish_edit/",
    Routes.OVERVIEW,
    Routes.ROOM_LIST
)

/**
 * Root navigation graph composable.
 *
 * Manages the full navigation structure of the app including:
 * - Welcome/Login flow as start destination when no token exists
 * - Main screen with bottom navigation (Menu, Orders, Profile)
 * - Detail screens (Cart, OrderDetail, DishEdit, Overview)
 *
 * @param navController The NavHostController for managing navigation state.
 */
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine if bottom bar should be shown
    // Uses prefix matching to handle parameterized routes (e.g., "dish_edit/5")
    val showBottomBar = routesWithoutBottomBarPrefixes.none { prefix ->
        currentRoute != null && (currentRoute == prefix || currentRoute.startsWith(prefix))
    }

    // Check if user is logged in by looking at shared preferences
    val hasToken = rememberHasToken()
    val hasActiveRoom = com.meals.app.data.local.Preferences.activeRoomId > 0
    // Go to MAIN only if logged in AND has an active room
    val startDestination = if (hasToken && hasActiveRoom) Routes.MAIN else Routes.WELCOME

    var pendingOrderCount by remember { mutableIntStateOf(0) }

    // Load pending order count for badge
    LaunchedEffect(currentRoute) {
        if (currentRoute == Routes.ORDER_HISTORY || currentRoute == Routes.MENU || currentRoute == Routes.PROFILE) {
            try {
                val roomId = Preferences.activeRoomId
                if (roomId > 0) {
                    val response = ApiClient.getApiService().getOrders(roomId, 1, 50)
                    val body = response.body()
                    if (response.isSuccessful && body?.code == 0 && body.data != null) {
                        pendingOrderCount = body.data.count { it.status == "pending" || it.status == "preparing" }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                MealsBottomNavBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    pendingOrderCount = pendingOrderCount
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Welcome / Login / Register screen
            composable(Routes.WELCOME) {
                val alreadyLoggedIn = hasToken && !hasActiveRoom
                WelcomeScreen(
                    skipToRoomSetup = alreadyLoggedIn,
                    onNavigateToMain = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    }
                )
            }

            // Join / Create room entry for logged-in users
            composable(Routes.JOIN_ROOM) {
                WelcomeScreen(
                    joinRoom = true,
                    onNavigateToMain = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    }
                )
            }

            // Main container - this is the hub for bottom navigation tabs
            composable(Routes.MAIN) {
                // The MAIN route serves as a container.
                // When navigating to MAIN, redirect to the MENU tab via LaunchedEffect
                // to avoid calling navigate() during composition.
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.MENU) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            }

            // Menu tab - shows available dishes and categories
            composable(Routes.MENU) {
                MenuScreen(
                    navController = navController,
                    onNavigateToCart = {
                        navController.navigate(Routes.CART)
                    },
                    onNavigateToOrderDetail = { orderId ->
                        navController.navigate(Routes.orderDetail(orderId.toString()))
                    },
                    onNavigateToAdmin = {
                        navController.navigate(Routes.ADMIN)
                    },
                    onNavigateToDishEdit = { dishId ->
                        navController.navigate(Routes.dishEdit(dishId))
                    }
                )
            }

            // Cart screen - review and submit order
            composable(Routes.CART) {
                CartScreen(
                    navController = navController,
                    onOrderPlaced = { orderId ->
                        navController.navigate(Routes.orderDetail(orderId)) {
                            popUpTo(Routes.MENU) { inclusive = false }
                        }
                    }
                )
            }

            // Order History tab - list of past orders
            composable(Routes.ORDER_HISTORY) {
                OrderHistoryScreen(
                    navController = navController,
                    onNavigateToOrderDetail = { orderId ->
                        navController.navigate(Routes.orderDetail(orderId.toString()))
                    },
                    onNavigateToOverview = {
                        navController.navigate(Routes.OVERVIEW)
                    }
                )
            }

            // Order Detail screen - single order details
            composable(
                route = Routes.ORDER_DETAIL,
                arguments = listOf(
                    navArgument("orderId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                OrderDetailScreen(
                    navController = navController,
                    orderId = orderId
                )
            }

            // Profile tab - user profile and settings
            composable(Routes.PROFILE) {
                ProfileScreen(
                    navController = navController,
                    onNavigateToAdmin = {
                        navController.navigate(Routes.ADMIN)
                    }
                )
            }

            // Admin screen - admin panel for dish management
            composable(Routes.ADMIN) {
                AdminScreen(
                    navController = navController,
                    onNavigateToDishEdit = { dishId ->
                        navController.navigate(Routes.dishEdit(dishId.toString()))
                    }
                )
            }

            // Dish Edit screen - create or edit a dish
            composable(
                route = Routes.DISH_EDIT,
                arguments = listOf(
                    navArgument("dishId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val dishId = backStackEntry.arguments?.getString("dishId") ?: "-1"
                DishEditScreen(
                    navController = navController,
                    dishId = dishId
                )
            }

            // Overview screen - aggregate order statistics
            composable(Routes.OVERVIEW) {
                OverviewScreen(
                    navController = navController
                )
            }

            // Room List screen - switch between joined rooms
            composable(Routes.ROOM_LIST) {
                RoomListScreen(
                    onRoomSelected = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Bottom navigation bar composable with 3 tabs: Menu, Orders, Profile.
 */
@Composable
private fun MealsBottomNavBar(
    navController: NavHostController,
    currentRoute: String?,
    pendingOrderCount: Int = 0
) {
    val orangePrimary = Color(0xFFFF6B35)
    val grayDesc = Color(0xFF9E9E9E)

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
    ) {
        bottomNavItems.forEachIndexed { index, item ->
            val selected = currentRoute == item.route

            NavigationBarItem(
                icon = {
                    if (item.route == Routes.ORDER_HISTORY && pendingOrderCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = Color(0xFFE53935),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (pendingOrderCount > 99) "99+" else pendingOrderCount.toString(),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = orangePrimary,
                    selectedTextColor = orangePrimary,
                    unselectedIconColor = grayDesc,
                    unselectedTextColor = grayDesc,
                    indicatorColor = Color(0xFFFFF3E0)
                )
            )

            // Add divider between items
            if (index < bottomNavItems.lastIndex) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color(0xFFEEEEEE))
                )
            }
        }
    }
}

/**
 * Remembers whether the user has a valid auth token stored in Preferences.
 *
 * Delegates to the [Preferences] singleton which is initialized in [MealsApplication.onCreate].
 */
@Composable
private fun rememberHasToken(): Boolean {
    return com.meals.app.data.local.Preferences.isLoggedIn
}
