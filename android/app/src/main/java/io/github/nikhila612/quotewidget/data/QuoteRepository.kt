package io.github.nikhila612.quotewidget.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.nikhila612.quotewidget.core.CheckIn
import io.github.nikhila612.quotewidget.core.DailyPick
import io.github.nikhila612.quotewidget.core.DayKind
import io.github.nikhila612.quotewidget.core.Feeling
import io.github.nikhila612.quotewidget.core.MoodPicker
import io.github.nikhila612.quotewidget.core.QuoteKind
import io.github.nikhila612.quotewidget.core.Wish
import io.github.nikhila612.quotewidget.core.Quote
import io.github.nikhila612.quotewidget.core.QuotePicker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

private val Context.dataStore by preferencesDataStore(name = "quote_prefs")

enum class WidgetTheme {
    SUNRISE, LIGHT, DARK, OCEAN, FOREST, LAVENDER, ROSE, SAND, MIDNIGHT,

    /** Android 12+: colours taken from the wallpaper (Material You). */
    WALLPAPER;

    val title: String get() = if (this == WALLPAPER) "Wallpaper" else name.lowercase().replaceFirstChar { it.uppercase() }
}

/** Quotes, install state, favourites and theme — shared by the app and the widget. */
class QuoteRepository(context: Context) {

    private val appContext = context.applicationContext
    private val store = appContext.dataStore

    val quotes: List<Quote> by lazy { loadQuotes(appContext) }

    suspend fun quoteFor(date: LocalDate = LocalDate.now()): Quote {
        moodPicks()[date.toString()]?.let { id -> quoteWithId(id)?.let { return it } }
        val (seed, installDay) = installState()
        val custom = customQuotes.first()
        val dayIndex = date.toEpochDay() - installDay
        return when (val pick = QuotePicker.dailyPick(dayIndex, quotes.size, custom.size, seed.toULong())) {
            is DailyPick.Library -> quotes[pick.index]
            is DailyPick.Custom -> custom[pick.index]
        }
    }

    // Daily check-in. Picks are stored per day (yyyy-MM-dd -> quote id) so History stays stable.

    /** Changes whenever today's quote could change: own quotes or a check-in. The widget watches it. */
    val pickInputs: Flow<List<String?>> = store.data.map { prefs ->
        listOf(prefs[CUSTOM_QUOTES], prefs[MOOD_PICKS], prefs[CHECKIN_DAY], prefs[CHECKIN_ANSWERS], prefs[PINNED_DAY])
    }

    /** True until the user checks in or skips today. */
    suspend fun needsCheckIn(date: LocalDate = LocalDate.now()): Boolean {
        val prefs = store.data.first()
        val key = date.toString()
        return prefs[CHECKIN_DAY] != key && prefs[SKIP_DAY] != key
    }

    suspend fun skipCheckIn(date: LocalDate = LocalDate.now()) {
        store.edit { it[SKIP_DAY] = date.toString() }
    }

    /** Today's answers, if the user has checked in today. */
    suspend fun checkInFor(date: LocalDate = LocalDate.now()): CheckIn? {
        val prefs = store.data.first()
        if (prefs[CHECKIN_DAY] != date.toString()) return null
        return prefs[CHECKIN_ANSWERS]?.let { runCatching { parseCheckIn(it) }.getOrNull() }
    }

    /** Records the answers and picks today's quote to match them. Returns today's quote. */
    suspend fun checkIn(answers: CheckIn, date: LocalDate = LocalDate.now()): Quote {
        val key = date.toString()
        store.edit { if (it[PINNED_DAY] == key) it.remove(PINNED_DAY) }
        val picks = moodPicks().toMutableMap().apply { remove(key) }
        val tag = MoodPicker.tag(answers)
        if (tag == null) {
            store.edit {
                it[CHECKIN_DAY] = key
                it[CHECKIN_ANSWERS] = checkInJson(answers)
                it[MOOD_PICKS] = JSONObject(picks as Map<*, *>).toString()
            }
            return quoteFor(date)
        }
        store.edit { it[MOOD_PICKS] = JSONObject(picks as Map<*, *>).toString() }
        val candidates = quotes.indices.filter { tag in quotes[it].tags }
        val avoid = (1L..30L).mapNotNull { quotes.indexOf(quoteFor(date.minusDays(it))).takeIf { i -> i >= 0 } }.toSet()
        val uses = tagUses()
        val (seed, _) = installState()
        val choice = MoodPicker.pick(candidates, tag, seed.toULong(), uses[tag] ?: 0, avoid) ?: return quoteFor(date)
        val oldest = date.minusDays(60).toString()
        picks[key] = quotes[choice.index].id
        store.edit {
            it[CHECKIN_DAY] = key
            it[CHECKIN_ANSWERS] = checkInJson(answers)
            it[TAG_USES] = JSONObject((uses + (tag to (uses[tag] ?: 0) + choice.consumed)) as Map<*, *>).toString()
            it[MOOD_PICKS] = JSONObject(picks.filterKeys { day -> day >= oldest } as Map<*, *>).toString()
        }
        return quotes[choice.index]
    }

    // Showing one of your own quotes today.

    /** Puts [quote] (usually one the user wrote) on today's widget. A later check-in replaces it. */
    suspend fun showToday(quote: Quote, date: LocalDate = LocalDate.now()) {
        val key = date.toString()
        val picks = moodPicks() + (key to quote.id)
        store.edit {
            it[MOOD_PICKS] = JSONObject(picks as Map<*, *>).toString()
            it[PINNED_DAY] = key
        }
    }

    /** The id of the quote the user chose to show today, if they did. */
    suspend fun shownTodayId(date: LocalDate = LocalDate.now()): String? {
        val key = date.toString()
        return if (store.data.first()[PINNED_DAY] == key) moodPicks()[key] else null
    }

    /** True when today's quote came from a check-in (not from the user choosing a quote). */
    suspend fun isMoodPick(date: LocalDate = LocalDate.now()): Boolean =
        checkInFor(date) != null && shownTodayId(date) == null

    private suspend fun moodPicks(): Map<String, String> =
        store.data.first()[MOOD_PICKS]?.let { json ->
            runCatching { JSONObject(json).let { o -> o.keys().asSequence().associateWith { k -> o.getString(k) } } }.getOrNull()
        } ?: emptyMap()

    private suspend fun tagUses(): Map<String, Int> =
        store.data.first()[TAG_USES]?.let { json ->
            runCatching { JSONObject(json).let { o -> o.keys().asSequence().associateWith { k -> o.getInt(k) } } }.getOrNull()
        } ?: emptyMap()

    private suspend fun quoteWithId(id: String): Quote? =
        quotes.firstOrNull { it.id == id } ?: customQuotes.first().firstOrNull { it.id == id }

    // The user's own quotes, stored on the device as a JSON array.

    val customQuotes: Flow<List<Quote>> = store.data.map { prefs ->
        prefs[CUSTOM_QUOTES]?.let { runCatching { parseQuotes(it) }.getOrNull() } ?: emptyList()
    }

    /** Adds a new quote, or updates the one with [id]. Returns null when the text is empty. */
    suspend fun saveCustomQuote(text: String, author: String, id: String? = null): Quote? {
        val cleanText = text.trim().take(MAX_CUSTOM_LENGTH)
        if (cleanText.isEmpty()) return null
        val quote = Quote(
            id = id ?: "u-${UUID.randomUUID()}",
            text = cleanText,
            author = author.trim().ifEmpty { DEFAULT_CUSTOM_AUTHOR },
            tags = listOf("mine"),
        )
        store.edit { prefs ->
            val all = prefs[CUSTOM_QUOTES]?.let { runCatching { parseQuotes(it) }.getOrNull() } ?: emptyList()
            val updated = if (all.any { it.id == quote.id }) all.map { if (it.id == quote.id) quote else it } else all + quote
            prefs[CUSTOM_QUOTES] = toJson(updated)
        }
        return quote
    }

    suspend fun deleteCustomQuote(id: String) {
        store.edit { prefs ->
            val all = prefs[CUSTOM_QUOTES]?.let { runCatching { parseQuotes(it) }.getOrNull() } ?: emptyList()
            prefs[CUSTOM_QUOTES] = toJson(all.filterNot { it.id == id })
            prefs[FAVOURITES] = (prefs[FAVOURITES] ?: emptySet()) - id
            prefs[MOOD_PICKS]?.let { json ->
                val o = JSONObject(json)
                o.keys().asSequence().toList().filter { o.getString(it) == id }.forEach { o.remove(it) }
                prefs[MOOD_PICKS] = o.toString()
            }
        }
    }

    /** Today first, then earlier days, never before the install day. */
    suspend fun history(days: Int = 30, today: LocalDate = LocalDate.now()): List<Pair<LocalDate, Quote>> {
        val (_, installDay) = installState()
        val available = minOf(days.toLong(), today.toEpochDay() - installDay + 1).coerceAtLeast(1)
        return (0 until available).map { offset ->
            val day = today.minusDays(offset)
            day to quoteFor(day)
        }
    }

    val favouriteIds: Flow<Set<String>> = store.data.map { it[FAVOURITES] ?: emptySet() }

    suspend fun toggleFavourite(id: String) {
        store.edit { prefs ->
            val current = prefs[FAVOURITES] ?: emptySet()
            prefs[FAVOURITES] = if (id in current) current - id else current + id
        }
    }

    val theme: Flow<WidgetTheme> = store.data.map { prefs ->
        prefs[THEME]?.let { name -> WidgetTheme.entries.firstOrNull { it.name == name } } ?: WidgetTheme.SUNRISE
    }

    suspend fun setTheme(theme: WidgetTheme) {
        store.edit { it[THEME] = theme.name }
    }

    suspend fun currentTheme(): WidgetTheme = theme.first()

    private suspend fun installState(): Pair<Long, Long> {
        val prefs = store.edit { prefs ->
            if (prefs[SEED] == null) prefs[SEED] = Random.nextLong()
            if (prefs[INSTALL_DAY] == null) prefs[INSTALL_DAY] = LocalDate.now().toEpochDay()
        }
        return prefs[SEED]!! to prefs[INSTALL_DAY]!!
    }

    companion object {
        const val MAX_CUSTOM_LENGTH = 160
        const val DEFAULT_CUSTOM_AUTHOR = "Me"

        private val SEED = longPreferencesKey("seed")
        private val INSTALL_DAY = longPreferencesKey("install_epoch_day")
        private val FAVOURITES = stringSetPreferencesKey("favourites")
        private val THEME = stringPreferencesKey("theme")
        private val CUSTOM_QUOTES = stringPreferencesKey("custom_quotes")
        private val MOOD_PICKS = stringPreferencesKey("mood_picks")
        private val TAG_USES = stringPreferencesKey("tag_uses")
        private val CHECKIN_DAY = stringPreferencesKey("checkin_day")
        private val CHECKIN_ANSWERS = stringPreferencesKey("checkin_answers")
        private val SKIP_DAY = stringPreferencesKey("skip_day")
        private val PINNED_DAY = stringPreferencesKey("pinned_day")

        private fun checkInJson(c: CheckIn): String = JSONObject().apply {
            c.feeling?.let { put("feeling", it.name) }
            c.day?.let { put("day", it.name) }
            c.wish?.let { put("wish", it.name) }
            c.kind?.let { put("kind", it.name) }
        }.toString()

        private fun parseCheckIn(json: String): CheckIn {
            val o = JSONObject(json)
            return CheckIn(
                feeling = o.optString("feeling").takeIf { it.isNotEmpty() }?.let { Feeling.valueOf(it) },
                day = o.optString("day").takeIf { it.isNotEmpty() }?.let { DayKind.valueOf(it) },
                wish = o.optString("wish").takeIf { it.isNotEmpty() }?.let { Wish.valueOf(it) },
                kind = o.optString("kind").takeIf { it.isNotEmpty() }?.let { QuoteKind.valueOf(it) },
            )
        }

        private fun toJson(quotes: List<Quote>): String = JSONArray().apply {
            quotes.forEach { q ->
                put(JSONObject().put("id", q.id).put("text", q.text).put("author", q.author).put("tags", JSONArray(q.tags)))
            }
        }.toString()

        private fun parseQuotes(json: String): List<Quote> {
            val array = JSONArray(json)
            return List(array.length()) { i ->
                val item = array.getJSONObject(i)
                val tags = item.optJSONArray("tags")
                Quote(
                    id = item.getString("id"),
                    text = item.getString("text"),
                    author = item.getString("author"),
                    tags = List(tags?.length() ?: 0) { tags!!.getString(it) },
                )
            }
        }

        private val FALLBACK = Quote(
            id = "fallback",
            text = "Every day may not be good, but there is something good in every day.",
            author = "Unknown",
            tags = listOf("happiness"),
        )

        private fun loadQuotes(context: Context): List<Quote> = runCatching {
            parseQuotes(context.assets.open("quotes.json").bufferedReader().use { it.readText() })
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: listOf(FALLBACK)
    }
}
