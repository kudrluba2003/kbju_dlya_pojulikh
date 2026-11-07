package com.senior.kbju.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DatabaseSeederTest {

    @Test
    fun testParseProductsSeedJson() {
        val seedFile = File("src/main/assets/products_seed.json")
        assertTrue("products_seed.json must exist in assets", seedFile.exists())

        val json = seedFile.readText(Charsets.UTF_8)
        val foods = DatabaseSeeder.parseProductsJson(json)

        assertTrue("Parsed foods count should be >= 1200, got: ${foods.size}", foods.size >= 1200)

        // Check for specific staple items
        val appleJuice = foods.find { it.name.contains("яблочный сок", ignoreCase = true) || it.name.contains("сок яблочный", ignoreCase = true) }
        assertNotNull("Should contain apple juice variation", appleJuice)

        // Ensure all items have non-negative calories
        for (f in foods) {
            assertTrue("Calories must be >= 0 for ${f.name}", f.kcal100 >= 0.0)
            assertTrue("Protein must be >= 0 for ${f.name}", f.protein100 >= 0.0)
            assertTrue("Fat must be >= 0 for ${f.name}", f.fat100 >= 0.0)
            assertTrue("Carbs must be >= 0 for ${f.name}", f.carbs100 >= 0.0)
        }
    }
}
