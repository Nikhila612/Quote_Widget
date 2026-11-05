package io.github.nikhila612.quotewidget.core

/**
 * The daily check-in. Every answer is optional.
 * Must stay identical to ios/QuoteCore/Sources/QuoteCore/Mood.swift — both share the same test vectors.
 */
data class CheckIn(
    val feeling: Feeling? = null,
    val day: DayKind? = null,
    val wish: Wish? = null,
    val kind: QuoteKind? = null,
) {
    val isEmpty: Boolean get() = feeling == null && day == null && wish == null && kind == null
}

/** "What's your mood right now?" */
enum class Feeling(val tags: List<String>) {
    HAPPY(listOf("happiness", "gratitude")),
    CALM(listOf("calm", "gratitude")),
    TIRED(listOf("calm", "kindness")),
    WORRIED(listOf("calm", "courage")),
    SAD(listOf("kindness", "happiness", "courage")),
    MOTIVATED(listOf("growth", "courage"));

    val title: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

/** "How will your day be?" */
enum class DayKind(val tags: List<String>) {
    BUSY(listOf("calm", "growth")),
    RELAXED(listOf("happiness", "gratitude")),
    CHALLENGING(listOf("courage", "growth")),
    EXCITING(listOf("happiness", "courage"));

    val title: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

/** "How do you want today to feel?" */
enum class Wish(val tag: String) {
    PEACEFUL("calm"),
    CONFIDENT("courage"),
    JOYFUL("happiness"),
    PRODUCTIVE("growth"),
    GRATEFUL("gratitude"),
    KIND("kindness");

    val title: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

/** "What kind of quote today?" */
enum class QuoteKind(val tag: String?) {
    HAPPINESS("happiness"),
    COURAGE("courage"),
    CALM("calm"),
    GROWTH("growth"),
    KINDNESS("kindness"),
    GRATITUDE("gratitude"),
    SURPRISE(null);

    val title: String get() = if (this == SURPRISE) "Surprise me" else name.lowercase().replaceFirstChar { it.uppercase() }
}

data class MoodChoice(val index: Int, val consumed: Int)

object MoodPicker {
    /** The six quote tags, in a fixed order used to break ties and to salt each tag's shuffle. */
    val TAGS = listOf("happiness", "courage", "calm", "growth", "kindness", "gratitude")

    /** The one tag that best fits the answers, or null when there's nothing to go on. */
    fun tag(checkIn: CheckIn): String? {
        val score = mutableMapOf<String, Double>()
        fun add(tag: String, points: Double) { score[tag] = (score[tag] ?: 0.0) + points }
        checkIn.kind?.tag?.let { add(it, 3.0) }
        checkIn.wish?.let { add(it.tag, 2.0) }
        checkIn.feeling?.let { f -> f.tags.forEach { add(it, 2.0 / f.tags.size) } }
        checkIn.day?.let { d -> d.tags.forEach { add(it, 1.0 / d.tags.size) } }
        val best = score.values.maxOrNull() ?: return null
        if (best <= 0) return null
        val priority = listOfNotNull(checkIn.kind?.tag, checkIn.wish?.tag) +
            (checkIn.feeling?.tags ?: emptyList()) + (checkIn.day?.tags ?: emptyList()) + TAGS
        return priority.first { kotlin.math.abs((score[it] ?: 0.0) - best) < 1e-9 }
    }

    /**
     * Picks from [candidates] (library indices carrying the tag, in library order), walking the
     * tag's own shuffled order from [uses] and skipping anything in [avoid].
     */
    fun pick(candidates: List<Int>, tag: String, seed: ULong, uses: Int, avoid: Set<Int>): MoodChoice? {
        if (candidates.isEmpty()) return null
        val salt = (TAGS.indexOf(tag).coerceAtLeast(0) + 1).toULong() * 0x100000001B3uL
        val order = QuotePicker.shuffledOrder(candidates.size, seed xor salt)
        for (step in candidates.indices) {
            val index = candidates[order[(uses + step) % candidates.size]]
            if (index !in avoid) return MoodChoice(index, step + 1)
        }
        return MoodChoice(candidates[order[uses % candidates.size]], 1)
    }
}
