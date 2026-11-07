package com.senior.kbju.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.senior.kbju.data.model.Gender
import com.senior.kbju.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSettings by preferencesDataStore(name = "user_settings")

// Профиль хранится в DataStore (Preferences), дневник питания — в Room.
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val HEIGHT_CM = intPreferencesKey("height_cm")
        val WEIGHT_KG = doublePreferencesKey("weight_kg")
        val AGE = intPreferencesKey("age")
        val GENDER = stringPreferencesKey("gender")
        val ACTIVITY_FACTOR = doublePreferencesKey("activity_factor")
        val GOAL_PERCENT = intPreferencesKey("goal_percent")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val START_WEIGHT_KG = doublePreferencesKey("start_weight_kg")
        val TARGET_WEIGHT_KG = doublePreferencesKey("target_weight_kg")
        val TARGET_WATER_GLASSES = intPreferencesKey("target_water_glasses")
        val LUNCH_REMINDER_HOUR = intPreferencesKey("lunch_reminder_hour")
        val LUNCH_REMINDER_MINUTE = intPreferencesKey("lunch_reminder_minute")
        val DINNER_REMINDER_HOUR = intPreferencesKey("dinner_reminder_hour")
        val DINNER_REMINDER_MINUTE = intPreferencesKey("dinner_reminder_minute")
    }

    val lunchReminderTime: Flow<Pair<Int, Int>> = context.userSettings.data.map { prefs ->
        val hour = prefs[Keys.LUNCH_REMINDER_HOUR] ?: 13
        val min = prefs[Keys.LUNCH_REMINDER_MINUTE] ?: 0
        hour to min
    }

    val dinnerReminderTime: Flow<Pair<Int, Int>> = context.userSettings.data.map { prefs ->
        val hour = prefs[Keys.DINNER_REMINDER_HOUR] ?: 19
        val min = prefs[Keys.DINNER_REMINDER_MINUTE] ?: 0
        hour to min
    }

    suspend fun saveReminderTimes(lunchHour: Int, lunchMin: Int, dinnerHour: Int, dinnerMin: Int) {
        context.userSettings.edit { prefs ->
            prefs[Keys.LUNCH_REMINDER_HOUR] = lunchHour
            prefs[Keys.LUNCH_REMINDER_MINUTE] = lunchMin
            prefs[Keys.DINNER_REMINDER_HOUR] = dinnerHour
            prefs[Keys.DINNER_REMINDER_MINUTE] = dinnerMin
        }
    }

    val remindersEnabled: Flow<Boolean> = context.userSettings.data.map { prefs ->
        prefs[Keys.REMINDERS_ENABLED] ?: true
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.userSettings.edit { it[Keys.REMINDERS_ENABLED] = enabled }
    }

    // null — профиль ещё не заполнен, нужен онбординг.
    val userProfile: Flow<UserProfile?> = context.userSettings.data.map { prefs ->
        val heightCm = prefs[Keys.HEIGHT_CM] ?: return@map null
        val weightKg = prefs[Keys.WEIGHT_KG] ?: return@map null
        val age = prefs[Keys.AGE] ?: return@map null
        val gender = Gender.entries.firstOrNull { it.name == prefs[Keys.GENDER] } ?: return@map null
        val activityFactor = prefs[Keys.ACTIVITY_FACTOR] ?: return@map null
        val goalPercent = prefs[Keys.GOAL_PERCENT] ?: return@map null
        val startWeightKg = prefs[Keys.START_WEIGHT_KG] ?: weightKg
        val targetWeightKg = prefs[Keys.TARGET_WEIGHT_KG] ?: weightKg
        val targetWaterGlasses = prefs[Keys.TARGET_WATER_GLASSES] ?: 6
        UserProfile(
            heightCm = heightCm,
            weightKg = weightKg,
            age = age,
            gender = gender,
            activityFactor = activityFactor,
            goalPercent = goalPercent,
            startWeightKg = startWeightKg,
            targetWeightKg = targetWeightKg,
            targetWaterGlasses = targetWaterGlasses
        )
    }

    suspend fun save(profile: UserProfile) {
        context.userSettings.edit { prefs ->
            prefs[Keys.HEIGHT_CM] = profile.heightCm
            prefs[Keys.WEIGHT_KG] = profile.weightKg
            prefs[Keys.AGE] = profile.age
            prefs[Keys.GENDER] = profile.gender.name
            prefs[Keys.ACTIVITY_FACTOR] = profile.activityFactor
            prefs[Keys.GOAL_PERCENT] = profile.goalPercent
            prefs[Keys.START_WEIGHT_KG] = profile.startWeightKg
            prefs[Keys.TARGET_WEIGHT_KG] = profile.targetWeightKg
            prefs[Keys.TARGET_WATER_GLASSES] = profile.targetWaterGlasses
        }
    }

    suspend fun updateWeight(newWeightKg: Double) {
        context.userSettings.edit { prefs ->
            prefs[Keys.WEIGHT_KG] = newWeightKg
            if (!prefs.contains(Keys.START_WEIGHT_KG)) {
                prefs[Keys.START_WEIGHT_KG] = newWeightKg
            }
        }
    }

    suspend fun updateTargetWater(glasses: Int) {
        context.userSettings.edit { prefs ->
            prefs[Keys.TARGET_WATER_GLASSES] = glasses.coerceIn(2, 20)
        }
    }

    suspend fun updateTargetWeight(targetWeightKg: Double) {
        context.userSettings.edit { prefs ->
            prefs[Keys.TARGET_WEIGHT_KG] = targetWeightKg
        }
    }
}
