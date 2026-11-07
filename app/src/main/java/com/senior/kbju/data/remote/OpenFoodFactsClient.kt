package com.senior.kbju.data.remote

import android.util.Log
import com.senior.kbju.data.local.entity.FoodEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object OpenFoodFactsClient {
    private const val TAG = "OpenFoodFactsClient"

    suspend fun searchOnline(query: String): List<FoodEntity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val urlString = "https://world.openfoodfacts.org/api/v2/search?search_terms=$encoded&fields=product_name,product_name_ru,nutriments,brands,categories&page_size=15"
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SeniorKbjuApp - Android - Version 2.1 - kudrluba2003@gmail.com")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (conn.responseCode !in 200..299) {
                Log.w(TAG, "OpenFoodFacts responded with code: ${conn.responseCode}")
                return@withContext emptyList()
            }

            val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonText)
            val products = root.optJSONArray("products") ?: return@withContext emptyList()

            val results = mutableListOf<FoodEntity>()
            for (i in 0 until products.length()) {
                val item = products.optJSONObject(i) ?: continue
                val nameRu = item.optString("product_name_ru").trim()
                val nameDef = item.optString("product_name").trim()
                val brand = item.optString("brands").trim()

                val rawName = when {
                    nameRu.isNotEmpty() -> nameRu
                    nameDef.isNotEmpty() -> nameDef
                    else -> continue
                }

                val displayName = if (brand.isNotEmpty() && !rawName.contains(brand, ignoreCase = true)) {
                    "$rawName «$brand»"
                } else rawName

                val nutriments = item.optJSONObject("nutriments") ?: continue
                val kcal = nutriments.optDouble("energy-kcal_100g", Double.NaN).let {
                    if (it.isNaN() || it <= 0) nutriments.optDouble("energy-kcal", Double.NaN) else it
                }
                if (kcal.isNaN() || kcal < 0.0) continue

                val protein = nutriments.optDouble("proteins_100g", 0.0).coerceAtLeast(0.0)
                val fat = nutriments.optDouble("fat_100g", 0.0).coerceAtLeast(0.0)
                val carbs = nutriments.optDouble("carbohydrates_100g", 0.0).coerceAtLeast(0.0)

                results.add(
                    FoodEntity(
                        name = displayName,
                        kcal100 = kcal,
                        protein100 = protein,
                        fat100 = fat,
                        carbs100 = carbs,
                        category = "Интернет-каталог",
                        isUserCreated = false
                    )
                )
            }
            results
        } catch (e: Exception) {
            Log.i(TAG, "OpenFoodFacts request failed (using offline DB): ${e.message}")
            emptyList()
        }
    }
}
