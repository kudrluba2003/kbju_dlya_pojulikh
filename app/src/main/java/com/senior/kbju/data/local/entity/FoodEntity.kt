package com.senior.kbju.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food",
    indices = [
        Index(value = ["name"]),
        Index(value = ["isUserCreated", "name"])
    ]
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcal100: Double,
    val protein100: Double,
    val fat100: Double,
    val carbs100: Double,
    val gramsPerGlass: Double? = null,
    val gramsPerSpoon: Double? = null,
    // Категория из большой базы (products_seed.json): «Каши», «Мясо» и т.д.
    val category: String? = null,
    // Продукты, созданные самим пользователем: введённые с этикетки
    // или собранные блюда, пересчитанные на 100 г. Они «навсегда» в базе.
    val isUserCreated: Boolean = false
)
