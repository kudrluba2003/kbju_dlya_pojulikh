package com.senior.kbju.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.senior.kbju.data.local.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {

    @Query("SELECT * FROM weight_log ORDER BY day DESC, createdAt DESC")
    fun observeAll(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_log WHERE day >= :fromDay ORDER BY day ASC, createdAt ASC")
    fun observeSince(fromDay: Long): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_log ORDER BY day DESC, createdAt DESC LIMIT 1")
    suspend fun getLatest(): WeightLogEntity?

    @Insert
    suspend fun insert(entry: WeightLogEntity): Long

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<WeightLogEntity>)

    @Query("SELECT * FROM weight_log ORDER BY day DESC")
    suspend fun getAllWeights(): List<WeightLogEntity>

    @Query("DELETE FROM weight_log WHERE id = :id")
    suspend fun delete(id: Long)
}
