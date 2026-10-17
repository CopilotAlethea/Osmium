package org.dazai.osmium.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dazai.osmium.domain.model.Annotation
import org.dazai.osmium.domain.model.AnnotationType
import org.jetbrains.compose.resources.stringResource
import osmium.shared.generated.resources.*
import org.dazai.osmium.ui.reader.toLocator

@Composable
actual fun AnnotationDetailDialog(
    annotation: Annotation,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (annotation.type) {
                    AnnotationType.NOTE -> stringResource(Res.string.note)
                    AnnotationType.HIGHLIGHT -> stringResource(Res.string.highlight)
                    AnnotationType.UNDERLINE -> stringResource(Res.string.underline)
                },
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            val locator = remember(annotation) { annotation.toLocator() }
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (!annotation.note.isNullOrEmpty()) {
                    Text(
                        text = annotation.note,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                val originalText = locator?.text?.highlight ?: ""
                if (originalText.isNotEmpty()) {
                    Row {
                        Text(
                            text = "“",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.offset(y = (-8).dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = originalText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                val chapterPart = annotation.chapter
                val progressionPart = annotation.chapterProgression?.let { "${(it * 100).toInt()}%" }
                val totalPart = locator?.locations?.totalProgression?.let { "${(it * 100).toInt()}%" }

                val locationText = when {
                    chapterPart != null && progressionPart != null -> "$chapterPart · $progressionPart"
                    chapterPart != null -> chapterPart
                    totalPart != null -> totalPart
                    else -> ""
                }

                Text(
                    text = locationText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onNavigate(); onDismiss() }) {
                Text(stringResource(Res.string.go_to_location))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        }
    )
}

