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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.ModernCard
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentBlueBorder
import com.senior.kbju.ui.theme.AccentBlueLight
import com.senior.kbju.ui.theme.CardBorder
import com.senior.kbju.ui.theme.SurfaceWhite
import com.senior.kbju.ui.theme.TextDark
import com.senior.kbju.ui.theme.TextMuted
import com.senior.kbju.ui.components.OutlineBigButton
import com.senior.kbju.viewmodel.DashboardViewModel

@Composable
fun WaterScreen(
    onBack: (() -> Unit)? = null,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    if (state.isLoading || state.profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(56.dp), color = AccentBlue)
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
            text = "💧 Водный баланс",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AccentBlue
        )

        ModernCard(
            shape = RoundedCornerShape(22.dp),
            backgroundColor = AccentBlueLight,
            borderColor = AccentBlueBorder,
            elevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${state.waterGlasses} из ${state.targetWaterGlasses} стаканов",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentBlue
                )
                Text(
                    text = "${state.waterMl} мл из ${state.targetWaterMl} мл",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark
                )

                // Сетка стаканов
                val target = state.targetWaterGlasses
                val filled = state.waterGlasses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val glassesIcons = buildString {
                        for (i in 1..target) {
                            append(if (i <= filled) "🥛" else "⚪")
                            if (i % 5 == 0 && i != target) append(" ")
                        }
                        if (filled > target) {
                            append(" (+${filled - target}🥛)")
                        }
                    }
                    Text(
                        text = glassesIcons,
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BigButton(
                        text = "➕ ВЫПИТЬ СТАКАН (250 мл)",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.addWater()
                        },
                        color = AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.waterGlasses > 0) {
                        BigButton(
                            text = "➖ 1",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.removeWater()
                            },
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.weight(0.35f)
                        )
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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "💧 Ваша норма: ${state.targetWaterGlasses} ст. (${state.targetWaterMl} мл)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                )
                Text(
                    text = "Изменить норму воды или рассчитать её по весу (30 мл на 1 кг) можно в разделе «Мой профиль».",
                    fontSize = 15.sp,
                    color = TextMuted
                )
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
                    text = "💡 Совет для здоровья",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "Пейте воду небольшими глотками в течение дня: один стакан после пробуждения, за 30 минут до еды и между приёмами пищи. Это поддерживает давление, суставы и пищеварение в тонусе.",
                    fontSize = 16.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}