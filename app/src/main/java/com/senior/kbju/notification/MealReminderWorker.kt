package com.senior.kbju.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.senior.kbju.KbjuApp
import com.senior.kbju.MainActivity
import kotlinx.coroutines.flow.first

class MealReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = context.applicationContext as? KbjuApp ?: return Result.success()
        val isEnabled = app.userPreferencesRepository.remindersEnabled.first()
        if (!isEnabled) {
            return Result.success()
        }

        val targetHour = inputData.getInt(KEY_TARGET_HOUR, 13)
        val (title, text) = when {
            targetHour < 11 -> "Дневник питания" to "Доброе утро! Не забудьте записать завтрак 🥞"
            targetHour < 16 -> "Дневник питания" to "Не забудьте записать ваш обед 🍲"
            else -> "Дневник питания" to "Время ужина! Записали, что съели? 🥣"
        }

        showNotification(title, text)
        return Result.success()
    }

    private fun showNotification(title: String, text: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания о приёмах пищи",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Мягкие напоминания записать обед и ужин"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = if (inputData.getInt(KEY_TARGET_HOUR, 13) < 16) 1001 else 1002
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "meal_reminders_channel"
        const val KEY_TARGET_HOUR = "target_hour"
    }
}
