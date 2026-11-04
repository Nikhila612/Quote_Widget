package io.github.nikhila612.quotewidget.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import io.github.nikhila612.quotewidget.core.Quote
import io.github.nikhila612.quotewidget.data.QuoteRepository

/** Who is being edited: nobody (closed), a new quote, or an existing one. */
private sealed interface EditTarget {
    data object New : EditTarget
    data class Existing(val quote: Quote) : EditTarget
}

/** The user's own quotes. One of them shows in the widget every 7th day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQuotesScreen(
    quotes: List<Quote>,
    onSave: (text: String, author: String, existing: Quote?) -> Unit,
    onDelete: (Quote) -> Unit,
) {
    var editing by rememberSaveable(stateSaver = EditTargetSaver) { mutableStateOf<EditTarget?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(title = { Text("My Quotes") })
            if (quotes.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Write your own", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Add words that lift you up. One of your quotes appears in the widget once a week.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { editing = EditTarget.New }) { Text("Write a quote") }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Text(
                            "One of your quotes appears in the widget once a week, taking turns.",
                            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(quotes, key = { it.id }) { quote ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { editing = EditTarget.Existing(quote) }
                                .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(quote.text, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif))
                                Text("— ${quote.author}", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onDelete(quote) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete quote")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
        if (quotes.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { editing = EditTarget.New },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Write a quote") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }

    editing?.let { target ->
        val existing = (target as? EditTarget.Existing)?.quote
        QuoteEditorDialog(
            existing = existing,
            onDismiss = { editing = null },
            onSave = { text, author ->
                onSave(text, author, existing)
                editing = null
            },
        )
    }
}

@Composable
private fun QuoteEditorDialog(existing: Quote?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(existing?.text ?: "") }
    var author by rememberSaveable {
        mutableStateOf(existing?.author?.takeIf { it != QuoteRepository.DEFAULT_CUSTOM_AUTHOR } ?: "")
    }
    val max = QuoteRepository.MAX_CUSTOM_LENGTH

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New quote" else "Edit quote") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(max) },
                    label = { Text("Quote") },
                    placeholder = { Text("Something that makes you smile…") },
                    supportingText = { Text("${text.length}/$max") },
                    minLines = 3,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author") },
                    placeholder = { Text(QuoteRepository.DEFAULT_CUSTOM_AUTHOR) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text, author) }, enabled = text.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val EditTargetSaver = androidx.compose.runtime.saveable.Saver<EditTarget?, List<String>>(
    save = { target ->
        when (target) {
            null -> emptyList()
            EditTarget.New -> listOf("new")
            is EditTarget.Existing -> with(target.quote) { listOf("edit", id, text, author) }
        }
    },
    restore = { saved ->
        when (saved.firstOrNull()) {
            "new" -> EditTarget.New
            "edit" -> EditTarget.Existing(Quote(saved[1], saved[2], saved[3], listOf("mine")))
            else -> null
        }
    },
)
