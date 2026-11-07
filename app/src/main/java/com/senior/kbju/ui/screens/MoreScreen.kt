package com.senior.kbju.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.ui.components.ModernCard
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentBlueLight
import com.senior.kbju.ui.theme.AccentGreen
import com.senior.kbju.ui.theme.AccentGreenLight
import com.senior.kbju.ui.theme.CardBorder
import com.senior.kbju.ui.theme.SoftGray
import com.senior.kbju.ui.theme.SurfaceWhite
import com.senior.kbju.ui.theme.TextDark
import com.senior.kbju.ui.theme.TextMuted
import com.senior.kbju.viewmodel.DashboardViewModel

@Composable
fun MoreScreen(
    onOpenWater: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "Дополнительно",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "Вода, контроль веса, отчёты и настройки",
                fontSize = 17.sp,
                color = TextMuted
            )
        }

        // 1. Карточка Вода
        val waterBadge = if (state.targetWaterMl > 0) {
            "Сегодня: ${state.waterMl} мл из ${state.targetWaterMl} мл"
        } else "Учёт стаканов воды"
        MoreNavCard(
            title = "Вода",
            icon = "💧",
            subtitle = "Учёт выпитых стаканов за день",
            badgeText = waterBadge,
            badgeColor = AccentBlueLight,
            badgeTextColor = AccentBlue,
            onClick = onOpenWater
        )

        // 2. Карточка Вес
        val weightBadge = if (state.currentWeightKg > 0) {
            "Текущий вес: ${state.currentWeightKg} кг"
        } else "Записать вес"
        MoreNavCard(
            title = "Контроль веса",
            icon = "⚖️",
            subtitle = "Взвешивания, история и цель",
            badgeText = weightBadge,
            badgeColor = AccentGreenLight,
            badgeTextColor = AccentGreen,
            onClick = onOpenWeight
        )

        // 3. Карточка История и отчёты
        MoreNavCard(
            title = "История и отчёты",
            icon = "📜",
            subtitle = "Недельная сводка и отчёт для врача",
            badgeText = "Посмотреть историю питания",
            badgeColor = SoftGray,
            badgeTextColor = TextDark,
            onClick = onOpenHistory
        )

        // 4. Карточка Настройки
        MoreNavCard(
            title = "Настройки",
            icon = "⚙️",
            subtitle = "Напоминания, обновления, отзывы, копия",
            badgeText = "Управление приложением",
            badgeColor = SoftGray,
            badgeTextColor = TextDark,
            onClick = onOpenSettings
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MoreNavCard(
    title: String,
    icon: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    badgeTextColor: Color,
    onClick: () -> Unit
) {
    ModernCard(
        shape = RoundedCornerShape(20.dp),
        backgroundColor = SurfaceWhite,
        borderColor = CardBorder,
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(SoftGray, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 28.sp)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = subtitle,
                        fontSize = 15.sp,
                        color = TextMuted
                    )
                    Box(
                        modifier = Modifier
                            .background(badgeColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    }
                }
            }

            Text(
                text = "›",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
