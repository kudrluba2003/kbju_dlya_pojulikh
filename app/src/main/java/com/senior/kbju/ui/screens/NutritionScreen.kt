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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.senior.kbju.ui.theme.CardBorder
import com.senior.kbju.ui.theme.ErrorRed
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

    if (state.isLoading || state.profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(56.dp), color = AccentGreen)
        }
        return
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

        SectionTitle(text = "Записи за день")
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
                    Text(
                        text = "${type.label} • $groupKcal ккал",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
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

        Spacer(modifier = Modifier.height(24.dp))
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