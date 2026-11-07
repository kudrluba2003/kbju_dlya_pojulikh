package com.senior.kbju.data.backup

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializationTest {

    @Test
    fun testJsonStructureRoundTrip() {
        val json = JSONObject().apply {
            put("version", 1)
            put("app", "SeniorKbju")
            put("exportedAt", "2026-11-07T14:00:00Z")

            put("profile", JSONObject().apply {
                put("heightCm", 168)
                put("weightKg", 72.5)
                put("age", 67)
                put("gender", "FEMALE")
                put("activityFactor", 1.2)
                put("goalPercent", -10)
            })

            put("meals", JSONArray().apply {
                put(JSONObject().apply {
                    put("day", 20000L)
                    put("name", "Гречка с маслом")
                    put("grams", 200.0)
                    put("kcal", 250.0)
                    put("protein", 8.0)
                    put("fat", 5.0)
                    put("carbs", 42.0)
                    put("mealType", "BREAKFAST")
                })
            })

            put("weights", JSONArray().apply {
                put(JSONObject().apply {
                    put("day", 20000L)
                    put("weightKg", 72.5)
                    put("createdAt", 1700000000000L)
                })
            })

            put("waters", JSONArray().apply {
                put(JSONObject().apply {
                    put("day", 20000L)
                    put("glasses", 5)
                    put("ml", 1250)
                    put("createdAt", 1700000000000L)
                })
            })

            put("customFoods", JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "Пирог с капустой")
                    put("kcal100", 180.0)
                    put("protein100", 4.5)
                    put("fat100", 6.0)
                    put("carbs100", 27.0)
                })
            })
        }

        val jsonStr = json.toString(2)
        assertNotNull(jsonStr)

        val parsed = JSONObject(jsonStr)
        assertEquals(1, parsed.getInt("version"))
        assertEquals("SeniorKbju", parsed.getString("app"))

        val profile = parsed.getJSONObject("profile")
        assertEquals(168, profile.getInt("heightCm"))
        assertEquals(72.5, profile.getDouble("weightKg"), 0.01)
        assertEquals("FEMALE", profile.getString("gender"))

        val meals = parsed.getJSONArray("meals")
        assertEquals(1, meals.length())
        val meal0 = meals.getJSONObject(0)
        assertEquals("Гречка с маслом", meal0.getString("name"))
        assertEquals("BREAKFAST", meal0.getString("mealType"))

        val waters = parsed.getJSONArray("waters")
        assertEquals(1, waters.length())
        assertEquals(5, waters.getJSONObject(0).getInt("glasses"))

        val foods = parsed.getJSONArray("customFoods")
        assertEquals(1, foods.length())
        assertEquals("Пирог с капустой", foods.getJSONObject(0).getString("name"))
    }
}
