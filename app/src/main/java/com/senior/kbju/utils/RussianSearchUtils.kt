package com.senior.kbju.utils

object RussianSearchUtils {

    private val ADJ_ENDINGS = listOf(
        "тельский", "тельская", "тельское", "тельские",
        "овский", "евский", "инский",
        "ский", "ская", "ское", "ские", "ского", "скому", "ским", "ских", "скую",
        "нного", "ными", "ным",
        "нный", "нная", "нное", "нные",
        "ный", "ная", "ное", "ные", "ного", "ному", "ным", "ных", "ную",
        "тий", "тая", "тое", "тые",
        "ший", "шая", "шее", "шие",
        "кий", "кая", "кое", "кие",
        "ий", "ый", "ой", "ая", "яя", "ое", "ее", "ые", "ие",
        "ого", "его", "ому", "ему", "ым", "им", "ом", "ем", "ых", "их", "ую", "юю"
    )

    private val NOUN_ENDINGS = listOf(
        "ками", "ами", "ями",
        "ком", "кой", "ков", "ках", "кам",
        "ка", "ки", "ку", "ке", "цы", "ца", "це", "цу", "цей",
        "ок", "ек", "ик",
        "ов", "ев", "ей", "ам", "ям", "ах", "ях",
        "ом", "ем",
        "а", "я", "о", "е", "у", "ю", "ы", "и", "ь"
    )

    fun stem(word: String): String {
        val lower = word.lowercase().trim()
        if (lower.length <= 3) return lower

        // Специальные частые корни с чередованиями (к/ч, г/ж)
        if (lower.startsWith("греч")) return "греч" // гречка, гречневая, гречневый
        if (lower.startsWith("овсян")) return "овсян" // овсянка, овсяная, овсяные
        if (lower.startsWith("ябло")) return "ябло" // яблоко, яблоки, яблочный, яблочное
        if (lower.startsWith("кур")) return "кур" // курица, куриное, куриный, куриная
        if (lower.startsWith("моло")) return "моло" // молоко, молочный, молочная
        if (lower.startsWith("творо")) return "творо" // творог, творожный, творожная
        if (lower.startsWith("говяж") || lower.startsWith("говяд")) return "говя" // говядина, говяжий
        if (lower.startsWith("свин")) return "свин" // свинина, свиной

        for (end in ADJ_ENDINGS) {
            if (lower.endsWith(end) && lower.length - end.length >= 3) {
                return lower.substring(0, lower.length - end.length)
            }
        }
        for (end in NOUN_ENDINGS) {
            if (lower.endsWith(end) && lower.length - end.length >= 3) {
                return lower.substring(0, lower.length - end.length)
            }
        }
        return lower
    }

    fun extractTokens(query: String): List<String> {
        return query.trim()
            .split(Regex("[\\s,;+]+"))
            .filter { it.isNotBlank() }
            .map { stem(it) }
            .filter { it.isNotBlank() }
    }
}
