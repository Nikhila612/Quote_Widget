package io.github.nikhila612.quotewidget.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class QuotePickerTest {

    // Shared vectors: ios/QuoteCore/Tests/QuoteCoreTests/QuotePickerTests.swift asserts the same values.
    @Test
    fun shuffleMatchesSharedVectors() {
        assertArrayEquals(intArrayOf(0, 9, 5, 8, 6, 4, 7, 2, 1, 3), QuotePicker.shuffledOrder(10, 42uL))
        assertArrayEquals(intArrayOf(4, 2, 5, 6, 1, 3, 9, 8, 7, 0), QuotePicker.shuffledOrder(10, 43uL))
        assertArrayEquals(intArrayOf(6, 3, 2, 9, 8, 1, 4, 7, 0, 5), QuotePicker.shuffledOrder(10, 0uL))
    }

    @Test
    fun noRepeatWithinACycle() {
        val count = 36
        for (cycle in 0 until 3) {
            val picks = (0 until count).map { QuotePicker.quoteIndex((cycle * count + it).toLong(), count, 7uL) }
            assertEquals("cycle $cycle repeated a quote", count, picks.toSet().size)
        }
    }

    @Test
    fun secondCycleUsesNextSeed() {
        assertEquals(4, QuotePicker.quoteIndex(10, 10, 42uL))
    }

    // Shared vectors: iOS testDailyPickMatchesSharedVectors asserts the same values.
    @Test
    fun dailyPickMatchesSharedVectors() {
        fun pick(day: Long, custom: Int = 3) = QuotePicker.dailyPick(day, 10, custom, 42uL)
        assertEquals(DailyPick.Library(4), pick(5))
        assertEquals(DailyPick.Custom(0), pick(6))
        assertEquals(DailyPick.Custom(1), pick(13))
        assertEquals(DailyPick.Custom(2), pick(20))
        assertEquals(DailyPick.Custom(0), pick(27))
        assertEquals(DailyPick.Library(7), pick(6, custom = 0))
    }

    @Test
    fun negativeDayClampsToZero() {
        assertEquals(QuotePicker.quoteIndex(0, 10, 42uL), QuotePicker.quoteIndex(-5, 10, 42uL))
    }
}
