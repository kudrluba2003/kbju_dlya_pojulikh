package com.senior.kbju.utils

/**
 * Распознавание КБЖУ с этикетки (результат OCR ML Kit).
 * Работает полностью офлайн. Устойчив к различным форматам упаковок РФ/СНГ.
 */
data class ParsedNutrition(
    val kcal: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val rawText: String = ""
) {
    val hasAnyData: Boolean
        get() = kcal != null || protein != null || fat != null || carbs != null
}

object NutritionOcrParser {

    // Шаблоны для калорий: приоритет — число непосредственно рядом с "ккал" / "kcal"
    // Используем (?iu) для поддержки регистра как латиницы, так и кириллицы (Unicode Case)
    private val KCAL_BEFORE_UNIT = Regex("""(?iu)([\d]+[.,]?[\d]*)\s*(?:ккал|kcal)""")
    private val KCAL_AFTER_UNIT = Regex("""(?iu)(?:ккал|kcal)[^\d\n]*?([\d]+[.,]?[\d]*)""")
    private val ENERGY_LINE = Regex("""(?iu)(?:энергетическ|калорийн|калории?)[^\d\n]*?([\d]+[.,]?[\d]*)""")

    // Шаблоны для БЖУ
    private val PROTEIN_PATTERN = Regex("""(?iu)(?:белк|protein)[^\d\n]*?([\d]+[.,]?[\d]*)""")
    private val FAT_PATTERN = Regex("""(?iu)(?:жир|fat|lipid)[^\d\n]*?([\d]+[.,]?[\d]*)""")
    private val CARBS_PATTERN = Regex("""(?iu)(?:углевод|carbohydrate|carbs)[^\d\n]*?([\d]+[.,]?[\d]*)""")

    // Резервный поиск числа в строке
    private val NUMBER_IN_STRING = Regex("""([\d]+[.,]?[\d]*)""")

    fun parse(rawText: String): ParsedNutrition {
        if (rawText.isBlank()) return ParsedNutrition(rawText = rawText)

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        var kcal = parseKcal(rawText, lines)
        var protein = parseNutrient(lines, PROTEIN_PATTERN, "белк", "protein")
        var fat = parseNutrient(lines, FAT_PATTERN, "жир", "fat")
        var carbs = parseNutrient(lines, CARBS_PATTERN, "углевод", "carb")

        return ParsedNutrition(
            kcal = kcal,
            protein = protein,
            fat = fat,
            carbs = carbs,
            rawText = rawText
        )
    }

    private fun parseKcal(rawText: String, lines: List<String>): Double? {
        // 1. Ищем строго число перед "ккал": например "250 ккал" или "60,5 ккал"
        KCAL_BEFORE_UNIT.find(rawText)?.groupValues?.getOrNull(1)?.let {
            parseNumber(it)?.let { num -> return num }
        }

        // 2. Ищем строго число после "ккал": например "ккал: 250"
        KCAL_AFTER_UNIT.find(rawText)?.groupValues?.getOrNull(1)?.let {
            parseNumber(it)?.let { num -> return num }
        }

        // 3. Ищем в строке со словами "энергетическая ценность" или "калорийность"
        for (line in lines) {
            if (line.contains("энергетическ", ignoreCase = true) ||
                line.contains("калори", ignoreCase = true)
            ) {
                // Если в этой строке есть и кДж и ккал, вытаскиваем то, что с ккал
                val kcalMatch = KCAL_BEFORE_UNIT.find(line) ?: KCAL_AFTER_UNIT.find(line)
                if (kcalMatch != null) {
                    val num = parseNumber(kcalMatch.groupValues[1])
                    if (num != null) return num
                }

                // Иначе проверяем прямое совпадение по шаблону ENERGY_LINE
                val match = ENERGY_LINE.find(line)
                if (match != null) {
                    val num = parseNumber(match.groupValues[1])
                    if (num != null) return num
                }

                // Либо берем первое подходящее число из строки (обычно если только калории указаны)
                // Отсекаем слишком большие числа (обычно кДж > 1000)
                val allNumbers = NUMBER_IN_STRING.findAll(line).mapNotNull {
                    parseNumber(it.groupValues[1])
                }.toList()

                val plausibleKcal = allNumbers.firstOrNull { it in 10.0..999.0 }
                if (plausibleKcal != null) return plausibleKcal
            }
        }

        return null
    }

    private fun parseNutrient(
        lines: List<String>,
        pattern: Regex,
        vararg keywords: String
    ): Double? {
        // Сначала ищем по точному regex в каждой строке
        for (i in lines.indices) {
            val line = lines[i]
            val match = pattern.find(line)
            if (match != null && match.groupValues.size > 1) {
                val candidate = match.groupValues[1]
                val num = parseNumber(candidate)
                if (num != null) return num
            }

            // Если строка содержит ключевое слово, но цифр в ней нет вовсе (число на следующей строке):
            val hasKeyword = keywords.any { line.contains(it, ignoreCase = true) }
            if (hasKeyword && !line.any { it.isDigit() } && i + 1 < lines.size) {
                val nextLine = lines[i + 1]
                val nextMatch = NUMBER_IN_STRING.find(nextLine)
                if (nextMatch != null) {
                    val num = parseNumber(nextMatch.groupValues[1])
                    if (num != null && num in 0.0..100.0) return num
                }
            }
        }
        return null
    }
}
