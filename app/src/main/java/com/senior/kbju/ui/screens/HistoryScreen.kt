package com.senior.kbju.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.senior.kbju.ui.components.OutlineBigButton
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentGreen
import com.senior.kbju.ui.theme.AccentOrange
import com.senior.kbju.ui.theme.SoftGray
import com.senior.kbju.viewmodel.HistoryDayUiModel
import com.senior.kbju.viewmodel.HistoryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(onBack: () -> Unit, viewModel: HistoryViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(
            text = "История за неделю",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            text = "Норму сравниваем с сегодняшней: ${state.targetKcal} ккал в день",
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(56.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
            ) {
                item {
                    BigButton(
                        text = "ОТПРАВИТЬ ОТЧЁТ РОДНЫМ ИЛИ ВРАЧУ",
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
                    OutlineBigButton(text = "НА ГОЛОВНУЮ", onClick = onBack)
                }
            }
        }
    }
}

@Composable
private fun HistoryDayCard(day: HistoryDayUiModel) {
    val balance = day.balance
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SoftGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = dayTitle(day.day),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(text = "Съедено: ${day.eatenKcal} ккал", fontSize = 20.sp, color = Color.Black)
            Text(
                text = "Потрачено активностью: ${day.burnedKcal} ккал",
                fontSize = 20.sp,
                color = Color.Black
            )
            if (day.waterMl > 0) {
                Text(
                    text = "Выпито воды: ${day.waterMl} мл (${day.waterMl / 250} стак.)",
                    fontSize = 20.sp,
                    color = AccentBlue
                )
            }
            Text(
                text = if (balance >= 0) "Итог: дефицит $balance ккал" else "Итог: перебор ${-balance} ккал",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (balance >= 0) AccentGreen else AccentOrange
            )
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
