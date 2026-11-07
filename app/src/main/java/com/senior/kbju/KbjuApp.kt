package com.senior.kbju

import android.app.Application
import com.senior.kbju.data.backup.BackupManager
import com.senior.kbju.data.local.AppDatabase
import com.senior.kbju.data.local.DatabaseSeeder
import com.senior.kbju.data.preferences.UserPreferencesRepository
import com.senior.kbju.data.repository.DiaryRepository
import com.senior.kbju.data.repository.FoodRepository
import com.senior.kbju.notification.MealReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KbjuApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Профиль — в DataStore, дневник питания и активности — в Room.
    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(applicationContext)
    }
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val foodRepository: FoodRepository by lazy { FoodRepository(database.foodDao()) }
    val diaryRepository: DiaryRepository by lazy {
        DiaryRepository(
            database.mealLogDao(),
            database.activityDao(),
            database.weightLogDao(),
            database.waterLogDao()
        )
    }
    val backupManager: BackupManager by lazy {
        BackupManager(this, database, userPreferencesRepository)
    }

    override fun onCreate() {
        super.onCreate()
        com.senior.kbju.utils.CrashHandler.init(this)
        applicationScope.launch {
            DatabaseSeeder(database, this@KbjuApp).seedIfEmpty()
            if (userPreferencesRepository.remindersEnabled.first()) {
                MealReminderScheduler.scheduleAll(this@KbjuApp)
            }
        }
    }
}
