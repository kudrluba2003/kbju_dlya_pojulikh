package com.senior.kbju.utils

/**
 * Распознавание КБЖУ с этикетки (результат OCR ML Kit).
 * Работает полностью офлайн. Устойчив к различным форматам упаковок РФ/СНГ,
 * включая сокращения (Б/Ж/У), наклейки супермаркетов («Эн.цен», «Пищ.цен»),
 * латинскую транслитерацию и артефакты OCR.
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

    // Шаблоны для калорий:
    // 1) Число непосредственно перед единицей измерения
    private val KCAL_BEFORE_UNIT = Regex("""(?iu)([0-9oO]+[.,]?[0-9oO]*)\s*(?:ккал|kcal|ккап|ккаn|ккaл|кcal|kkal|кк|кал|cal|kca1|kcaI|kkan|кка|kca|ккл|кд|kcat)(?!\p{L})""")
    // 2) Число непосредственно после единицы измерения
    private val KCAL_AFTER_UNIT = Regex("""(?iu)(?:ккал|kcal|ккап|ккаn|ккaл|кcal|kkal|кк|кал|cal|kca1|kcaI|kkan|кка|kca|ккл|кд|kcat)[^\d\n,;]*?([0-9oO]+[.,]?[0-9oO]*)""")

    // Шаблон для формата супермаркетов: «Эн.цен: 174ккал/728кДж» или «Эн.цен:343.8 / 439»
    private val ENERGY_SLASH_PATTERN = Regex("""(?iu)(?:эн[.\s]*цен[.\s]*:?|энерг[.\s]*цен[.\s]*:?|калорийн[.\s]*:?)\s*([0-9oO]+[.,]?[0-9oO]*)\s*(?:ккал|kcal|кка|kca)?\s*[/]\s*([0-9oO]+[.,]?[0-9oO]*)""")

    // Шаблон для последовательной записи БЖУ на наклейках супермаркетов:
    // «Пищ.цен: Б-31,8г, Ж-19,2г, У-11,0г» или «б-15,3г,ж-11,7г,у-2,9г» или «6-31.8g, x-19.2g, y-11.0g»
    private val BJU_SEQUENCE_PATTERN = Regex(
        """(?iu)(?:б|b|6|белк[а-яa-z]*)\s*[-:—–]?\s*([0-9oO]+[.,]?[0-9oO]*)\s*(?:г|g|r)?\s*[,; \t/]+\s*(?:ж|x|zh|жир[а-яa-z]*)\s*[-:—–]?\s*([0-9oO]+[.,]?[0-9oO]*)\s*(?:г|g|r)?\s*[,; \t/]+\s*(?:у|y|u|углев[а-яa-z]*)\s*[-:—–]?\s*([0-9oO]+[.,]?[0-9oO]*)"""
    )

    // Шаблоны для отдельных БЖУ:
    // Поддерживаем как русские слова, так и латинские эквиваленты, сокращения (Б, Ж, У),
    // и визуальные артефакты OCR (например '6' или 'b' вместо 'Б', 'xup' вместо 'жир', 'yr' вместо 'уг')
    private val PROTEIN_AFTER_LABEL = Regex("""(?iu)(?:белк[а-яa-z]*|белок|protein|prot|6e[лl][кk][а-яa-z]*|6en[кk][а-яa-z]*|be[лl][кk][а-яa-z]*|(?<!\p{L})[бbB](?!\p{L}))[^\d\n,;]*?([0-9oO]+[.,]?[0-9oO]*)""")
    private val PROTEIN_BEFORE_LABEL = Regex("""(?iu)([0-9oO]+[.,]?[0-9oO]*)\s*(?:г|g|r)?\s*(?:белк[а-яa-z]*|белок|protein)(?!\p{L})""")

    private val FAT_AFTER_LABEL = Regex("""(?iu)(?:жир[а-яa-z]*|fat|lipid|[xх][uи][pр][а-яa-z]*|zhir[а-яa-z]*|(?<!\p{L})[жЖxX](?!\p{L}))[^\d\n,;]*?([0-9oO]+[.,]?[0-9oO]*)""")
    private val FAT_BEFORE_LABEL = Regex("""(?iu)([0-9oO]+[.,]?[0-9oO]*)\s*(?:г|g|r)?\s*(?:жир[а-яa-z]*|fat|lipid)(?!\p{L})""")

    private val CARBS_AFTER_LABEL = Regex("""(?iu)(?:углев[а-яa-z]*|углевод[а-яa-z]*|carbohydrate|carbs|carb|[yу]г[лl][еe]в[а-яa-z]*|[yу]r[лl][еe]в[а-яa-z]*|uglev[а-яa-z]*|(?<!\p{L})[уyYuU](?!\p{L}))[^\d\n,;]*?([0-9oO]+[.,]?[0-9oO]*)""")
    private val CARBS_BEFORE_LABEL = Regex("""(?iu)([0-9oO]+[.,]?[0-9oO]*)\s*(?:г|g|r)?\s*(?:углев[а-яa-z]*|углевод[а-яa-z]*|carbs|carbohydrate)(?!\p{L})""")

    // Резервный поиск чисел в строке
    private val NUMBER_IN_STRING = Regex("""([0-9oO]+[.,]?[0-9oO]*)""")

    fun parse(rawText: String): ParsedNutrition {
        if (rawText.isBlank()) return ParsedNutrition(rawText = rawText)

        // Нормализуем текст и получаем строки
        val normalized = rawText.replace("\r", "")
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }
        val singleLineText = lines.joinToString(" ")

        var kcal = parseKcal(rawText, lines, singleLineText)
        var protein: Double? = null
        var fat: Double? = null
        var carbs: Double? = null

        // 1. Проверяем последовательный шаблон супермаркетов: «Б-31,8г, Ж-19,2г, У-11,0г»
        val bjuMatch = BJU_SEQUENCE_PATTERN.find(singleLineText)
        if (bjuMatch != null && bjuMatch.groupValues.size >= 4) {
            val p = parseNumber(bjuMatch.groupValues[1])
            val f = parseNumber(bjuMatch.groupValues[2])
            val c = parseNumber(bjuMatch.groupValues[3])
            if (p != null && p in 0.0..100.0) protein = p
            if (f != null && f in 0.0..100.0) fat = f
            if (c != null && c in 0.0..100.0) carbs = c
        }

        // 2. Проверяем табличную структуру (когда все три Б/Ж/У идут в строке заголовка, а на следующей — числа)
        if (protein == null || fat == null || carbs == null) {
            val tableIndex = lines.indexOfFirst { l ->
                val lower = l.lowercase()
                val hasP = lower.contains("белк") || lower.contains("protein") || lower.contains("6e") || lower.contains("belk") || Regex("""(?<!\p{L})[бb](?!\p{L})""").containsMatchIn(lower)
                val hasF = lower.contains("жир") || lower.contains("fat") || lower.contains("xup") || lower.contains("zhir") || Regex("""(?<!\p{L})[жx](?!\p{L})""").containsMatchIn(lower)
                val hasC = lower.contains("углев") || lower.contains("carb") || lower.contains("uglev") || Regex("""(?<!\p{L})[уy](?!\p{L})""").containsMatchIn(lower)
                hasP && hasF && hasC
            }
            if (tableIndex != -1 && tableIndex + 1 < lines.size) {
                val numberLine = lines[tableIndex + 1]
                val numbers = NUMBER_IN_STRING.findAll(numberLine)
                    .mapNotNull { parseNumber(it.groupValues[1]) }
                    .filter { it in 0.0..100.0 }
                    .toList()
                if (numbers.size >= 3) {
                    if (protein == null) protein = numbers.getOrNull(0)
                    if (fat == null) fat = numbers.getOrNull(1)
                    if (carbs == null) carbs = numbers.getOrNull(2)
                }
            }
        }

        // 3. Добираем недостающие нутриенты по отдельным строкам и единой строке
        val allSearchSources = lines + listOf(singleLineText)
        if (protein == null) protein = parseNutrient(allSearchSources, PROTEIN_AFTER_LABEL, PROTEIN_BEFORE_LABEL, "белк", "protein", "belk")
        if (fat == null) fat = parseNutrient(allSearchSources, FAT_AFTER_LABEL, FAT_BEFORE_LABEL, "жир", "fat", "xup", "zhir")
        if (carbs == null) carbs = parseNutrient(allSearchSources, CARBS_AFTER_LABEL, CARBS_BEFORE_LABEL, "углев", "carb", "uglev")

        return ParsedNutrition(
            kcal = kcal,
            protein = protein,
            fat = fat,
            carbs = carbs,
            rawText = rawText
        )
    }

    private fun parseKcal(rawText: String, lines: List<String>, singleLineText: String): Double? {
        // Очищаем текст от кДж (например 1420 кДж), чтобы они не принимались за ккал
        val textWithoutKj = singleLineText.replace(Regex("""(?iu)[\d]+[.,]?[\d]*\s*(?:кДж|kj|кдж)(?!\p{L})"""), " ")

        // 1. Ищем формат «Эн.цен: 174ккал/728кДж» или «343.8 / 439»
        ENERGY_SLASH_PATTERN.find(singleLineText)?.let { match ->
            val n1 = parseNumber(match.groupValues[1])
            val n2 = parseNumber(match.groupValues[2])
            // Калории всегда меньше кДж (1 ккал = ~4.184 кДж)
            if (n1 != null && n1 in 5.0..1500.0) return n1
            if (n2 != null && n2 in 5.0..1500.0) return n2
        }

        // 2. Ищем число перед "ккал": например "250 ккал" или "60,5 ккал"
        KCAL_BEFORE_UNIT.findAll(textWithoutKj).forEach { match ->
            parseNumber(match.groupValues[1])?.let { num ->
                if (num in 5.0..1500.0) return num
            }
        }

        // 3. Ищем число после "ккал": например "ккал: 250"
        KCAL_AFTER_UNIT.findAll(textWithoutKj).forEach { match ->
            parseNumber(match.groupValues[1])?.let { num ->
                if (num in 5.0..1500.0) return num
            }
        }

        // 4. Ищем в строках со словами "энергетическая ценность", "калорийность", "эн.цен", "эн цен"
        val candidates = lines + listOf(singleLineText)
        for (line in candidates) {
            val lower = line.lowercase()
            val isEnergyLine = lower.contains("энерг") || lower.contains("калор") || lower.contains("energy") || lower.contains("calor") ||
                lower.contains("эн.цен") || lower.contains("эн. цен") || lower.contains("эн цен") || lower.contains("эн-цен") ||
                lower.contains("эн.ц") || lower.contains("en.cen") || lower.contains("э/цен") || lower.contains("э.ц")

            if (isEnergyLine) {
                val cleanedLine = line
                    .replace(Regex("""(?iu)100\s*(?:г|g|мл|ml|продукт[а-я]*)"""), " ")
                    .replace(Regex("""(?iu)[\d]+[.,]?[\d]*\s*(?:кДж|kj|кдж)\b"""), " ")

                val kcalMatch = KCAL_BEFORE_UNIT.find(cleanedLine) ?: KCAL_AFTER_UNIT.find(cleanedLine)
                if (kcalMatch != null) {
                    val num = parseNumber(kcalMatch.groupValues[1])
                    if (num != null && num in 5.0..1500.0) return num
                }

                val allNumbers = NUMBER_IN_STRING.findAll(cleanedLine).mapNotNull {
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
        afterPattern: Regex,
        beforePattern: Regex,
        vararg keywords: String
    ): Double? {
        for (i in lines.indices) {
            val line = lines[i]

            // 1. Число после названия (Белки: 7.5 г)
            val afterMatch = afterPattern.find(line)
            if (afterMatch != null && afterMatch.groupValues.size > 1) {
                val num = parseNumber(afterMatch.groupValues[1])
                if (num != null && num in 0.0..100.0) return num
            }

            // 2. Число перед названием (7.5 г белка)
            val beforeMatch = beforePattern.find(line)
            if (beforeMatch != null && beforeMatch.groupValues.size > 1) {
                val num = parseNumber(beforeMatch.groupValues[1])
                if (num != null && num in 0.0..100.0) return num
            }

            // 3. Если слово на одной строке, а число на следующей
            val hasKeyword = keywords.any { line.contains(it, ignoreCase = true) }
            val hasOtherNutrients = listOf("белк", "жир", "углев", "protein", "fat", "carb").count { line.contains(it, ignoreCase = true) } > 1
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
