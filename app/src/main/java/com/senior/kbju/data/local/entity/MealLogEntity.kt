package com.senior.kbju.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_log",
    indices = [
        Index(value = ["day"])
    ]
)
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val name: String,
    val grams: Double,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val mealType: String = "LUNCH",
    val createdAt: Long = System.currentTimeMillis()
)
