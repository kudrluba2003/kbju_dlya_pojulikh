package com.senior.kbju.data.local

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.senior.kbju.data.local.entity.FoodEntity
import com.senior.kbju.data.local.seed.SeedData
import org.json.JSONArray

class DatabaseSeeder(
    private val database: AppDatabase,
    private val context: Context
) {

    // Сеем один раз: если таблица уже заполнена, повторный запуск ничего не делает,
    // поэтому поиск по продуктам всегда работает локально и мгновенно.
    suspend fun seedIfEmpty() {
        val assetFoods = loadFoodsFromAssets()
        val allSeeds = (assetFoods ?: emptyList()) + SeedData.foods()
        if (database.foodDao().count() == 0) {
            insertFoods(allSeeds)
            Log.i(TAG, "Продукты засеяны: ${allSeeds.size} записей")
        } else {
            // При обновлении приложения досеиваем новые продукты из assets и SeedData без изменения пользовательских данных
            val existingNames = database.foodDao().getAllNames().toHashSet()
            val missing = allSeeds.filter { it.name !in existingNames }
            if (missing.isNotEmpty()) {
                insertFoods(missing)
                Log.i(TAG, "Досеяно новых продуктов: ${missing.size} записей")
            }
        }
        if (database.activityDao().count() == 0) {
            database.activityDao().insertAll(SeedData.activities())
        }
    }

    private suspend fun insertFoods(foods: List<FoodEntity>) {
        database.withTransaction {
            foods.chunked(CHUNK_SIZE).forEach { chunk ->
                database.foodDao().insertAll(chunk)
            }
        }
    }

    // Большая база из products_seed.json (собирается скриптом tools/scrape_pbprog.py).
    // Файла нет или он не читается — откатываемся на встроенный список из 78 продуктов,
    // чтобы приложение работало сразу, без внешних файлов.
    private fun loadFoodsFromAssets(): List<FoodEntity>? = try {
        val json = context.assets.open(SEED_FILE).bufferedReader().use { it.readText() }
        parseProductsJson(json).takeIf { it.isNotEmpty() }
    } catch (e: java.io.IOException) {
        Log.i(TAG, "products_seed.json в assets не найден — сеем встроенный список")
        null
    } catch (e: Exception) {
        Log.w(TAG, "products_seed.json не разобрался — сеем встроенный список", e)
        null
    }

    companion object {
        private const val TAG = "DatabaseSeeder"
        private const val SEED_FILE = "products_seed.json"
        private const val CHUNK_SIZE = 500

        // Строки без названия или калорий пропускаем — битая запись не должна ломать весь seed.
        fun parseProductsJson(json: String): List<FoodEntity> {
            val root = JSONArray(json)
            val foods = mutableListOf<FoodEntity>()
            for (i in 0 until root.length()) {
                val item = root.optJSONObject(i) ?: continue
                val name = item.optString("name").trim()
                val kcal = item.optDouble("calories", Double.NaN)
                if (name.isEmpty() || kcal.isNaN() || kcal < 0.0) continue
                foods.add(
                    FoodEntity(
                        name = name,
                        kcal100 = kcal,
                        protein100 = item.optDouble("protein", 0.0).coerceAtLeast(0.0),
                        fat100 = item.optDouble("fat", 0.0).coerceAtLeast(0.0),
                        carbs100 = item.optDouble("carbs", 0.0).coerceAtLeast(0.0),
                        category = item.optString("category", "").trim().ifEmpty { null }
                    )
                )
            }
            return foods
        }
    }
}
