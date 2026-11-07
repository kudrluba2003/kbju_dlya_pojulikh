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
    private val KCAL_BEFORE_UNIT = Regex("""(?iu)([\d]+[.,]?[\d]*)\s*(?:ккал|kcal|ккап)""")
    private val KCAL_AFTER_UNIT = Regex("""(?iu)(?:ккал|kcal|ккап)[^\d\n]*?([\d]+[.,]?[\d]*)""")

    // Шаблоны для БЖУ
    private val PROTEIN_PATTERN = Regex("""(?iu)(?:белк|белок|белка|protein|б\b)[^\d\n]*?([\d]+[.,]?[\d]*)""")
    private val FAT_PATTERN = Regex("""(?iu)(?:жир|жиры|жира|fat|lipid|ж\b)[^\d\n]*?([\d]+[.,]?[\d]*)""")
    private val CARBS_PATTERN = Regex("""(?iu)(?:углев|углевод|углеводы|carbohydrate|carbs|у\b)[^\d\n]*?([\d]+[.,]?[\d]*)""")

    // Резервный поиск числа в строке
    private val NUMBER_IN_STRING = Regex("""([\d]+[.,]?[\d]*)""")

    fun parse(rawText: String): ParsedNutrition {
        if (rawText.isBlank()) return ParsedNutrition(rawText = rawText)

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        var kcal = parseKcal(rawText, lines)
        var protein: Double? = null
        var fat: Double? = null
        var carbs: Double? = null

        // 1. Проверяем табличную структуру (когда все три Б/Ж/У идут в заголовке, а на следующей строке числа)
        val tableIndex = lines.indexOfFirst { l ->
            val lower = l.lowercase()
            (lower.contains("белк") || lower.contains("protein")) &&
            (lower.contains("жир") || lower.contains("fat")) &&
            (lower.contains("углев") || lower.contains("carb"))
        }
        if (tableIndex != -1 && tableIndex + 1 < lines.size) {
            val numberLine = lines[tableIndex + 1]
            val numbers = NUMBER_IN_STRING.findAll(numberLine)
                .mapNotNull { parseNumber(it.groupValues[1]) }
                .filter { it in 0.0..100.0 }
                .toList()
            if (numbers.size >= 3) {
                protein = numbers.getOrNull(0)
                fat = numbers.getOrNull(1)
                carbs = numbers.getOrNull(2)
            }
        }

        // 2. Добираем недостающие нутриенты по строкам
        if (protein == null) protein = parseNutrient(lines, PROTEIN_PATTERN, "белк", "protein")
        if (fat == null) fat = parseNutrient(lines, FAT_PATTERN, "жир", "fat")
        if (carbs == null) carbs = parseNutrient(lines, CARBS_PATTERN, "углев", "carb")

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
            parseNumber(it)?.let { num ->
                if (num in 5.0..1500.0) return num
            }
        }

        // 2. Ищем строго число после "ккал": например "ккал: 250"
        KCAL_AFTER_UNIT.find(rawText)?.groupValues?.getOrNull(1)?.let {
            parseNumber(it)?.let { num ->
                if (num in 5.0..1500.0) return num
            }
        }

        // 3. Ищем в строке со словами "энергетическая ценность" или "калорийность"
        for (line in lines) {
            val lower = line.lowercase()
            if (lower.contains("энергетическ") || lower.contains("калори") || lower.contains("energy")) {
                // Если в этой строке есть "ккал", сначала ищем совпадение рядом с "ккал"
                val kcalMatch = KCAL_BEFORE_UNIT.find(line) ?: KCAL_AFTER_UNIT.find(line)
                if (kcalMatch != null) {
                    val num = parseNumber(kcalMatch.groupValues[1])
                    if (num != null && num in 5.0..1500.0) return num
                }

                // Очищаем строку от "100 г" (чтобы 100 не принялось за калории) и кДж
                val cleanedLine = line
                    .replace(Regex("""(?iu)100\s*(?:г|g|мл|ml|продукт[а-я]*)"""), " ")
                    .replace(Regex("""(?iu)[\d]+[.,]?[\d]*\s*(?:кДж|kj)"""), " ")

                val allNumbers = NUMBER_IN_STRING.findAll(cleanedLine).mapNotNull {
                    parseNumber(it.groupValues[1])
                }.toList()

                // Отбираем число калорий (обычно в диапазоне 10..999)
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
                if (num != null && num in 0.0..100.0) return num
            }

            // Если строка содержит ключевое слово, но цифр в ней нет вовсе (число на следующей строке):
            val hasKeyword = keywords.any { line.contains(it, ignoreCase = true) }
            val hasOtherNutrients = listOf("белк", "жир", "углев").count { line.contains(it, ignoreCase = true) } > 1
            if (hasKeyword && !hasOtherNutrients && !line.any { it.isDigit() } && i + 1 < lines.size) {
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
