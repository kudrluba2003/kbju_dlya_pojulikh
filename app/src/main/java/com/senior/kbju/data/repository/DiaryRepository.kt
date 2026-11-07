package com.senior.kbju.data.repository

import com.senior.kbju.data.local.dao.ActivityDao
import com.senior.kbju.data.local.dao.MealLogDao
import com.senior.kbju.data.local.dao.WaterLogDao
import com.senior.kbju.data.local.dao.WeightLogDao
import com.senior.kbju.data.local.entity.ActivityEntity
import com.senior.kbju.data.local.entity.ActivityLogEntity
import com.senior.kbju.data.local.entity.MealLogEntity
import com.senior.kbju.data.local.entity.WaterLogEntity
import com.senior.kbju.data.local.entity.WeightLogEntity
import com.senior.kbju.data.local.projection.DayTotal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

class DiaryRepository(
    private val mealLogDao: MealLogDao,
    private val activityDao: ActivityDao,
    private val weightLogDao: WeightLogDao,
    private val waterLogDao: WaterLogDao
) {

    // Реактивный поток текущей даты: обновляется сразу при старте и следит за переходом через полночь.
    fun observeCurrentEpochDay(): Flow<Long> = flow {
        var lastDay = LocalDate.now().toEpochDay()
        emit(lastDay)
        while (true) {
            delay(15_000L) // проверка каждые 15 секунд
            val currentDay = LocalDate.now().toEpochDay()
            if (currentDay != lastDay) {
                lastDay = currentDay
                emit(currentDay)
            }
        }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTodayMeals(): Flow<List<MealLogEntity>> =
        observeCurrentEpochDay().flatMapLatest { day ->
            mealLogDao.observeForDay(day)
        }

    fun observeMealsForDay(day: Long): Flow<List<MealLogEntity>> = mealLogDao.observeForDay(day)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTodayActivities(): Flow<List<ActivityLogEntity>> =
        observeCurrentEpochDay().flatMapLatest { day ->
            activityDao.observeLogsForDay(day)
        }

    fun observeActivitiesForDay(day: Long): Flow<List<ActivityLogEntity>> = activityDao.observeLogsForDay(day)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTodayWater(): Flow<WaterLogEntity?> =
        observeCurrentEpochDay().flatMapLatest { day ->
            waterLogDao.observeForDay(day)
        }

    fun observeWaterForDay(day: Long): Flow<WaterLogEntity?> = waterLogDao.observeForDay(day)

    suspend fun addWaterGlass(day: Long = LocalDate.now().toEpochDay()) {
        val current = waterLogDao.getForDay(day)
        val newGlasses = (current?.glasses ?: 0) + 1
        waterLogDao.upsert(
            WaterLogEntity(
                id = current?.id ?: 0,
                day = day,
                glasses = newGlasses,
                ml = newGlasses * 250
            )
        )
    }

    suspend fun removeWaterGlass(day: Long = LocalDate.now().toEpochDay()) {
        val current = waterLogDao.getForDay(day) ?: return
        if (current.glasses <= 1) {
            waterLogDao.deleteForDay(day)
        } else {
            val newGlasses = current.glasses - 1
            waterLogDao.upsert(
                current.copy(
                    glasses = newGlasses,
                    ml = newGlasses * 250
                )
            )
        }
    }

    fun observeAllWeights(): Flow<List<WeightLogEntity>> = weightLogDao.observeAll()

    fun observeWeightsSince(fromDay: Long): Flow<List<WeightLogEntity>> =
        weightLogDao.observeSince(fromDay)

    suspend fun logWeight(weightKg: Double, day: Long = LocalDate.now().toEpochDay()) {
        weightLogDao.insert(WeightLogEntity(day = day, weightKg = weightKg))
    }

    suspend fun deleteWeightLog(id: Long) {
        weightLogDao.delete(id)
    }

    fun observeActivityCatalog(): Flow<List<ActivityEntity>> = activityDao.observeAll()

    suspend fun mealTotalsSince(fromDay: Long): List<DayTotal> = mealLogDao.totalsSince(fromDay)

    suspend fun activityTotalsSince(fromDay: Long): List<DayTotal> = activityDao.burnedSince(fromDay)

    suspend fun addMeal(meal: MealLogEntity) = mealLogDao.insert(meal)

    suspend fun deleteMeal(id: Long) = mealLogDao.delete(id)

    suspend fun addActivityLog(log: ActivityLogEntity) = activityDao.insertLog(log)

    suspend fun deleteActivityLog(id: Long) = activityDao.deleteLog(id)
}
