package io.github.nikhila612.quotewidget.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.nikhila612.quotewidget.core.DailyPick
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

enum class WidgetTheme { SUNRISE, LIGHT, DARK }

/** Quotes, install state, favourites and theme — shared by the app and the widget. */
class QuoteRepository(context: Context) {

    private val appContext = context.applicationContext
    private val store = appContext.dataStore

    val quotes: List<Quote> by lazy { loadQuotes(appContext) }

    suspend fun quoteFor(date: LocalDate = LocalDate.now()): Quote {
        val (seed, installDay) = installState()
        val custom = customQuotes.first()
        val dayIndex = date.toEpochDay() - installDay
        return when (val pick = QuotePicker.dailyPick(dayIndex, quotes.size, custom.size, seed.toULong())) {
            is DailyPick.Library -> quotes[pick.index]
            is DailyPick.Custom -> custom[pick.index]
        }
    }

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
