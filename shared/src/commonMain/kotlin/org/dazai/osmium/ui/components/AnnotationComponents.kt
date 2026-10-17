package org.dazai.osmium.ui.components

import androidx.compose.runtime.Composable
import org.dazai.osmium.domain.model.Annotation

@Composable
expect fun AnnotationDetailDialog(
    annotation: Annotation,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit
)

