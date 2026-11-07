package com.senior.kbju.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.senior.kbju.KbjuApp
import com.senior.kbju.data.local.entity.FoodEntity
import com.senior.kbju.data.local.entity.MealLogEntity
import com.senior.kbju.data.model.MealType
import com.senior.kbju.utils.Nutrients
import com.senior.kbju.utils.NutritionMath
import com.senior.kbju.utils.NutritionOcrParser
import com.senior.kbju.utils.parseNumber
import com.senior.kbju.utils.pretty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

enum class MeasureUnit(val label: String) {
    GRAMS("Граммы"),
    GLASS("Стакан"),
    SPOON("Ложка")
}

// Сколько граммов продукта в одной единице измерения. Стакан — 250 мл, ложка — столовая.
fun FoodEntity.gramsPerUnit(unit: MeasureUnit): Double? = when (unit) {
    MeasureUnit.GRAMS -> 1.0
    MeasureUnit.GLASS -> gramsPerGlass
    MeasureUnit.SPOON -> gramsPerSpoon
}

data class IngredientRow(
    val food: FoodEntity,
    val amountText: String = "100",
    val unit: MeasureUnit = MeasureUnit.GRAMS
) {
    val grams: Double
        get() = (parseNumber(amountText) ?: 0.0) * (food.gramsPerUnit(unit) ?: 1.0)
}

sealed interface AddFoodStep {
    data object Catalog : AddFoodStep
    data class QuickPortion(val food: FoodEntity) : AddFoodStep
    data object Label : AddFoodStep
    data object RecipeBuilder : AddFoodStep
    data object RecipePortion : AddFoodStep
    data object Done : AddFoodStep
}

private fun defaultMealType(): MealType {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 5..11 -> MealType.BREAKFAST
        in 12..16 -> MealType.LUNCH
        in 17..21 -> MealType.DINNER
        else -> MealType.SNACK
    }
}

enum class FoodCatalogTab(val label: String) {
    SIMPLE("🍎 Простые продукты"),
    RECIPES("📖 Мои рецепты")
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AddFoodViewModel(app: Application) : AndroidViewModel(app) {

    private val foodRepository = (app as KbjuApp).foodRepository
    private val diaryRepository = (app as KbjuApp).diaryRepository

    val step = MutableStateFlow<AddFoodStep>(AddFoodStep.Catalog)
    val selectedMealType = MutableStateFlow(defaultMealType())
    val catalogTab = MutableStateFlow(FoodCatalogTab.SIMPLE)

    // ----- Поиск по каталогу -----
    val searchQuery = MutableStateFlow("")

    val searchResults: StateFlow<List<FoodEntity>> = searchQuery
        .debounce(200)
        .flatMapLatest { query -> foodRepository.search(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val simpleFoods: StateFlow<List<FoodEntity>> = searchQuery
        .debounce(150)
        .flatMapLatest { query -> foodRepository.searchSimple(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userRecipes: StateFlow<List<FoodEntity>> = searchQuery
        .debounce(150)
        .flatMapLatest { query -> foodRepository.searchUserRecipes(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ----- Быстрая запись отдельного продукта -----
    val quickFood = MutableStateFlow<FoodEntity?>(null)
    val quickAmount = MutableStateFlow("100")
    val quickUnit = MutableStateFlow(MeasureUnit.GRAMS)
    val quickError = MutableStateFlow<String?>(null)

    // ----- Режим «с этикетки» -----
    val labelName = MutableStateFlow("")
    val labelKcal = MutableStateFlow("")
    val labelProtein = MutableStateFlow("")
    val labelFat = MutableStateFlow("")
    val labelCarbs = MutableStateFlow("")
    val labelGrams = MutableStateFlow("")
    val labelError = MutableStateFlow<String?>(null)
    val ocrStatus = MutableStateFlow<String?>(null)
    val isOcrProcessing = MutableStateFlow(false)

    fun onOcrTextRecognized(rawText: String) {
        val parsed = NutritionOcrParser.parse(rawText)
        if (parsed.hasAnyData) {
            parsed.kcal?.let { labelKcal.value = it.pretty() }
            parsed.protein?.let { labelProtein.value = it.pretty() }
            parsed.fat?.let { labelFat.value = it.pretty() }
            parsed.carbs?.let { labelCarbs.value = it.pretty() }
            val foundList = mutableListOf<String>()
            if (parsed.kcal != null) foundList.add("калории")
            if (parsed.protein != null) foundList.add("белки")
            if (parsed.fat != null) foundList.add("жиры")
            if (parsed.carbs != null) foundList.add("углеводы")
            ocrStatus.value = "Найдено с этикетки: ${foundList.joinToString(", ")}. Проверьте значения ниже."
        } else {
            ocrStatus.value = "КБЖУ не найдены на фото. Сфотографируйте ближе или введите вручную."
        }
    }

    fun onOcrFailure(message: String) {
        ocrStatus.value = message
    }

    // ----- Режим «сложное блюдо из продуктов» -----
    val ingredients = MutableStateFlow<List<IngredientRow>>(emptyList())
    val recipeError = MutableStateFlow<String?>(null)

    val totalGrams: StateFlow<Double> = ingredients
        .map { rows -> rows.sumOf { it.grams } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalKcal: StateFlow<Double> = ingredients
        .map { rows -> rows.sumOf { it.grams * it.food.kcal100 / 100.0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val dishPer100 = MutableStateFlow<Nutrients?>(null)
    val totalWeight = MutableStateFlow(0.0)
    val dishName = MutableStateFlow("")
    val portionText = MutableStateFlow("")
    val portionError = MutableStateFlow<String?>(null)

    val savedMessage = MutableStateFlow<String?>(null)

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun selectQuickFood(food: FoodEntity) {
        quickFood.value = food
        quickAmount.value = "100"
        quickUnit.value = MeasureUnit.GRAMS
        quickError.value = null
        step.value = AddFoodStep.QuickPortion(food)
    }

    fun quickPortionPreview(): Nutrients? {
        val food = quickFood.value ?: return null
        val amount = parseNumber(quickAmount.value) ?: return null
        if (amount <= 0.0) return null
        val grams = amount * (food.gramsPerUnit(quickUnit.value) ?: 1.0)
        return NutritionMath.portion(
            Nutrients(food.kcal100, food.protein100, food.fat100, food.carbs100),
            grams
        )
    }

    fun saveQuickFood() {
        val food = quickFood.value ?: return
        val amount = parseNumber(quickAmount.value)
        val grams = if (amount != null && amount > 0.0) {
            amount * (food.gramsPerUnit(quickUnit.value) ?: 1.0)
        } else null

        if (grams == null || grams <= 0.0) {
            quickError.value = "Введите число больше нуля"
            return
        }
        quickError.value = null

        val nutrition = NutritionMath.portion(
            Nutrients(food.kcal100, food.protein100, food.fat100, food.carbs100),
            grams
        )
        viewModelScope.launch {
            diaryRepository.addMeal(
                MealLogEntity(
                    day = LocalDate.now().toEpochDay(),
                    name = food.name,
                    grams = grams,
                    kcal = nutrition.kcal,
                    protein = nutrition.protein,
                    fat = nutrition.fat,
                    carbs = nutrition.carbs,
                    mealType = selectedMealType.value.name
                )
            )
            savedMessage.value = "«${food.name}» (${selectedMealType.value.label}) — ${nutrition.kcal.roundToInt()} ккал. Записали!"
            step.value = AddFoodStep.Done
        }
    }

    fun chooseLabelMode() {
        step.value = AddFoodStep.Label
    }

    fun chooseRecipeMode() {
        clearRecipeBuilder()
        step.value = AddFoodStep.RecipeBuilder
    }

    fun clearRecipeBuilder() {
        ingredients.value = emptyList()
        dishName.value = ""
        dishPer100.value = null
        totalWeight.value = 0.0
        portionText.value = ""
        recipeError.value = null
    }

    fun deleteUserRecipe(food: FoodEntity) {
        viewModelScope.launch {
            foodRepository.deleteUserFood(food.id)
        }
    }

    fun backToCatalog() {
        step.value = AddFoodStep.Catalog
    }

    fun addIngredient(food: FoodEntity) {
        if (ingredients.value.none { it.food.id == food.id }) {
            ingredients.value = ingredients.value + IngredientRow(food)
        }
    }

    fun updateIngredientAmount(index: Int, amount: String) {
        ingredients.value = ingredients.value.mapIndexed { i, row ->
            if (i == index) row.copy(amountText = amount) else row
        }
    }

    fun updateIngredientUnit(index: Int, unit: MeasureUnit) {
        ingredients.value = ingredients.value.mapIndexed { i, row ->
            if (i == index) row.copy(unit = unit) else row
        }
    }

    fun removeIngredient(index: Int) {
        ingredients.value = ingredients.value.filterIndexed { i, _ -> i != index }
    }

    fun goToPortion() {
        val rows = ingredients.value
        val grams = rows.sumOf { it.grams }
        if (rows.isEmpty() || grams < 1.0) {
            recipeError.value = "Добавьте продукты и напишите, сколько грамм каждого вы взяли"
            return
        }
        recipeError.value = null
        val totals = Nutrients(
            kcal = rows.sumOf { it.grams * it.food.kcal100 / 100.0 },
            protein = rows.sumOf { it.grams * it.food.protein100 / 100.0 },
            fat = rows.sumOf { it.grams * it.food.fat100 / 100.0 },
            carbs = rows.sumOf { it.grams * it.food.carbs100 / 100.0 }
        )
        totalWeight.value = grams
        dishPer100.value = NutritionMath.per100(grams, totals)
        dishName.value = buildDishName(rows)
        portionText.value = ""
        portionError.value = null
        step.value = AddFoodStep.RecipePortion
    }

    private fun buildDishName(rows: List<IngredientRow>): String {
        val names = rows.take(2).map { it.food.name.substringBefore('(').trim() }
        return if (names.size == 2) {
            "${names[0]} с ${names[1].replaceFirstChar { it.lowercase() }}"
        } else {
            names[0]
        }
    }

    fun labelPortionPreview(): Nutrients? {
        val kcal = parseNumber(labelKcal.value) ?: return null
        if (kcal < 0.0) return null
        val grams = parseNumber(labelGrams.value) ?: return null
        if (grams <= 0.0) return null
        val protein = parseNumber(labelProtein.value) ?: 0.0
        val fat = parseNumber(labelFat.value) ?: 0.0
        val carbs = parseNumber(labelCarbs.value) ?: 0.0
        return NutritionMath.portion(Nutrients(kcal, protein, fat, carbs), grams)
    }

    fun savePortion() {
        val per = dishPer100.value ?: return
        val grams = parseNumber(portionText.value)
        val name = dishName.value.trim()

        portionError.value = when {
            name.isEmpty() -> "Придумайте блюду название — например, «Моя каша»"
            grams == null || grams <= 0.0 -> "Напишите число — сколько грамм вы съели"
            else -> null
        }
        val gramsValue = grams ?: return
        if (name.isEmpty() || gramsValue <= 0.0) return

        val nutrition = NutritionMath.portion(per, gramsValue)
        viewModelScope.launch {
            if (foodRepository.findByName(name) == null) {
                foodRepository.insert(
                    FoodEntity(
                        name = name,
                        kcal100 = per.kcal,
                        protein100 = per.protein,
                        fat100 = per.fat,
                        carbs100 = per.carbs,
                        category = "Мои рецепты",
                        isUserCreated = true
                    )
                )
            }
            diaryRepository.addMeal(
                MealLogEntity(
                    day = LocalDate.now().toEpochDay(),
                    name = name,
                    grams = gramsValue,
                    kcal = nutrition.kcal,
                    protein = nutrition.protein,
                    fat = nutrition.fat,
                    carbs = nutrition.carbs,
                    mealType = selectedMealType.value.name
                )
            )
            savedMessage.value =
                "«$name» (${selectedMealType.value.label}) — ${nutrition.kcal.roundToInt()} ккал. Записали! Блюдо сохранено в вашей базе."
            step.value = AddFoodStep.Done
        }
    }

    fun saveLabel() {
        viewModelScope.launch {
            val name = labelName.value.trim()
            val kcal = parseNumber(labelKcal.value)
            val protein = parseNumber(labelProtein.value) ?: 0.0
            val fat = parseNumber(labelFat.value) ?: 0.0
            val carbs = parseNumber(labelCarbs.value) ?: 0.0
            val grams = parseNumber(labelGrams.value)

            labelError.value = when {
                name.isEmpty() -> "Напишите название продукта"
                kcal == null || kcal < 0.0 -> "Введите калории с этикетки числом"
                grams == null || grams <= 0.0 -> "Напишите, сколько грамм вы съели"
                else -> null
            }
            val kcalValue = kcal ?: return@launch
            val gramsValue = grams ?: return@launch
            if (name.isEmpty() || kcalValue < 0.0 || gramsValue <= 0.0) return@launch

            if (foodRepository.findByName(name) == null) {
                foodRepository.insert(
                    FoodEntity(
                        name = name,
                        kcal100 = kcalValue,
                        protein100 = protein,
                        fat100 = fat,
                        carbs100 = carbs,
                        category = "С этикетки",
                        isUserCreated = true
                    )
                )
            }
            val nutrition = NutritionMath.portion(
                Nutrients(kcalValue, protein, fat, carbs),
                gramsValue
            )
            diaryRepository.addMeal(
                MealLogEntity(
                    day = LocalDate.now().toEpochDay(),
                    name = name,
                    grams = gramsValue,
                    kcal = nutrition.kcal,
                    protein = nutrition.protein,
                    fat = nutrition.fat,
                    carbs = nutrition.carbs,
                    mealType = selectedMealType.value.name
                )
            )
            savedMessage.value = "«$name» (${selectedMealType.value.label}) — ${nutrition.kcal.roundToInt()} ккал. Записали!"
            step.value = AddFoodStep.Done
        }
    }
}
