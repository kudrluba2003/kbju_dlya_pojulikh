package com.senior.kbju.utils

import com.senior.kbju.data.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class NutritionMathTest {

    @Test
    fun testBasalMetabolismMale() {
        // Формула Миффлина-Сан Жеора для мужчины: 10 * вес + 6.25 * рост - 5 * возраст + 5
        // 10 * 80 + 6.25 * 175 - 5 * 65 + 5 = 800 + 1093.75 - 325 + 5 = 1573.75
        val bmr = NutritionMath.basalMetabolism(weightKg = 80.0, heightCm = 175, age = 65, gender = Gender.MALE)
        assertEquals(1573.75, bmr, 0.001)
    }

    @Test
    fun testBasalMetabolismFemale() {
        // Формула Миффлина-Сан Жеора для женщины: 10 * вес + 6.25 * рост - 5 * возраст - 161
        // 10 * 65 + 6.25 * 160 - 5 * 70 - 161 = 650 + 1000 - 350 - 161 = 1139.0
        val bmr = NutritionMath.basalMetabolism(weightKg = 65.0, heightCm = 160, age = 70, gender = Gender.FEMALE)
        assertEquals(1139.0, bmr, 0.001)
    }

    @Test
    fun testDailyTargetsEnforcesMinDailyKcal() {
        // Маленький вес и пожилой возраст с дефицитом дают меньше 1200 ккал
        // Должна сработать защитная планка безопасности MIN_DAILY_KCAL = 1200
        val targets = NutritionMath.dailyTargets(
            weightKg = 45.0,
            heightCm = 145,
            age = 80,
            gender = Gender.FEMALE,
            activityFactor = 1.0,
            goalPercent = -20
        )
        assertEquals(NutritionMath.MIN_DAILY_KCAL, targets.kcal)
        // Проверяем распределение макронутриентов для 1200 ккал:
        // Белки: 1200 * 0.25 / 4 = 75 г
        // Жиры: 1200 * 0.30 / 9 = 40 г
        // Углеводы: 1200 * 0.45 / 4 = 135 г
        assertEquals(75, targets.protein)
        assertEquals(40, targets.fat)
        assertEquals(135, targets.carbs)
    }

    @Test
    fun testDailyTargetsNormalCalculation() {
        val targets = NutritionMath.dailyTargets(
            weightKg = 70.0,
            heightCm = 170,
            age = 60,
            gender = Gender.MALE,
            activityFactor = 1.2,
            goalPercent = 0
        )
        // BMR: 10*70 + 6.25*170 - 5*60 + 5 = 700 + 1062.5 - 300 + 5 = 1467.5
        // Maintenance: 1467.5 * 1.2 = 1761.0
        assertEquals(1761, targets.kcal)
        assertTrue(targets.kcal > NutritionMath.MIN_DAILY_KCAL)
        assertEquals((1761 * 0.25 / 4.0).roundToInt(), targets.protein)
        assertEquals((1761 * 0.30 / 9.0).roundToInt(), targets.fat)
        assertEquals((1761 * 0.45 / 4.0).roundToInt(), targets.carbs)
    }

    @Test
    fun testActivityCaloriesAgeFactors() {
        val met = 3.0
        val weight = 70.0
        val minutes = 60
        // Базовый расход: met * 3.5 * weight / 200.0 * minutes = 3 * 3.5 * 70 / 200 * 60 = 220.5
        // До 50 лет: factor 1.0 -> 221
        assertEquals(221, NutritionMath.activityCalories(met, weight, minutes, age = 40))
        // 50-64 года: factor 0.90 -> 220.5 * 0.90 = 198.45 -> 198
        assertEquals(198, NutritionMath.activityCalories(met, weight, minutes, age = 55))
        // 65-74 года: factor 0.85 -> 220.5 * 0.85 = 187.425 -> 187
        assertEquals(187, NutritionMath.activityCalories(met, weight, minutes, age = 68))
        // 75+ лет: factor 0.80 -> 220.5 * 0.80 = 176.4 -> 176
        assertEquals(176, NutritionMath.activityCalories(met, weight, minutes, age = 80))
    }

    @Test
    fun testPortionAndPer100Calculations() {
        val per100 = Nutrients(kcal = 200.0, protein = 10.0, fat = 5.0, carbs = 25.0)
        // Порция 50 г (ровно половина)
        val portion50 = NutritionMath.portion(per100, grams = 50.0)
        assertEquals(100.0, portion50.kcal, 0.001)
        assertEquals(5.0, portion50.protein, 0.001)
        assertEquals(2.5, portion50.fat, 0.001)
        assertEquals(12.5, portion50.carbs, 0.001)

        // Обратный пересчет в per100
        val backToPer100 = NutritionMath.per100(totalGrams = 50.0, total = portion50)
        assertEquals(200.0, backToPer100.kcal, 0.001)
        assertEquals(10.0, backToPer100.protein, 0.001)
        assertEquals(5.0, backToPer100.fat, 0.001)
        assertEquals(25.0, backToPer100.carbs, 0.001)
    }
}
