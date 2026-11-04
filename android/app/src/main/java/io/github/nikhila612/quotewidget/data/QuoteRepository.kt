package io.github.nikhila612.quotewidget.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.nikhila612.quotewidget.core.Quote
import io.github.nikhila612.quotewidget.core.QuotePicker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.time.LocalDate
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
        val dayIndex = date.toEpochDay() - installDay
        return quotes[QuotePicker.quoteIndex(dayIndex, quotes.size, seed.toULong())]
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

    private companion object {
        val SEED = longPreferencesKey("seed")
        val INSTALL_DAY = longPreferencesKey("install_epoch_day")
        val FAVOURITES = stringSetPreferencesKey("favourites")
        val THEME = stringPreferencesKey("theme")

        val FALLBACK = Quote(
            id = "fallback",
            text = "Every day may not be good, but there is something good in every day.",
            author = "Unknown",
            tags = listOf("happiness"),
        )

        fun loadQuotes(context: Context): List<Quote> = runCatching {
            val json = context.assets.open("quotes.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            List(array.length()) { i ->
                val item = array.getJSONObject(i)
                val tags = item.optJSONArray("tags")
                Quote(
                    id = item.getString("id"),
                    text = item.getString("text"),
                    author = item.getString("author"),
                    tags = List(tags?.length() ?: 0) { tags!!.getString(it) },
                )
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: listOf(FALLBACK)
    }
}
