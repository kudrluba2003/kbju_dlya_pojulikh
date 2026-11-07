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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.OutlineBigButton
import com.senior.kbju.ui.components.SectionTitle
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.SoftGray
import com.senior.kbju.utils.pretty
import com.senior.kbju.viewmodel.DashboardViewModel
import kotlin.math.roundToInt

@Composable
fun WaterScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    var waterExplanation by remember { mutableStateOf<String?>(null) }

    if (state.isLoading || state.profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(56.dp))
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Водный баланс",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AccentBlue
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray),
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
                    text = "💧 ${state.waterGlasses} из ${state.targetWaterGlasses} стаканов",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                )
                Text(
                    text = "${state.waterMl} мл из ${state.targetWaterMl} мл",
                    fontSize = 20.sp,
                    color = Color.DarkGray
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                            color = Color.LightGray,
                            modifier = Modifier.weight(0.4f)
                        )
                    }
                }
            }
        }

        SectionTitle(text = "Настройка дневной нормы")
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Норма: ${state.targetWaterGlasses} ст. (${state.targetWaterMl} мл)",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BigButton(
                            text = "➖",
                            onClick = {
                                if (state.targetWaterGlasses > 2) {
                                    viewModel.updateTargetWater(state.targetWaterGlasses - 1)
                                }
                            },
                            color = Color.LightGray
                        )
                        BigButton(
                            text = "➕",
                            onClick = {
                                if (state.targetWaterGlasses < 20) {
                                    viewModel.updateTargetWater(state.targetWaterGlasses + 1)
                                }
                            },
                            color = AccentBlue
                        )
                    }
                }

                OutlineBigButton(
                    text = "💧 РАССЧИТАТЬ ПО ВЕСУ (30 мл / кг)",
                    onClick = {
                        val weight = state.currentWeightKg
                        if (weight > 20) {
                            val ml = (weight * 30.0).roundToInt()
                            val glasses = (ml / 250.0).roundToInt().coerceIn(4, 16)
                            viewModel.updateTargetWater(glasses)
                            waterExplanation = "Для веса ${weight.pretty()} кг норма: ~$ml мл ($glasses ст. по 250 мл)"
                        }
                    }
                )

                waterExplanation?.let { text ->
                    Text(
                        text = text,
                        fontSize = 16.sp,
                        color = AccentBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "💡 Совет для здоровья",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                )
                Text(
                    text = "С возрастом чувство жажды притупляется. Старайтесь выпивать по половине или целому стакану воды между приёмами пищи, не дожидаясь жажды.",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
