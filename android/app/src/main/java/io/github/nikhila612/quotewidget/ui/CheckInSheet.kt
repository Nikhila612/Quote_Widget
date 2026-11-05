package io.github.nikhila612.quotewidget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.nikhila612.quotewidget.core.CheckIn
import io.github.nikhila612.quotewidget.core.DayKind
import io.github.nikhila612.quotewidget.core.Feeling
import io.github.nikhila612.quotewidget.core.QuoteKind
import io.github.nikhila612.quotewidget.core.Wish

/** The daily check-in: four optional questions answered with a tap each. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInSheet(initial: CheckIn, onSubmit: (CheckIn) -> Unit, onSkip: () -> Unit) {
    var answers by remember { mutableStateOf(initial) }
    ModalBottomSheet(onDismissRequest = onSkip, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("How are you today?", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Answer any you like, and today's quote will match.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Question("What's your mood right now?", Feeling.entries, { it.title }, answers.feeling) {
                answers = answers.copy(feeling = it)
            }
            Question("How will your day be?", DayKind.entries, { it.title }, answers.day) {
                answers = answers.copy(day = it)
            }
            Question("How do you want today to feel?", Wish.entries, { it.title }, answers.wish) {
                answers = answers.copy(wish = it)
            }
            Question("What kind of quote today?", QuoteKind.entries, { it.title }, answers.kind) {
                answers = answers.copy(kind = it)
            }
            Button(onClick = { onSubmit(answers) }, enabled = !answers.isEmpty, modifier = Modifier.fillMaxWidth()) {
                Text("Show my quote")
            }
            TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)) {
                Text("Skip for today")
            }
        }
    }
}

/** One question with its answers as chips; tapping the chosen chip again clears it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Question(title: String, options: List<T>, label: (T) -> String, selected: T?, onChange: (T?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onChange(if (option == selected) null else option) },
                    label = { Text(label(option)) },
                )
            }
        }
    }
}
