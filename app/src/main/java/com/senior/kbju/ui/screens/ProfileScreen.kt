package com.senior.kbju.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import com.senior.kbju.BuildConfig
import com.senior.kbju.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.data.model.ActivityLevel
import com.senior.kbju.data.model.Gender
import com.senior.kbju.data.model.Goal
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.BigToggleButton
import com.senior.kbju.ui.components.ErrorText
import com.senior.kbju.ui.components.ModernCard
import com.senior.kbju.ui.components.NumberField
import com.senior.kbju.ui.components.OutlineBigButton
import com.senior.kbju.ui.components.SectionTitle
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentBlueBorder
import com.senior.kbju.ui.theme.AccentBlueLight
import com.senior.kbju.ui.theme.AccentGreen
import com.senior.kbju.ui.theme.AccentGreenBorder
import com.senior.kbju.ui.theme.AccentGreenLight
import com.senior.kbju.ui.theme.CardBorder
import com.senior.kbju.ui.theme.ErrorRed
import com.senior.kbju.ui.theme.SoftGray
import com.senior.kbju.ui.theme.SurfaceWhite
import com.senior.kbju.ui.theme.TextDark
import com.senior.kbju.ui.theme.TextMuted
import com.senior.kbju.utils.pretty
import com.senior.kbju.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProfileScreen(onFinished: () -> Unit, viewModel: ProfileViewModel = viewModel()) {
    val height by viewModel.height.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val startWeight by viewModel.startWeight.collectAsState()
    val targetWeight by viewModel.targetWeight.collectAsState()
    val targetWaterGlasses by viewModel.targetWaterGlasses.collectAsState()
    val age by viewModel.age.collectAsState()
    val birthDateEpochDay by viewModel.birthDateEpochDay.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val activityLevel by viewModel.activityLevel.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val crashReport by viewModel.crashReport.collectAsState()
    val waterCalcExplanation by viewModel.waterCalcExplanation.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val birthDateText = remember(birthDateEpochDay) {
        birthDateEpochDay?.let {
            LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("d MMMM yyyy г.", Locale("ru")))
        }
    }

    fun showBirthDatePicker() {
        val initialDate = birthDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.of(1960, 1, 1)
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = LocalDate.of(year, month + 1, dayOfMonth)
                viewModel.setBirthDate(picked.toEpochDay())
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).show()
    }

    // После сохранения параметров в DataStore уходим на главный экран.
    LaunchedEffect(saved) {
        if (saved) onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        crashReport?.let { report ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "⚠️ Зафиксирован сбой приложения",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                    Text(
                        text = "Ранее приложение неожиданно закрылось. Вы можете отправить отчёт родным или разработчику, чтобы мы всё исправили.",
                        fontSize = 16.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BigButton(
                            text = "ОТПРАВИТЬ ОТЧЁТ",
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Сбой SeniorKbju")
                                    putExtra(Intent.EXTRA_TEXT, report)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Отправить отчёт о сбое"))
                            },
                            color = ErrorRed,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::clearCrashReport) {
                            Text(text = "Закрыть", fontSize = 16.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Дневник питания",
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .padding(vertical = 4.dp)
        )
        Text(
            text = if (isEditing) "Ваш профиль" else "Давайте познакомимся",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = "Мы посчитаем вашу норму калорий. Отвечайте как есть.", fontSize = 20.sp)

        NumberField(
            label = "Рост",
            value = height,
            onValueChange = { viewModel.height.value = it },
            suffix = "см"
        )
        NumberField(
            label = "Текущий вес",
            value = weight,
            onValueChange = viewModel::onWeightChanged,
            suffix = "кг"
        )
        NumberField(
            label = "Исходный вес (с какого начали)",
            value = startWeight,
            onValueChange = { viewModel.startWeight.value = it },
            suffix = "кг"
        )
        NumberField(
            label = "Желаемый вес (цель)",
            value = targetWeight,
            onValueChange = { viewModel.targetWeight.value = it },
            suffix = "кг"
        )

        // Подсказка прогресса и разницы до цели
        val curW = com.senior.kbju.utils.parseNumberIn(weight, 25.0, 300.0)
        val startW = com.senior.kbju.utils.parseNumberIn(startWeight, 25.0, 300.0)
        val tgtW = com.senior.kbju.utils.parseNumberIn(targetWeight, 25.0, 300.0)
        if (curW != null && tgtW != null) {
            val diff = curW - tgtW
            val dropped = if (startW != null) startW - curW else null

            ModernCard(
                shape = RoundedCornerShape(16.dp),
                backgroundColor = AccentGreenLight,
                borderColor = AccentGreenBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (dropped != null && kotlin.math.abs(dropped) >= 0.1) {
                        val dropMsg = if (dropped > 0) {
                            "🎉 Уже сброшено со старта: ${dropped.pretty()} кг"
                        } else {
                            "💪 Набрано со старта: ${(-dropped).pretty()} кг"
                        }
                        Text(
                            text = dropMsg,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }

                    val goalMsg = when {
                        kotlin.math.abs(diff) < 0.2 -> "🏆 Текущий вес совпадает с желаемым!"
                        diff > 0 -> "🎯 До цели осталось сбросить: ${diff.pretty()} кг"
                        else -> "🎯 До цели осталось набрать: ${(-diff).pretty()} кг"
                    }
                    Text(
                        text = goalMsg,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue
                    )
                }
            }
        }

        SectionTitle(text = "Дата рождения и возраст")
        ModernCard(
            shape = RoundedCornerShape(18.dp),
            backgroundColor = SurfaceWhite,
            borderColor = CardBorder,
            elevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (birthDateText != null) {
                    Text(
                        text = "🎂 Дата рождения: $birthDateText",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val parsedAge = age.toIntOrNull()
                    if (parsedAge != null && parsedAge > 0) {
                        Text(
                            text = "Ваш возраст: ${formatAgeString(parsedAge)} (обновляется автоматически)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentGreen
                        )
                    }
                } else {
                    Text(
                        text = "Укажите дату рождения — возраст в приложении будет обновляться автоматически каждый год.",
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                }

                BigButton(
                    text = if (birthDateText != null) "📅 ИЗМЕНИТЬ ДАТУ РОЖДЕНИЯ" else "📅 ВЫБРАТЬ ДАТУ РОЖДЕНИЯ",
                    onClick = { showBirthDatePicker() },
                    color = AccentBlue
                )
            }
        }

        SectionTitle(text = "Пол")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigToggleButton(
                text = "Мужской",
                selected = gender == Gender.MALE,
                onClick = { viewModel.gender.value = Gender.MALE },
                modifier = Modifier.weight(1f)
            )
            BigToggleButton(
                text = "Женский",
                selected = gender == Gender.FEMALE,
                onClick = { viewModel.gender.value = Gender.FEMALE },
                modifier = Modifier.weight(1f)
            )
        }

        SectionTitle(text = "Как вы проводите день?")
        ActivityLevel.entries.forEach { level ->
            BigToggleButton(
                text = level.label,
                selected = activityLevel == level,
                onClick = { viewModel.activityLevel.value = level }
            )
        }

        SectionTitle(text = "Какая у вас цель?")
        Goal.entries.forEach { goalItem ->
            BigToggleButton(
                text = goalItem.label,
                selected = goal == goalItem,
                onClick = { viewModel.selectGoal(goalItem) }
            )
        }

        SectionTitle(text = "Норма воды в день")
        ModernCard(
            shape = RoundedCornerShape(18.dp),
            backgroundColor = SurfaceWhite,
            borderColor = CardBorder,
            elevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "💧 Ваша норма: ${targetWaterGlasses * 250} мл ($targetWaterGlasses стаканов)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                )
                Text(
                    text = "Выберите стандартную норму или рассчитайте по весу (30 мл на 1 кг):",
                    fontSize = 16.sp,
                    color = TextMuted
                )

                // Готовые пресеты
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(4 to "1 л", 6 to "1.5 л", 8 to "2 л").forEach { (glasses, label) ->
                        BigToggleButton(
                            text = "$glasses ст.\n($label)",
                            selected = targetWaterGlasses == glasses,
                            onClick = { viewModel.setTargetWater(glasses) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Ручной ввод нормы в мл
                var customWaterInput by remember { mutableStateOf("") }
                NumberField(
                    label = "Ввести норму вручную (в миллилитрах)",
                    value = customWaterInput,
                    onValueChange = { customWaterInput = it },
                    suffix = "мл"
                )
                BigButton(
                    text = "СОХРАНИТЬ ВВЕДЁННУЮ НОРМУ",
                    onClick = {
                        val ml = customWaterInput.toIntOrNull()
                        if (ml != null && ml in 500..5000) {
                            viewModel.applyManualWaterMl(ml)
                            customWaterInput = ""
                        }
                    },
                    color = AccentBlue
                )

                // Кнопка рассчитать по весу
                BigButton(
                    text = "💧 РАССЧИТАТЬ ПО ВЕСУ (30 мл / кг)",
                    onClick = viewModel::calculateWaterByWeight,
                    color = AccentGreen
                )

                waterCalcExplanation?.let { text ->
                    Text(
                        text = text,
                        fontSize = 16.sp,
                        color = AccentBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Живой предпросмотр: норма пересчитывается при каждом изменении полей.
        val preview = viewModel.calculateDailyTargets()
        if (preview != null) {
            ModernCard(
                shape = RoundedCornerShape(18.dp),
                backgroundColor = AccentGreenLight,
                borderColor = AccentGreenBorder,
                elevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "⚡ Ваш базовый метаболизм: ${preview.bmr} ккал",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue
                    )
                    Text(
                        text = "Суточная норма: ${preview.kcal} ккал в день",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentGreen
                    )
                    Text(
                        text = "Белки: ${preview.protein} г • Жиры: ${preview.fat} г • Углеводы: ${preview.carbs} г",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark
                    )
                }
            }
        }

        ErrorText(errorMessage)
        BigButton(text = "РАССЧИТАТЬ И СОХРАНИТЬ", onClick = viewModel::save)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun formatAgeString(years: Int): String {
    val mod10 = years % 10
    val mod100 = years % 100
    val word = when {
        mod100 in 11..19 -> "лет"
        mod10 == 1 -> "год"
        mod10 in 2..4 -> "года"
        else -> "лет"
    }
    return "$years $word"
}
