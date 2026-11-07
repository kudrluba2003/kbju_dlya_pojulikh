package com.senior.kbju.data.repository

import com.senior.kbju.data.local.dao.FoodDao
import com.senior.kbju.data.local.entity.FoodEntity
import com.senior.kbju.utils.RussianSearchUtils
import kotlinx.coroutines.flow.Flow

class FoodRepository(private val dao: FoodDao) {

    fun search(query: String): Flow<List<FoodEntity>> {
        val trimmed = query.trim()
        val tokens = RussianSearchUtils.extractTokens(trimmed)
        return dao.searchTokens(
            rawQuery = trimmed,
            t1 = tokens.getOrNull(0),
            t2 = tokens.getOrNull(1),
            t3 = tokens.getOrNull(2)
        )
    }

    fun searchSimple(query: String): Flow<List<FoodEntity>> {
        val trimmed = query.trim()
        val tokens = RussianSearchUtils.extractTokens(trimmed)
        return dao.searchSimpleTokens(
            rawQuery = trimmed,
            t1 = tokens.getOrNull(0),
            t2 = tokens.getOrNull(1),
            t3 = tokens.getOrNull(2)
        )
    }

    fun searchUserRecipes(query: String): Flow<List<FoodEntity>> {
        val trimmed = query.trim()
        val tokens = RussianSearchUtils.extractTokens(trimmed)
        return dao.searchUserRecipesTokens(
            rawQuery = trimmed,
            t1 = tokens.getOrNull(0),
            t2 = tokens.getOrNull(1),
            t3 = tokens.getOrNull(2)
        )
    }

    suspend fun deleteUserFood(id: Long) = dao.deleteUserFood(id)

    suspend fun findByName(name: String): FoodEntity? = dao.findByName(name.trim())

    suspend fun insert(food: FoodEntity): Long = dao.insert(food)

    suspend fun count(): Int = dao.count()
}
