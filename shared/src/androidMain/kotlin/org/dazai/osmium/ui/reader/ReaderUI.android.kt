package org.dazai.osmium.ui.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dazai.osmium.domain.model.Annotation
import org.dazai.osmium.domain.model.AnnotationType
import org.dazai.osmium.domain.repository.LibraryRepository
import org.dazai.osmium.util.currentTimeMillis
import org.dazai.osmium.util.generateUUID

@Composable
fun SelectionMenuOverlay(
    state: ReaderState,
    bookId: String,
    onClearSelection: () -> Unit
) {
    if (state.showSelectionMenu && state.selectionLocatorJson != null) {
        val density = LocalDensity.current
        val rect = state.selectionRect
        val scope = rememberCoroutineScope()

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (rect != null) {
                val left = with(density) { rect.left.toDp() }
                val top = with(density) { rect.top.toDp() }
                val right = with(density) { rect.right.toDp() }
                val bottom = with(density) { rect.bottom.toDp() }

                val menuWidth = 300.dp
                val menuHeight = 56.dp
                
                var isTopPopup = true
                var targetY = top - menuHeight - 16.dp
                
                if (targetY < 64.dp) {
                    targetY = bottom + 16.dp
                    isTopPopup = false
                }
                
                if (targetY + menuHeight > maxHeight - 48.dp) {
                    targetY = (maxHeight - menuHeight) / 2
                    isTopPopup = true
                }

                var targetX = (left + right) / 2 - menuWidth / 2
                targetX = targetX.coerceIn(16.dp, maxWidth - menuWidth - 16.dp)

                Box(
                    modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTapGestures(onTap = { state.showSelectionMenu = false; onClearSelection() })
                    }
                )

                Box(
                    modifier = Modifier.offset(x = targetX, y = targetY)
                ) {
                    SelectionButtonGroup(
                        isTopPopup = isTopPopup,
                        lastUnderlineColor = state.lastUnderlineColor,
                        lastHighlightColor = state.lastHighlightColor,
                        lastStyle = state.lastStyle,
                        onAction = { type, style, color ->
                            state.selectionLocatorJson?.toLocator()?.let { locator ->
                                scope.launch {
                                    val chapter = state.currentHierarchy.lastOrNull()
                                    LibraryRepository.addAnnotation(
                                        Annotation(
                                            id = generateUUID(),
                                            bookId = bookId,
                                            href = locator.href.toString(),
                                            mediaType = locator.mediaType.toString(),
                                            locations = locator.locations.toJSON().toString(),
                                            text = locator.text.toJSON().toString(),
                                            title = locator.title,
                                            totalProgression = locator.locations.totalProgression ?: 0.0,
                                            type = type,
                                            style = style,
                                            color = color,
                                            chapter = chapter,
                                            chapterProgression = state.selectionProgression,
                                            timestamp = currentTimeMillis()
                                        )
                                    )
                                }
                            }
                            state.showSelectionMenu = false
                            onClearSelection()
                        },
                        onToolUpdate = { type, color, style ->
                            state.lastToolType = type
                            if (type == AnnotationType.UNDERLINE) {
                                state.lastUnderlineColor = color
                            } else {
                                state.lastHighlightColor = color
                            }
                            state.lastStyle = style
                        },
                        onNote = { state.showNoteInput = true },
                        onDelete = {
                            state.selectionLocatorJson?.toLocator()?.let { locator ->
                                LibraryRepository.removeAnnotationsByLocator(
                                    bookId, 
                                    locator.href.toString(), 
                                    locator.locations.toJSON().toString()
                                )
                            }
                            state.showSelectionMenu = false
                            onClearSelection()
                        },
                        onDismiss = {
                            state.showSelectionMenu = false
                            onClearSelection()
                        }
                    )
                }
            } else {
                Box(modifier = Modifier.align(Alignment.Center)) {
                    SelectionButtonGroup(
                        isTopPopup = true,
                        lastUnderlineColor = state.lastUnderlineColor,
                        lastHighlightColor = state.lastHighlightColor,
                        lastStyle = state.lastStyle,
                        onAction = { type, style, color ->
                            state.selectionLocatorJson?.toLocator()?.let { locator ->
                                scope.launch {
                                    val chapter = state.currentHierarchy.lastOrNull()
                                    LibraryRepository.addAnnotation(
                                        Annotation(
                                            id = generateUUID(),
                                            bookId = bookId,
                                            href = locator.href.toString(),
                                            mediaType = locator.mediaType.toString(),
                                            locations = locator.locations.toJSON().toString(),
                                            text = locator.text.toJSON().toString(),
                                            title = locator.title,
                                            totalProgression = locator.locations.totalProgression ?: 0.0,
                                            type = type,
                                            style = style,
                                            color = color,
                                            chapter = chapter,
                                            chapterProgression = state.selectionProgression,
                                            timestamp = currentTimeMillis()
                                        )
                                    )
                                }
                            }
                            state.showSelectionMenu = false
                            onClearSelection()
                        },
                        onToolUpdate = { type, color, style ->
                            state.lastToolType = type
                            if (type == AnnotationType.UNDERLINE) {
                                state.lastUnderlineColor = color
                            } else {
                                state.lastHighlightColor = color
                            }
                            state.lastStyle = style
                        },
                        onNote = { state.showNoteInput = true },
                        onDelete = {
                            state.selectionLocatorJson?.toLocator()?.let { locator ->
                                LibraryRepository.removeAnnotationsByLocator(
                                    bookId, 
                                    locator.href.toString(), 
                                    locator.locations.toJSON().toString()
                                )
                            }
                            state.showSelectionMenu = false
                            onClearSelection()
                        },
                        onDismiss = {
                            state.showSelectionMenu = false
                            onClearSelection()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NoteInputOverlay(
    state: ReaderState,
    bookId: String,
    annotations: List<Annotation>,
    onClearSelection: () -> Unit
) {
    if (state.showNoteInput) {
        val scope = rememberCoroutineScope()
        val editingAnnotation = remember(state.activeAnnotationId, annotations) {
            state.activeAnnotationId?.let { id -> annotations.find { it.id == id } }
        }
        
        NoteInputDialog(
            initialText = editingAnnotation?.note ?: "",
            onDismiss = { 
                state.showNoteInput = false 
                state.activeAnnotationId = null
            },
            onConfirm = { note ->
                val locatorJson = state.selectionLocatorJson
                if (locatorJson != null) {
                    val locator = locatorJson.toLocator()
                    if (locator != null) {
                        scope.launch {
                            val chapter = state.currentHierarchy.lastOrNull()
                            LibraryRepository.addAnnotation(
                                Annotation(
                                    id = state.activeAnnotationId ?: generateUUID(),
                                    bookId = bookId,
                                    href = locator.href.toString(),
                                    mediaType = locator.mediaType.toString(),
                                    locations = locator.locations.toJSON().toString(),
                                    text = locator.text.toJSON().toString(),
                                    title = locator.title,
                                    totalProgression = locator.locations.totalProgression ?: 0.0,
                                    type = AnnotationType.NOTE,
                                    color = if (state.activeAnnotationId != null) editingAnnotation?.color else state.lastHighlightColor,
                                    note = note,
                                    chapter = chapter,
                                    chapterProgression = state.selectionProgression,
                                    timestamp = currentTimeMillis()
                                )
                            )
                        }
                    }
                }
                state.showNoteInput = false
                state.activeAnnotationId = null
                onClearSelection()
            }
        )
    }
}

