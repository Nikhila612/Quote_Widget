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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.LaunchedEffect
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
import io.github.nikhila612.quotewidget.core.CheckIn
import io.github.nikhila612.quotewidget.core.MoodPicker
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
    private val resumes = androidx.compose.runtime.mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = QuoteRepository(this)
        setContent { QuoteAppTheme { QuoteApp(repository, resumes.intValue) } }
    }

    override fun onResume() {
        super.onResume()
        resumes.intValue++
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
private fun QuoteApp(repository: QuoteRepository, resumes: Int) {
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val favourites by repository.favouriteIds.collectAsState(initial = emptySet())
    val theme by repository.theme.collectAsState(initial = WidgetTheme.SUNRISE)
    val customQuotes by repository.customQuotes.collectAsState(initial = emptyList())
    val pickInputs by repository.pickInputs.collectAsState(initial = null)
    // Recomputed when own quotes or a check-in change today's quote.
    val today by produceState<Quote?>(initialValue = null, pickInputs, resumes) { value = repository.quoteFor() }
    val checkIn by produceState<CheckIn?>(initialValue = null, pickInputs, resumes) { value = repository.checkInFor() }
    val shownTodayId by produceState<String?>(initialValue = null, pickInputs, resumes) { value = repository.shownTodayId() }
    val isMoodPick by produceState(initialValue = false, pickInputs, resumes) { value = repository.isMoodPick() }
    var showCheckIn by rememberSaveable { mutableStateOf(false) }
    // Ask once a day, when the app comes to the front.
    LaunchedEffect(resumes) { if (repository.needsCheckIn()) showCheckIn = true }

    if (showCheckIn) {
        CheckInSheet(
            initial = checkIn ?: CheckIn(),
            onSubmit = { answers ->
                showCheckIn = false
                scope.launch {
                    repository.checkIn(answers)
                    QuoteGlanceWidget().updateAll(context)
                }
            },
            onSkip = {
                showCheckIn = false
                scope.launch { repository.skipCheckIn() }
            },
        )
    }

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
                Tab.Today -> today?.let {
                    TodayScreen(it, theme, it.id in favourites, toggle, checkIn?.takeIf { isMoodPick }?.let { c -> MoodPicker.tag(c) }) { showCheckIn = true }
                }
                Tab.MyQuotes -> MyQuotesScreen(
                    quotes = customQuotes,
                    shownTodayId = shownTodayId,
                    onSave = { text, author, existing, showToday ->
                        scope.launch {
                            val saved = repository.saveCustomQuote(text, author, existing?.id)
                            if (saved != null && (showToday || saved.id == shownTodayId)) repository.showToday(saved)
                            customChanged()
                        }
                    },
                    onShowToday = { quote ->
                        scope.launch {
                            repository.showToday(quote)
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

/** Background for the Today screen in each style; Wallpaper uses the dynamic colour scheme. */
@Composable
fun WidgetTheme.appBrush(): Brush = when (this) {
    WidgetTheme.SUNRISE -> Brush.linearGradient(listOf(Color(0xFFFFCCAB), Color(0xFFFA9E9E), Color(0xFFC2A8E8)))
    WidgetTheme.LIGHT -> Brush.verticalGradient(listOf(Color(0xFFFCFCFC), Color(0xFFF2F2F2)))
    WidgetTheme.DARK -> Brush.verticalGradient(listOf(Color(0xFF1F1F2E), Color(0xFF12121A)))
    WidgetTheme.OCEAN -> Brush.linearGradient(listOf(Color(0xFFBDEFF5), Color(0xFF8CCCEB), Color(0xFF80A6E3)))
    WidgetTheme.FOREST -> Brush.linearGradient(listOf(Color(0xFFDEF0D1), Color(0xFFA8D4A1), Color(0xFF80B58F)))
    WidgetTheme.LAVENDER -> Brush.linearGradient(listOf(Color(0xFFEDE3FF), Color(0xFFCCB8F5), Color(0xFFA89CEB)))
    WidgetTheme.ROSE -> Brush.linearGradient(listOf(Color(0xFFFFE3ED), Color(0xFFF7B5CC), Color(0xFFE68FB0)))
    WidgetTheme.SAND -> Brush.linearGradient(listOf(Color(0xFFFAF2E3), Color(0xFFEDD9B8), Color(0xFFDEBF94)))
    WidgetTheme.MIDNIGHT -> Brush.linearGradient(listOf(Color(0xFF2B3A66), Color(0xFF1C2647), Color(0xFF0F1430)))
    WidgetTheme.WALLPAPER -> Brush.verticalGradient(
        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer))
}

/** Text colour for each style in the app. */
@Composable
fun WidgetTheme.appInk(): Color =
    if (this == WidgetTheme.WALLPAPER) MaterialTheme.colorScheme.onPrimaryContainer else ink

@Composable
private fun TodayScreen(
    quote: Quote,
    theme: WidgetTheme,
    isFavourite: Boolean,
    onToggle: (Quote) -> Unit,
    moodTag: String?,
    onCheckIn: () -> Unit,
) {
    val context = LocalContext.current
    val ink = theme.appInk()
    Column(
        modifier = Modifier.fillMaxSize().background(theme.appBrush()).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, androidx.compose.ui.Alignment.CenterVertically),
    ) {
        Text(
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
            color = ink.copy(alpha = 0.7f),
            style = MaterialTheme.typography.titleSmall,
        )
        AnimatedContent(targetState = quote, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "quote") { q ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "“${q.text}”",
                    color = ink,
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
                )
                Text("— ${q.author}", color = ink, fontWeight = FontWeight.SemiBold)
            }
        }
        Row {
            IconButton(onClick = { onToggle(quote) }) {
                Icon(
                    if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavourite) "Remove from favourites" else "Add to favourites",
                    tint = ink,
                )
            }
            IconButton(onClick = {
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "“${quote.text}” — ${quote.author}")
                context.startActivity(Intent.createChooser(send, "Share quote"))
            }) {
                Icon(Icons.Filled.Share, contentDescription = "Share quote", tint = ink)
            }
        }
        androidx.compose.material3.AssistChip(
            onClick = onCheckIn,
            label = {
                Text(moodTag?.let { "Picked for you: ${it.replaceFirstChar { c -> c.uppercase() }}" } ?: "How are you today?")
            },
            leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, tint = ink) },
            colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(labelColor = ink),
        )
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(theme: WidgetTheme, onThemeChange: (WidgetTheme) -> Unit) {
    // Wallpaper colours need Android 12 (Material You).
    val styles = WidgetTheme.entries.filter { it != WidgetTheme.WALLPAPER || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings") })
        Column(
            Modifier.padding(20.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Widget style", style = MaterialTheme.typography.titleSmall)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                styles.forEach { option -> ThemeSwatch(option, option == theme) { onThemeChange(option) } }
            }
            Text(
                "Pick the one that suits your wallpaper. Wallpaper uses your phone's own wallpaper colours and changes when your wallpaper does.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "A new quote appears every day at midnight. To add the widget, touch and hold your home screen, tap Widgets, then find Sunote.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A small preview of a widget style: its colours with "Aa" in its text colour. */
@Composable
private fun ThemeSwatch(theme: WidgetTheme, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width = 92.dp, height = 64.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .background(theme.appBrush())
                .border(
                    if (selected) 3.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                )
                .clickable(onClickLabel = "Use ${theme.title} style", onClick = onClick),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Text("Aa", color = theme.appInk(), style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif))
        }
        Text(theme.title, style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(top = 4.dp))
    }
}
