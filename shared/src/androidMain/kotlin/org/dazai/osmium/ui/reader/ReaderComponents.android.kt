package org.dazai.osmium.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import org.dazai.osmium.ui.components.AnnotationDetailDialog
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dazai.osmium.domain.model.Annotation
import org.dazai.osmium.domain.model.AnnotationType
import org.jetbrains.compose.resources.stringResource
import osmium.shared.generated.resources.*
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.mediatype.MediaType
import org.dazai.osmium.ui.components.AnnotationDetailDialog

import androidx.compose.ui.geometry.Rect

@Composable
fun ReaderTocDialog(
    anchorBounds: Rect?,
    toc: List<Link>,
    onDismiss: () -> Unit,
    onNavigate: (Locator) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    ReaderBubbleDialog(anchorBounds = anchorBounds, onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    stringResource(Res.string.toc),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            HorizontalDivider(color = colorScheme.outlineVariant)
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .background(colorScheme.surface)
            ) {
                if (toc.isEmpty()) {
                    EmptyState(stringResource(Res.string.no_toc))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(toc) { link ->
                            TocItem(link, onClick = { targetLink ->
                                val locator = targetLink.toLocator() ?: Locator(
                                    href = targetLink.url(),
                                    mediaType = targetLink.mediaType ?: MediaType.HTML,
                                    title = targetLink.title,
                                    locations = Locator.Locations(progression = 0.0)
                                )
                                onNavigate(locator)
                                onDismiss()
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
actual fun ReaderAnnotationsDialog(
    anchorBounds: Rect?,
    annotations: List<Annotation>,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    onDeleteAnnotation: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var detailAnnotation by remember { mutableStateOf<Annotation?>(null) }

    ReaderBubbleDialog(anchorBounds = anchorBounds, onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 头部区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(Res.string.notes_and_annotations), // "笔记与标注"
                    style = MaterialTheme.typography.titleMedium
                )
            }

            HorizontalDivider(color = colorScheme.outlineVariant)

            // 列表内容区域

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .background(colorScheme.surface)
            ) {
                if (annotations.isEmpty()) {
                    EmptyState(stringResource(Res.string.no_annotations)) 
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(annotations, key = { _, annot -> annot.id }) { index, annotation ->
                            AnnotationItem(
                                annotation = annotation,
                                isEven = index % 2 == 0,
                                onClick = { detailAnnotation = annotation },
                                onNavigate = { locator ->
                                    onNavigate(locator.toJsonString())
                                    onDismiss()
                                },
                                onDelete = onDeleteAnnotation
                            )
                        }
                    }
                }
            }
        }
    }

    detailAnnotation?.let {
        AnnotationDetailDialog(
            annotation = it,
            onDismiss = { detailAnnotation = null },
            onNavigate = {
                it.toLocator()?.let { locator ->
                    onNavigate(locator.toJsonString())
                    onDismiss()
                }
            }
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TocItem(
    link: Link,
    onClick: (Link) -> Unit,
    defaultExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(defaultExpanded) }
    val hasChildren = link.children.isNotEmpty()

    Column {
        ListItem(
            headlineContent = {
                Text(
                    text = link.title ?: stringResource(Res.string.untitled),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            modifier = Modifier.clickable {
                if (hasChildren) {
                    isExpanded = !isExpanded
                } else {
                    onClick(link)
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            trailingContent = {
                if (hasChildren) {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            }
        )

        if (hasChildren) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    link.children.forEach { child ->
                        TocItem(child, onClick, defaultExpanded)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotationItem(
    annotation: Annotation,
    isEven: Boolean,
    onClick: () -> Unit,
    onNavigate: (Locator) -> Unit,
    onDelete: (String) -> Unit
) {
    val locator = remember(annotation) { annotation.toLocator() } ?: return

    // 滑动交互状态管理
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete(annotation.id)
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onNavigate(locator)
                    false // 不消失，仅触发跳转
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true, // 允许右滑跳转
        enableDismissFromEndToStart = true, // 允许左滑删除
        backgroundContent = {
            val isDeleting = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val isNavigating = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd
            
            val backgroundColor by animateColorAsState(
                targetValue = when {
                    isDeleting -> MaterialTheme.colorScheme.errorContainer
                    isNavigating -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 20.dp)
            ) {
                if (isDeleting) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.delete),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                } else if (isNavigating) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(Res.string.go_to_location),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
            }
        }
    ) {
        val containerColor = if (isEven) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surface
        
        ListItem(
            headlineContent = {
                val locatorText = locator.text.highlight ?: ""
                Column {
                    if (!annotation.note.isNullOrEmpty()) {
                        // === 笔记样式 ===
                        Text(
                            text = annotation.note,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "“",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.offset(y = (-4).dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = locatorText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // === 纯标注样式 ===
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "“",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.offset(y = (-8).dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = locatorText,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            supportingContent = {
                val typeText = when (annotation.type) {
                    AnnotationType.HIGHLIGHT -> stringResource(Res.string.highlight)
                    AnnotationType.UNDERLINE -> stringResource(Res.string.underline)
                    AnnotationType.NOTE -> stringResource(Res.string.note)
                }
                val chapterPart = annotation.chapter
                val progressionPart = annotation.chapterProgression?.let { "${(it * 100).toInt()}%" }
                val totalPart = (locator.locations.totalProgression?.times(100))?.toInt()?.let { "$it%" }
                
                val locationText = when {
                    chapterPart != null && progressionPart != null -> "$chapterPart · $progressionPart"
                    chapterPart != null -> chapterPart
                    totalPart != null -> totalPart
                    else -> ""
                }

                Text(
                    text = "$typeText · $locationText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            modifier = Modifier.clickable { onClick() },
            colors = ListItemDefaults.colors(containerColor = containerColor)
        )
    }
}

private fun Link.toLocator(): Locator? {
    return try {
        Locator(
            href = this.url(),
            mediaType = this.mediaType ?: MediaType.BINARY,
            title = this.title,
            locations = Locator.Locations(progression = 0.0)
        )
    } catch (e: Exception) {
        null
    }
}

