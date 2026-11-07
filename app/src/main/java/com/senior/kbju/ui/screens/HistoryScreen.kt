package com.senior.kbju.ui.screens

import android.content.Intent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.ModernCard
import com.senior.kbju.ui.components.OutlineBigButton
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentGreen
import com.senior.kbju.ui.theme.AccentOrange
import com.senior.kbju.ui.theme.CardBorder
import com.senior.kbju.ui.theme.SurfaceWhite
import com.senior.kbju.ui.theme.TextDark
import com.senior.kbju.ui.theme.TextMuted
import com.senior.kbju.viewmodel.HistoryDayUiModel
import com.senior.kbju.viewmodel.HistoryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(onBack: () -> Unit, viewModel: HistoryViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(modifier = Modifier.height(12.dp))
        OutlineBigButton(
            text = "← НАЗАД В МЕНЮ ЕЩЁ",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )
        Text(
            text = "📅 История за неделю",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = "Ваша суточная норма: ${state.targetKcal} ккал",
            fontSize = 17.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(56.dp), color = AccentGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
            ) {
                item {
                    BigButton(
                        text = "📤 ОТПРАВИТЬ ОТЧЁТ РОДНЫМ ИЛИ ВРАЧУ",
                        onClick = {
                            val text = viewModel.generateWeeklyReport()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Отчёт о питании за неделю")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Поделиться отчётом")
                            context.startActivity(shareIntent)
                        },
                        color = AccentBlue
                    )
                }
                items(state.days, key = { it.day.toEpochDay() }) { day ->
                    HistoryDayCard(day)
                }
                item {
                    OutlineBigButton(text = "НА ГЛАВНУЮ", onClick = onBack)
                }
            }
        }
    }
}

@Composable
private fun HistoryDayCard(day: HistoryDayUiModel) {
    val balance = day.balance
    ModernCard(
        shape = RoundedCornerShape(18.dp),
        backgroundColor = SurfaceWhite,
        borderColor = CardBorder,
        elevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = dayTitle(day.day),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "🍽️ Съедено:", fontSize = 18.sp, color = TextMuted)
                Text(text = "${day.eatenKcal} ккал", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "🏃 Активность:", fontSize = 18.sp, color = TextMuted)
                Text(text = "${day.burnedKcal} ккал", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
            }
            if (day.waterMl > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "💧 Выпито воды:", fontSize = 18.sp, color = TextMuted)
                    Text(text = "${day.waterMl} мл (${day.waterMl / 250} ст.)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (balance >= 0) "Дефицит за день:" else "Перебор нормы:",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = if (balance >= 0) "$balance ккал" else "${-balance} ккал",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = if (balance >= 0) AccentGreen else AccentOrange
                )
            }
        }
    }
}

// «Сегодня, 24 октября», «Вчера, 23 октября», «Пятница, 17 октября».
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