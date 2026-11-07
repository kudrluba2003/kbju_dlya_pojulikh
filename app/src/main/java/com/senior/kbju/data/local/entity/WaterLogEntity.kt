package com.senior.kbju.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_log")
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val glasses: Int,
    val ml: Int,
    val createdAt: Long = System.currentTimeMillis()
)
