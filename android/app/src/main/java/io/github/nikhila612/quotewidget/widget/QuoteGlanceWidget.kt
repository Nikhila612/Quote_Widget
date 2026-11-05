package io.github.nikhila612.quotewidget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.width
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.nikhila612.quotewidget.R
import io.github.nikhila612.quotewidget.core.CheckIn
import io.github.nikhila612.quotewidget.core.Feeling
import io.github.nikhila612.quotewidget.core.Quote
import io.github.nikhila612.quotewidget.data.QuoteRepository
import io.github.nikhila612.quotewidget.data.WidgetTheme
import io.github.nikhila612.quotewidget.ui.MainActivity

class QuoteGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = QuoteRepository(context)
        val initialQuote = repository.quoteFor()
        val initialTheme = repository.currentTheme()
        val initialFeeling = repository.checkInFor()?.feeling
        provideContent {
            // Keep watching settings: a Glance session can outlive the first draw,
            // so a theme change or a new quote of the user's must update it in place.
            val theme by repository.theme.collectAsState(initial = initialTheme)
            val pickInputs by repository.pickInputs.collectAsState(initial = null)
            var quote by remember { mutableStateOf(initialQuote) }
            var feeling by remember { mutableStateOf(initialFeeling) }
            LaunchedEffect(pickInputs) {
                quote = repository.quoteFor()
                feeling = repository.checkInFor()?.feeling
            }
            QuoteWidgetContent(quote, theme, feeling)
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 110.dp)
        val LARGE = DpSize(250.dp, 250.dp)
    }
}

/** The moods offered on the large widget (the app's check-in has all six). */
private val WIDGET_MOODS = listOf(Feeling.HAPPY, Feeling.CALM, Feeling.TIRED, Feeling.WORRIED)

@Composable
private fun QuoteWidgetContent(quote: Quote, theme: WidgetTheme, feeling: Feeling?) {
    val size = LocalSize.current
    val isLarge = size.height >= QuoteGlanceWidget.LARGE.height
    val showAuthor = size.width >= QuoteGlanceWidget.MEDIUM.width || isLarge
    val ink = ColorProvider(theme.ink)
    val quoteSize = when {
        isLarge -> if (quote.text.length > 110) 18.sp else 21.sp
        size.width >= QuoteGlanceWidget.MEDIUM.width -> if (quote.text.length > 90) 15.sp else 18.sp
        else -> if (quote.text.length > 60) 12.sp else 14.sp
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(theme.backgroundRes))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .clickable(actionStartActivity<MainActivity>())
                .semantics { contentDescription = "Today's quote: ${quote.text}, by ${quote.author}" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "“${quote.text}”",
                style = TextStyle(color = ink, fontSize = quoteSize, fontFamily = FontFamily.Serif),
            )
            if (showAuthor) {
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text = "— ${quote.author}",
                    style = TextStyle(color = ink, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                )
            }
        }
        if (isLarge) {
            Spacer(GlanceModifier.height(10.dp))
            Text("How are you?", style = TextStyle(color = ink, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(6.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                WIDGET_MOODS.forEachIndexed { i, mood ->
                    if (i > 0) Spacer(GlanceModifier.width(6.dp))
                    val isOn = mood == feeling
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(30.dp)
                            .cornerRadius(15.dp)
                            .background(ColorProvider(theme.ink.copy(alpha = if (isOn) 0.22f else 0.08f)))
                            .clickable(actionRunCallback<MoodAction>(actionParametersOf(MoodAction.MOOD to mood.name)))
                            .semantics { contentDescription = "I feel ${mood.title.lowercase()}" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            mood.title,
                            style = TextStyle(
                                color = ink, fontSize = 12.sp,
                                fontWeight = if (isOn) FontWeight.Bold else FontWeight.Normal,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** Tapping a mood on the large widget re-picks today's quote for that mood. */
class MoodAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mood = parameters[MOOD]?.let { runCatching { Feeling.valueOf(it) }.getOrNull() } ?: return
        // A widget tap means "this is how I feel now", so it replaces the morning's answers.
        QuoteRepository(context).checkIn(CheckIn(feeling = mood))
        QuoteGlanceWidget().update(context, glanceId)
    }

    companion object {
        val MOOD = ActionParameters.Key<String>("mood")
    }
}

private val WidgetTheme.backgroundRes: Int
    get() = when (this) {
        WidgetTheme.SUNRISE -> R.drawable.widget_bg_sunrise
        WidgetTheme.LIGHT -> R.drawable.widget_bg_light
        WidgetTheme.DARK -> R.drawable.widget_bg_dark
    }

val WidgetTheme.ink: Color
    get() = when (this) {
        WidgetTheme.SUNRISE -> Color(0xFF3B1F29)
        WidgetTheme.LIGHT -> Color(0xFF1F1F1F)
        WidgetTheme.DARK -> Color(0xFFF5F5F5)
    }

class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuoteGlanceWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        MidnightScheduler.schedule(context)
    }
}
