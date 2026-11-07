package com.senior.kbju.ui.screens

import android.content.Intent
import android.net.Uri
import com.senior.kbju.BuildConfig
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.OutlineBigButton
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
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val updateCheckMessage by viewModel.updateCheckMessage.collectAsState()
    val context = LocalContext.current

    if (state.isLoading || state.profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(56.dp))
        }
        return
    }

    val dateText = remember(state.dateEpochDay) {
        LocalDate.ofEpochDay(state.dateEpochDay)
            .format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru")))
    }

    val progress = if (state.targetKcal > 0) {
        state.eatenKcal.toFloat() / state.targetKcal
    } else 0f
    val remaining = state.remainingKcal

    // Кольцо зажигается оранжевым только при превышении нормы более чем на 100 ккал (погрешность +-100 ккал остается в зеленой зоне)
    val isExceeded = state.eatenKcal > state.targetKcal + 100
    val ringColor = if (isExceeded) AccentOrange else AccentGreen

    // Дефицит за день: съедено - сожжено
    val netCalories = state.eatenKcal - state.burnedKcal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Баннер доступного обновления
        availableUpdate?.let { update ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "🎉 Доступно обновление v${update.tagName}!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                    Text(
                        text = "Новая версия с улучшениями готова к установке.",
                        fontSize = 16.sp
                    )
                    BigButton(
                        text = "📥 СКАЧАТЬ И ОБНОВИТЬ",
                        onClick = {
                            val url = update.apkDownloadUrl ?: update.releaseUrl
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(browserIntent)
                        },
                        color = AccentGreen
                    )
                }
            }
        }

        // Дата
        Text(
            text = "Сегодня, $dateText",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

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

        // Плашка: Дефицит за день (съедено - сожжено)
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
                    Text(
                        text = "(съедено − сожжено)",
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                }
                Text(
                    text = "$netCalories ккал",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (netCalories <= state.targetKcal) AccentGreen else AccentOrange
                )
            }
        }

        // Карточка проверки обновлений на главной
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Приложение v${BuildConfig.VERSION_NAME}", fontSize = 16.sp, color = Color.DarkGray)
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
                OutlineBigButton(
                    text = "🔄 ПРОВЕРИТЬ ОБНОВЛЕНИЯ",
                    onClick = { viewModel.checkForUpdates(silent = false) }
                )
                updateCheckMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 16.sp,
                        color = AccentBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Оставить отзыв / предложить функцию
        var feedbackInput by remember { mutableStateOf("") }
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "✉️ Оставить отзыв разработчику",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Напишите, каких продуктов или функций вам не хватает в приложении:",
                    fontSize = 15.sp,
                    color = Color.DarkGray
                )
                OutlinedTextField(
                    value = feedbackInput,
                    onValueChange = { feedbackInput = it },
                    placeholder = { Text("Напишите ваше пожелание...", fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 17.sp),
                    shape = RoundedCornerShape(8.dp)
                )
                BigButton(
                    text = "📨 ОТПРАВИТЬ ОТЗЫВ",
                    onClick = {
                        val body = feedbackInput.trim()
                        val sendIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:kudrluba2003@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "НОВОВВЕДЕНИЕ ПО ПРОСЬБЕ ПОЛЬЗОВАТЕЛЯ")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "${if (body.isNotBlank()) body else "(Пользователь не указал текст)"}\n\n---\nПриложение: Дневник питания v${BuildConfig.VERSION_NAME} (сборка ${BuildConfig.VERSION_CODE})\nAndroid: ${android.os.Build.VERSION.RELEASE}"
                            )
                        }
                        try {
                            context.startActivity(Intent.createChooser(sendIntent, "Отправить отзыв"))
                        } catch (_: Exception) {}
                    },
                    color = AccentGreen
                )
            }
        }

        // Кнопка изменить профиль
        TextButton(
            onClick = onEditProfile,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = "⚙️ Изменить профиль и настройки", fontSize = 18.sp, color = Color.DarkGray)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
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
        }
    }
}
