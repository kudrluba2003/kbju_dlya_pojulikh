package com.senior.kbju.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.senior.kbju.KbjuApp
import com.senior.kbju.utils.NutritionMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

data class HistoryDayUiModel(
    val day: LocalDate,
    val eatenKcal: Int,
    val burnedKcal: Int,
    val targetKcal: Int,
    val waterMl: Int = 0
) {
    // Положительное значение — дефицит (норма минус съеденное плюс сожжённое),
    // отрицательное — перебор.
    val balance: Int get() = targetKcal - eatenKcal + burnedKcal
}

data class HistoryUiState(
    val isLoading: Boolean = true,
    val targetKcal: Int = 0,
    val days: List<HistoryDayUiModel> = emptyList()
)

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val diaryRepository = (app as KbjuApp).diaryRepository
    private val userPreferences = (app as KbjuApp).userPreferencesRepository

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    init {
        viewModelScope.launch {
            val profile = userPreferences.userProfile.first()
            val targetKcal = profile?.let {
                NutritionMath.dailyTargets(
                    weightKg = it.weightKg,
                    heightCm = it.heightCm,
                    age = it.age,
                    gender = it.gender,
                    activityFactor = it.activityFactor,
                    goalPercent = it.goalPercent
                ).kcal
            } ?: 0

            // Итоги по дням собирает SQL (GROUP BY day), отсюда берем только нужные 7 дней.
            val fromDay = LocalDate.now().minusDays(6).toEpochDay()
            val eatenByDay = diaryRepository.mealTotalsSince(fromDay).associate { it.day to it.total }
            val burnedByDay = diaryRepository.activityTotalsSince(fromDay).associate { it.day to it.total }

            // От сегодня к прошлой неделе.
            val today = LocalDate.now()
            val waterDao = (app as KbjuApp).database.waterLogDao()
            val days = (0..6).map { offset ->
                val date = today.minusDays(offset.toLong())
                val epochDay = date.toEpochDay()
                val water = waterDao.getForDay(epochDay)
                HistoryDayUiModel(
                    day = date,
                    eatenKcal = eatenByDay[epochDay]?.roundToInt() ?: 0,
                    burnedKcal = burnedByDay[epochDay]?.roundToInt() ?: 0,
                    targetKcal = targetKcal,
                    waterMl = water?.ml ?: 0
                )
            }
            _uiState.value = HistoryUiState(isLoading = false, targetKcal = targetKcal, days = days)
        }
    }

    fun generateWeeklyReport(): String {
        val state = _uiState.value
        if (state.days.isEmpty()) return ""
        val sb = StringBuilder()
        val firstDay = state.days.last().day.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM", java.util.Locale("ru")))
        val lastDay = state.days.first().day.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM", java.util.Locale("ru")))
        sb.appendLine("📊 Отчёт о питании и здоровье за неделю ($firstDay — $lastDay)")
        sb.appendLine("Суточная норма калорий: ${state.targetKcal} ккал\n")

        val avgEaten = state.days.map { it.eatenKcal }.average().roundToInt()
        val avgBurned = state.days.map { it.burnedKcal }.average().roundToInt()
        val avgWater = state.days.map { it.waterMl }.average().roundToInt()

        val today = LocalDate.now()
        state.days.forEach { day ->
            val dateText = day.day.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM", java.util.Locale("ru")))
            val dayName = when (day.day) {
                today -> "Сегодня, $dateText"
                today.minusDays(1) -> "Вчера, $dateText"
                else -> {
                    val weekday = day.day.format(java.time.format.DateTimeFormatter.ofPattern("EEEE", java.util.Locale("ru")))
                    weekday.replaceFirstChar { it.uppercase() } + ", $dateText"
                }
            }
            val balance = day.balance
            val balanceStr = if (balance >= 0) "дефицит $balance ккал" else "перебор ${-balance} ккал"
            val waterStr = if (day.waterMl > 0) " | Вода: ${day.waterMl} мл" else ""
            sb.appendLine("• $dayName:")
            sb.appendLine("  Съедено: ${day.eatenKcal} ккал | Сожжено: ${day.burnedKcal} ккал ($balanceStr)$waterStr")
        }

        sb.appendLine("\n📈 Средние показатели за день:")
        sb.appendLine("Съедено в среднем: $avgEaten ккал")
        sb.appendLine("Сожжено в среднем: $avgBurned ккал")
        if (avgWater > 0) {
            sb.appendLine("Вода в среднем: $avgWater мл")
        }
        sb.appendLine("\nДневник «КБЖУ для старших»")
        return sb.toString()
    }
}
