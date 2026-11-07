package com.senior.kbju.utils

/**
 * Распознавание КБЖУ с этикетки (результат OCR ML Kit).
 * Работает полностью офлайн. Устойчив к различным форматам упаковок РФ/СНГ,
 * включая сокращения (Б/Ж/У), наклейки супермаркетов («Эн.цен», «Пищ.цен»),
 * ГОСТ-стандарты («Белки, г: 7.5»), латинскую транслитерацию и артефакты OCR.
 */
data class ParsedNutrition(
    val kcal: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val suggestedName: String? = null,
    val rawText: String = ""
) {
    val hasAnyData: Boolean
        get() = kcal != null || protein != null || fat != null || carbs != null

    val isComplete: Boolean
        get() = kcal != null && protein != null && fat != null && carbs != null

    fun mergeWith(other: ParsedNutrition): ParsedNutrition {
        return ParsedNutrition(
            kcal = this.kcal ?: other.kcal,
            protein = this.protein ?: other.protein,
            fat = this.fat ?: other.fat,
            carbs = this.carbs ?: other.carbs,
            suggestedName = this.suggestedName ?: other.suggestedName,
            rawText = if (this.rawText.isNotBlank()) this.rawText else other.rawText
        )
    }
}

object NutritionOcrParser {

    // Шаблоны для калорий:
    // 1) Число непосредственно перед единицей измерения
    private val KCAL_BEFORE_UNIT = Regex("""(?iu)([0-9oO]+[.,]?[0-9oO]*)\s*(?:ккал|kcal|ккап|ккаn|ккaл|кcal|kkal|кал|cal|kca1|kcaI|kkan|кка|kca)(?!\p{L})""")
    // 2) Число непосредственно после единицы измерения (поддерживает скобки, слэши, "ккал/100г: 250", "(ккал): 250")
    private val KCAL_AFTER_UNIT = Regex("""(?iu)(?:ккал|kcal|ккап|ккаn|ккaл|кcal|kkal|кал|cal|kca1|kcaI|kkan|кка|kca)[\)\s,/]*(?:(?:на\s*)?100\s*(?:г|g|мл|ml|%)[)\s,/]*)?[\s:\-—–=\.]*([0-9oO]+[.,]?[0-9oO]*)""")

    // Шаблон для формата супермаркетов: «Эн.цен: 174ккал/728кДж» или «Эн.цен:343.8 / 439»
    private val ENERGY_SLASH_PATTERN = Regex("""(?iu)(?:эн[.\s]*цен[.\s]*:?|энерг[.\s]*цен[.\s]*:?|калорийн[.\s]*:?)\s*([0-9oO]+[.,]?[0-9oO]*)\s*(?:ккал|kcal|кка|kca)?\s*[/]\s*([0-9oO]+[.,]?[0-9oO]*)""")

    // Шаблон для строк со словом «калорийность» или «энергетическая ценность» и числом
    private val KCAL_LINE_PATTERN = Regex("""(?iu)(?:калорийн[а-яa-z]*|энерг[.\s]*цен[а-яa-z]*|эн[.\s]*цен[а-яa-z]*|energy|calories?)[\)\s,/]*(?:(?:на\s*)?100\s*(?:г|g|мл|ml|%)[)\s,/]*)?[\s:\-—–=\.]*([0-9oO]+[.,]?[0-9oO]*)""")

    // Шаблон для последовательной записи БЖУ на наклейках супермаркетов:
    // «Пищ.цен: Б-31,8г, Ж-19,2г, У-11,0г» или «б-15,3г,ж-11,7г,у-2,9г» или «6-31.8g, x-19.2g, y-11.0g»
    private val BJU_SEQUENCE_PATTERN = Regex(
        """(?iu)(?:б|b|6|белк[а-яa-z]*)\s*[-:—–]?\s*([0-9oO]+[.,]?[0-9oO]*)\s*(?:г|g|r)?\s*[,; \t/]+\s*(?:ж|x|zh|жир[а-яa-z]*)\s*[-:—–]?\s*([0-9oO]+[.,]?[0-9oO]*)\s*(?:г|g|r)?\s*[,; \t/]+\s*(?:у|y|u|углев[а-яa-z]*)\s*[-:—–]?\s*([0-9oO]+[.,]?[0-9oO]*)"""
    )

    // Шаблоны для отдельных БЖУ:
    // Поддерживаем русские слова, ГОСТ-стандарты («Белки, г: 7.5», «Белки (г) - 7.5», «Белки, г/100г: 7.5»),
    // латинские эквиваленты, сокращения (Б, Ж, У), и визуальные артефакты OCR ('6'/'b' вместо 'Б', 'x' вместо 'ж', 'y'/'yr' вместо 'уг')
    private val PROTEIN_AFTER_LABEL = Regex(
        """(?iu)(?:белк[а-яa-z]*|белок|protein[a-z]*|prot[a-z]*|6e[лl][кk][а-яa-z]*|6en[кk][а-яa-z]*|be[лl][кk][а-яa-z]*|(?<!\p{L})[бbB](?!\p{L}))""" +
        """(?:[\s,\(]*(?:г|g|r|мл|ml|%)[\)\s,]*)?""" +
        """(?:[/,\s\(]*(?:на\s*)?100\s*(?:г|g|мл|ml|%)[/,\s\)]*)?""" +
        """[\s:\-—–=\.]*""" +
        """([0-9oO]+[.,]?[0-9oO]*)"""
    )
    private val PROTEIN_BEFORE_LABEL = Regex("""(?iu)([0-9oO]+[.,]?[0-9oO]*)\s*(?:г|g|r)?\s*(?:белк[а-яa-z]*|белок|protein[a-z]*)(?!\p{L})""")

    private val FAT_AFTER_LABEL = Regex(
        """(?iu)(?:жир[а-яa-z]*|fat[s]?|lipid[s]?|[xх][uи][pр][а-яa-z]*|zhir[а-яa-z]*|(?<!\p{L})[жЖxX](?!\p{L}))""" +
        """(?:[\s,\(]*(?:г|g|r|мл|ml|%)[\)\s,]*)?""" +
        """(?:[/,\s\(]*(?:на\s*)?100\s*(?:г|g|мл|ml|%)[/,\s\)]*)?""" +
        """[\s:\-—–=\.]*""" +
        """([0-9oO]+[.,]?[0-9oO]*)"""
    )
    private val FAT_BEFORE_LABEL = Regex("""(?iu)([0-9oO]+[.,]?[0-9oO]*)\s*(?:г|g|r)?\s*(?:жир[а-яa-z]*|fat[s]?|lipid[s]?)(?!\p{L})""")

    private val CARBS_AFTER_LABEL = Regex(
        """(?iu)(?:углев[а-яa-z]*|углевод[а-яa-z]*|carbohydrat[a-z]*|carbs?|carb[a-z]*|[yу]г[лl][еe]в[а-яa-z]*|[yу]r[лl][еe]в[а-яa-z]*|uglev[а-яa-z]*|(?<!\p{L})[уyYuU](?!\p{L}))""" +
        """(?:[\s,\(]*(?:г|g|r|мл|ml|%)[\)\s,]*)?""" +
        """(?:[/,\s\(]*(?:на\s*)?100\s*(?:г|g|мл|ml|%)[/,\s\)]*)?""" +
        """[\s:\-—–=\.]*""" +
        """([0-9oO]+[.,]?[0-9oO]*)"""
    )
    private val CARBS_BEFORE_LABEL = Regex("""(?iu)([0-9oO]+[.,]?[0-9oO]*)\s*(?:г|g|r)?\s*(?:углев[а-яa-z]*|углевод[а-яa-z]*|carbohydrat[a-z]*|carbs?)(?!\p{L})""")

    // Резервный поиск чисел в строке
    private val NUMBER_IN_STRING = Regex("""([0-9oO]+[.,]?[0-9oO]*)""")

    fun normalizeOcrText(raw: String): String {
        return raw
            .replace("\r", "")
            // Исправление типичных артефактов латинского OCR для русских слов
            .replace(Regex("""(?iu)\b3нepre"""), "энерге")
            .replace(Regex("""(?iu)\b(?:nuщeв|nuweв|nnuweв)"""), "пищев")
            .replace(Regex("""(?iu)(?:kkan|kkar|kca1|kcaI|ккап|ккаn)\b"""), "ккал")
            .replace(Regex("""(?iu)\b(?:6eлк|6enк|6eлk|6enk|benk|belk)"""), "белк")
            .replace(Regex("""(?iu)\b(?:xup|kup|zhir)\b"""), "жир")
            .replace(Regex("""(?iu)\b(?:yrneв|yrлeв|urleв|uglev)"""), "углев")
            .replace(")|(", "ж")
            .replace("}{", "ж")
    }

    fun parse(rawText: String): ParsedNutrition {
        if (rawText.isBlank()) return ParsedNutrition(rawText = rawText)

        val normalized = normalizeOcrText(rawText)
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

        val suggestedName = extractSuggestedProductName(rawText)

        return ParsedNutrition(
            kcal = kcal,
            protein = protein,
            fat = fat,
            carbs = carbs,
            suggestedName = suggestedName,
            rawText = rawText
        )
    }

    fun extractSuggestedProductName(rawText: String): String? {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        for (line in lines.take(8)) {
            val lower = line.lowercase()
            if (lower.startsWith("пищев") || lower.startsWith("энерг") || lower.startsWith("состав") ||
                lower.startsWith("гост") || lower.startsWith("ту ") || lower.startsWith("изм.") ||
                lower.startsWith("срок") || lower.startsWith("масса") || lower.startsWith("вес") ||
                lower.startsWith("nutrition") || lower.startsWith("ингредиент") || lower.startsWith("хранить") ||
                lower.startsWith("дата") || lower.contains("ккал") || lower.contains("кдж") ||
                lower.length < 3 || lower.length > 50
            ) {
                continue
            }
            val cleaned = line.replace(Regex("""^[\d\s\W«"']+|[\d\s\W»"']+$"""), "").trim()
            if (cleaned.length in 3..50 && cleaned.any { it.isLetter() }) {
                return cleaned
            }
        }
        return null
    }

    private fun parseKcal(rawText: String, lines: List<String>, singleLineText: String): Double? {
        // Очищаем текст от кДж (например 1420 кДж), чтобы они гарантированно не принимались за ккал
        val textWithoutKj = singleLineText.replace(Regex("""(?iu)[\d]+[.,]?[\d]*\s*(?:кДж|kj|кдж)(?!\p{L})"""), " ")

        // 1. Ищем формат с разделителем «/»: «Эн.цен: 174ккал/728кДж» или «343.8 / 439»
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

        // 3. Ищем число после "ккал": например "ккал: 250", "ккал/100г: 250", "(ккал): 250"
        KCAL_AFTER_UNIT.findAll(textWithoutKj).forEach { match ->
            parseNumber(match.groupValues[1])?.let { num ->
                if (num in 5.0..1500.0) return num
            }
        }

        // 4. Ищем по строкам со словами "энергетическая ценность", "калорийность", "эн.цен"
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

                val kcalMatch = KCAL_BEFORE_UNIT.find(cleanedLine) ?: KCAL_AFTER_UNIT.find(cleanedLine) ?: KCAL_LINE_PATTERN.find(cleanedLine)
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
        // Первый проход: ищем основные строки, не содержащие «в т.ч.» / «насыщенные» / «сахара»
        for (i in lines.indices) {
            val line = lines[i]
            val lower = line.lowercase()
            val isSubItem = lower.contains("в т.ч") || lower.contains("в том числе") || lower.contains("из них") || lower.contains("насыщен") || lower.contains("сахар")
            if (isSubItem) continue

            // 1. Число после названия (Белки: 7.5 г, Белки, г: 7.5, Белки (г) - 7.5)
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
            val lineWithout100 = line.replace(Regex("""(?iu)100\s*(?:г|g|мл|ml)"""), "")
            if (hasKeyword && !hasOtherNutrients && !lineWithout100.any { it.isDigit() } && i + 1 < lines.size) {
                val nextLine = lines[i + 1]
                val nextMatch = NUMBER_IN_STRING.find(nextLine)
                if (nextMatch != null) {
                    val num = parseNumber(nextMatch.groupValues[1])
                    if (num != null && num in 0.0..100.0) return num
                }
            }
        }

        // Второй проход: если ничего не найдено, проверяем любые строки
        for (line in lines) {
            val afterMatch = afterPattern.find(line)
            if (afterMatch != null && afterMatch.groupValues.size > 1) {
                val num = parseNumber(afterMatch.groupValues[1])
                if (num != null && num in 0.0..100.0) return num
            }

            val beforeMatch = beforePattern.find(line)
            if (beforeMatch != null && beforeMatch.groupValues.size > 1) {
                val num = parseNumber(beforeMatch.groupValues[1])
                if (num != null && num in 0.0..100.0) return num
            }
        }

        return null
    }
}
