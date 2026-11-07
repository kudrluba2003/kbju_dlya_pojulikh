package com.senior.kbju.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.senior.kbju.ui.screens.MoreScreen
import com.senior.kbju.ui.screens.NutritionScreen
import com.senior.kbju.ui.screens.ProfileScreen
import com.senior.kbju.ui.screens.SettingsScreen
import com.senior.kbju.ui.screens.WaterScreen
import com.senior.kbju.ui.screens.WeightScreen
import com.senior.kbju.ui.theme.AccentGreen
import com.senior.kbju.ui.theme.AccentGreenLight
import com.senior.kbju.ui.theme.CardBorder
import com.senior.kbju.ui.theme.SurfaceWhite
import com.senior.kbju.ui.theme.TextMuted
import kotlinx.coroutines.flow.map

object Routes {
    const val PROFILE = "profile"
    const val DASHBOARD = "dashboard"
    const val NUTRITION = "nutrition"
    const val ACTIVITY = "activity"
    const val MORE = "more"
    const val WATER = "water"
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
                    Routes.ACTIVITY,
                    Routes.MORE,
                    Routes.WATER,
                    Routes.WEIGHT,
                    Routes.HISTORY,
                    Routes.SETTINGS
                )
            }

            val moreSubRoutes = remember {
                setOf(
                    Routes.MORE,
                    Routes.WATER,
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
                        Column {
                            HorizontalDivider(thickness = 1.dp, color = CardBorder)
                            NavigationBar(
                                containerColor = SurfaceWhite,
                                tonalElevation = 0.dp
                            ) {
                                val items = listOf(
                                    NavigationTabItem(Routes.DASHBOARD, "Главная", "🏠"),
                                    NavigationTabItem(Routes.NUTRITION, "Еда", "🍽️"),
                                    NavigationTabItem(Routes.ACTIVITY, "Спорт", "🏃"),
                                    NavigationTabItem(Routes.MORE, "Ещё", "⋯")
                                )
                                items.forEach { item ->
                                    val selected = if (item.route == Routes.MORE) {
                                        currentRoute in moreSubRoutes
                                    } else {
                                        currentRoute == item.route
                                    }

                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            if (item.route == Routes.MORE) {
                                                if (currentRoute in moreSubRoutes && currentRoute != Routes.MORE) {
                                                    val popped = navController.popBackStack(Routes.MORE, inclusive = false)
                                                    if (!popped) {
                                                        navController.navigate(Routes.MORE) {
                                                            launchSingleTop = true
                                                        }
                                                    }
                                                } else if (currentRoute != Routes.MORE) {
                                                    navController.navigate(Routes.MORE) {
                                                        popUpTo(Routes.DASHBOARD) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            } else {
                                                if (currentRoute != item.route) {
                                                    navController.navigate(item.route) {
                                                        popUpTo(Routes.DASHBOARD) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                        },
                                        icon = {
                                            Text(text = item.icon, fontSize = 24.sp)
                                        },
                                        label = {
                                            Text(
                                                text = item.label,
                                                fontSize = 14.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = AccentGreen,
                                            selectedTextColor = AccentGreen,
                                            unselectedTextColor = TextMuted,
                                            indicatorColor = AccentGreenLight
                                        )
                                    )
                                }
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
                                    if (!navController.popBackStack()) {
                                        navController.navigate(Routes.DASHBOARD) {
                                            popUpTo(Routes.PROFILE) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                        composable(Routes.DASHBOARD) {
                            DashboardScreen(
                                onEditProfile = { navController.navigate(Routes.PROFILE) },
                                onAddFood = { navController.navigate(Routes.ADD_FOOD) },
                                onOpenWeight = { navController.navigate(Routes.WEIGHT) }
                            )
                        }
                        composable(Routes.NUTRITION) {
                            NutritionScreen(
                                onAddFood = { navController.navigate(Routes.ADD_FOOD) }
                            )
                        }
                        composable(Routes.ACTIVITY) {
                            ActivityScreen(
                                onAddActivity = { navController.navigate(Routes.ADD_ACTIVITY) }
                            )
                        }
                        composable(Routes.MORE) {
                            MoreScreen(
                                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                                onOpenWater = { navController.navigate(Routes.WATER) },
                                onOpenWeight = { navController.navigate(Routes.WEIGHT) },
                                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                            )
                        }
                        composable(Routes.WATER) {
                            WaterScreen(
                                onBack = { navController.navigate(Routes.MORE) { popUpTo(Routes.MORE) { inclusive = false } } }
                            )
                        }
                        composable(Routes.WEIGHT) {
                            WeightScreen(
                                onFinished = {
                                    if (!navController.popBackStack()) {
                                        navController.navigate(Routes.DASHBOARD)
                                    }
                                }
                            )
                        }
                        composable(Routes.HISTORY) {
                            HistoryScreen(
                                onBack = { navController.navigate(Routes.MORE) { popUpTo(Routes.MORE) { inclusive = false } } }
                            )
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                onBack = { navController.navigate(Routes.MORE) { popUpTo(Routes.MORE) { inclusive = false } } }
                            )
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
