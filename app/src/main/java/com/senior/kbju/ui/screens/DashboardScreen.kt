package com.senior.kbju.ui.screens

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.RingProgress
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentGreen
import com.senior.kbju.ui.theme.AccentOrange
import com.senior.kbju.ui.theme.SoftGray
import com.senior.kbju.viewmodel.DashboardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    onEditProfile: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading || state.profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(56.dp))
        }
        return
    }

    val context = LocalContext.current
    val localDate = remember(state.dateEpochDay) {
        LocalDate.ofEpochDay(state.dateEpochDay)
    }
    val dateText = remember(state.dateEpochDay) {
        localDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")))
    }

    fun showDatePicker() {
        android.app.DatePickerDialog(
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

    val progress = if (state.targetKcal > 0) {
        state.eatenKcal.toFloat() / state.targetKcal
    } else 0f
    val remaining = state.remainingKcal

    // Кольцо зажигается оранжевым только при превышении нормы более чем на 100 ккал (погрешность +-100 ккал остается в зеленой зоне)
    val isExceeded = state.eatenKcal > state.targetKcal + 100
    val ringColor = if (isExceeded) AccentOrange else AccentGreen

    // Дефицит за день: съедено минус всего сожжено (базовый метаболизм + активность)
    val netCalories = state.netCalories

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Переключатель дней с календариком
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.shiftDay(-1) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text("◀", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }

                    Row(
                        modifier = Modifier
                            .clickable { showDatePicker() }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📅", fontSize = 28.sp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (state.isToday) "Сегодня" else dateText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (state.isToday) dateText else "нажмите для выбора даты",
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.shiftDay(1) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text("▶", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }
                }

                if (!state.isToday) {
                    TextButton(
                        onClick = { viewModel.selectToday() },
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text("« Вернуться на сегодня", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }
                }
            }
        }

        // Кольцо калорий
        RingProgress(
            progress = progress,
            progressColor = ringColor,
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Съедено", fontSize = 20.sp)
                Text(
                    text = "${state.eatenKcal}",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentGreen
                )
                Text(text = "из ${state.targetKcal} ккал", fontSize = 20.sp)
                if (isExceeded) {
                    Text(
                        text = "Превышение нормы",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                }
            }
        }

        // Плашки: Сожжено (оранжевая) и Осталось съесть (зеленая)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "Сожжено",
                value = "${state.burnedKcal} ккал",
                subtitle = if (state.activityBurnedKcal > 0) "базовый ${state.basalMetabolismKcal} + активность ${state.activityBurnedKcal}" else "базовый метаболизм",
                modifier = Modifier.weight(1f),
                valueColor = AccentOrange
            )
            StatCard(
                title = if (remaining >= 0) "Осталось съесть" else "Сверху нормы",
                value = if (remaining >= 0) "$remaining ккал" else "${-remaining} ккал",
                modifier = Modifier.weight(1f),
                valueColor = if (isExceeded) AccentOrange else AccentGreen
            )
        }

        // Карточка: Ваш базовый метаболизм (авто-пересчет по весу)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ваш базовый метаболизм",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Энергия на дыхание и работу организма при вашем весе ${state.currentWeightKg} кг",
                        fontSize = 15.sp,
                        color = Color.DarkGray
                    )
                }
                Text(
                    text = "${state.basalMetabolismKcal} ккал",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                )
            }
        }

        // Плашка: Дефицит за день (без надписи съедено-сожжено)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Дефицит за день",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "$netCalories ккал",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (netCalories <= 0) AccentGreen else AccentOrange
                )
            }
        }

        // Крупная кнопка "Мой профиль"
        BigButton(
            text = "👤 МОЙ ПРОФИЛЬ",
            onClick = onEditProfile,
            color = Color(0xFF37474F),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Black
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SoftGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
