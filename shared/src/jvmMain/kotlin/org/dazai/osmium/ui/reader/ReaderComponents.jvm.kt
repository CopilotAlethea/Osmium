package org.dazai.osmium.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Rect
import org.dazai.osmium.domain.model.Annotation
import org.jetbrains.compose.resources.stringResource
import osmium.shared.generated.resources.*

@Composable
actual fun ReaderAnnotationsDialog(
    anchorBounds: Rect?,
    annotations: List<Annotation>,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    onDeleteAnnotation: (String) -> Unit
) {
    ReaderBubbleDialog(
        anchorBounds = anchorBounds,
        onDismiss = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Text(
                text = stringResource(Res.string.annotations),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold
            )

            if (annotations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.no_annotations),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(annotations) { annotation ->
                        AnnotationItem(
                            annotation = annotation,
                            onNavigate = {
                                onNavigate(annotation.locations)
                                onDismiss()
                            },
                            onDelete = { onDeleteAnnotation(annotation.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnotationItem(
    annotation: Annotation,
    onNavigate: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onNavigate() },
        headlineContent = {
            Text(
                text = annotation.note ?: annotation.chapter ?: "",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        supportingContent = {
            annotation.chapter?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}

