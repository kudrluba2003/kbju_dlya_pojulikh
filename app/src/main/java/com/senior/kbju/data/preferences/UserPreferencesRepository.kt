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
        UserProfile(
            heightCm = heightCm,
            weightKg = weightKg,
            age = age,
            gender = gender,
            activityFactor = activityFactor,
            goalPercent = goalPercent
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
        }
    }

    suspend fun updateWeight(newWeightKg: Double) {
        context.userSettings.edit { it[Keys.WEIGHT_KG] = newWeightKg }
    }
}
