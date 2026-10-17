package org.dazai.osmium.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.zIndex
import org.dazai.osmium.domain.model.Annotation
import org.dazai.osmium.domain.model.AnnotationType
import org.dazai.osmium.domain.model.Book
import org.dazai.osmium.domain.repository.LibraryRepository
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource
import osmium.shared.generated.resources.*

@Composable
fun BookEditDialog(
    book: Book,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.edit_book_info)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(Res.string.book_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp)
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(Res.string.book_author)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, author) }) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagEditDialog(
    book: Book,
    allAvailableTags: List<Pair<String, StringResource>>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var selectedTags by remember { mutableStateOf(book.tags.toSet()) }
    val isEpub = book.path.lowercase().endsWith(".epub")
    val formatTag = if (isEpub) "EPUB" else "PDF"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.select_tags)) },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text(formatTag) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = null,
                    enabled = false
                )

                allAvailableTags.forEach { (key, res) ->
                    val isSelected = selectedTags.contains(key)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTags = if (isSelected) selectedTags - key else selectedTags + key
                        },
                        label = { Text(stringResource(res)) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedTags.toList()) }) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        }
    )
}




