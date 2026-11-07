package com.senior.kbju.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.senior.kbju.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Query("""
        SELECT * FROM food 
        WHERE (:t1 IS NULL OR LOWER(name) LIKE '%' || LOWER(:t1) || '%')
          AND (:t2 IS NULL OR LOWER(name) LIKE '%' || LOWER(:t2) || '%')
          AND (:t3 IS NULL OR LOWER(name) LIKE '%' || LOWER(:t3) || '%')
        ORDER BY 
          isUserCreated DESC,
          CASE 
            WHEN LOWER(name) = LOWER(:rawQuery) THEN 0
            WHEN LOWER(name) LIKE LOWER(:rawQuery) || '%' THEN 1
            WHEN LOWER(name) LIKE '%' || LOWER(:rawQuery) || '%' THEN 2
            ELSE 3 
          END,
          name
        LIMIT 60
    """)
    fun searchTokens(
        rawQuery: String,
        t1: String?,
        t2: String?,
        t3: String?
    ): Flow<List<FoodEntity>>

    @Query("""
        SELECT * FROM food 
        WHERE isUserCreated = 0 
          AND (:t1 IS NULL OR LOWER(name) LIKE '%' || LOWER(:t1) || '%')
          AND (:t2 IS NULL OR LOWER(name) LIKE '%' || LOWER(:t2) || '%')
          AND (:t3 IS NULL OR LOWER(name) LIKE '%' || LOWER(:t3) || '%')
        ORDER BY 
          CASE 
            WHEN LOWER(name) = LOWER(:rawQuery) THEN 0
            WHEN LOWER(name) LIKE LOWER(:rawQuery) || '%' THEN 1
            WHEN LOWER(name) LIKE '%' || LOWER(:rawQuery) || '%' THEN 2
            ELSE 3 
          END,
          name
        LIMIT 60
    """)
    fun searchSimpleTokens(
        rawQuery: String,
        t1: String?,
        t2: String?,
        t3: String?
    ): Flow<List<FoodEntity>>

    @Query("""
        SELECT * FROM food 
        WHERE isUserCreated = 1 
          AND (:t1 IS NULL OR LOWER(name) LIKE '%' || LOWER(:t1) || '%')
          AND (:t2 IS NULL OR LOWER(name) LIKE '%' || LOWER(:t2) || '%')
          AND (:t3 IS NULL OR LOWER(name) LIKE '%' || LOWER(:t3) || '%')
        ORDER BY 
          CASE 
            WHEN LOWER(name) = LOWER(:rawQuery) THEN 0
            WHEN LOWER(name) LIKE LOWER(:rawQuery) || '%' THEN 1
            WHEN LOWER(name) LIKE '%' || LOWER(:rawQuery) || '%' THEN 2
            ELSE 3 
          END,
          id DESC
    """)
    fun searchUserRecipesTokens(
        rawQuery: String,
        t1: String?,
        t2: String?,
        t3: String?
    ): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): FoodEntity?

    @Query("SELECT name FROM food")
    suspend fun getAllNames(): List<String>

    @Query("SELECT COUNT(*) FROM food")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(food: FoodEntity): Long

    @androidx.room.Update
    suspend fun update(food: FoodEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(foods: List<FoodEntity>)

    @Query("DELETE FROM food WHERE id = :id AND isUserCreated = 1")
    suspend fun deleteUserFood(id: Long)

    @Query("SELECT * FROM food WHERE isUserCreated = 1")
    suspend fun getUserCreatedFoods(): List<FoodEntity>
}
