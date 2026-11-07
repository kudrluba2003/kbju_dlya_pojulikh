package com.senior.kbju.data.backup

import android.content.Context
import com.senior.kbju.data.local.AppDatabase
import com.senior.kbju.data.local.entity.FoodEntity
import com.senior.kbju.data.local.entity.MealLogEntity
import com.senior.kbju.data.local.entity.WaterLogEntity
import com.senior.kbju.data.local.entity.WeightLogEntity
import com.senior.kbju.data.model.Gender
import com.senior.kbju.data.model.UserProfile
import com.senior.kbju.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

data class BackupImportResult(
    val mealsRestored: Int,
    val weightsRestored: Int,
    val watersRestored: Int,
    val foodsRestored: Int,
    val profileRestored: Boolean
) {
    val totalRestored: Int
        get() = mealsRestored + weightsRestored + watersRestored + foodsRestored
}

class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val preferencesRepository: UserPreferencesRepository
) {

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "SeniorKbju")
        root.put("exportedAt", Instant.now().toString())

        // 1. Профиль
        val profile = preferencesRepository.userProfile.firstOrNull()
        if (profile != null) {
            val profileObj = JSONObject().apply {
                put("heightCm", profile.heightCm)
                put("weightKg", profile.weightKg)
                put("age", profile.age)
                put("gender", profile.gender.name)
                put("activityFactor", profile.activityFactor)
                put("goalPercent", profile.goalPercent)
            }
            root.put("profile", profileObj)
        }

        // 2. Дневник питания
        val meals = database.mealLogDao().getAllMeals()
        val mealsArray = JSONArray()
        meals.forEach { meal ->
            val mealObj = JSONObject().apply {
                put("day", meal.day)
                put("name", meal.name)
                put("grams", meal.grams)
                put("kcal", meal.kcal)
                put("protein", meal.protein)
                put("fat", meal.fat)
                put("carbs", meal.carbs)
                put("mealType", meal.mealType)
            }
            mealsArray.put(mealObj)
        }
        root.put("meals", mealsArray)

        // 3. Записи веса
        val weights = database.weightLogDao().getAllWeights()
        val weightsArray = JSONArray()
        weights.forEach { w ->
            val wObj = JSONObject().apply {
                put("day", w.day)
                put("weightKg", w.weightKg)
                put("createdAt", w.createdAt)
            }
            weightsArray.put(wObj)
        }
        root.put("weights", weightsArray)

        // 4. Вода
        val waters = database.waterLogDao().getAllWater()
        val watersArray = JSONArray()
        waters.forEach { wt ->
            val wtObj = JSONObject().apply {
                put("day", wt.day)
                put("glasses", wt.glasses)
                put("ml", wt.ml)
                put("createdAt", wt.createdAt)
            }
            watersArray.put(wtObj)
        }
        root.put("waters", watersArray)

        // 5. Продукты пользователя
        val customFoods = database.foodDao().getUserCreatedFoods()
        val foodsArray = JSONArray()
        customFoods.forEach { f ->
            val fObj = JSONObject().apply {
                put("name", f.name)
                put("kcal100", f.kcal100)
                put("protein100", f.protein100)
                put("fat100", f.fat100)
                put("carbs100", f.carbs100)
                f.category?.let { put("category", it) }
                f.gramsPerGlass?.let { put("gramsPerGlass", it) }
                f.gramsPerSpoon?.let { put("gramsPerSpoon", it) }
            }
            foodsArray.put(fObj)
        }
        root.put("customFoods", foodsArray)

        root.toString(2)
    }

    suspend fun importFromJson(jsonString: String): BackupImportResult = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)

        var profileRestored = false
        if (root.has("profile")) {
            val p = root.getJSONObject("profile")
            val gender = Gender.entries.firstOrNull { it.name == p.optString("gender") } ?: Gender.FEMALE
            val profile = UserProfile(
                heightCm = p.optInt("heightCm", 165),
                weightKg = p.optDouble("weightKg", 70.0),
                age = p.optInt("age", 65),
                gender = gender,
                activityFactor = p.optDouble("activityFactor", 1.2),
                goalPercent = p.optInt("goalPercent", 0)
            )
            preferencesRepository.save(profile)
            profileRestored = true
        }

        // Восстановление продуктов пользователя
        var foodsCount = 0
        if (root.has("customFoods")) {
            val fArray = root.getJSONArray("customFoods")
            for (i in 0 until fArray.length()) {
                val fObj = fArray.getJSONObject(i)
                val name = fObj.optString("name").trim()
                if (name.isNotEmpty() && database.foodDao().findByName(name) == null) {
                    database.foodDao().insert(
                        FoodEntity(
                            name = name,
                            kcal100 = fObj.optDouble("kcal100", 0.0),
                            protein100 = fObj.optDouble("protein100", 0.0),
                            fat100 = fObj.optDouble("fat100", 0.0),
                            carbs100 = fObj.optDouble("carbs100", 0.0),
                            category = if (fObj.has("category")) fObj.getString("category") else null,
                            gramsPerGlass = if (fObj.has("gramsPerGlass")) fObj.getDouble("gramsPerGlass") else null,
                            gramsPerSpoon = if (fObj.has("gramsPerSpoon")) fObj.getDouble("gramsPerSpoon") else null,
                            isUserCreated = true
                        )
                    )
                    foodsCount++
                }
            }
        }

        // Восстановление приёмов пищи
        var mealsCount = 0
        if (root.has("meals")) {
            val mArray = root.getJSONArray("meals")
            val mealsToInsert = mutableListOf<MealLogEntity>()
            for (i in 0 until mArray.length()) {
                val m = mArray.getJSONObject(i)
                mealsToInsert.add(
                    MealLogEntity(
                        day = m.optLong("day"),
                        name = m.optString("name", "Блюдо"),
                        grams = m.optDouble("grams", 100.0),
                        kcal = m.optDouble("kcal", 0.0),
                        protein = m.optDouble("protein", 0.0),
                        fat = m.optDouble("fat", 0.0),
                        carbs = m.optDouble("carbs", 0.0),
                        mealType = m.optString("mealType", "LUNCH")
                    )
                )
            }
            if (mealsToInsert.isNotEmpty()) {
                database.mealLogDao().insertAll(mealsToInsert)
                mealsCount = mealsToInsert.size
            }
        }

        // Восстановление веса
        var weightsCount = 0
        if (root.has("weights")) {
            val wArray = root.getJSONArray("weights")
            val weightsToInsert = mutableListOf<WeightLogEntity>()
            for (i in 0 until wArray.length()) {
                val w = wArray.getJSONObject(i)
                weightsToInsert.add(
                    WeightLogEntity(
                        day = w.optLong("day"),
                        weightKg = w.optDouble("weightKg"),
                        createdAt = w.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            if (weightsToInsert.isNotEmpty()) {
                database.weightLogDao().insertAll(weightsToInsert)
                weightsCount = weightsToInsert.size
            }
        }

        // Восстановление воды
        var watersCount = 0
        if (root.has("waters")) {
            val wtArray = root.getJSONArray("waters")
            val watersToInsert = mutableListOf<WaterLogEntity>()
            for (i in 0 until wtArray.length()) {
                val wt = wtArray.getJSONObject(i)
                val glasses = wt.optInt("glasses", wt.optInt("glassesCount", 0))
                val ml = wt.optInt("ml", glasses * 250)
                watersToInsert.add(
                    WaterLogEntity(
                        day = wt.optLong("day"),
                        glasses = glasses,
                        ml = ml,
                        createdAt = wt.optLong("createdAt", wt.optLong("updatedAt", System.currentTimeMillis()))
                    )
                )
            }
            if (watersToInsert.isNotEmpty()) {
                database.waterLogDao().insertAll(watersToInsert)
                watersCount = watersToInsert.size
            }
        }

        BackupImportResult(
            mealsRestored = mealsCount,
            weightsRestored = weightsCount,
            watersRestored = watersCount,
            foodsRestored = foodsCount,
            profileRestored = profileRestored
        )
    }
}
