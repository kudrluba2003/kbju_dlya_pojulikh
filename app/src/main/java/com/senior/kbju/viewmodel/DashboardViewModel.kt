package com.senior.kbju.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.senior.kbju.KbjuApp
import com.senior.kbju.data.local.entity.ActivityLogEntity
import com.senior.kbju.data.local.entity.MealLogEntity
import com.senior.kbju.data.model.MealType
import com.senior.kbju.data.model.UserProfile
import com.senior.kbju.utils.AppUpdateChecker
import com.senior.kbju.utils.AppUpdateInfo
import com.senior.kbju.utils.DailyTargets
import com.senior.kbju.utils.NutritionMath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    val basalMetabolismKcal: Int = 0,
    val meals: List<MealLogEntity> = emptyList(),
    val activities: List<ActivityLogEntity> = emptyList(),
    val waterGlasses: Int = 0,
    val targetWaterGlasses: Int = profile?.targetWaterGlasses ?: 6
) {
    val eatenKcal: Int get() = meals.sumOf { it.kcal }.roundToInt()
    val activityBurnedKcal: Int get() = activities.sumOf { it.kcal }.roundToInt()
    // Сожжено за день: базовый метаболизм (на работу организма) + потраченное на активность
    val burnedKcal: Int get() = basalMetabolismKcal + activityBurnedKcal
    // Норма питания фиксированная: активность учитывается отдельно и НЕ плюсуется к норме дневной
    val remainingKcal: Int get() = targetKcal - eatenKcal
    // Баланс за день: съедено минус всего сожжено
    val netCalories: Int get() = eatenKcal - burnedKcal
    val protein: Double get() = meals.sumOf { it.protein }
    val fat: Double get() = meals.sumOf { it.fat }
    val carbs: Double get() = meals.sumOf { it.carbs }
    val waterMl: Int get() = waterGlasses * 250
    val targetWaterMl: Int get() = targetWaterGlasses * 250
    val isToday: Boolean get() = dateEpochDay == LocalDate.now().toEpochDay()

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

    val availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val isCheckingUpdate = MutableStateFlow(false)
    val updateCheckMessage = MutableStateFlow<String?>(null)

    init {
        checkForUpdates(silent = true)
    }

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            isCheckingUpdate.value = true
            if (!silent) updateCheckMessage.value = null
            try {
                val update = AppUpdateChecker.checkLatestRelease()
                availableUpdate.value = update
                if (!silent && update == null) {
                    updateCheckMessage.value = "У вас самая актуальная версия (${com.senior.kbju.BuildConfig.VERSION_NAME})"
                }
            } catch (e: Exception) {
                if (!silent) {
                    updateCheckMessage.value = "Не удалось проверить обновления: проверьте интернет"
                }
            } finally {
                isCheckingUpdate.value = false
            }
        }
    }

    val selectedEpochDay = MutableStateFlow(LocalDate.now().toEpochDay())

    fun selectDate(epochDay: Long) {
        selectedEpochDay.value = epochDay
    }

    fun shiftDay(delta: Long) {
        selectedEpochDay.value = selectedEpochDay.value + delta
    }

    fun selectToday() {
        selectedEpochDay.value = LocalDate.now().toEpochDay()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        userPreferences.userProfile,
        selectedEpochDay,
        selectedEpochDay.flatMapLatest { day -> diaryRepository.observeMealsForDay(day) },
        selectedEpochDay.flatMapLatest { day -> diaryRepository.observeActivitiesForDay(day) },
        selectedEpochDay.flatMapLatest { day -> diaryRepository.observeWaterForDay(day) }
    ) { profile, day, meals, activities, water ->
        // Норма пересчитывается из параметров профиля при каждом обновлении данных.
        val targets = profile?.let {
            NutritionMath.dailyTargets(
                weightKg = it.weightKg,
                heightCm = it.heightCm,
                age = it.currentAge,
                gender = it.gender,
                activityFactor = it.activityFactor,
                goalPercent = it.goalPercent
            )
        } ?: DailyTargets(0, 0, 0, 0)
        DashboardUiState(
            isLoading = false,
            profile = profile,
            dateEpochDay = day,
            targetKcal = targets.kcal,
            targetProtein = targets.protein,
            targetFat = targets.fat,
            targetCarbs = targets.carbs,
            basalMetabolismKcal = targets.bmr,
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
            diaryRepository.addWaterGlass(selectedEpochDay.value)
        }
    }

    fun removeWater() {
        viewModelScope.launch {
            diaryRepository.removeWaterGlass(selectedEpochDay.value)
        }
    }

    fun updateTargetWater(glasses: Int) {
        viewModelScope.launch {
            userPreferences.updateTargetWater(glasses)
        }
    }
}
