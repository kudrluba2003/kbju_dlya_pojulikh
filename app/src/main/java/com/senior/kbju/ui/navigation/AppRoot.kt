package com.senior.kbju.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.senior.kbju.KbjuApp
import com.senior.kbju.data.model.UserProfile
import com.senior.kbju.ui.screens.AddActivityScreen
import com.senior.kbju.ui.screens.AddFoodScreen
import com.senior.kbju.ui.screens.DashboardScreen
import com.senior.kbju.ui.screens.HistoryScreen
import com.senior.kbju.ui.screens.ProfileScreen
import com.senior.kbju.ui.screens.WeightScreen
import kotlinx.coroutines.flow.map

object Routes {
    const val PROFILE = "profile"
    const val DASHBOARD = "dashboard"
    const val ADD_FOOD = "add_food"
    const val ADD_ACTIVITY = "add_activity"
    const val WEIGHT = "weight"
    const val HISTORY = "history"
}

// Loading — DataStore ещё не ответил. Ready — уже ответил, и профиль при этом
// может быть null (незаполнен): это НЕ загрузка, это старт с онбординга.
// Без разделения свежая установка навсегда застревает на спиннере.
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
            // Стартовый экран фиксируем один раз: профиль есть — дневник, нет — онбординг.
            val startDestination = remember {
                if (state.profile != null) Routes.DASHBOARD else Routes.PROFILE
            }
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
                        onAddFood = { navController.navigate(Routes.ADD_FOOD) },
                        onAddActivity = { navController.navigate(Routes.ADD_ACTIVITY) },
                        onHistory = { navController.navigate(Routes.HISTORY) },
                        onUpdateWeight = { navController.navigate(Routes.WEIGHT) },
                        onEditProfile = { navController.navigate(Routes.PROFILE) }
                    )
                }
                composable(Routes.ADD_FOOD) {
                    AddFoodScreen(onFinished = { navController.popBackStack(Routes.DASHBOARD, false) })
                }
                composable(Routes.ADD_ACTIVITY) {
                    AddActivityScreen(onFinished = { navController.popBackStack(Routes.DASHBOARD, false) })
                }
                composable(Routes.WEIGHT) {
                    WeightScreen(onFinished = { navController.popBackStack() })
                }
                composable(Routes.HISTORY) {
                    HistoryScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
