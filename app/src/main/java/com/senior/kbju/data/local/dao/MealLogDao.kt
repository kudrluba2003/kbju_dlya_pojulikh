package com.senior.kbju.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.senior.kbju.data.local.entity.MealLogEntity
import com.senior.kbju.data.local.projection.DayTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface MealLogDao {

    @Query("SELECT * FROM meal_log WHERE day = :day ORDER BY id DESC")
    fun observeForDay(day: Long): Flow<List<MealLogEntity>>

    // «История за неделю»: сколько съедено в каждый из последних дней.
    @Query("SELECT day, SUM(kcal) AS total FROM meal_log WHERE day >= :fromDay GROUP BY day ORDER BY day DESC")
    suspend fun totalsSince(fromDay: Long): List<DayTotal>

    @Insert
    suspend fun insert(meal: MealLogEntity): Long

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(meals: List<MealLogEntity>)

    @Query("SELECT * FROM meal_log ORDER BY day DESC")
    suspend fun getAllMeals(): List<MealLogEntity>

    @Query("DELETE FROM meal_log WHERE id = :id")
    suspend fun delete(id: Long)
}
