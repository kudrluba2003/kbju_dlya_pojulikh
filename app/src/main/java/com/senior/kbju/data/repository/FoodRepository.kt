package com.senior.kbju.data.repository

import com.senior.kbju.data.local.dao.FoodDao
import com.senior.kbju.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

class FoodRepository(private val dao: FoodDao) {

    fun search(query: String): Flow<List<FoodEntity>> = dao.search(query.trim())

    fun searchSimple(query: String): Flow<List<FoodEntity>> = dao.searchSimple(query.trim())

    fun searchUserRecipes(query: String): Flow<List<FoodEntity>> = dao.searchUserRecipes(query.trim())

    suspend fun deleteUserFood(id: Long) = dao.deleteUserFood(id)

    suspend fun findByName(name: String): FoodEntity? = dao.findByName(name.trim())

    suspend fun insert(food: FoodEntity): Long = dao.insert(food)

    suspend fun count(): Int = dao.count()
}
