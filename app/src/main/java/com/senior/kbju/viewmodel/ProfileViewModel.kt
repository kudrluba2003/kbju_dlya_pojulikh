package com.senior.kbju.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.senior.kbju.KbjuApp
import com.senior.kbju.data.model.ActivityLevel
import com.senior.kbju.data.model.Gender
import com.senior.kbju.data.model.Goal
import com.senior.kbju.data.model.UserProfile
import com.senior.kbju.notification.MealReminderScheduler
import com.senior.kbju.utils.AppUpdateChecker
import com.senior.kbju.utils.AppUpdateInfo
import com.senior.kbju.utils.CrashHandler
import com.senior.kbju.utils.DailyTargets
import com.senior.kbju.utils.NutritionMath
import com.senior.kbju.utils.parseNumberIn
import com.senior.kbju.utils.pretty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val userPreferences = (app as KbjuApp).userPreferencesRepository

    val height = MutableStateFlow("")
    val weight = MutableStateFlow("")
    val startWeight = MutableStateFlow("")
    val targetWeight = MutableStateFlow("")
    val targetWaterGlasses = MutableStateFlow(6)
    val age = MutableStateFlow("")
    val gender = MutableStateFlow<Gender?>(null)
    val activityLevel = MutableStateFlow<ActivityLevel?>(null)
    val goal = MutableStateFlow<Goal?>(null)
    val remindersEnabled = MutableStateFlow(true)
    val errorMessage = MutableStateFlow<String?>(null)
    val saved = MutableStateFlow(false)
    val isEditing = MutableStateFlow(false)
    val backupStatus = MutableStateFlow<String?>(null)
    val isBackupBusy = MutableStateFlow(false)
    val crashReport = MutableStateFlow<String?>(null)
    val availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val isCheckingUpdate = MutableStateFlow(false)
    val updateCheckMessage = MutableStateFlow<String?>(null)

    val lunchHour = MutableStateFlow(13)
    val lunchMinute = MutableStateFlow(0)
    val dinnerHour = MutableStateFlow(19)
    val dinnerMinute = MutableStateFlow(0)
    val waterCalcExplanation = MutableStateFlow<String?>(null)
    val feedbackText = MutableStateFlow("")

    private data class ParsedInputs(
        val heightCm: Int,
        val weightKg: Double,
        val age: Int,
        val gender: Gender,
        val level: ActivityLevel,
        val goal: Goal
    )

    init {
        viewModelScope.launch {
            crashReport.value = CrashHandler.getLastCrashReport(getApplication())
            remindersEnabled.value = userPreferences.remindersEnabled.first()
            val (lHour, lMin) = userPreferences.lunchReminderTime.first()
            lunchHour.value = lHour
            lunchMinute.value = lMin
            val (dHour, dMin) = userPreferences.dinnerReminderTime.first()
            dinnerHour.value = dHour
            dinnerMinute.value = dMin
            val existing = userPreferences.userProfile.first()
            if (existing != null) {
                isEditing.value = true
                height.value = existing.heightCm.toString()
                weight.value = existing.weightKg.pretty()
                startWeight.value = existing.startWeightKg.pretty()
                targetWeight.value = existing.targetWeightKg.pretty()
                targetWaterGlasses.value = existing.targetWaterGlasses
                age.value = existing.age.toString()
                gender.value = existing.gender
                activityLevel.value = ActivityLevel.entries.firstOrNull { it.factor == existing.activityFactor }
                goal.value = Goal.entries.firstOrNull { it.percent == existing.goalPercent }
            }
            // Фоновая проверка обновления при входе в профиль
            try {
                availableUpdate.value = AppUpdateChecker.checkLatestRelease()
            } catch (_: Exception) {}
        }
    }

    fun onWeightChanged(newWeight: String) {
        weight.value = newWeight
        if (startWeight.value.isBlank()) {
            startWeight.value = newWeight
        }
        val currentW = parseNumberIn(newWeight, 25.0, 300.0)
        if (currentW != null && targetWeight.value.isBlank()) {
            when (goal.value) {
                Goal.LOSE -> targetWeight.value = (currentW * 0.9).pretty()
                Goal.GAIN -> targetWeight.value = (currentW * 1.05).pretty()
                else -> targetWeight.value = currentW.pretty()
            }
        }
    }

    fun selectGoal(newGoal: Goal) {
        goal.value = newGoal
        val currentW = parseNumberIn(weight.value, 25.0, 300.0)
        if (currentW != null && (targetWeight.value.isBlank() || targetWeight.value == weight.value)) {
            when (newGoal) {
                Goal.LOSE -> targetWeight.value = (currentW * 0.9).pretty()
                Goal.GAIN -> targetWeight.value = (currentW * 1.05).pretty()
                Goal.MAINTAIN -> targetWeight.value = currentW.pretty()
            }
        }
    }

    fun setTargetWater(glasses: Int) {
        targetWaterGlasses.value = glasses.coerceIn(2, 20)
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            isCheckingUpdate.value = true
            updateCheckMessage.value = null
            try {
                val update = AppUpdateChecker.checkLatestRelease()
                availableUpdate.value = update
                if (update == null) {
                    updateCheckMessage.value = "У вас установлена самая актуальная версия (${com.senior.kbju.BuildConfig.VERSION_NAME})"
                }
            } catch (e: Exception) {
                updateCheckMessage.value = "Не удалось проверить обновления: проверьте интернет"
            } finally {
                isCheckingUpdate.value = false
            }
        }
    }

    fun clearCrashReport() {
        CrashHandler.clearCrashReport(getApplication())
        crashReport.value = null
    }

    suspend fun getBackupJson(): String {
        return (getApplication() as KbjuApp).backupManager.exportToJson()
    }

    fun restoreBackup(jsonString: String) {
        viewModelScope.launch {
            isBackupBusy.value = true
            try {
                val result = (getApplication() as KbjuApp).backupManager.importFromJson(jsonString)
                backupStatus.value = "Восстановлено: ${result.mealsRestored} блюд, ${result.weightsRestored} весов, ${result.foodsRestored} продуктов!"
                val existing = userPreferences.userProfile.first()
                if (existing != null) {
                    height.value = existing.heightCm.toString()
                    weight.value = existing.weightKg.pretty()
                    startWeight.value = existing.startWeightKg.pretty()
                    targetWeight.value = existing.targetWeightKg.pretty()
                    targetWaterGlasses.value = existing.targetWaterGlasses
                    age.value = existing.age.toString()
                    gender.value = existing.gender
                    activityLevel.value = ActivityLevel.entries.firstOrNull { it.factor == existing.activityFactor }
                    goal.value = Goal.entries.firstOrNull { it.percent == existing.goalPercent }
                }
            } catch (e: Exception) {
                backupStatus.value = "Ошибка при чтении файла: ${e.localizedMessage ?: "неверный формат"}"
            } finally {
                isBackupBusy.value = false
            }
        }
    }

    fun setReminders(enabled: Boolean) {
        remindersEnabled.value = enabled
        viewModelScope.launch {
            userPreferences.setRemindersEnabled(enabled)
            if (enabled) {
                MealReminderScheduler.scheduleAll(
                    getApplication(),
                    lunchHour = lunchHour.value,
                    lunchMinute = lunchMinute.value,
                    dinnerHour = dinnerHour.value,
                    dinnerMinute = dinnerMinute.value
                )
            } else {
                MealReminderScheduler.cancelAll(getApplication())
            }
        }
    }

    fun adjustLunchHour(delta: Int) {
        val newHour = (lunchHour.value + delta).coerceIn(10, 16)
        lunchHour.value = newHour
        persistReminderTimes()
    }

    fun adjustDinnerHour(delta: Int) {
        val newHour = (dinnerHour.value + delta).coerceIn(16, 22)
        dinnerHour.value = newHour
        persistReminderTimes()
    }

    private fun persistReminderTimes() {
        viewModelScope.launch {
            userPreferences.saveReminderTimes(
                lunchHour.value,
                lunchMinute.value,
                dinnerHour.value,
                dinnerMinute.value
            )
            if (remindersEnabled.value) {
                MealReminderScheduler.scheduleAll(
                    getApplication(),
                    lunchHour = lunchHour.value,
                    lunchMinute = lunchMinute.value,
                    dinnerHour = dinnerHour.value,
                    dinnerMinute = dinnerMinute.value
                )
            }
        }
    }

    fun calculateWaterByWeight() {
        val w = parseNumberIn(weight.value, 25.0, 300.0)
        if (w == null) {
            waterCalcExplanation.value = "Сначала укажите ваш вес выше"
            return
        }
        val ml = kotlin.math.round((w * 30.0)).toInt()
        val glasses = (ml / 250.0).let { kotlin.math.round(it).toInt() }.coerceIn(4, 16)
        targetWaterGlasses.value = glasses
        waterCalcExplanation.value = "По весу ${w.pretty()} кг норма: ~${ml} мл (${glasses} стаканов по 250 мл)"
    }

    // Расчёт суточной нормы КБЖУ по формуле Миффлина-Сан Жеора:
    // (10 × вес + 6.25 × рост − 5 × возраст + поправка на пол) × коэффициент активности × (1 ± процент цели).
    // Возвращает null, если поля заполнены неверно — конкретный текст ошибки даёт validationMessage().
    fun calculateDailyTargets(): DailyTargets? =
        parsedInputs()?.let {
            NutritionMath.dailyTargets(
                weightKg = it.weightKg,
                heightCm = it.heightCm,
                age = it.age,
                gender = it.gender,
                activityFactor = it.level.factor,
                goalPercent = it.goal.percent
            )
        }

    fun save() {
        viewModelScope.launch {
            val inputs = parsedInputs()
            errorMessage.value = if (inputs == null) validationMessage() else null
            val valid = inputs ?: return@launch
            val startKg = parseNumberIn(startWeight.value, 25.0, 300.0) ?: valid.weightKg
            val targetKg = parseNumberIn(targetWeight.value, 25.0, 300.0) ?: valid.weightKg
            val waterGlasses = targetWaterGlasses.value.coerceIn(2, 20)

            userPreferences.save(
                UserProfile(
                    heightCm = valid.heightCm,
                    weightKg = valid.weightKg,
                    age = valid.age,
                    gender = valid.gender,
                    activityFactor = valid.level.factor,
                    goalPercent = valid.goal.percent,
                    startWeightKg = startKg,
                    targetWeightKg = targetKg,
                    targetWaterGlasses = waterGlasses
                )
            )
            saved.value = true
        }
    }

    private fun parsedInputs(): ParsedInputs? {
        val heightCm = parseNumberIn(height.value, 100.0, 250.0) ?: return null
        val weightKg = parseNumberIn(weight.value, 25.0, 300.0) ?: return null
        val ageYears = parseNumberIn(age.value, 18.0, 120.0) ?: return null
        val selectedGender = gender.value ?: return null
        val selectedLevel = activityLevel.value ?: return null
        val selectedGoal = goal.value ?: return null
        return ParsedInputs(
            heightCm = heightCm.toInt(),
            weightKg = weightKg,
            age = ageYears.toInt(),
            gender = selectedGender,
            level = selectedLevel,
            goal = selectedGoal
        )
    }

    private fun validationMessage(): String = when {
        parseNumberIn(height.value, 100.0, 250.0) == null -> "Проверьте рост: введите число от 100 до 250"
        parseNumberIn(weight.value, 25.0, 300.0) == null -> "Проверьте текущий вес: введите число от 25 до 300"
        parseNumberIn(age.value, 18.0, 120.0) == null -> "Проверьте возраст: введите число от 18 до 120"
        gender.value == null -> "Выберите пол"
        activityLevel.value == null -> "Выберите, как вы проводите день"
        goal.value == null -> "Выберите цель"
        else -> "Заполните все поля"
    }
}
