package io.github.nikhila612.quotewidget.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import io.github.nikhila612.quotewidget.core.Quote
import io.github.nikhila612.quotewidget.data.QuoteRepository
import io.github.nikhila612.quotewidget.data.WidgetTheme
import io.github.nikhila612.quotewidget.widget.QuoteGlanceWidget
import io.github.nikhila612.quotewidget.widget.ink
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.os.Build

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = QuoteRepository(this)
        setContent { QuoteAppTheme { QuoteApp(repository) } }
    }
}

@Composable
private fun QuoteAppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Today("Today", Icons.Filled.Home),
    MyQuotes("My Quotes", Icons.Filled.Create),
    Favourites("Favourites", Icons.Filled.Favorite),
    History("History", Icons.Filled.DateRange),
    Settings("Settings", Icons.Filled.Settings),
}

@Composable
private fun QuoteApp(repository: QuoteRepository) {
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val favourites by repository.favouriteIds.collectAsState(initial = emptySet())
    val theme by repository.theme.collectAsState(initial = WidgetTheme.SUNRISE)
    val customQuotes by repository.customQuotes.collectAsState(initial = emptyList())
    // Recomputed when the user's own quotes change: today may be one of theirs.
    val today by produceState<Quote?>(initialValue = null, customQuotes) { value = repository.quoteFor() }

    val toggle: (Quote) -> Unit = { quote -> scope.launch { repository.toggleFavourite(quote.id) } }
    val customChanged: suspend () -> Unit = { QuoteGlanceWidget().updateAll(context) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.Today -> today?.let { TodayScreen(it, theme, it.id in favourites, toggle) }
                Tab.MyQuotes -> MyQuotesScreen(
                    quotes = customQuotes,
                    onSave = { text, author, existing ->
                        scope.launch {
                            repository.saveCustomQuote(text, author, existing?.id)
                            customChanged()
                        }
                    },
                    onDelete = { quote ->
                        scope.launch {
                            repository.deleteCustomQuote(quote.id)
                            customChanged()
                        }
                    },
                )
                Tab.Favourites -> QuoteListScreen(
                    title = "Favourites",
                    items = (customQuotes + repository.quotes).filter { it.id in favourites }.map { null to it },
                    empty = "Tap the heart on a quote to save it here.",
                )
                Tab.History -> {
                    val history by produceState(initialValue = emptyList<Pair<LocalDate, Quote>>()) {
                        value = repository.history()
                    }
                    QuoteListScreen("History", history.map { it.first to it.second }, "No quotes yet.")
                }
                Tab.Settings -> SettingsScreen(theme) { newTheme ->
                    scope.launch {
                        repository.setTheme(newTheme)
                        QuoteGlanceWidget().updateAll(context)
                    }
                }
            }
        }
    }
}

private val WidgetTheme.brush: Brush
    get() = when (this) {
        WidgetTheme.SUNRISE -> Brush.linearGradient(listOf(Color(0xFFFFCCAB), Color(0xFFFA9E9E), Color(0xFFC2A8E8)))
        WidgetTheme.LIGHT -> Brush.verticalGradient(listOf(Color(0xFFFCFCFC), Color(0xFFF2F2F2)))
        WidgetTheme.DARK -> Brush.verticalGradient(listOf(Color(0xFF1F1F2E), Color(0xFF12121A)))
    }

@Composable
private fun TodayScreen(quote: Quote, theme: WidgetTheme, isFavourite: Boolean, onToggle: (Quote) -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().background(theme.brush).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, androidx.compose.ui.Alignment.CenterVertically),
    ) {
        Text(
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
            color = theme.ink.copy(alpha = 0.7f),
            style = MaterialTheme.typography.titleSmall,
        )
        AnimatedContent(targetState = quote, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "quote") { q ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "“${q.text}”",
                    color = theme.ink,
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
                )
                Text("— ${q.author}", color = theme.ink, fontWeight = FontWeight.SemiBold)
            }
        }
        Row {
            IconButton(onClick = { onToggle(quote) }) {
                Icon(
                    if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavourite) "Remove from favourites" else "Add to favourites",
                    tint = theme.ink,
                )
            }
            IconButton(onClick = {
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "“${quote.text}” — ${quote.author}")
                context.startActivity(Intent.createChooser(send, "Share quote"))
            }) {
                Icon(Icons.Filled.Share, contentDescription = "Share quote", tint = theme.ink)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuoteListScreen(title: String, items: List<Pair<LocalDate?, Quote>>, empty: String) {
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEE, d MMM") }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(title) })
        if (items.isEmpty()) {
            Text(empty, Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn {
                items(items, key = { "${it.first}-${it.second.id}" }) { (date, quote) ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        date?.let {
                            Text(it.format(dateFormat), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(quote.text, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif))
                        Text("— ${quote.author}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(theme: WidgetTheme, onThemeChange: (WidgetTheme) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings") })
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Widget style", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                WidgetTheme.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == theme,
                        onClick = { onThemeChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, WidgetTheme.entries.size),
                    ) { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }
                }
            }
            Text(
                "A new quote appears every day at midnight. To add the widget, touch and hold your home screen, tap Widgets, then find Sunnote.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
