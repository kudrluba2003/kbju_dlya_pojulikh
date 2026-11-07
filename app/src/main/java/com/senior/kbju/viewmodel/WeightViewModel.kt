package com.senior.kbju.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.senior.kbju.KbjuApp
import com.senior.kbju.data.local.entity.WeightLogEntity
import com.senior.kbju.data.model.UserProfile
import com.senior.kbju.utils.NutritionMath
import com.senior.kbju.utils.parseNumberIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class WeightViewModel(app: Application) : AndroidViewModel(app) {

    private val userPreferences = (app as KbjuApp).userPreferencesRepository
    private val diaryRepository = (app as KbjuApp).diaryRepository

    val profile: StateFlow<UserProfile?> = userPreferences.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Норма для текущих сохранённых параметров — показываем на экране до ввода нового веса.
    val currentTargetKcal: StateFlow<Int?> = userPreferences.userProfile
        .map { current ->
            current?.let {
                NutritionMath.dailyTargets(
                    weightKg = it.weightKg,
                    heightCm = it.heightCm,
                    age = it.age,
                    gender = it.gender,
                    activityFactor = it.activityFactor,
                    goalPercent = it.goalPercent
                ).kcal
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // История за последние 30 дней (по возрастанию даты) для графика
    val weightHistory: StateFlow<List<WeightLogEntity>> =
        diaryRepository.observeWeightsSince(LocalDate.now().minusDays(30).toEpochDay())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Все взвешивания (по убыванию) для списка
    val allWeights: StateFlow<List<WeightLogEntity>> =
        diaryRepository.observeAllWeights()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightText = MutableStateFlow("")
    val errorMessage = MutableStateFlow<String?>(null)
    val confirmedNewNorm = MutableStateFlow<Int?>(null)

    fun save() {
        viewModelScope.launch {
            val current = userPreferences.userProfile.first()
            if (current == null) {
                errorMessage.value = "Сначала заполните профиль"
                return@launch
            }
            val newWeight = parseNumberIn(weightText.value, 25.0, 300.0)
            if (newWeight == null) {
                errorMessage.value = "Введите вес числом: от 25 до 300 кг"
                return@launch
            }
            errorMessage.value = null

            // Обновляем вес в DataStore (для профиля) и добавляем в журнал взвешиваний (Room)
            userPreferences.updateWeight(newWeight)
            diaryRepository.logWeight(newWeight)
            weightText.value = ""

            val targets = NutritionMath.dailyTargets(
                weightKg = newWeight,
                heightCm = current.heightCm,
                age = current.age,
                gender = current.gender,
                activityFactor = current.activityFactor,
                goalPercent = current.goalPercent
            )
            confirmedNewNorm.value = targets.kcal
        }
    }

    fun deleteWeight(id: Long) {
        viewModelScope.launch {
            diaryRepository.deleteWeightLog(id)
        }
    }

    fun dismissConfirmation() {
        confirmedNewNorm.value = null
    }
}
