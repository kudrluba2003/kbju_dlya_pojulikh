package com.senior.kbju.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.senior.kbju.data.local.entity.WaterLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {

    @Query("SELECT * FROM water_log WHERE day = :day LIMIT 1")
    fun observeForDay(day: Long): Flow<WaterLogEntity?>

    @Query("SELECT * FROM water_log WHERE day = :day LIMIT 1")
    suspend fun getForDay(day: Long): WaterLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(waterLog: WaterLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(waterLogs: List<WaterLogEntity>)

    @Query("SELECT * FROM water_log ORDER BY day DESC")
    suspend fun getAllWater(): List<WaterLogEntity>

    @Query("DELETE FROM water_log WHERE day = :day")
    suspend fun deleteForDay(day: Long)
}
