package com.senior.kbju.ui.screens

import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.ErrorText
import com.senior.kbju.ui.components.NumberField
import com.senior.kbju.ui.components.OutlineBigButton
import com.senior.kbju.ui.components.SectionTitle
import com.senior.kbju.ui.components.WeightChart
import com.senior.kbju.ui.theme.AccentOrange
import com.senior.kbju.ui.theme.ErrorRed
import com.senior.kbju.ui.theme.SoftGray
import com.senior.kbju.utils.pretty
import com.senior.kbju.viewmodel.WeightViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeightScreen(onFinished: () -> Unit, viewModel: WeightViewModel = viewModel()) {
    val profile by viewModel.profile.collectAsState()
    val currentTarget by viewModel.currentTargetKcal.collectAsState()
    val weightHistory by viewModel.weightHistory.collectAsState()
    val allWeights by viewModel.allWeights.collectAsState()
    val weight by viewModel.weightText.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val newNorm by viewModel.confirmedNewNorm.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val norm = newNorm
    LaunchedEffect(norm) {
        if (norm != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val text = "Ваш вес обновлен! Ваша новая норма: $norm ккал"
            val styled = SpannableString(text)
            styled.setSpan(
                AbsoluteSizeSpan(22, true),
                0,
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            Toast.makeText(context, styled, Toast.LENGTH_LONG).show()
            viewModel.dismissConfirmation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Контроль веса", fontSize = 30.sp, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Текущий вес: ${profile?.weightKg?.pretty() ?: "—"} кг",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Суточная норма: ${currentTarget ?: "—"} ккал в день",
                    fontSize = 20.sp
                )
            }
        }

        // График веса за месяц
        WeightChart(weights = weightHistory)

        SectionTitle(text = "Записать новое взвешивание")
        NumberField(
            label = "Ваш вес сегодня",
            value = weight,
            onValueChange = { viewModel.weightText.value = it },
            suffix = "кг"
        )
        Text(
            text = "Взвешивайтесь утром натощак — так числа будут точнее.",
            fontSize = 18.sp
        )
        ErrorText(error)
        BigButton(text = "СОХРАНИТЬ ВЕС", onClick = viewModel::save, color = AccentOrange)

        // История последних записей взвешиваний
        if (allWeights.isNotEmpty()) {
            SectionTitle(text = "История взвешиваний")
            allWeights.take(10).forEach { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftGray)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            val dateStr = LocalDate.ofEpochDay(item.day)
                                .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")))
                            Text(text = "$dateStr г.", fontSize = 18.sp)
                            Text(
                                text = "${item.weightKg.pretty()} кг",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = { viewModel.deleteWeight(item.id) }) {
                            Text(
                                text = "Удалить",
                                color = ErrorRed,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        OutlineBigButton(text = "НА ГОЛОВНУЮ", onClick = onFinished)
        Spacer(modifier = Modifier.height(24.dp))
    }
}
