package com.senior.kbju.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object MealReminderScheduler {

    const val WORK_LUNCH = "meal_reminder_lunch"
    const val WORK_DINNER = "meal_reminder_dinner"

    fun scheduleAll(context: Context) {
        scheduleDailyReminder(context, WORK_LUNCH, targetHour = 13, targetMinute = 0)
        scheduleDailyReminder(context, WORK_DINNER, targetHour = 19, targetMinute = 0)
    }

    fun cancelAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_LUNCH)
        workManager.cancelUniqueWork(WORK_DINNER)
    }

    private fun scheduleDailyReminder(
        context: Context,
        uniqueWorkName: String,
        targetHour: Int,
        targetMinute: Int
    ) {
        val initialDelayMs = calculateDelayToNext(targetHour, targetMinute, LocalDateTime.now())
        val request = PeriodicWorkRequestBuilder<MealReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(MealReminderWorker.KEY_TARGET_HOUR to targetHour))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun calculateDelayToNext(
        targetHour: Int,
        targetMinute: Int,
        now: LocalDateTime = LocalDateTime.now()
    ): Long {
        var target = now.withHour(targetHour).withMinute(targetMinute).withSecond(0).withNano(0)
        if (!now.isBefore(target)) {
            target = target.plusDays(1)
        }
        return Duration.between(now, target).toMillis()
    }
}
