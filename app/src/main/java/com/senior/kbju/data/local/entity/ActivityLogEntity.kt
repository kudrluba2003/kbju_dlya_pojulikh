package com.senior.kbju.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_log")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val name: String,
    val minutes: Int,
    val kcal: Double,
    val createdAt: Long = System.currentTimeMillis()
)
