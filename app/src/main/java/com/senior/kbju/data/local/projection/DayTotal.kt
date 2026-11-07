package com.senior.kbju.data.local.projection

// Результат агрегирующего SQL-запроса: итог калорий за один день
// (day — LocalDate.toEpochDay(), total — сумма ккал).
data class DayTotal(
    val day: Long,
    val total: Double
)
