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

    @Test
    fun testRealRussianDairyPackage() {
        val ocr = """
            Молоко питьевое пастеризованное 3,2%
            Пищевая ценность 100 г продукта:
            жир - 3,2 г, белок - 2,8 г, углеводы - 4,7 г.
            Энергетическая ценность (калорийность):
            240 кДж (60 ккал).
        """.trimIndent()

        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(60.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(2.8, parsed.protein ?: 0.0, 0.01)
        assertEquals(3.2, parsed.fat ?: 0.0, 0.01)
        assertEquals(4.7, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testShortNotationWithColonsAndSemicolons() {
        val ocr = "Б: 7,5г; Ж: 12,0г; У: 55,2г; 360ккал"
        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(360.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(7.5, parsed.protein ?: 0.0, 0.01)
        assertEquals(12.0, parsed.fat ?: 0.0, 0.01)
        assertEquals(55.2, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testNumberBeforeNutrientName() {
        val ocr = """
            В 100 г продукта содержится:
            8.5 г белка, 14.0 г жира, 52.0 г углеводов.
            Энергетическая ценность 370 ккал.
        """.trimIndent()
        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(370.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(8.5, parsed.protein ?: 0.0, 0.01)
        assertEquals(14.0, parsed.fat ?: 0.0, 0.01)
        assertEquals(52.0, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testOcrLetterOInNumbers() {
        val ocr = "белки: 7,O г; жиры: 1O г; углеводы: 5O г; 35O ккал"
        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(350.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(7.0, parsed.protein ?: 0.0, 0.01)
        assertEquals(10.0, parsed.fat ?: 0.0, 0.01)
        assertEquals(50.0, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testUserPhoto1_EasternWings() {
        val ocr = """
            КРЫЛЫШКИ ПО-ВОСТОЧНОМУ УПАКОВАННЫЕ
            Состав: крылышки куриные, соус соевый (вода, соя, пшеница, соль, консервант (бензоат натрия)), масло растительное, соус чили (вода, томатная паста, сахар, соль, перец чили, чеснок, регулятор кислотности, стабилизатор, консервант), чеснок, сахар, перец. Эн.цен:343.8ккал/439кДж. Пищ.цен:Б-31,8г,Ж-19,2г,У-11,0г Изм.№5, ТУ921-001-49776166-06.
        """.trimIndent()
        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(343.8, parsed.kcal ?: 0.0, 0.01)
        assertEquals(31.8, parsed.protein ?: 0.0, 0.01)
        assertEquals(19.2, parsed.fat ?: 0.0, 0.01)
        assertEquals(11.0, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testUserPhoto2_MaheevKetchup() {
        val ocr = """
            RU КЕТЧУП «ТОМАТНЫЙ». ПЕРВАЯ КАТЕГОРИЯ. ГОСТ 32063-2013.
            Энергетическая ценность 100 г продукта: 366 кДж/86 ккал.
            Пищевая ценность (средние значения) 100 г продукта: углеводы - 20,2 г, белки - 1,3 г.
        """.trimIndent()
        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(86.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(1.3, parsed.protein ?: 0.0, 0.01)
        assertEquals(20.2, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testUserPhoto3_SparShashlik() {
        val ocr = """
            ШАШЛЫК ИЗ ОКОРОКА В МАРИНАДЕ НА МАНГАЛЕ
            Состав: свинина,паприка,морковь,лук,черный перец,имбирь,семена горчицы,соль. ТУ 9214-001-49776166-06. Эн.цен: 174ккал/728кДж. Пищ.цен: б-15,3г,ж-11,7г,у-2,9г. Срок годности при t +65C-3ч.
        """.trimIndent()
        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(174.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(15.3, parsed.protein ?: 0.0, 0.01)
        assertEquals(11.7, parsed.fat ?: 0.0, 0.01)
        assertEquals(2.9, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testUserPhoto4_BabyfoxChocolate() {
        val ocr = """
            RU МОЛОЧНЫЙ ШОКОЛАД BABYFOX С МОЛОЧНОЙ НАЧИНКОЙ.
            Пищевая ценность в 100 г продукта (средние значения): белки - 7,5 г; жиры - 38 г (в том числе насыщенные жирные кислоты - 22,3 %, трансизомеры ненасыщенных жирных кислот - 0,34 %); углеводы - 49 г (в том числе сахара - 48,7 г); соль - 0,39 г. Энергетическая ценность/калорийность - 2370 кДж/570 ккал.
        """.trimIndent()
        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(570.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(7.5, parsed.protein ?: 0.0, 0.01)
        assertEquals(38.0, parsed.fat ?: 0.0, 0.01)
        assertEquals(49.0, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testUserPhoto1_EasternWings_OcrArtifacts() {
        // Проверяем устойчивость к оцифровке 'Б' как '6' и латинских символов
        val ocr = """
            КРЫЛЫШКИ ПО-ВОСТОЧНОМУ
            Эн.цен:343.8 / 439кДж.
            Пищ.цен: 6-31.8g, x-19.2g, y-11.0g
        """.trimIndent()
        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(343.8, parsed.kcal ?: 0.0, 0.01)
        assertEquals(31.8, parsed.protein ?: 0.0, 0.01)
        assertEquals(19.2, parsed.fat ?: 0.0, 0.01)
        assertEquals(11.0, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testUserPhoto3_SparShashlik_MultilineAndLatin() {
        // Разрыв строк между Эн.цен и значениями, латиница b, x, y
        val ocr = """
            ШАШЛЫК ИЗ ОКОРОКА В МАРИНАДЕ НА МАНГАЛЕ
            ТУ 9214-001-49776166-06. Эн.цен:
            174ккал/728кДж.
            Пищ.цен: b-15,3г, x-11,7г, y-2,9г.
        """.trimIndent()
        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(174.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(15.3, parsed.protein ?: 0.0, 0.01)
        assertEquals(11.7, parsed.fat ?: 0.0, 0.01)
        assertEquals(2.9, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testUserPhoto2_MaheevKetchup_NoFatOrder() {
        val ocr = """
            КЕТЧУП ТОМАТНЫЙ
            Энергетическая ценность 100 г продукта: 366 кДж/86 ккал.
            Пищевая ценность: углеводы - 20,2 г, белки - 1,3 г.
        """.trimIndent()
        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(86.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(1.3, parsed.protein ?: 0.0, 0.01)
        assertEquals(null, parsed.fat)
        assertEquals(20.2, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testGostFormatCommaGramsColon() {
        // Стандартный формат ГОСТ РФ: «Белки, г:», «Жиры, г:», «Углеводы, г:»
        val ocr = """
            ГОСТ 31450-2013
            Пищевая ценность 100 г:
            Белки, г: 3,0
            Жиры, г: 3,2
            Углеводы, г: 4,7
            Энергетическая ценность, ккал: 59,0
        """.trimIndent()
        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(59.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(3.0, parsed.protein ?: 0.0, 0.01)
        assertEquals(3.2, parsed.fat ?: 0.0, 0.01)
        assertEquals(4.7, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testParenthesesAndSlashesUnits() {
        val ocr = """
            ПИЩЕВАЯ ЦЕННОСТЬ В 100Г:
            Белки (г) - 8.5
            Жиры (г) - 1.2
            Углеводы (г) - 72.0
            Энергетическая ценность, ккал/100г: 330
        """.trimIndent()
        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(330.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(8.5, parsed.protein ?: 0.0, 0.01)
        assertEquals(1.2, parsed.fat ?: 0.0, 0.01)
        assertEquals(72.0, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testKcalWithParenthesesAndNoKjConfusion() {
        val ocr = """
            Калорийность (ккал): 250
            Белки, г/100г: 12.0
            Жиры, г/100г: 4.5
            Углеводы, г/100г: 58.0
        """.trimIndent()
        val parsed = NutritionOcrParser.parse(ocr)
        assertEquals(250.0, parsed.kcal ?: 0.0, 0.01)
        assertEquals(12.0, parsed.protein ?: 0.0, 0.01)
        assertEquals(4.5, parsed.fat ?: 0.0, 0.01)
        assertEquals(58.0, parsed.carbs ?: 0.0, 0.01)
    }

    @Test
    fun testParsedNutritionMerge() {
        val p1 = ParsedNutrition(kcal = 250.0, protein = null, fat = 3.0, carbs = null)
        val p2 = ParsedNutrition(kcal = null, protein = 14.0, fat = 3.2, carbs = 48.0)
        val merged = p1.mergeWith(p2)

        assertEquals(250.0, merged.kcal ?: 0.0, 0.01)
        assertEquals(14.0, merged.protein ?: 0.0, 0.01)
        assertEquals(3.0, merged.fat ?: 0.0, 0.01) // Первое сохранено
        assertEquals(48.0, merged.carbs ?: 0.0, 0.01)
        assertTrue(merged.isComplete)
    }
}
