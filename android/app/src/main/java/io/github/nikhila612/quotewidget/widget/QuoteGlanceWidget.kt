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
import androidx.glance.layout.Column
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
        provideContent {
            // Keep watching settings: a Glance session can outlive the first draw,
            // so a theme change or a new quote of the user's must update it in place.
            val theme by repository.theme.collectAsState(initial = initialTheme)
            val customQuotes by repository.customQuotes.collectAsState(initial = null)
            var quote by remember { mutableStateOf(initialQuote) }
            LaunchedEffect(customQuotes) { quote = repository.quoteFor() }
            QuoteWidgetContent(quote, theme)
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 110.dp)
        val LARGE = DpSize(250.dp, 250.dp)
    }
}

@Composable
private fun QuoteWidgetContent(quote: Quote, theme: WidgetTheme) {
    val size = LocalSize.current
    val showAuthor = size.width >= QuoteGlanceWidget.MEDIUM.width || size.height >= QuoteGlanceWidget.LARGE.height
    val ink = ColorProvider(theme.ink)
    val quoteSize = when {
        size.height >= QuoteGlanceWidget.LARGE.height -> 22.sp
        size.width >= QuoteGlanceWidget.MEDIUM.width -> if (quote.text.length > 90) 15.sp else 18.sp
        else -> if (quote.text.length > 60) 12.sp else 14.sp
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(theme.backgroundRes))
            .padding(16.dp)
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
