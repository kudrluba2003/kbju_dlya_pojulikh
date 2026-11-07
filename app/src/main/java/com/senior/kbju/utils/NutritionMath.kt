package com.senior.kbju.utils

import com.senior.kbju.data.model.Gender
import kotlin.math.roundToInt

data class Nutrients(val kcal: Double, val protein: Double, val fat: Double, val carbs: Double)

data class DailyTargets(val kcal: Int, val protein: Int, val fat: Int, val carbs: Int, val bmr: Int = 0)

object NutritionMath {

    const val MIN_DAILY_KCAL = 1200

    // Формула Миффлина-Сан Жеора
    fun basalMetabolism(weightKg: Double, heightCm: Int, age: Int, gender: Gender): Double {
        val genderPart = if (gender == Gender.MALE) 5.0 else -161.0
        return 10.0 * weightKg + 6.25 * heightCm - 5.0 * age + genderPart
    }

    // Суточная норма КБЖУ: базовый обмен × коэффициент активности × (1 ± процент цели).
    fun dailyTargets(
        weightKg: Double,
        heightCm: Int,
        age: Int,
        gender: Gender,
        activityFactor: Double,
        goalPercent: Int
    ): DailyTargets {
        val bmr = basalMetabolism(weightKg, heightCm, age, gender)
        val maintenance = bmr * activityFactor
        val withGoal = maintenance * (1.0 + goalPercent / 100.0)
        val kcal = maxOf(withGoal, MIN_DAILY_KCAL.toDouble()).roundToInt()
        return DailyTargets(
            kcal = kcal,
            protein = (kcal * 0.25 / 4.0).roundToInt(),
            fat = (kcal * 0.30 / 9.0).roundToInt(),
            carbs = (kcal * 0.45 / 4.0).roundToInt(),
            bmr = bmr.roundToInt()
        )
    }

    // Расход по MET: (MET * 3.5 * вес_кг / 200) * минуты, с поправкой на возраст
    fun activityCalories(met: Double, weightKg: Double, minutes: Int, age: Int): Int {
        val ageFactor = when {
            age >= 75 -> 0.80
            age >= 65 -> 0.85
            age >= 50 -> 0.90
            else -> 1.00
        }
        return (met * 3.5 * weightKg / 200.0 * minutes * ageFactor).roundToInt()
    }

    fun per100(totalGrams: Double, total: Nutrients): Nutrients {
        val factor = 100.0 / totalGrams
        return Nutrients(
            kcal = total.kcal * factor,
            protein = total.protein * factor,
            fat = total.fat * factor,
            carbs = total.carbs * factor
        )
    }

    fun portion(per100: Nutrients, grams: Double): Nutrients {
        val factor = grams / 100.0
        return Nutrients(
            kcal = per100.kcal * factor,
            protein = per100.protein * factor,
            fat = per100.fat * factor,
            carbs = per100.carbs * factor
        )
    }
}
