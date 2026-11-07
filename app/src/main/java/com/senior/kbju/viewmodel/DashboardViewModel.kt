package com.senior.kbju.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.senior.kbju.KbjuApp
import com.senior.kbju.data.local.entity.ActivityLogEntity
import com.senior.kbju.data.local.entity.MealLogEntity
import com.senior.kbju.data.model.MealType
import com.senior.kbju.data.model.UserProfile
import com.senior.kbju.utils.DailyTargets
import com.senior.kbju.utils.NutritionMath
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

data class DashboardUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val dateEpochDay: Long = LocalDate.now().toEpochDay(),
    val targetKcal: Int = 0,
    val targetProtein: Int = 0,
    val targetFat: Int = 0,
    val targetCarbs: Int = 0,
    val meals: List<MealLogEntity> = emptyList(),
    val activities: List<ActivityLogEntity> = emptyList(),
    val waterGlasses: Int = 0,
    val targetWaterGlasses: Int = profile?.targetWaterGlasses ?: 6
) {
    val eatenKcal: Int get() = meals.sumOf { it.kcal }.roundToInt()
    val burnedKcal: Int get() = activities.sumOf { it.kcal }.roundToInt()
    // Норма питания фиксированная: активность учитывается отдельно и НЕ плюсуется к норме дневной
    val remainingKcal: Int get() = targetKcal - eatenKcal
    val protein: Double get() = meals.sumOf { it.protein }
    val fat: Double get() = meals.sumOf { it.fat }
    val carbs: Double get() = meals.sumOf { it.carbs }
    val waterMl: Int get() = waterGlasses * 250
    val targetWaterMl: Int get() = targetWaterGlasses * 250

    val currentWeightKg: Double get() = profile?.weightKg ?: 0.0
    val startWeightKg: Double get() = profile?.startWeightKg ?: currentWeightKg
    val targetWeightKg: Double get() = profile?.targetWeightKg ?: currentWeightKg
    val weightDiffToTarget: Double get() = currentWeightKg - targetWeightKg
    val weightDiffFromStart: Double get() = currentWeightKg - startWeightKg

    val mealsByGroup: Map<MealType, List<MealLogEntity>>
        get() {
            val map = MealType.entries.associateWith { mutableListOf<MealLogEntity>() }
            meals.forEach { meal ->
                val type = MealType.entries.firstOrNull { it.name == meal.mealType } ?: MealType.LUNCH
                map[type]?.add(meal)
            }
            return map
        }
}

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val diaryRepository = (app as KbjuApp).diaryRepository
    private val userPreferences = (app as KbjuApp).userPreferencesRepository

    val uiState: StateFlow<DashboardUiState> = combine(
        userPreferences.userProfile,
        diaryRepository.observeCurrentEpochDay(),
        diaryRepository.observeTodayMeals(),
        diaryRepository.observeTodayActivities(),
        diaryRepository.observeTodayWater()
    ) { profile, currentDay, meals, activities, water ->
        // Норма пересчитывается из параметров профиля при каждом обновлении данных.
        val targets = profile?.let {
            NutritionMath.dailyTargets(
                weightKg = it.weightKg,
                heightCm = it.heightCm,
                age = it.age,
                gender = it.gender,
                activityFactor = it.activityFactor,
                goalPercent = it.goalPercent
            )
        } ?: DailyTargets(0, 0, 0, 0)
        DashboardUiState(
            isLoading = false,
            profile = profile,
            dateEpochDay = currentDay,
            targetKcal = targets.kcal,
            targetProtein = targets.protein,
            targetFat = targets.fat,
            targetCarbs = targets.carbs,
            meals = meals,
            activities = activities,
            waterGlasses = water?.glasses ?: 0,
            targetWaterGlasses = profile?.targetWaterGlasses ?: 6
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun deleteMeal(id: Long) {
        viewModelScope.launch {
            diaryRepository.deleteMeal(id)
        }
    }

    fun deleteActivity(id: Long) {
        viewModelScope.launch {
            diaryRepository.deleteActivityLog(id)
        }
    }

    fun addWater() {
        viewModelScope.launch {
            diaryRepository.addWaterGlass()
        }
    }

    fun removeWater() {
        viewModelScope.launch {
            diaryRepository.removeWaterGlass()
        }
    }

    fun updateTargetWater(glasses: Int) {
        viewModelScope.launch {
            userPreferences.updateTargetWater(glasses)
        }
    }
}
