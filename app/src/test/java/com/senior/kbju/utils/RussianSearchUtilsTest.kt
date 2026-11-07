package com.senior.kbju.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RussianSearchUtilsTest {

    @Test
    fun testWordStemming() {
        assertEquals("ябло", RussianSearchUtils.stem("яблочный"))
        assertEquals("ябло", RussianSearchUtils.stem("яблоко"))
        assertEquals("ябло", RussianSearchUtils.stem("яблоки"))
        assertEquals("сок", RussianSearchUtils.stem("сок"))
        assertEquals("овсян", RussianSearchUtils.stem("овсяная"))
        assertEquals("овсян", RussianSearchUtils.stem("овсянка"))
        assertEquals("греч", RussianSearchUtils.stem("гречневая"))
        assertEquals("греч", RussianSearchUtils.stem("гречка"))
        assertEquals("творо", RussianSearchUtils.stem("творог"))
        assertEquals("творо", RussianSearchUtils.stem("творожный"))
        assertEquals("моло", RussianSearchUtils.stem("молоко"))
        assertEquals("моло", RussianSearchUtils.stem("молочный"))
        assertEquals("хлеб", RussianSearchUtils.stem("хлеб"))
        assertEquals("бород", RussianSearchUtils.stem("бородинский"))
        assertEquals("говя", RussianSearchUtils.stem("говядина"))
        assertEquals("говя", RussianSearchUtils.stem("говяжий"))
    }

    @Test
    fun testExtractTokens() {
        val tokens1 = RussianSearchUtils.extractTokens("яблочный сок")
        assertEquals(listOf("ябло", "сок"), tokens1)

        val tokens2 = RussianSearchUtils.extractTokens("сок яблочный")
        assertEquals(listOf("сок", "ябло"), tokens2)

        val tokens3 = RussianSearchUtils.extractTokens("каша гречневая на молоке")
        assertTrue(tokens3.contains("каш"))
        assertTrue(tokens3.contains("греч"))
        assertTrue(tokens3.contains("моло"))
    }
}
