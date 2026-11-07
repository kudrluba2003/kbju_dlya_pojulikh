package com.senior.kbju.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.senior.kbju.KbjuApp
import com.senior.kbju.data.local.entity.ActivityEntity
import com.senior.kbju.data.local.entity.ActivityLogEntity
import com.senior.kbju.utils.NutritionMath
import com.senior.kbju.utils.parseNumber
import com.senior.kbju.utils.sanitizeNumberInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

class AddActivityViewModel(app: Application) : AndroidViewModel(app) {

    private val diaryRepository = (app as KbjuApp).diaryRepository
    private val userPreferences = (app as KbjuApp).userPreferencesRepository

    val activities: StateFlow<List<ActivityEntity>> = diaryRepository.observeActivityCatalog()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Верх экрана: 6 самых частых активностей огромными кнопками.
    val quickActivities: StateFlow<List<ActivityEntity>> = activities
        .map { list -> list.filter { it.isQuickAccess } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Полный каталог по категориям; порядок категорий — как в seed-данных.
    val groupedActivities: StateFlow<Map<String, List<ActivityEntity>>> = activities
        .map { list -> list.groupBy { it.category ?: "Другое" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val selected = MutableStateFlow<ActivityEntity?>(null)
    val minutesText = MutableStateFlow("")
    val errorMessage = MutableStateFlow<String?>(null)
    val savedMessage = MutableStateFlow<String?>(null)

    // Живой расчёт: MET × 3.5 × вес / 200 × минуты × поправка на возраст.
    val burnedKcal: StateFlow<Int?> = combine(
        selected,
        minutesText,
        userPreferences.userProfile
    ) { activity, minutesRaw, profile ->
        val minutes = parseNumber(minutesRaw)?.roundToInt() ?: return@combine null
        if (activity == null || profile == null || minutes !in 1..600) {
            null
        } else {
            NutritionMath.activityCalories(activity.met, profile.weightKg, minutes, profile.age)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun select(activity: ActivityEntity) {
        selected.value = activity
    }

    fun onMinutesChange(raw: String) {
        minutesText.value = sanitizeNumberInput(raw)
    }

    fun save() {
        viewModelScope.launch {
            val activity = selected.value
            val profile = userPreferences.userProfile.first()
            val minutes = parseNumber(minutesText.value)?.roundToInt()

            errorMessage.value = when {
                activity == null -> "Выберите занятие из списка"
                minutes == null || minutes !in 1..600 -> "Введите минуты числом: от 1 до 600"
                profile == null -> "Сначала заполните профиль"
                else -> null
            }
            val selectedActivity = activity ?: return@launch
            val minutesValue = minutes ?: return@launch
            val currentProfile = profile ?: return@launch
            if (minutesValue !in 1..600) return@launch

            val kcal = NutritionMath.activityCalories(
                selectedActivity.met,
                currentProfile.weightKg,
                minutesValue,
                currentProfile.age
            )
            diaryRepository.addActivityLog(
                ActivityLogEntity(
                    day = LocalDate.now().toEpochDay(),
                    name = selectedActivity.name,
                    minutes = minutesValue,
                    kcal = kcal.toDouble()
                )
            )
            savedMessage.value =
                "${selectedActivity.name}, $minutesValue минут: сожгли примерно $kcal ккал. Записали!"
        }
    }
}
