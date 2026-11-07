package com.senior.kbju.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.BigToggleButton
import com.senior.kbju.ui.components.ModernCard
import com.senior.kbju.ui.components.SectionTitle
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentBlueLight
import com.senior.kbju.ui.theme.AccentGreen
import com.senior.kbju.ui.theme.AccentGreenLight
import com.senior.kbju.ui.theme.AccentOrange
import com.senior.kbju.ui.theme.AccentOrangeLight
import com.senior.kbju.ui.theme.CardBorder
import com.senior.kbju.ui.theme.SurfaceWhite
import com.senior.kbju.ui.theme.TextDark
import com.senior.kbju.ui.theme.TextMuted
import com.senior.kbju.viewmodel.HistoryDayUiModel
import com.senior.kbju.viewmodel.HistoryPeriod
import com.senior.kbju.viewmodel.HistorySummary
import com.senior.kbju.viewmodel.HistoryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(onBack: () -> Unit = {}, viewModel: HistoryViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "📊 Отчёт и статистика",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Text(
            text = "Ваша суточная норма: ${state.targetKcal} ккал",
            fontSize = 17.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // 1. Две крупные кнопки выбора периода: За неделю и За месяц
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HistoryPeriod.entries.forEach { p ->
                BigToggleButton(
                    text = p.label,
                    selected = state.period == p,
                    onClick = { viewModel.setPeriod(p) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(56.dp), color = AccentGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                // 2. Суммарная сводка: средний дефицит, среднее потребление, средняя активность, ср. кол-во воды
                item {
                    SummaryCard(summary = state.summary, period = state.period)
                }

                // 3. Кнопка отправки отчета
                item {
                    BigButton(
                        text = "📤 ПОДЕЛИТЬСЯ ОТЧЁТОМ",
                        onClick = {
                            val text = viewModel.generateReport()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Отчёт о питании и здоровье")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Поделиться отчётом")
                            context.startActivity(shareIntent)
                        },
                        color = AccentBlue,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    )
                }

                // 4. Заголовок перед днями
                item {
                    SectionTitle(
                        text = "📅 Подробно по дням (${state.days.size})"
                    )
                }

                // 5. Статистика по каждому дню
                items(state.days, key = { it.day.toEpochDay() }) { day ->
                    HistoryDayCard(day)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: HistorySummary, period: HistoryPeriod) {
    ModernCard(
        shape = RoundedCornerShape(20.dp),
        backgroundColor = SurfaceWhite,
        borderColor = CardBorder,
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "📈 В среднем за день (${period.label.lowercase()}):",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Средний дефицит
                val defSign = if (summary.avgDeficit >= 0) "+" else ""
                val defColor = if (summary.avgDeficit >= 0) AccentGreen else AccentOrange
                MetricCell(
                    icon = "⚖️",
                    title = "Ср. дефицит",
                    value = "$defSign${summary.avgDeficit} ккал",
                    valueColor = defColor,
                    backgroundColor = if (summary.avgDeficit >= 0) AccentGreenLight else AccentOrangeLight,
                    modifier = Modifier.weight(1f)
                )

                // Среднее потребление
                MetricCell(
                    icon = "🍽️",
                    title = "Ср. съедено",
                    value = "${summary.avgEatenKcal} ккал",
                    valueColor = TextDark,
                    backgroundColor = Color(0xFFF8FAFC),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Средняя активность
                MetricCell(
                    icon = "🏃",
                    title = "Ср. активность",
                    value = "${summary.avgBurnedKcal} ккал",
                    valueColor = AccentOrange,
                    backgroundColor = AccentOrangeLight,
                    modifier = Modifier.weight(1f)
                )

                // Среднее кол-во воды
                MetricCell(
                    icon = "💧",
                    title = "Ср. вода",
                    value = "${summary.avgWaterMl} мл",
                    valueColor = AccentBlue,
                    backgroundColor = AccentBlueLight,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricCell(
    icon: String,
    title: String,
    value: String,
    valueColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    ModernCard(
        shape = RoundedCornerShape(14.dp),
        backgroundColor = backgroundColor,
        borderColor = CardBorder.copy(alpha = 0.6f),
        elevation = 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 18.sp)
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = title, fontSize = 15.sp, color = TextMuted, fontWeight = FontWeight.Medium)
            }
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun HistoryDayCard(day: HistoryDayUiModel) {
    val balance = day.balance
    var expanded by remember { mutableStateOf(false) }

    ModernCard(
        shape = RoundedCornerShape(18.dp),
        backgroundColor = SurfaceWhite,
        borderColor = CardBorder,
        elevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayTitle(day.day),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    fontSize = 16.sp,
                    color = TextMuted
                )
            }

            // Главный показатель: баланс
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (balance >= 0) "Дефицит за день:" else "Перебор нормы:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = if (balance >= 0) "+$balance ккал" else "$balance ккал",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = if (balance >= 0) AccentGreen else AccentOrange
                )
            }

            // Детали дня
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "🍽️ Съедено:", fontSize = 17.sp, color = TextMuted)
                Text(text = "${day.eatenKcal} ккал", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "🏃 Активность:", fontSize = 17.sp, color = TextMuted)
                Text(text = "${day.burnedKcal} ккал", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
            }
            if (day.waterMl > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "💧 Выпито воды:", fontSize = 17.sp, color = TextMuted)
                    Text(
                        text = "${day.waterMl} мл (${day.waterMl / 250} ст.)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                ModernCard(
                    shape = RoundedCornerShape(10.dp),
                    backgroundColor = Color(0xFFF8FAFC),
                    borderColor = CardBorder,
                    elevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Суточная норма: ${day.targetKcal} ккал",
                            fontSize = 15.sp,
                            color = TextMuted
                        )
                        val status = if (balance >= 0) {
                            "✅ Вы уложились в норму и сохранили дефицит $balance ккал."
                        } else {
                            "⚠️ Превышение суточной нормы на ${-balance} ккал."
                        }
                        Text(
                            text = status,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (balance >= 0) AccentGreen else AccentOrange
                        )
                    }
                }
            }
        }
    }
}

// «Сегодня, 7 ноября», «Вчера, 6 ноября», «Пятница, 5 ноября».
private fun dayTitle(date: LocalDate): String {
    val today = LocalDate.now()
    val dateText = date.format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru")))
    return when (date) {
        today -> "Сегодня, $dateText"
        today.minusDays(1) -> "Вчера, $dateText"
        else -> {
            val weekday = date.format(DateTimeFormatter.ofPattern("EEEE", Locale("ru")))
            weekday.replaceFirstChar { it.uppercase() } + ", $dateText"
        }
    }
}
