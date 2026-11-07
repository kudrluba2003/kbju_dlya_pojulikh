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
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class HistoryPeriod(val daysCount: Int, val label: String) {
    WEEK(7, "За неделю"),
    MONTH(30, "За месяц")
}

data class HistoryDayUiModel(
    val day: LocalDate,
    val eatenKcal: Int,
    val burnedKcal: Int,
    val targetKcal: Int,
    val waterMl: Int = 0
) {
    // Положительное значение — дефицит (осталось до нормы), отрицательное — перебор.
    val balance: Int get() = targetKcal - eatenKcal
}

data class HistorySummary(
    val avgDeficit: Int,
    val avgWaterMl: Int,
    val avgBurnedKcal: Int,
    val avgEatenKcal: Int
)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val period: HistoryPeriod = HistoryPeriod.WEEK,
    val targetKcal: Int = 0,
    val summary: HistorySummary = HistorySummary(0, 0, 0, 0),
    val days: List<HistoryDayUiModel> = emptyList()
)

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val diaryRepository = (app as KbjuApp).diaryRepository
    private val userPreferences = (app as KbjuApp).userPreferencesRepository

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    init {
        loadData(HistoryPeriod.WEEK)
    }

    fun setPeriod(period: HistoryPeriod) {
        if (_uiState.value.period == period && !_uiState.value.isLoading) return
        loadData(period)
    }

    private fun loadData(period: HistoryPeriod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, period = period)

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

            val count = period.daysCount
            val fromDay = LocalDate.now().minusDays((count - 1).toLong()).toEpochDay()
            val eatenByDay = diaryRepository.mealTotalsSince(fromDay).associate { it.day to it.total }
            val burnedByDay = diaryRepository.activityTotalsSince(fromDay).associate { it.day to it.total }

            val today = LocalDate.now()
            val waterDao = (getApplication() as KbjuApp).database.waterLogDao()

            val days = (0 until count).map { offset ->
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

            val avgEaten = if (days.isNotEmpty()) days.map { it.eatenKcal }.average().roundToInt() else 0
            val avgBurned = if (days.isNotEmpty()) days.map { it.burnedKcal }.average().roundToInt() else 0
            val avgWater = if (days.isNotEmpty()) days.map { it.waterMl }.average().roundToInt() else 0
            val avgDeficit = if (days.isNotEmpty()) days.map { it.balance }.average().roundToInt() else 0

            val summary = HistorySummary(
                avgDeficit = avgDeficit,
                avgWaterMl = avgWater,
                avgBurnedKcal = avgBurned,
                avgEatenKcal = avgEaten
            )

            _uiState.value = HistoryUiState(
                isLoading = false,
                period = period,
                targetKcal = targetKcal,
                summary = summary,
                days = days
            )
        }
    }

    fun generateReport(): String {
        val state = _uiState.value
        if (state.days.isEmpty()) return ""
        val sb = StringBuilder()
        val firstDay = state.days.last().day.format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru")))
        val lastDay = state.days.first().day.format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru")))
        val periodText = if (state.period == HistoryPeriod.WEEK) "неделю" else "месяц"
        sb.appendLine("📊 Отчёт о питании и активности за $periodText ($firstDay — $lastDay)")
        sb.appendLine("Суточная норма: ${state.targetKcal} ккал\n")

        sb.appendLine("📈 Средние показатели за день:")
        val defSign = if (state.summary.avgDeficit >= 0) "дефицит +" else "перебор "
        sb.appendLine("• Средний дефицит: $defSign${state.summary.avgDeficit} ккал")
        sb.appendLine("• Среднее потребление: ${state.summary.avgEatenKcal} ккал")
        sb.appendLine("• Средняя активность: ${state.summary.avgBurnedKcal} ккал")
        sb.appendLine("• Среднее количество воды: ${state.summary.avgWaterMl} мл\n")

        sb.appendLine("📅 Статистика по дням:")
        val today = LocalDate.now()
        state.days.forEach { day ->
            val dateText = day.day.format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru")))
            val dayName = when (day.day) {
                today -> "Сегодня, $dateText"
                today.minusDays(1) -> "Вчера, $dateText"
                else -> {
                    val weekday = day.day.format(DateTimeFormatter.ofPattern("EEEE", Locale("ru")))
                    weekday.replaceFirstChar { it.uppercase() } + ", $dateText"
                }
            }
            val balance = day.balance
            val balanceStr = if (balance >= 0) "дефицит $balance ккал" else "перебор ${-balance} ккал"
            val waterStr = if (day.waterMl > 0) " | Вода: ${day.waterMl} мл" else ""
            sb.appendLine("• $dayName:")
            sb.appendLine("  Съедено: ${day.eatenKcal} ккал | Сожжено: ${day.burnedKcal} ккал ($balanceStr)$waterStr")
        }

        sb.appendLine("\nДневник «КБЖУ для старших»")
        return sb.toString()
    }
}
