package com.senior.kbju.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.senior.kbju.KbjuApp
import com.senior.kbju.data.model.UserProfile
import com.senior.kbju.ui.screens.ActivityScreen
import com.senior.kbju.ui.screens.AddActivityScreen
import com.senior.kbju.ui.screens.AddFoodScreen
import com.senior.kbju.ui.screens.DashboardScreen
import com.senior.kbju.ui.screens.HistoryScreen
import com.senior.kbju.ui.screens.NutritionScreen
import com.senior.kbju.ui.screens.ProfileScreen
import com.senior.kbju.ui.screens.SettingsScreen
import com.senior.kbju.ui.screens.WaterScreen
import com.senior.kbju.ui.screens.WeightScreen
import com.senior.kbju.ui.theme.AccentGreen
import kotlinx.coroutines.flow.map

object Routes {
    const val PROFILE = "profile"
    const val DASHBOARD = "dashboard"
    const val NUTRITION = "nutrition"
    const val WATER = "water"
    const val ACTIVITY = "activity"
    const val WEIGHT = "weight"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ADD_FOOD = "add_food"
    const val ADD_ACTIVITY = "add_activity"
}

private data class NavigationTabItem(
    val route: String,
    val label: String,
    val icon: String
)

private sealed interface ProfileGate {
    data object Loading : ProfileGate
    data class Ready(val profile: UserProfile?) : ProfileGate
}

@Composable
fun AppRoot() {
    val app = LocalContext.current.applicationContext as KbjuApp
    val gate by app.userPreferencesRepository.userProfile
        .map { profile -> ProfileGate.Ready(profile) }
        .collectAsState(initial = ProfileGate.Loading)

    when (val state = gate) {
        ProfileGate.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(56.dp))
            }
        }
        is ProfileGate.Ready -> {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val bottomBarRoutes = remember {
                setOf(
                    Routes.DASHBOARD,
                    Routes.NUTRITION,
                    Routes.WATER,
                    Routes.ACTIVITY,
                    Routes.WEIGHT,
                    Routes.HISTORY,
                    Routes.SETTINGS
                )
            }

            val startDestination = remember {
                if (state.profile != null) Routes.DASHBOARD else Routes.PROFILE
            }

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (currentRoute in bottomBarRoutes) {
                        NavigationBar(
                            containerColor = Color.White,
                            tonalElevation = 8.dp
                        ) {
                            val items = listOf(
                                NavigationTabItem(Routes.DASHBOARD, "Главная", "🏠"),
                                NavigationTabItem(Routes.NUTRITION, "Еда", "🍽️"),
                                NavigationTabItem(Routes.WATER, "Вода", "💧"),
                                NavigationTabItem(Routes.ACTIVITY, "Спорт", "🏃"),
                                NavigationTabItem(Routes.WEIGHT, "Вес", "⚖️"),
                                NavigationTabItem(Routes.HISTORY, "История", "📅"),
                                NavigationTabItem(Routes.SETTINGS, "Настройки", "⚙️")
                            )
                            items.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(Routes.DASHBOARD) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Text(text = item.icon, fontSize = 22.sp)
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentGreen,
                                        selectedTextColor = AccentGreen,
                                        indicatorColor = Color(0xFFE8F5E9)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable(Routes.PROFILE) {
                            ProfileScreen(
                                onFinished = {
                                    navController.navigate(Routes.DASHBOARD) {
                                        popUpTo(Routes.PROFILE) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Routes.DASHBOARD) {
                            DashboardScreen(
                                onEditProfile = { navController.navigate(Routes.PROFILE) }
                            )
                        }
                        composable(Routes.NUTRITION) {
                            NutritionScreen(
                                onAddFood = { navController.navigate(Routes.ADD_FOOD) }
                            )
                        }
                        composable(Routes.WATER) {
                            WaterScreen()
                        }
                        composable(Routes.ACTIVITY) {
                            ActivityScreen(
                                onAddActivity = { navController.navigate(Routes.ADD_ACTIVITY) }
                            )
                        }
                        composable(Routes.WEIGHT) {
                            WeightScreen(
                                onFinished = {
                                    navController.navigate(Routes.DASHBOARD) {
                                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Routes.HISTORY) {
                            HistoryScreen(
                                onBack = {
                                    navController.navigate(Routes.DASHBOARD) {
                                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen()
                        }
                        composable(Routes.ADD_FOOD) {
                            AddFoodScreen(onFinished = { navController.popBackStack() })
                        }
                        composable(Routes.ADD_ACTIVITY) {
                            AddActivityScreen(onFinished = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
