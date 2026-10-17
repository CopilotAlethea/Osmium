package org.dazai.osmium.ui.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dazai.osmium.domain.model.Annotation
import org.dazai.osmium.domain.model.AnnotationType
import org.dazai.osmium.domain.repository.LibraryRepository
import org.dazai.osmium.util.currentTimeMillis

@Composable
fun AnnotationActionsOverlay(
    showAnnotationActions: Boolean,
    activeAnnotationId: String?,
    activeAnnotationRect: SelectionRect?,
    annotations: List<Annotation>,
    lastUnderlineColor: Int,
    lastHighlightColor: Int,
    lastStyle: String?,
    onToolUpdate: (AnnotationType, Int, String?) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (Annotation) -> Unit,
    onDismiss: () -> Unit
) {
    if (showAnnotationActions && activeAnnotationId != null) {
        val annotation = annotations.find { it.id == activeAnnotationId }
        
        if (annotation == null) {
            onDismiss()
            return
        }

        val density = LocalDensity.current
        val scope = rememberCoroutineScope()

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // 点击外部消失
            Box(
                modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                }
            )

            val menuHeight = 56.dp

            val targetX: androidx.compose.ui.unit.Dp
            val targetY: androidx.compose.ui.unit.Dp
            var isTopPopup = true

            if (activeAnnotationRect != null) {
                val left = with(density) { activeAnnotationRect.left.toDp() }
                val top = with(density) { activeAnnotationRect.top.toDp() }
                val right = with(density) { activeAnnotationRect.right.toDp() }
                val bottom = with(density) { activeAnnotationRect.bottom.toDp() }

                var y = top - menuHeight - 16.dp
                if (y < 64.dp) {
                    y = bottom + 16.dp
                    isTopPopup = false
                }
                if (y + menuHeight > maxHeight - 48.dp) {
                    y = (maxHeight - menuHeight) / 2
                }
                targetY = y

                // SelectionButtonGroup 宽度大约是 200dp
                val estimatedWidth = 200.dp 
                val x = (left + right) / 2 - estimatedWidth / 2
                targetX = x.coerceIn(16.dp, maxWidth - estimatedWidth - 16.dp)
            } else {
                targetX = (maxWidth - 200.dp) / 2
                targetY = (maxHeight - menuHeight) / 2
            }

            Box(
                modifier = Modifier.offset(x = targetX, y = targetY)
            ) {
                SelectionButtonGroup(
                    isTopPopup = isTopPopup,
                    lastUnderlineColor = lastUnderlineColor,
                    lastHighlightColor = lastHighlightColor,
                    lastStyle = lastStyle,
                    onAction = { type, style, color ->
                        scope.launch {
                            LibraryRepository.addAnnotation(
                                annotation.copy(
                                    type = type,
                                    style = style,
                                    color = color,
                                    timestamp = currentTimeMillis()
                                )
                            )
                        }
                        onDismiss()
                    },
                    onToolUpdate = onToolUpdate,
                    onNote = {
                        onEdit(annotation)
                        onDismiss()
                    },
                    onDelete = {
                        onDelete(annotation.id)
                        onDismiss()
                    },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

