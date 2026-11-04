package io.github.nikhila612.quotewidget.core

data class Quote(
    val id: String,
    val text: String,
    val author: String,
    val tags: List<String>,
)

/**
 * Deterministic daily pick. Must stay identical to
 * ios/QuoteCore/Sources/QuoteCore/QuotePicker.swift — both share the same test vectors.
 */
object QuotePicker {

    /** Index into the quote list for day [dayIndex] (days since install). No repeats within a cycle. */
    fun quoteIndex(dayIndex: Long, count: Int, seed: ULong): Int {
        require(count > 0) { "count must be positive" }
        val day = maxOf(0L, dayIndex)
        val cycle = (day / count).toULong()
        val position = (day % count).toInt()
        return shuffledOrder(count, seed + cycle)[position]
    }

    /** Fisher–Yates shuffle of 0 until [count] driven by SplitMix64. */
    fun shuffledOrder(count: Int, seed: ULong): IntArray {
        val order = IntArray(count) { it }
        val rng = SplitMix64(seed)
        for (i in count - 1 downTo 1) {
            val j = (rng.next() % (i + 1).toULong()).toInt()
            val tmp = order[i]
            order[i] = order[j]
            order[j] = tmp
        }
        return order
    }
}

class SplitMix64(private var state: ULong) {
    fun next(): ULong {
        state += 0x9E3779B97F4A7C15uL
        var z = state
        z = (z xor (z shr 30)) * 0xBF58476D1CE4E5B9uL
        z = (z xor (z shr 27)) * 0x94D049BB133111EBuL
        return z xor (z shr 31)
    }
}
