package com.senior.kbju.data.model

import java.time.LocalDate
import java.time.Period

// Профиль пользователя в том виде, в каком он хранится в DataStore:
// исходные параметры, норма КБЖУ каждый раз вычисляется из них.
data class UserProfile(
    val heightCm: Int,
    val weightKg: Double,
    val age: Int,
    val gender: Gender,
    val activityFactor: Double,
    val goalPercent: Int,
    val startWeightKg: Double = weightKg,
    val targetWeightKg: Double = weightKg,
    val targetWaterGlasses: Int = 6,
    val birthDateEpochDay: Long? = null
) {
    // Автоматический пересчёт возраста на текущую дату по дню рождения
    val currentAge: Int
        get() {
            if (birthDateEpochDay != null) {
                val bDate = LocalDate.ofEpochDay(birthDateEpochDay)
                val years = Period.between(bDate, LocalDate.now()).years
                if (years in 1..125) return years
            }
            return age
        }
}
