package com.senior.kbju.utils

import com.senior.kbju.data.local.entity.ActivityLogEntity
import com.senior.kbju.data.local.entity.MealLogEntity
import com.senior.kbju.data.model.Gender
import com.senior.kbju.data.model.UserProfile
import com.senior.kbju.viewmodel.DashboardUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardLogicTest {

    @Test
    fun testActivityCaloriesAreNotAddedToEatingBudget() {
        val meals = listOf(
            MealLogEntity(id = 1, day = 1, name = "Овсянка", grams = 200.0, kcal = 300.0, protein = 10.0, fat = 5.0, carbs = 50.0, mealType = "BREAKFAST")
        )
        val activities = listOf(
            ActivityLogEntity(id = 1, day = 1, name = "Ходьба", minutes = 60, kcal = 250.0)
        )
        val state = DashboardUiState(
            targetKcal = 1600,
            meals = meals,
            activities = activities
        )

        assertEquals(300, state.eatenKcal)
        assertEquals(250, state.burnedKcal)
        // Остаток калорий ДОЛЖЕН быть строго 1600 - 300 = 1300 ккал (активность НЕ плюсуется к норме)
        assertEquals(1300, state.remainingKcal)
    }

    @Test
    fun testWeightGoalProgressCalculation() {
        val profile = UserProfile(
            heightCm = 170,
            weightKg = 78.5,
            age = 65,
            gender = Gender.FEMALE,
            activityFactor = 1.2,
            goalPercent = -20,
            startWeightKg = 84.0,
            targetWeightKg = 72.0,
            targetWaterGlasses = 8
        )
        val state = DashboardUiState(profile = profile)

        assertEquals(78.5, state.currentWeightKg, 0.01)
        assertEquals(84.0, state.startWeightKg, 0.01)
        assertEquals(72.0, state.targetWeightKg, 0.01)
        // До цели осталось: 78.5 - 72.0 = 6.5 кг
        assertEquals(6.5, state.weightDiffToTarget, 0.01)
        // С начала пути: 78.5 - 84.0 = -5.5 кг (сброшено 5.5 кг)
        assertEquals(-5.5, state.weightDiffFromStart, 0.01)
        assertEquals(8, state.targetWaterGlasses)
        assertEquals(2000, state.targetWaterMl)
    }
}
