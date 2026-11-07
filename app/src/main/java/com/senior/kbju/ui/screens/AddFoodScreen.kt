package com.senior.kbju.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.senior.kbju.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.data.local.entity.FoodEntity
import com.senior.kbju.data.model.MealType
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.BigToggleButton
import com.senior.kbju.ui.components.ErrorText
import com.senior.kbju.ui.components.NumberField
import com.senior.kbju.ui.components.OutlineBigButton
import com.senior.kbju.ui.components.PlainTextField
import com.senior.kbju.ui.components.SectionTitle
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentGreen
import com.senior.kbju.ui.theme.ErrorRed
import com.senior.kbju.ui.theme.SoftGray
import com.senior.kbju.utils.NutritionOcrParser
import com.senior.kbju.utils.pretty
import com.senior.kbju.viewmodel.AddFoodStep
import com.senior.kbju.viewmodel.AddFoodViewModel
import com.senior.kbju.viewmodel.IngredientRow
import com.senior.kbju.viewmodel.MeasureUnit
import com.senior.kbju.viewmodel.gramsPerUnit
import kotlin.math.roundToInt

@Composable
fun AddFoodScreen(onFinished: () -> Unit, viewModel: AddFoodViewModel = viewModel()) {
    val step by viewModel.step.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (val currentStep = step) {
            AddFoodStep.Catalog -> CatalogContent(viewModel)
            is AddFoodStep.QuickPortion -> QuickPortionContent(viewModel, currentStep.food)
            AddFoodStep.Label -> LabelContent(viewModel)
            AddFoodStep.RecipeBuilder -> RecipeBuilderContent(viewModel)
            AddFoodStep.RecipePortion -> RecipePortionContent(viewModel)
            AddFoodStep.Done -> DoneContent(viewModel, onFinished)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MealTypeSelector(viewModel: AddFoodViewModel) {
    val currentType by viewModel.selectedMealType.collectAsState()
    SectionTitle(text = "Приём пищи")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MealType.entries.forEach { type ->
            BigToggleButton(
                text = type.label,
                selected = currentType == type,
                onClick = { viewModel.selectedMealType.value = type },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CatalogContent(viewModel: AddFoodViewModel) {
    val currentTab by viewModel.catalogTab.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val simpleFoods by viewModel.simpleFoods.collectAsState()
    val userRecipes by viewModel.userRecipes.collectAsState()

    // Лаунчер для распознавания речи (офлайн/онлайн системный ввод Android)
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.onSearchQueryChange(spokenText)
            }
        }
    }

    Text(text = "Добавить еду", fontSize = 30.sp, fontWeight = FontWeight.Bold)

    // Выбор приёма пищи (Завтрак / Обед / Ужин / Перекус)
    MealTypeSelector(viewModel)

    // Переключение между простыми продуктами и своими рецептами
    SectionTitle(text = "Что вы хотите добавить?")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        com.senior.kbju.viewmodel.FoodCatalogTab.entries.forEach { tab ->
            BigToggleButton(
                text = tab.label,
                selected = currentTab == tab,
                onClick = { viewModel.catalogTab.value = tab },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (currentTab == com.senior.kbju.viewmodel.FoodCatalogTab.SIMPLE) {
        // Кнопки альтернативных способов ввода сразу в поле зрения
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BigButton(
                text = "📷 С ЭТИКЕТКИ",
                onClick = viewModel::chooseLabelMode,
                color = AccentBlue,
                modifier = Modifier.weight(1f)
            )
            OutlineBigButton(
                text = "🍳 СВОЁ БЛЮДО",
                onClick = viewModel::chooseRecipeMode,
                modifier = Modifier.weight(1f)
            )
        }

        SectionTitle(text = "Поиск в базе простых продуктов")
        PlainTextField(
            label = "Название продукта",
            value = query,
            onValueChange = viewModel::onSearchQueryChange
        )

        OutlineBigButton(
            text = "🎤 СКАЗАТЬ ГОЛОСОМ",
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Назовите продукт (например «Гречка» или «Яблоко»)")
                }
                try {
                    voiceLauncher.launch(intent)
                } catch (_: Exception) {}
            }
        )

        // Показываем список только когда пользователь что-то ищет, чтобы экран не забивался длинным списком
        if (query.isNotBlank()) {
            Text(
                text = "Результаты поиска (нажмите для записи):",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            val displayedFoods = simpleFoods.take(10)
            Column(modifier = Modifier.fillMaxWidth()) {
                if (simpleFoods.isEmpty()) {
                    Text(
                        text = "Ничего не нашлось. Попробуйте сказать иначе или сфотографировать этикетку выше.",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    displayedFoods.forEach { food ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectQuickFood(food) }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = food.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "на 100 г: ${food.kcal100.roundToInt()} ккал • Б ${food.protein100.pretty()} • Ж ${food.fat100.pretty()} • У ${food.carbs100.pretty()}" +
                                    (food.category?.let { " • $it" } ?: ""),
                                fontSize = 18.sp,
                                color = Color.DarkGray
                            )
                            HorizontalDivider(thickness = 1.dp, color = Color(0xFFDDDDDD))
                        }
                    }
                }
            }
        }
    } else {
        // Раздел «Мои рецепты»
        BigButton(
            text = "🍳 СОБРАТЬ НОВОЕ БЛЮДО",
            onClick = viewModel::chooseRecipeMode,
            color = AccentGreen
        )

        SectionTitle(text = "Мои рецепты и блюда")
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "🔒 Ваши рецепты и блюда сохраняются только на этом устройстве и не передаются другим пользователям.",
                fontSize = 16.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(12.dp)
            )
        }

        PlainTextField(
            label = "Поиск среди моих рецептов",
            value = query,
            onValueChange = viewModel::onSearchQueryChange
        )

        if (userRecipes.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "У вас пока нет сохранённых рецептов.",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Вы можете собрать сложное блюдо из нескольких ингредиентов (кнопка выше), и оно сохранится здесь для быстрого выбора в будущем.",
                        fontSize = 17.sp,
                        color = Color.DarkGray
                    )
                }
            }
        } else {
            Text(text = "Нажмите на рецепт, чтобы записать съеденную порцию:", fontSize = 18.sp)
            Column(modifier = Modifier.fillMaxWidth()) {
                val displayedRecipes = if (query.isBlank()) userRecipes.take(8) else userRecipes.take(15)
                displayedRecipes.forEach { recipe ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectQuickFood(recipe) }
                        ) {
                            Text(
                                text = recipe.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "на 100 г: ${recipe.kcal100.roundToInt()} ккал • Б ${recipe.protein100.pretty()} • Ж ${recipe.fat100.pretty()} • У ${recipe.carbs100.pretty()}",
                                fontSize = 17.sp,
                                color = Color.DarkGray
                            )
                        }
                        TextButton(
                            onClick = { viewModel.deleteUserRecipe(recipe) }
                        ) {
                            Text(text = "🗑️ Удалить", fontSize = 16.sp, color = ErrorRed)
                        }
                    }
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFDDDDDD))
                }
            }
        }
    }
}

@Composable
private fun QuickPortionContent(viewModel: AddFoodViewModel, food: FoodEntity) {
    val amount by viewModel.quickAmount.collectAsState()
    val unit by viewModel.quickUnit.collectAsState()
    val error by viewModel.quickError.collectAsState()
    val preview = viewModel.quickPortionPreview()
    val haptic = LocalHapticFeedback.current

    Text(text = food.name, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Text(
        text = "На 100 грамм: ${food.kcal100.roundToInt()} ккал • Б ${food.protein100.pretty()} • Ж ${food.fat100.pretty()} • У ${food.carbs100.pretty()}",
        fontSize = 18.sp,
        color = Color.DarkGray
    )

    OutlineBigButton(text = "НАЗАД К ПОИСКУ", onClick = viewModel::backToCatalog)

    MealTypeSelector(viewModel)

    SectionTitle(text = "Сколько вы съели?")
    NumberField(
        label = "Количество",
        value = amount,
        onValueChange = { viewModel.quickAmount.value = it },
        suffix = unit.label.lowercase()
    )

    // Кнопки быстрого выбора порций
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("100", "150", "200").forEach { preset ->
            BigToggleButton(
                text = "$preset г",
                selected = amount == preset && unit == MeasureUnit.GRAMS,
                onClick = {
                    viewModel.quickAmount.value = preset
                    viewModel.quickUnit.value = MeasureUnit.GRAMS
                },
                modifier = Modifier.weight(1f)
            )
        }
        if (food.gramsPerGlass != null) {
            BigToggleButton(
                text = "1 стакан",
                selected = amount == "1" && unit == MeasureUnit.GLASS,
                onClick = {
                    viewModel.quickAmount.value = "1"
                    viewModel.quickUnit.value = MeasureUnit.GLASS
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (preview != null) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "В вашей порции:", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${preview.kcal.roundToInt()} ккал • Б ${preview.protein.pretty()} г • Ж ${preview.fat.pretty()} г • У ${preview.carbs.pretty()} г",
                    fontSize = 20.sp
                )
            }
        }
    }

    ErrorText(error)
    BigButton(
        text = "ЗАПИСАТЬ В ДНЕВНИК",
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.saveQuickFood()
        },
        color = AccentGreen
    )
}

@Composable
private fun LabelContent(viewModel: AddFoodViewModel) {
    val context = LocalContext.current
    val name by viewModel.labelName.collectAsState()
    val kcal by viewModel.labelKcal.collectAsState()
    val protein by viewModel.labelProtein.collectAsState()
    val fat by viewModel.labelFat.collectAsState()
    val carbs by viewModel.labelCarbs.collectAsState()
    val grams by viewModel.labelGrams.collectAsState()
    val error by viewModel.labelError.collectAsState()
    val ocrStatus by viewModel.ocrStatus.collectAsState()
    var isProcessingOcr by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Стабильный файл для снимка с камеры (перезаписывается при каждом фото)
    val photoFile = remember { File(context.cacheDir, "camera_label_photo.jpg") }
    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    fun processOcrImage(uri: Uri) {
        isProcessingOcr = true
        scope.launch(Dispatchers.Default) {
            try {
                // Масштабируем фото с высоким разрешением и исправляем поворот по EXIF
                val scaledBitmap = ImageUtils.loadRotatedScaledBitmap(context, uri)
                val image = if (scaledBitmap != null) {
                    InputImage.fromBitmap(scaledBitmap, 0)
                } else {
                    InputImage.fromFilePath(context, uri)
                }

                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val parsed = NutritionOcrParser.parse(visionText.text)
                        if (parsed.hasAnyData || scaledBitmap == null) {
                            isProcessingOcr = false
                            viewModel.onOcrTextRecognized(visionText.text)
                        } else {
                            // Если КБЖУ не найдены при текущей ориентации, проверяем поворот на 90 градусов (для фото боком)
                            val rotated90 = ImageUtils.rotateBitmap(scaledBitmap, 90f)
                            recognizer.process(InputImage.fromBitmap(rotated90, 0))
                                .addOnSuccessListener { r90Text ->
                                    val r90Parsed = NutritionOcrParser.parse(r90Text.text)
                                    if (r90Parsed.hasAnyData) {
                                        isProcessingOcr = false
                                        viewModel.onOcrTextRecognized(r90Text.text)
                                    } else {
                                        // Пробуем 270 градусов
                                        val rotated270 = ImageUtils.rotateBitmap(scaledBitmap, 270f)
                                        recognizer.process(InputImage.fromBitmap(rotated270, 0))
                                            .addOnSuccessListener { r270Text ->
                                                isProcessingOcr = false
                                                val r270Parsed = NutritionOcrParser.parse(r270Text.text)
                                                if (r270Parsed.hasAnyData) {
                                                    viewModel.onOcrTextRecognized(r270Text.text)
                                                } else {
                                                    viewModel.onOcrTextRecognized(visionText.text)
                                                }
                                            }
                                            .addOnFailureListener {
                                                isProcessingOcr = false
                                                viewModel.onOcrTextRecognized(visionText.text)
                                            }
                                    }
                                }
                                .addOnFailureListener {
                                    isProcessingOcr = false
                                    viewModel.onOcrTextRecognized(visionText.text)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        isProcessingOcr = false
                        viewModel.onOcrFailure("Не удалось прочитать текст: ${e.localizedMessage ?: "ошибка фото"}")
                    }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isProcessingOcr = false
                    viewModel.onOcrFailure("Не удалось открыть изображение")
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            processOcrImage(photoUri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { processOcrImage(it) }
    }

    Text(text = "Продукт с этикетки", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    OutlineBigButton(text = "НАЗАД", onClick = viewModel::backToCatalog)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SoftGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "📷 Автоматическое чтение этикетки",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Сфотографируйте таблицу «Пищевая ценность» с пачки — поля заполнятся автоматически.",
                fontSize = 17.sp,
                color = Color.DarkGray
            )

            if (isProcessingOcr) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(28.dp), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(text = "Распознаём этикетку...", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                BigButton(
                    text = "📷 СФОТОГРАФИРОВАТЬ",
                    onClick = {
                        try {
                            if (photoFile.exists()) photoFile.delete()
                            photoFile.createNewFile()
                            cameraLauncher.launch(photoUri)
                        } catch (e: Exception) {
                            viewModel.onOcrFailure("Камера недоступна: ${e.localizedMessage}")
                        }
                    },
                    color = AccentBlue
                )
                OutlineBigButton(
                    text = "🖼️ ВЫБРАТЬ ИЗ ГАЛЕРЕИ",
                    onClick = {
                        try {
                            galleryLauncher.launch("image/*")
                        } catch (_: Exception) {}
                    }
                )
            }

            ocrStatus?.let { status ->
                Text(
                    text = status,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (status.startsWith("Найдено")) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    MealTypeSelector(viewModel)

    PlainTextField(
        label = "Название продукта",
        value = name,
        onValueChange = { viewModel.labelName.value = it }
    )
    Text(text = "Все числа — как написано на 100 грамм продукта:", fontSize = 18.sp)
    NumberField(
        label = "Калории (ккал)",
        value = kcal,
        onValueChange = { viewModel.labelKcal.value = it },
        suffix = "на 100 г"
    )
    NumberField(
        label = "Белки",
        value = protein,
        onValueChange = { viewModel.labelProtein.value = it },
        suffix = "г на 100 г"
    )
    NumberField(
        label = "Жиры",
        value = fat,
        onValueChange = { viewModel.labelFat.value = it },
        suffix = "г на 100 г"
    )
    NumberField(
        label = "Углеводы",
        value = carbs,
        onValueChange = { viewModel.labelCarbs.value = it },
        suffix = "г на 100 г"
    )
    NumberField(
        label = "Сколько грамм вы съели",
        value = grams,
        onValueChange = { viewModel.labelGrams.value = it },
        suffix = "граммы"
    )
    val preview = viewModel.labelPortionPreview()
    if (preview != null) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "В вашей порции:", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${preview.kcal.roundToInt()} ккал • Белки ${preview.protein.pretty()} г • Жиры ${preview.fat.pretty()} г • Углеводы ${preview.carbs.pretty()} г",
                    fontSize = 20.sp
                )
            }
        }
    }
    ErrorText(error)
    BigButton(
        text = "ЗАПИСАТЬ В ДНЕВНИК",
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.saveLabel()
        },
        color = AccentGreen
    )
}

@Composable
private fun RecipeBuilderContent(viewModel: AddFoodViewModel) {
    val query by viewModel.searchQuery.collectAsState()
    val simpleFoods by viewModel.simpleFoods.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val totalGrams by viewModel.totalGrams.collectAsState()
    val totalKcal by viewModel.totalKcal.collectAsState()
    val error by viewModel.recipeError.collectAsState()

    Text(text = "Соберём сложное блюдо", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlineBigButton(
            text = "НАЗАД В КАТАЛОГ",
            onClick = viewModel::backToCatalog,
            modifier = Modifier.weight(1f)
        )
        if (ingredients.isNotEmpty()) {
            OutlineBigButton(
                text = "ОЧИСТИТЬ",
                onClick = viewModel::clearRecipeBuilder,
                modifier = Modifier.weight(1f)
            )
        }
    }

    PlainTextField(
        label = "Найдите ингредиент",
        value = query,
        onValueChange = viewModel::onSearchQueryChange
    )

    // Показываем ингредиенты только при поиске, чтобы не сдвигать вниз список уже добавленных продуктов
    if (query.isNotBlank()) {
        Text(
            text = "Результаты поиска (нажмите для добавления):",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        val candidateFoods = simpleFoods.take(8)
        Column(modifier = Modifier.fillMaxWidth()) {
            if (simpleFoods.isEmpty()) {
                Text(
                    text = "Ничего не нашлось. Попробуйте другое слово.",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                candidateFoods.forEach { food ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.addIngredient(food) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(text = "➕ ${food.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "на 100 г: ${food.kcal100.roundToInt()} ккал • Б ${food.protein100.pretty()} • Ж ${food.fat100.pretty()} • У ${food.carbs100.pretty()}",
                            fontSize = 17.sp,
                            color = Color.DarkGray
                        )
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFDDDDDD))
                    }
                }
            }
        }
    }

    SectionTitle(text = "Ингредиенты блюда")
    if (ingredients.isEmpty()) {
        Text(text = "Пока пусто. Выберите продукты выше.", fontSize = 18.sp)
    }
    ingredients.forEachIndexed { index, row ->
        IngredientCard(index = index, row = row, viewModel = viewModel)
    }

    if (ingredients.isNotEmpty()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = "Всего: ${totalGrams.pretty()} г",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Калории всего блюда: ${totalKcal.roundToInt()} ккал",
                    fontSize = 20.sp
                )
            }
        }
        ErrorText(error)
        BigButton(
            text = "ГОТОВО: ВВЕСТИ СКОЛЬКО СЪЕЛИ",
            onClick = viewModel::goToPortion,
            color = AccentGreen
        )
    }
}

@Composable
private fun IngredientCard(index: Int, row: IngredientRow, viewModel: AddFoodViewModel) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SoftGray)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.food.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { viewModel.removeIngredient(index) }) {
                    Text(text = "Удалить", color = ErrorRed, fontSize = 18.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigToggleButton(
                    text = "Граммы",
                    selected = row.unit == MeasureUnit.GRAMS,
                    onClick = { viewModel.updateIngredientUnit(index, MeasureUnit.GRAMS) },
                    modifier = Modifier.weight(1f)
                )
                if (row.food.gramsPerGlass != null) {
                    BigToggleButton(
                        text = "Стакан",
                        selected = row.unit == MeasureUnit.GLASS,
                        onClick = { viewModel.updateIngredientUnit(index, MeasureUnit.GLASS) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.food.gramsPerSpoon != null) {
                    BigToggleButton(
                        text = "Ложка",
                        selected = row.unit == MeasureUnit.SPOON,
                        onClick = { viewModel.updateIngredientUnit(index, MeasureUnit.SPOON) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = row.amountText,
                onValueChange = { viewModel.updateIngredientAmount(index, it) },
                label = { Text(text = "Сколько (${row.unit.label.lowercase()})", fontSize = 18.sp) },
                textStyle = LocalTextStyle.current.copy(fontSize = 22.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Text(text = "= ${row.grams.pretty()} грамм", fontSize = 18.sp, color = Color.DarkGray)
        }
    }
}

@Composable
private fun RecipePortionContent(viewModel: AddFoodViewModel) {
    val dishName by viewModel.dishName.collectAsState()
    val portionText by viewModel.portionText.collectAsState()
    val totalWeight by viewModel.totalWeight.collectAsState()
    val per100 by viewModel.dishPer100.collectAsState()
    val error by viewModel.portionError.collectAsState()
    val haptic = LocalHapticFeedback.current

    Text(text = "Сколько вы съели?", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    OutlineBigButton(text = "НАЗАД", onClick = viewModel::chooseRecipeMode)

    MealTypeSelector(viewModel)

    PlainTextField(
        label = "Название блюда",
        value = dishName,
        onValueChange = { viewModel.dishName.value = it }
    )
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SoftGray)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "Вес всего приготовленного: ${totalWeight.pretty()} г", fontSize = 18.sp)
            per100?.let {
                Text(
                    text = "На 100 г блюда: ${it.kcal.roundToInt()} ккал • Б ${it.protein.pretty()} • Ж ${it.fat.pretty()} • У ${it.carbs.pretty()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    NumberField(
        label = "Вес вашей порции",
        value = portionText,
        onValueChange = { viewModel.portionText.value = it },
        suffix = "грамм"
    )

    ErrorText(error)
    BigButton(
        text = "ЗАПИСАТЬ В ДНЕВНИК",
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.savePortion()
        },
        color = AccentGreen
    )
}

@Composable
private fun DoneContent(viewModel: AddFoodViewModel, onFinished: () -> Unit) {
    val message by viewModel.savedMessage.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "✓", fontSize = 72.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
        Text(
            text = "Записано!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = message ?: "",
            fontSize = 22.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        BigButton(text = "НА ГЛАВНУЮ", onClick = onFinished, color = AccentGreen)
    }
}
