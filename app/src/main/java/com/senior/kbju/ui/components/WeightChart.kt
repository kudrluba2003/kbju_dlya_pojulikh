package com.senior.kbju.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.senior.kbju.data.local.entity.WeightLogEntity
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentGreen
import com.senior.kbju.ui.theme.AccentOrange
import com.senior.kbju.ui.theme.SoftGray
import com.senior.kbju.utils.pretty
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun WeightChart(
    weights: List<WeightLogEntity>,
    modifier: Modifier = Modifier
) {
    val summaryText = remember(weights) {
        if (weights.isEmpty()) {
            "График веса: данных пока нет"
        } else if (weights.size == 1) {
            "График веса: одно взвешивание, ${weights.first().weightKg.pretty()} кг"
        } else {
            val first = weights.first().weightKg
            val last = weights.last().weightKg
            val diff = last - first
            "График веса за ${weights.size} взвешиваний. Начальный: ${first.pretty()} кг, текущий: ${last.pretty()} кг. Разница: ${if (diff > 0) "+${diff.pretty()}" else diff.pretty()} кг"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = summaryText },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "График веса за месяц",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            if (weights.size < 2) {
                Text(
                    text = "Здесь появится график, когда вы запишете хотя бы 2 взвешивания в разные дни.",
                    fontSize = 18.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                val firstWeight = weights.first().weightKg
                val lastWeight = weights.last().weightKg
                val diff = lastWeight - firstWeight
                val diffText = abs(diff).pretty()

                val (trendText, trendColor) = when {
                    diff < -0.2 -> "Снизился на $diffText кг 📉" to AccentGreen
                    diff > 0.2 -> "Увеличился на $diffText кг 📈" to AccentOrange
                    else -> "Вес стабилен ⚖️" to AccentBlue
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Динамика:", fontSize = 18.sp)
                    Text(
                        text = trendText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = trendColor
                    )
                }

                // Отрисовка графика на Canvas
                val minWeight = remember(weights) { (weights.minOf { it.weightKg } - 1.0).coerceAtLeast(0.0) }
                val maxWeight = remember(weights) { weights.maxOf { it.weightKg } + 1.0 }
                val range = (maxWeight - minWeight).coerceAtLeast(1.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val paddingBottom = 40f
                    val paddingTop = 30f
                    val chartHeight = h - paddingBottom - paddingTop

                    // Горизонтальные направляющие линии
                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val y = paddingTop + chartHeight * (i.toFloat() / gridSteps)
                        drawLine(
                            color = Color(0xFFCCCCCC),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Вычисляем координаты точек
                    val stepX = if (weights.size > 1) w / (weights.size - 1) else w
                    val points = weights.mapIndexed { index, item ->
                        val x = index * stepX
                        val normalizedY = ((item.weightKg - minWeight) / range).toFloat()
                        val y = paddingTop + chartHeight * (1f - normalizedY)
                        Offset(x, y)
                    }

                    // Рисуем соединительную линию графика
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = AccentBlue,
                        style = Stroke(
                            width = 4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Рисуем точки и текстовые подписи через nativeCanvas
                    val paintText = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 14.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }

                    val paintDate = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 12.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    points.forEachIndexed { idx, point ->
                        val item = weights[idx]
                        // Внешняя белая точка и внутренняя синяя
                        drawCircle(color = Color.White, radius = 7.dp.toPx(), center = point)
                        drawCircle(color = AccentBlue, radius = 5.dp.toPx(), center = point)

                        // Подпись веса над точкой
                        val text = "${item.weightKg.pretty()} кг"
                        drawContext.canvas.nativeCanvas.drawText(
                            text,
                            point.x.coerceIn(40f, w - 40f),
                            point.y - 12f,
                            paintText
                        )

                        // Подпись даты под графиком
                        val date = LocalDate.ofEpochDay(item.day)
                            .format(DateTimeFormatter.ofPattern("d MMM", Locale("ru")))
                        drawContext.canvas.nativeCanvas.drawText(
                            date,
                            point.x.coerceIn(30f, w - 30f),
                            h - 4f,
                            paintDate
                        )
                    }
                }
            }
        }
    }
}
