package com.senior.kbju.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_log")
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val weightKg: Double,
    val createdAt: Long = System.currentTimeMillis()
)
