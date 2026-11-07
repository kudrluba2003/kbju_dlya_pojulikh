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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.ModernCard
import com.senior.kbju.ui.components.SectionTitle
import com.senior.kbju.ui.theme.AccentOrange
import com.senior.kbju.ui.theme.AccentOrangeBorder
import com.senior.kbju.ui.theme.AccentOrangeLight
import com.senior.kbju.ui.theme.CardBorder
import com.senior.kbju.ui.theme.ErrorRed
import com.senior.kbju.ui.theme.SurfaceWhite
import com.senior.kbju.ui.theme.TextDark
import com.senior.kbju.ui.theme.TextMuted
import com.senior.kbju.viewmodel.DashboardViewModel
import kotlin.math.roundToInt

@Composable
fun ActivityScreen(
    onAddActivity: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    if (state.isLoading || state.profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(56.dp), color = AccentOrange)
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
            text = "🏃 Физическая активность",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AccentOrange
        )

        ModernCard(
            shape = RoundedCornerShape(22.dp),
            backgroundColor = AccentOrangeLight,
            borderColor = AccentOrangeBorder,
            elevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Всего сожжено сегодня:", fontSize = 19.sp, color = TextMuted)
                Text(
                    text = "${state.burnedKcal} ккал",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentOrange
                )
                val totalMinutes = state.activities.sumOf { it.minutes }
                Text(
                    text = "Базовый метаболизм ${state.basalMetabolismKcal} ккал + активность ${state.activityBurnedKcal} ккал ($totalMinutes мин)",
                    fontSize = 15.sp,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        BigButton(
            text = "➕ ДОБАВИТЬ АКТИВНОСТЬ",
            onClick = onAddActivity,
            color = AccentOrange
        )

        SectionTitle(text = "Занятия за день")
        if (state.activities.isEmpty()) {
            ModernCard(
                shape = RoundedCornerShape(18.dp),
                backgroundColor = SurfaceWhite,
                borderColor = CardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Пока нет записанной активности за этот день. Добавьте прогулку, зарядку или домашние дела по кнопке выше.",
                    fontSize = 17.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(18.dp)
                )
            }
        } else {
            state.activities.forEach { activity ->
                ModernCard(
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = SurfaceWhite,
                    borderColor = CardBorder,
                    elevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activity.name,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                maxLines = 2
                            )
                            Text(text = "${activity.minutes} мин", fontSize = 16.sp, color = TextMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "+${activity.kcal.roundToInt()} ккал",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentOrange
                            )
                            TextButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.deleteActivity(activity.id)
                            }) {
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
        }

        ModernCard(
            shape = RoundedCornerShape(18.dp),
            backgroundColor = SurfaceWhite,
            borderColor = CardBorder,
            elevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "💡 Польза движения",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "Даже 20-30 минут неспешной ходьбы в день нормализуют давление, улучшают сон и поддерживают тонус мышц. Калории от активности плюсуются к сожжённым за день.",
                    fontSize = 16.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}