package com.senior.kbju.data.model

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
    val targetWaterGlasses: Int = 6
)
