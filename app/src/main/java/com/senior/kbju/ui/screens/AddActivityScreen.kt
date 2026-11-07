package com.senior.kbju.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.BigToggleButton
import com.senior.kbju.ui.components.ErrorText
import com.senior.kbju.ui.components.NumberField
import com.senior.kbju.ui.components.OutlineBigButton
import com.senior.kbju.ui.components.SectionTitle
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentGreen
import com.senior.kbju.ui.theme.SoftGray
import com.senior.kbju.viewmodel.AddActivityViewModel

@Composable
fun AddActivityScreen(onFinished: () -> Unit, viewModel: AddActivityViewModel = viewModel()) {
    val quickActivities by viewModel.quickActivities.collectAsState()
    val groupedActivities by viewModel.groupedActivities.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val minutes by viewModel.minutesText.collectAsState()
    val burned by viewModel.burnedKcal.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val savedMessage by viewModel.savedMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val done = savedMessage
        if (done == null) {
            Text(text = "Добавить активность", fontSize = 30.sp, fontWeight = FontWeight.Bold)

            // ----- Быстрый доступ: сетка 2 × N огромных кнопок -----
            SectionTitle(text = "Быстрый доступ")
            quickActivities.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { activity ->
                        BigToggleButton(
                            text = activity.name,
                            selected = selected?.id == activity.id,
                            onClick = { viewModel.select(activity) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // ----- Полный каталог по категориям, спрятан за одной кнопкой -----
            var showAll by remember { mutableStateOf(false) }
            OutlineBigButton(
                text = if (showAll) "СКРЫТЬ ВСЕ АКТИВНОСТИ" else "ПОКАЗАТЬ ВСЕ АКТИВНОСТИ",
                onClick = { showAll = !showAll }
            )
            if (showAll) {
                groupedActivities.forEach { (category, activities) ->
                    SectionTitle(text = category)
                    activities.forEach { activity ->
                        BigToggleButton(
                            text = activity.name,
                            selected = selected?.id == activity.id,
                            onClick = { viewModel.select(activity) }
                        )
                    }
                }
            }

            val chosen = selected
            if (chosen != null) {
                Text(
                    text = "Выбрано: ${chosen.name}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
            }

            NumberField(
                label = "Сколько минут вы занимались?",
                value = minutes,
                onValueChange = viewModel::onMinutesChange,
                suffix = "минут"
            )

            val burnedValue = burned
            if (burnedValue != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftGray)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(text = "Сожжёте примерно:", fontSize = 20.sp)
                        Text(
                            text = "$burnedValue ккал",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }
                }
            }

            ErrorText(error)
            BigButton(text = "ЗАПИСАТЬ", onClick = viewModel::save, color = AccentBlue)
        } else {
            Text(text = "Готово!", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SoftGray)
            ) {
                Text(
                    text = done,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            BigButton(text = "НА ГОЛОВНУЮ", onClick = onFinished, color = AccentBlue)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
