package com.senior.kbju.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Безопасный разбор чисел: пользователь может ввести «70,5», «70.5», «2.8г» или текст — краха не будет.
fun parseNumber(raw: String): Double? {
    val trimmed = raw.trim().replace(',', '.')
    trimmed.toDoubleOrNull()?.let { return it }

    // Устраняем распространенную ошибку OCR, когда буква 'О'/'o' распознается вместо нуля среди цифр
    val oFixed = trimmed.replace(Regex("""(?<=\d)[oO]|[oO](?=\d)"""), "0")
    oFixed.toDoubleOrNull()?.let { return it }

    // Отрезаем хвостовые единицы измерения (например «2.8г», «250ккал», «7.5g», «100ml»)
    val withoutUnit = oFixed.replace(Regex("""(?iu)[^\d.]+$"""), "").trim()
    return withoutUnit.toDoubleOrNull()
}

fun parseNumberIn(raw: String, min: Double, max: Double): Double? =
    parseNumber(raw)?.takeIf { it >= min && it <= max }

// Оставляем только цифры и один разделитель — поле ввода не примет мусор.
fun sanitizeNumberInput(raw: String): String {
    var separatorSeen = false
    return raw.take(10).filter { ch ->
        when {
            ch.isDigit() -> true
            ch == '.' || ch == ',' -> {
                if (separatorSeen) false else {
                    separatorSeen = true
                    true
                }
            }
            else -> false
        }
    }
}

// «70,5» вместо «70.5» — числа показываем с запятой, по-русски.
fun Double.pretty(): String {
    val symbols = DecimalFormatSymbols(Locale("ru"))
    val format = DecimalFormat("0.#", symbols)
    return format.format(this)
}
