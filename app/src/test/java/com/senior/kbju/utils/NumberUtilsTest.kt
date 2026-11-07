package com.senior.kbju.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberUtilsTest {

    @Test
    fun testParseNumberValidVariants() {
        assertEquals(70.5, parseNumber("70.5")!!, 0.001)
        assertEquals(70.5, parseNumber("70,5")!!, 0.001)
        assertEquals(70.0, parseNumber(" 70 ")!!, 0.001)
        assertEquals(0.0, parseNumber("0")!!, 0.001)
        assertEquals(123.456, parseNumber("123,456")!!, 0.001)
    }

    @Test
    fun testParseNumberInvalidVariants() {
        assertNull(parseNumber(""))
        assertNull(parseNumber("   "))
        assertNull(parseNumber("abc"))
        assertNull(parseNumber("70..5"))
        assertNull(parseNumber("70,5,3"))
        assertNull(parseNumber("-"))
    }

    @Test
    fun testParseNumberIn() {
        assertEquals(65.0, parseNumberIn("65", min = 40.0, max = 150.0)!!, 0.001)
        assertEquals(40.0, parseNumberIn("40", min = 40.0, max = 150.0)!!, 0.001)
        assertEquals(150.0, parseNumberIn("150", min = 40.0, max = 150.0)!!, 0.001)

        // Меньше минимума или больше максимума
        assertNull(parseNumberIn("39.9", min = 40.0, max = 150.0))
        assertNull(parseNumberIn("150.1", min = 40.0, max = 150.0))
        assertNull(parseNumberIn("invalid", min = 40.0, max = 150.0))
    }

    @Test
    fun testSanitizeNumberInput() {
        // Обычные цифры
        assertEquals("123", sanitizeNumberInput("123"))

        // Точка и запятая
        assertEquals("12.3", sanitizeNumberInput("12.3"))
        assertEquals("12,3", sanitizeNumberInput("12,3"))

        // Повторный разделитель должен игнорироваться
        assertEquals("12.34", sanitizeNumberInput("12.3.4"))
        assertEquals("12,34", sanitizeNumberInput("12,3,4"))
        assertEquals("12.34", sanitizeNumberInput("12.3,4"))

        // Мусорные символы фильтруются
        assertEquals("705", sanitizeNumberInput("70кг5"))
        assertEquals("70.5", sanitizeNumberInput("70.5гр"))

        // Ограничение длины 10 символов
        assertEquals("1234567890", sanitizeNumberInput("1234567890123456"))
    }

    @Test
    fun testDoublePrettyRussianFormat() {
        assertEquals("70,5", 70.5.pretty())
        assertEquals("70", 70.0.pretty())
        assertEquals("0", 0.0.pretty())
        assertEquals("12,3", 12.34.pretty()) // формат 0.# округляет до одного знака
    }
}
