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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.ModernCard
import com.senior.kbju.ui.components.OutlineBigButton
import com.senior.kbju.ui.components.RingProgress
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentBlueLight
import com.senior.kbju.ui.theme.AccentGreen
import com.senior.kbju.ui.theme.AccentGreenBorder
import com.senior.kbju.ui.theme.AccentGreenLight
import com.senior.kbju.ui.theme.AccentOrange
import com.senior.kbju.ui.theme.AccentOrangeBorder
import com.senior.kbju.ui.theme.AccentOrangeLight
import com.senior.kbju.ui.theme.CardBorder
import com.senior.kbju.ui.theme.SoftGray
import com.senior.kbju.ui.theme.SurfaceWhite
import com.senior.kbju.ui.theme.TextDark
import com.senior.kbju.ui.theme.TextMuted
import com.senior.kbju.viewmodel.DashboardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    onEditProfile: () -> Unit,
    onAddFood: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading || state.profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(56.dp), color = AccentGreen)
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

    val isExceeded = state.eatenKcal > state.targetKcal + 100
    val ringColor = if (isExceeded) AccentOrange else AccentGreen
    val netCalories = state.netCalories

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Переключатель дней с календарём
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
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.shiftDay(-1) },
                        modifier = Modifier
                            .size(46.dp)
                            .background(SoftGray, RoundedCornerShape(14.dp))
                    ) {
                        Text("◀", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }

                    Row(
                        modifier = Modifier
                            .clickable { showDatePicker() }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(AccentBlueLight, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📅", fontSize = 22.sp)
                        }
                        Column {
                            Text(
                                text = if (state.isToday) "Сегодня" else dateText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = if (state.isToday) dateText else "нажмите для выбора даты",
                                fontSize = 14.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.shiftDay(1) },
                        modifier = Modifier
                            .size(46.dp)
                            .background(SoftGray, RoundedCornerShape(14.dp))
                    ) {
                        Text("▶", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
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
        ModernCard(
            shape = RoundedCornerShape(24.dp),
            backgroundColor = SurfaceWhite,
            borderColor = CardBorder,
            elevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RingProgress(
                    progress = progress,
                    progressColor = ringColor,
                    modifier = Modifier.size(240.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Съедено", fontSize = 18.sp, color = TextMuted)
                        Text(
                            text = "${state.eatenKcal}",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = ringColor
                        )
                        Text(
                            text = "из ${state.targetKcal} ккал",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                        if (isExceeded) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠️ Превышение",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange
                            )
                        }
                    }
                }
            }
        }

        // Растянутая плашка: Сожжено
        ModernCard(
            shape = RoundedCornerShape(18.dp),
            backgroundColor = AccentOrangeLight,
            borderColor = AccentOrangeBorder,
            elevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🔥", fontSize = 28.sp)
                    Column {
                        Text(
                            text = "Сожжено",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        val subtitle = if (state.activityBurnedKcal > 0) {
                            "спорт: ${state.activityBurnedKcal} ккал"
                        } else {
                            "в покое + шаги"
                        }
                        Text(
                            text = subtitle,
                            fontSize = 15.sp,
                            color = TextMuted
                        )
                    }
                }
                Text(
                    text = "${state.burnedKcal} ккал",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentOrange
                )
            }
        }

        // Плашка: Дефицит за день
        val deficitBg = if (netCalories <= 0) AccentGreenLight else AccentOrangeLight
        val deficitBorder = if (netCalories <= 0) AccentGreenBorder else AccentOrangeBorder
        val deficitColor = if (netCalories <= 0) AccentGreen else AccentOrange
        val deficitIcon = if (netCalories <= 0) "📉" else "📈"
        val deficitTitle = if (netCalories <= 0) "Дефицит за день" else "Профицит за день"
        val deficitSign = if (netCalories < 0) "-${kotlin.math.abs(netCalories)}" else "$netCalories"

        ModernCard(
            shape = RoundedCornerShape(18.dp),
            backgroundColor = deficitBg,
            borderColor = deficitBorder,
            elevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(deficitIcon, fontSize = 28.sp)
                    Text(
                        text = deficitTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
                Text(
                    text = "$deficitSign ккал",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = deficitColor
                )
            }
        }

        // Крупная кнопка "Мой профиль" с высокой контрастностью и чётким текстом
        OutlineBigButton(
            text = "👤 МОЙ ПРОФИЛЬ",
            onClick = onEditProfile,
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}


