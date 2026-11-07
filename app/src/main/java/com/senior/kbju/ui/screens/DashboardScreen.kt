package com.senior.kbju.ui.screens

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.data.local.entity.MealLogEntity
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.OutlineBigButton
import com.senior.kbju.ui.components.RingProgress
import com.senior.kbju.ui.components.SectionTitle
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentGreen
import com.senior.kbju.ui.theme.AccentOrange
import com.senior.kbju.ui.theme.ErrorRed
import com.senior.kbju.ui.theme.SoftGray
import com.senior.kbju.utils.pretty
import com.senior.kbju.viewmodel.DashboardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    onAddFood: () -> Unit,
    onAddActivity: () -> Unit,
    onHistory: () -> Unit,
    onUpdateWeight: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    if (state.isLoading || state.profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(56.dp))
        }
        return
    }
    val dateText = remember(state.dateEpochDay) {
        LocalDate.ofEpochDay(state.dateEpochDay)
            .format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru")))
    }
    val progress = if (state.targetKcal > 0) {
        state.eatenKcal.toFloat() / state.targetKcal
    } else 0f
    val remaining = state.remainingKcal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Сегодня, $dateText",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        RingProgress(
            progress = progress,
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Съедено", fontSize = 20.sp)
                Text(
                    text = "${state.eatenKcal}",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black
                )
                Text(text = "из ${state.targetKcal} ккал", fontSize = 20.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "Сожжено",
                value = "${state.burnedKcal} ккал",
                modifier = Modifier.weight(1f),
                valueColor = AccentBlue
            )
            StatCard(
                title = if (remaining >= 0) "Осталось съесть" else "Вы переели",
                value = if (remaining >= 0) "$remaining ккал" else "${-remaining} ккал сверху",
                modifier = Modifier.weight(1f),
                valueColor = if (remaining >= 0) AccentGreen else AccentOrange
            )
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MacroRow("Белки", state.protein, state.targetProtein)
                MacroRow("Жиры", state.fat, state.targetFat)
                MacroRow("Углеводы", state.carbs, state.targetCarbs)
            }
        }

        // Блок водного баланса
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💧 Водный баланс",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.waterMl} из ${state.targetWaterMl} мл",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue
                    )
                }

                // Визуальный ряд стаканов
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val total = maxOf(state.targetWaterGlasses, state.waterGlasses)
                    (1..total).forEach { i ->
                        val filled = i <= state.waterGlasses
                        Text(
                            text = if (filled) "💧" else "⚪",
                            fontSize = 24.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BigButton(
                        text = "+ СТАКАН ВОДЫ (250 мл)",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.addWater()
                        },
                        color = AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.waterGlasses > 0) {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.removeWater()
                            }
                        ) {
                            Text(
                                text = "– 1",
                                fontSize = 22.sp,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Регулировка дневной нормы воды
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Норма: ${state.targetWaterGlasses} ст. (${state.targetWaterMl} мл)",
                        fontSize = 17.sp,
                        color = Color.DarkGray
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                if (state.targetWaterGlasses > 2) {
                                    viewModel.updateTargetWater(state.targetWaterGlasses - 1)
                                }
                            }
                        ) {
                            Text("➖", fontSize = 18.sp)
                        }
                        TextButton(
                            onClick = {
                                if (state.targetWaterGlasses < 20) {
                                    viewModel.updateTargetWater(state.targetWaterGlasses + 1)
                                }
                            }
                        ) {
                            Text("➕", fontSize = 18.sp)
                        }
                    }
                }
            }
        }

        // Карточка цели и прогресса по весу
        if (state.profile != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SoftGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚖️ Контроль веса",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.currentWeightKg.pretty()} кг",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange
                        )
                    }

                    // Три ключевых веса: исходный, текущий, желаемый
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Исходный", fontSize = 16.sp, color = Color.Gray)
                            Text(text = "${state.startWeightKg.pretty()} кг", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Текущий", fontSize = 16.sp, color = Color.Gray)
                            Text(text = "${state.currentWeightKg.pretty()} кг", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Желаемый", fontSize = 16.sp, color = Color.Gray)
                            Text(text = "${state.targetWeightKg.pretty()} кг", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Баннер прогресса до цели
                    val diffToTarget = state.weightDiffToTarget
                    val goalText = when {
                        kotlin.math.abs(diffToTarget) < 0.2 -> "🎉 Поздравляем! Вы достигли цели!"
                        diffToTarget > 0 -> "🎯 До цели осталось сбросить: ${(diffToTarget).pretty()} кг"
                        else -> "🎯 До цели осталось набрать: ${(-diffToTarget).pretty()} кг"
                    }
                    val diffFromStart = state.weightDiffFromStart
                    val startMsg = when {
                        kotlin.math.abs(diffFromStart) < 0.2 -> null
                        diffFromStart < 0 -> "С начала пути сброшено: ${(-diffFromStart).pretty()} кг"
                        else -> "С начала пути набрано: ${(diffFromStart).pretty()} кг"
                    }

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = goalText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange
                            )
                            if (startMsg != null) {
                                Text(
                                    text = startMsg,
                                    fontSize = 15.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }

                    BigButton(
                        text = "⚖️ ОБНОВИТЬ ВЕС / ГРАФИК",
                        onClick = onUpdateWeight,
                        color = AccentOrange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        BigButton(text = "ДОБАВИТЬ ЕДУ", onClick = onAddFood, color = AccentGreen)
        BigButton(text = "ДОБАВИТЬ АКТИВНОСТЬ", onClick = onAddActivity, color = AccentBlue)
        OutlineBigButton(text = "ИСТОРИЯ ЗА НЕДЕЛЮ", onClick = onHistory)
        OutlineBigButton(text = "КОНТРОЛЬ ВЕСА", onClick = onUpdateWeight)

        SectionTitle(text = "Приёмы пищи за сегодня")
        if (state.meals.isEmpty()) {
            Text(text = "Пока ничего не записано.", fontSize = 18.sp)
        } else {
            state.mealsByGroup.forEach { (type, typeMeals) ->
                if (typeMeals.isNotEmpty()) {
                    val subtotal = typeMeals.sumOf { it.kcal }.roundToInt()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = type.label,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "$subtotal ккал",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }
                    typeMeals.forEach { meal ->
                        MealItemCard(
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

        if (state.activities.isNotEmpty()) {
            SectionTitle(text = "Активность")
            state.activities.forEach { activity ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftGray)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activity.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2
                            )
                            Text(text = "${activity.minutes} мин", fontSize = 18.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "-${activity.kcal.roundToInt()} ккал",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue
                            )
                            TextButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.deleteActivity(activity.id)
                            }) {
                                Text(
                                    text = "Удалить",
                                    fontSize = 18.sp,
                                    color = ErrorRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        TextButton(
            onClick = onEditProfile,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = "Изменить профиль", fontSize = 18.sp, color = Color.Black)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MealItemCard(meal: MealLogEntity, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SoftGray)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Text(
                    text = "${meal.grams.pretty()} г • Б ${meal.protein.pretty()} • Ж ${meal.fat.pretty()} • У ${meal.carbs.pretty()}",
                    fontSize = 18.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${meal.kcal.roundToInt()} ккал",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDelete) {
                    Text(
                        text = "Удалить",
                        fontSize = 18.sp,
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Black
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SoftGray)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun MacroRow(name: String, eaten: Double, target: Int) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = "${eaten.pretty()} из $target г", fontSize = 20.sp)
        }
        LinearProgressIndicator(
            progress = {
                if (target > 0) (eaten / target).toFloat().coerceIn(0f, 1f) else 0f
            },
            modifier = Modifier.fillMaxWidth().height(12.dp),
            color = AccentGreen,
            trackColor = Color.White
        )
    }
}
