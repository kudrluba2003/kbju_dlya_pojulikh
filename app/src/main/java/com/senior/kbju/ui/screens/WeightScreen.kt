package com.senior.kbju.ui.screens

import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.widget.Toast
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.ErrorText
import com.senior.kbju.ui.components.ModernCard
import com.senior.kbju.ui.components.NumberField
import com.senior.kbju.ui.components.OutlineBigButton
import com.senior.kbju.ui.components.SectionTitle
import com.senior.kbju.ui.components.WeightChart
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentGreen
import com.senior.kbju.ui.theme.AccentGreenBorder
import com.senior.kbju.ui.theme.AccentGreenLight
import com.senior.kbju.ui.theme.AccentOrange
import com.senior.kbju.ui.theme.CardBorder
import com.senior.kbju.ui.theme.ErrorRed
import com.senior.kbju.ui.theme.SurfaceWhite
import com.senior.kbju.ui.theme.TextDark
import com.senior.kbju.ui.theme.TextMuted
import com.senior.kbju.utils.pretty
import com.senior.kbju.viewmodel.WeightViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeightScreen(onFinished: () -> Unit, viewModel: WeightViewModel = viewModel()) {
    val profile by viewModel.profile.collectAsState()
    val currentTarget by viewModel.currentTargetKcal.collectAsState()
    val weightHistory by viewModel.weightHistory.collectAsState()
    val allWeights by viewModel.allWeights.collectAsState()
    val weight by viewModel.weightText.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val newNorm by viewModel.confirmedNewNorm.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val norm = newNorm
    LaunchedEffect(norm) {
        if (norm != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val text = "Ваш вес обновлен! Ваша новая норма: $norm ккал"
            val styled = SpannableString(text)
            styled.setSpan(
                AbsoluteSizeSpan(22, true),
                0,
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            Toast.makeText(context, styled, Toast.LENGTH_LONG).show()
            viewModel.dismissConfirmation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⚖️ Контроль веса",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        ModernCard(
            shape = RoundedCornerShape(20.dp),
            backgroundColor = SurfaceWhite,
            borderColor = CardBorder,
            elevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Текущий вес: ${profile?.weightKg?.pretty() ?: "—"} кг",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentGreen
                )

                val cur = profile?.weightKg
                val start = profile?.startWeightKg ?: cur
                val target = profile?.targetWeightKg ?: cur

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Исходный", fontSize = 15.sp, color = TextMuted)
                        Text(text = "${start?.pretty() ?: "—"} кг", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Желаемый (цель)", fontSize = 15.sp, color = TextMuted)
                        Text(text = "${target?.pretty() ?: "—"} кг", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                    }
                }

                if (cur != null && target != null) {
                    val diff = cur - target
                    val goalMsg = when {
                        kotlin.math.abs(diff) < 0.2 -> "🎉 Поздравляем! Вы достигли желаемого веса!"
                        diff > 0 -> "🎯 До цели осталось сбросить: ${(diff).pretty()} кг"
                        else -> "🎯 До цели осталось набрать: ${(-diff).pretty()} кг"
                    }
                    ModernCard(
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = AccentGreenLight,
                        borderColor = AccentGreenBorder,
                        elevation = 0.dp
                    ) {
                        Text(
                            text = goalMsg,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Text(
                    text = "Суточная норма: ${currentTarget ?: "—"} ккал в день",
                    fontSize = 17.sp,
                    color = TextMuted
                )
            }
        }

        // График веса за месяц
        WeightChart(weights = weightHistory)

        SectionTitle(text = "Записать новое взвешивание")
        NumberField(
            label = "Ваш вес сегодня",
            value = weight,
            onValueChange = { viewModel.weightText.value = it },
            suffix = "кг"
        )
        Text(
            text = "Взвешивайтесь утром натощак — так числа будут точнее.",
            fontSize = 16.sp,
            color = TextMuted
        )
        ErrorText(error)
        BigButton(text = "СОХРАНИТЬ ВЕС", onClick = viewModel::save, color = AccentGreen)

        // История последних записей взвешиваний
        if (allWeights.isNotEmpty()) {
            SectionTitle(text = "История взвешиваний")
            allWeights.take(10).forEach { item ->
                ModernCard(
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = SurfaceWhite,
                    borderColor = CardBorder,
                    elevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            val dateStr = LocalDate.ofEpochDay(item.day)
                                .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")))
                            Text(text = "$dateStr г.", fontSize = 16.sp, color = TextMuted)
                            Text(
                                text = "${item.weightKg.pretty()} кг",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                        TextButton(onClick = { viewModel.deleteWeight(item.id) }) {
                            Text(
                                text = "Удалить",
                                color = ErrorRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}