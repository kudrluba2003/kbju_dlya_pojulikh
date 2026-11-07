package com.senior.kbju.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.senior.kbju.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    // Ищем и во встроенной базе, и среди продуктов, созданных пользователем;
    // его собственные показываем первыми.
    @Query("SELECT * FROM food WHERE name LIKE '%' || :query || '%' ORDER BY isUserCreated DESC, name LIMIT 30")
    fun search(query: String): Flow<List<FoodEntity>>

    // Проверка перед сохранением, чтобы не плодить дубликаты с тем же названием.
    @Query("SELECT * FROM food WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): FoodEntity?

    @Query("SELECT COUNT(*) FROM food")
    suspend fun count(): Int

    @Insert
    suspend fun insert(food: FoodEntity): Long

    @Insert
    suspend fun insertAll(foods: List<FoodEntity>)

    @Query("SELECT * FROM food WHERE isUserCreated = 0 AND name LIKE '%' || :query || '%' ORDER BY name LIMIT 50")
    fun searchSimple(query: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE isUserCreated = 1 AND name LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchUserRecipes(query: String): Flow<List<FoodEntity>>

    @Query("DELETE FROM food WHERE id = :id AND isUserCreated = 1")
    suspend fun deleteUserFood(id: Long)

    @Query("SELECT * FROM food WHERE isUserCreated = 1")
    suspend fun getUserCreatedFoods(): List<FoodEntity>
}
