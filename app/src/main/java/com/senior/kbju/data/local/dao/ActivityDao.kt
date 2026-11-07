package com.senior.kbju.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.senior.kbju.data.local.entity.ActivityEntity
import com.senior.kbju.data.local.entity.ActivityLogEntity
import com.senior.kbju.data.local.projection.DayTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Query("SELECT * FROM activity ORDER BY id")
    fun observeAll(): Flow<List<ActivityEntity>>

    @Query("SELECT COUNT(*) FROM activity")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(activities: List<ActivityEntity>)

    @Query("SELECT * FROM activity_log WHERE day = :day ORDER BY id DESC")
    fun observeLogsForDay(day: Long): Flow<List<ActivityLogEntity>>

    // «История за неделю»: сколько сожжено в каждый из последних дней.
    @Query("SELECT day, SUM(kcal) AS total FROM activity_log WHERE day >= :fromDay GROUP BY day ORDER BY day DESC")
    suspend fun burnedSince(fromDay: Long): List<DayTotal>

    @Insert
    suspend fun insertLog(log: ActivityLogEntity): Long

    @Query("DELETE FROM activity_log WHERE id = :id")
    suspend fun deleteLog(id: Long)
}
