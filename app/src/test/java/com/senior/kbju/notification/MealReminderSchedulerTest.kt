package com.senior.kbju.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class MealReminderSchedulerTest {

    @Test
    fun testDelayWhenTargetIsLaterToday() {
        // Сейчас 10:00, цель 13:00 -> задержка ровно 3 часа
        val now = LocalDateTime.of(2026, 11, 7, 10, 0, 0)
        val delay = MealReminderScheduler.calculateDelayToNext(13, 0, now)
        assertEquals(TimeUnit.HOURS.toMillis(3), delay)
    }

    @Test
    fun testDelayWhenTargetHasPassedToday() {
        // Сейчас 14:00, цель 13:00 -> задержка до 13:00 следующего дня (23 часа)
        val now = LocalDateTime.of(2026, 11, 7, 14, 0, 0)
        val delay = MealReminderScheduler.calculateDelayToNext(13, 0, now)
        assertEquals(TimeUnit.HOURS.toMillis(23), delay)
    }

    @Test
    fun testDelayWhenCurrentTimeIsExactTarget() {
        // Сейчас ровно 13:00:00 -> должно перенестись на 24 часа вперед
        val now = LocalDateTime.of(2026, 11, 7, 13, 0, 0)
        val delay = MealReminderScheduler.calculateDelayToNext(13, 0, now)
        assertEquals(TimeUnit.HOURS.toMillis(24), delay)
    }

    @Test
    fun testDelayRangeIsValid() {
        val now = LocalDateTime.now()
        val delayLunch = MealReminderScheduler.calculateDelayToNext(13, 0, now)
        val delayDinner = MealReminderScheduler.calculateDelayToNext(19, 0, now)

        assertTrue(delayLunch > 0)
        assertTrue(delayLunch <= TimeUnit.HOURS.toMillis(24))

        assertTrue(delayDinner > 0)
        assertTrue(delayDinner <= TimeUnit.HOURS.toMillis(24))
    }
}
