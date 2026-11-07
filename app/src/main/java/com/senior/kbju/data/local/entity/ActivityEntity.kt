package com.senior.kbju.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val met: Double,
    // Категория для полного каталога: «Ходьба и бег», «Бытовые дела» и т.д.
    val category: String? = null,
    // Активности верхнего уровня — огромные кнопки быстрого доступа.
    val isQuickAccess: Boolean = false
)
