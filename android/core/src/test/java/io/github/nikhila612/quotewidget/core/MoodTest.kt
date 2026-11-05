package io.github.nikhila612.quotewidget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoodTest {

    // Shared vectors: ios/QuoteCore/Tests/QuoteCoreTests/MoodTests.swift asserts the same values.
    @Test
    fun tagMatchesSharedVectors() {
        assertEquals("calm", MoodPicker.tag(CheckIn(feeling = Feeling.WORRIED)))
        assertEquals("calm", MoodPicker.tag(CheckIn(feeling = Feeling.TIRED, day = DayKind.BUSY)))
        assertEquals("growth", MoodPicker.tag(CheckIn(feeling = Feeling.SAD, day = DayKind.EXCITING, wish = Wish.PRODUCTIVE)))
        assertEquals("gratitude", MoodPicker.tag(CheckIn(feeling = Feeling.MOTIVATED, kind = QuoteKind.GRATITUDE)))
        assertNull(MoodPicker.tag(CheckIn(kind = QuoteKind.SURPRISE)))
        assertEquals("courage", MoodPicker.tag(CheckIn(day = DayKind.CHALLENGING)))
        assertNull(MoodPicker.tag(CheckIn()))
    }

    @Test
    fun pickMatchesSharedVectors() {
        val candidates = listOf(3, 8, 11, 15, 21, 30, 42)
        assertEquals(42, MoodPicker.pick(candidates, "calm", 42uL, 0, emptySet())?.index)
        assertEquals(30, MoodPicker.pick(candidates, "calm", 42uL, 2, emptySet())?.index)
        assertEquals(MoodChoice(3, 2), MoodPicker.pick(candidates, "calm", 42uL, 0, setOf(42)))
        assertEquals(11, MoodPicker.pick(candidates, "courage", 42uL, 0, emptySet())?.index)
    }
}
