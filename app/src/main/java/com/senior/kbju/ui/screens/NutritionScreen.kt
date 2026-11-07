package com.senior.kbju.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentBlueLight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.data.local.entity.MealLogEntity
import com.senior.kbju.data.model.MealType
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.ModernCard
import com.senior.kbju.ui.components.SectionTitle
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
import com.senior.kbju.viewmodel.DashboardViewModel
import kotlin.math.roundToInt

@Composable
fun NutritionScreen(
    onAddFood: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Состояние раскрытого приёма пищи (аккордеон: только один раскрыт)
    var expandedMealType by remember { mutableStateOf<MealType?>(null) }

    if (state.isLoading || state.profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(56.dp), color = AccentGreen)
        }
        return
    }

    val localDate = remember(state.dateEpochDay) { LocalDate.ofEpochDay(state.dateEpochDay) }
    val dateText = remember(localDate) {
        localDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")))
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    fun showDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = LocalDate.of(year, month + 1, dayOfMonth)
                viewModel.selectDate(picked.toEpochDay())
            },
            localDate.year,
            localDate.monthValue - 1,
            localDate.dayOfMonth
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🍽️ Питание и КБЖУ",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGreen
        )

        // Переключатель дней для просмотра рациона
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
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.shiftDay(-1) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(SoftGray, RoundedCornerShape(12.dp))
                    ) {
                        Text("◀", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }

                    Row(
                        modifier = Modifier
                            .clickable { showDatePicker() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(AccentBlueLight, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📅", fontSize = 20.sp)
                        }
                        Column {
                            val today = remember { LocalDate.now() }
                            val label = when (localDate) {
                                today -> "Сегодня"
                                today.minusDays(1) -> "Вчера"
                                today.plusDays(1) -> "Завтра"
                                else -> dateText
                            }
                            Text(
                                text = "День: $label",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = if (state.isToday) "$dateText (нажмите для смены)" else "нажмите для календаря",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.shiftDay(1) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(SoftGray, RoundedCornerShape(12.dp))
                    ) {
                        Text("▶", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }
                }

                if (!state.isToday) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { viewModel.selectToday() }) {
                        Text(
                            text = "ВЕРНУТЬСЯ НА СЕГОДНЯ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                    }
                }
            }
        }

        BigButton(
            text = "➕ ДОБАВИТЬ ЕДУ В ДНЕВНИК",
            onClick = onAddFood,
            color = AccentGreen
        )

        SectionTitle(text = "Баланс питательных веществ")
        ModernCard(
            shape = RoundedCornerShape(20.dp),
            backgroundColor = SurfaceWhite,
            borderColor = CardBorder,
            elevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MacroNutritionRow(name = "Белки", eaten = state.protein, target = state.targetProtein)
                MacroNutritionRow(name = "Жиры", eaten = state.fat, target = state.targetFat)
                MacroNutritionRow(name = "Углеводы", eaten = state.carbs, target = state.targetCarbs)
            }
        }

        SectionTitle(text = "Приёмы пищи за день")
        if (state.meals.isEmpty()) {
            ModernCard(
                shape = RoundedCornerShape(18.dp),
                backgroundColor = SurfaceWhite,
                borderColor = CardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Пока нет записей о еде за этот день. Нажмите зелёную кнопку выше, чтобы добавить приём пищи.",
                    fontSize = 17.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(18.dp)
                )
            }
        } else {
            MealType.entries.forEach { type ->
                val list = state.mealsByGroup[type] ?: emptyList()
                if (list.isNotEmpty()) {
                    val groupKcal = list.sumOf { it.kcal }.roundToInt()
                    val isExpanded = expandedMealType == type

                    ModernCard(
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = if (isExpanded) AccentGreenLight else SurfaceWhite,
                        borderColor = if (isExpanded) AccentGreenBorder else CardBorder,
                        elevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                expandedMealType = if (isExpanded) null else type
                            }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val icon = when (type) {
                                        MealType.BREAKFAST -> "🍳"
                                        MealType.LUNCH -> "🍲"
                                        MealType.DINNER -> "🥗"
                                        MealType.SNACK -> "🍎"
                                    }
                                    Text(text = icon, fontSize = 26.sp)
                                    Column {
                                        Text(
                                            text = type.label,
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                        Text(
                                            text = "$groupKcal ккал • ${list.size} ${dishesWord(list.size)}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isExpanded) AccentGreen else TextMuted
                                        )
                                    }
                                }

                                // Крупная аккуратная кнопка-стрелочка
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(
                                            if (isExpanded) AccentGreen else SoftGray,
                                            RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isExpanded) "▲" else "▼",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpanded) Color.White else TextDark
                                    )
                                }
                            }

                            // Раскрывающийся список блюд конкретного приёма пищи
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = if (isExpanded) AccentGreenBorder else CardBorder
                                    )
                                    list.forEach { meal ->
                                        MealRecordCard(
                                            meal = meal,
                                            onDelete = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.deleteMeal(meal.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun dishesWord(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..19 -> "блюд"
        mod10 == 1 -> "блюдо"
        mod10 in 2..4 -> "блюда"
        else -> "блюд"
    }
}

@Composable
private fun MealRecordCard(meal: MealLogEntity, onDelete: () -> Unit) {
    ModernCard(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = SurfaceWhite,
        borderColor = CardBorder,
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.name,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 2
                )
                Text(
                    text = "${meal.grams.pretty()} г • Б ${meal.protein.pretty()} • Ж ${meal.fat.pretty()} • У ${meal.carbs.pretty()}",
                    fontSize = 16.sp,
                    color = TextMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${meal.kcal.roundToInt()} ккал",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentGreen
                )
                TextButton(onClick = onDelete) {
                    Text(
                        text = "Удалить",
                        fontSize = 16.sp,
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroNutritionRow(name: String, eaten: Double, target: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text(text = "${eaten.pretty()} из $target г", fontSize = 17.sp, color = TextMuted, fontWeight = FontWeight.Medium)
        }
        LinearProgressIndicator(
            progress = {
                if (target > 0) (eaten / target).toFloat().coerceIn(0f, 1f) else 0f
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = AccentGreen,
            trackColor = Color(0xFFE2E8F0),
            strokeCap = StrokeCap.Round
        )
    }
}
