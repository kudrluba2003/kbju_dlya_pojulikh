package com.senior.kbju.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionOcrParserTest {

    @Test
    fun testStandardRussianLabel() {
        val ocr = """
            Пищевая ценность в 100 г продукта:
            белки — 3,0 г
            жиры — 3,2 г
            углеводы — 4,7 г
            Энергетическая ценность (калорийность): 250 кДж / 60 ккал
        """.trimIndent()

        val parsed = NutritionOcrParser.parse(ocr)
        assertTrue(parsed.hasAnyData)
        assertEquals(60.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(3.0, parsed.protein ?: 0.0, 0.01)
        assertEquals(3.2, parsed.fat ?: 0.0, 0.01)
        assertEquals(4.7, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testLabelWithKjBeforeKcal() {
        val ocr = """
            100g:
            Энергетическая ценность: 1420 кДж / 340 ккал
            Белки: 12.5 г
            Жиры: 2.0 г
            Углеводы: 68 г
        """.trimIndent()

        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(340.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(12.5, parsed.protein ?: 0.0, 0.01)
        assertEquals(2.0, parsed.fat ?: 0.0, 0.01)
        assertEquals(68.0, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testMultilineNumberValue() {
        // Когда OCR разбил метку и значение на разные строки
        val ocr = """
            Белки
            15,4 г
            Жиры
            0,8 г
            Углеводы
            72,0 г
            Калорийность: 356 ккал
        """.trimIndent()

        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(356.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(15.4, parsed.protein ?: 0.0, 0.01)
        assertEquals(0.8, parsed.fat ?: 0.0, 0.01)
        assertEquals(72.0, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testEnglishLabel() {
        val ocr = """
            Nutrition Facts per 100g:
            Calories 210 kcal
            Protein: 7.5g
            Fat: 9.1g
            Carbohydrates: 24.3g
        """.trimIndent()

        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(210.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(7.5, parsed.protein ?: 0.0, 0.01)
        assertEquals(9.1, parsed.fat ?: 0.0, 0.01)
        assertEquals(24.3, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testAllUppercaseNoSpaces() {
        val ocr = """
            СОСТАВ И ПИЩЕВАЯ ЦЕННОСТЬ:
            БЕЛКИ-8.2Г
            ЖИРЫ-14Г
            УГЛЕВОДЫ-52.6Г
            КАЛОРИЙНОСТЬ: 370ККАЛ
        """.trimIndent()

        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(370.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(8.2, parsed.protein ?: 0.0, 0.01)
        assertEquals(14.0, parsed.fat ?: 0.0, 0.01)
        assertEquals(52.6, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testEmptyOrUnrelatedText() {
        val ocr = "Срок годности 12 месяцев. Хранить в сухом месте."
        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(null, parsed.kcal)
        assertEquals(null, parsed.protein)
        assertEquals(null, parsed.fat)
        assertEquals(null, parsed.carbs)
    }

    @Test
    fun testLabelWith100gDoesNotConfuseCalories() {
        val ocr = """
            Пищевая ценность:
            Энергетическая ценность на 100 г продукта: 380 ккал
            Белки: 10.0 г
            Жиры: 15.0 г
            Углеводы: 50.0 г
        """.trimIndent()

        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(380.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(10.0, parsed.protein ?: 0.0, 0.01)
        assertEquals(15.0, parsed.fat ?: 0.0, 0.01)
        assertEquals(50.0, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testTableFormat() {
        val ocr = """
            Пищевая ценность (на 100г):
            Белки | Жиры | Углеводы
            7.5 | 3.2 | 45.0
            Энергетическая ценность: 240 ккал
        """.trimIndent()

        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(240.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(7.5, parsed.protein ?: 0.0, 0.01)
        assertEquals(3.2, parsed.fat ?: 0.0, 0.01)
        assertEquals(45.0, parsed.carbs ?: 0.0, 0.01)
    }
}
