package com.senior.kbju.data.model

enum class Gender { MALE, FEMALE }

enum class ActivityLevel(val factor: Double, val label: String) {
    SEDENTARY(1.2, "Сижу дома"),
    LIGHT(1.375, "Гуляю каждый день"),
    MODERATE(1.55, "Активно работаю")
}

enum class Goal(val percent: Int, val label: String) {
    LOSE(-20, "Похудеть"),
    MAINTAIN(0, "Поддержать вес"),
    GAIN(15, "Набрать вес")
}

enum class MealType(val label: String) {
    BREAKFAST("Завтрак"),
    LUNCH("Обед"),
    DINNER("Ужин"),
    SNACK("Перекус")
}
