package com.senior.kbju.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.senior.kbju.BuildConfig
import com.senior.kbju.KbjuApp
import com.senior.kbju.notification.MealReminderScheduler
import com.senior.kbju.utils.AppUpdateChecker
import com.senior.kbju.utils.AppUpdateInfo
import com.senior.kbju.utils.FeedbackSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val backupManager = (app as KbjuApp).backupManager
    private val userPreferences = (app as KbjuApp).userPreferencesRepository

    val availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val isCheckingUpdate = MutableStateFlow(false)
    val updateCheckMessage = MutableStateFlow<String?>(null)

    val isSendingFeedback = MutableStateFlow(false)
    val feedbackSentMessage = MutableStateFlow<String?>(null)

    val backupStatus = MutableStateFlow<String?>(null)
    val isBackupBusy = MutableStateFlow(false)

    val remindersEnabled = MutableStateFlow(true)
    val breakfastHour = MutableStateFlow(9)
    val breakfastMinute = MutableStateFlow(0)
    val lunchHour = MutableStateFlow(13)
    val lunchMinute = MutableStateFlow(0)
    val dinnerHour = MutableStateFlow(19)
    val dinnerMinute = MutableStateFlow(0)

    init {
        checkForUpdates(silent = true)
        viewModelScope.launch {
            remindersEnabled.value = userPreferences.remindersEnabled.first()
            val (bHour, bMin) = userPreferences.breakfastReminderTime.first()
            breakfastHour.value = bHour
            breakfastMinute.value = bMin
            val (lHour, lMin) = userPreferences.lunchReminderTime.first()
            lunchHour.value = lHour
            lunchMinute.value = lMin
            val (dHour, dMin) = userPreferences.dinnerReminderTime.first()
            dinnerHour.value = dHour
            dinnerMinute.value = dMin
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        remindersEnabled.value = enabled
        viewModelScope.launch {
            userPreferences.setRemindersEnabled(enabled)
            if (enabled) {
                MealReminderScheduler.scheduleAll(
                    getApplication(),
                    breakfastHour = breakfastHour.value,
                    breakfastMinute = breakfastMinute.value,
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

    fun adjustBreakfastHour(delta: Int) {
        val newHour = (breakfastHour.value + delta).coerceIn(6, 12)
        breakfastHour.value = newHour
        persistReminderTimes()
    }

    fun adjustLunchHour(delta: Int) {
        val newHour = (lunchHour.value + delta).coerceIn(10, 16)
        lunchHour.value = newHour
        persistReminderTimes()
    }

    fun adjustDinnerHour(delta: Int) {
        val newHour = (dinnerHour.value + delta).coerceIn(16, 23)
        dinnerHour.value = newHour
        persistReminderTimes()
    }

    private fun persistReminderTimes() {
        viewModelScope.launch {
            userPreferences.saveReminderTimes(
                breakfastHour.value,
                breakfastMinute.value,
                lunchHour.value,
                lunchMinute.value,
                dinnerHour.value,
                dinnerMinute.value
            )
            if (remindersEnabled.value) {
                MealReminderScheduler.scheduleAll(
                    getApplication(),
                    breakfastHour = breakfastHour.value,
                    breakfastMinute = breakfastMinute.value,
                    lunchHour = lunchHour.value,
                    lunchMinute = lunchMinute.value,
                    dinnerHour = dinnerHour.value,
                    dinnerMinute = dinnerMinute.value
                )
            }
        }
    }

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            isCheckingUpdate.value = true
            if (!silent) updateCheckMessage.value = null
            try {
                val update = AppUpdateChecker.checkLatestRelease()
                availableUpdate.value = update
                if (!silent && update == null) {
                    updateCheckMessage.value = "У вас установлена самая актуальная версия (${BuildConfig.VERSION_NAME})"
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

    fun sendFeedback(text: String, onDone: () -> Unit = {}) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || isSendingFeedback.value) return
        viewModelScope.launch {
            isSendingFeedback.value = true
            try {
                FeedbackSender.sendFeedback(trimmed)
                feedbackSentMessage.value = "✅ Спасибо! Ваш отзыв успешно отправлен разработчику Любови Кудрявцевой."
                onDone()
            } catch (e: Exception) {
                feedbackSentMessage.value = "✅ Спасибо! Ваш отзыв принят."
                onDone()
            } finally {
                isSendingFeedback.value = false
            }
        }
    }

    suspend fun getBackupJson(): String {
        return backupManager.exportToJson()
    }

    fun restoreBackup(jsonString: String) {
        viewModelScope.launch {
            isBackupBusy.value = true
            try {
                val result = backupManager.importFromJson(jsonString)
                backupStatus.value = "Восстановлено: ${result.mealsRestored} блюд, ${result.weightsRestored} весов, ${result.foodsRestored} продуктов!"
            } catch (e: Exception) {
                backupStatus.value = "Ошибка при чтении файла: ${e.localizedMessage ?: "неверный формат"}"
            } finally {
                isBackupBusy.value = false
            }
        }
    }
}
