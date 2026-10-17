package org.dazai.osmium.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dazai.osmium.domain.model.Annotation
import org.dazai.osmium.domain.model.AnnotationType
import org.dazai.osmium.domain.model.Book
import org.dazai.osmium.domain.repository.LibraryRepository
import org.dazai.osmium.ui.library.BookCover
import org.dazai.osmium.ui.library.ModernSortSwitcher
import org.dazai.osmium.ui.components.AnnotationDetailDialog
import org.jetbrains.compose.resources.stringResource
import osmium.shared.generated.resources.*
import org.dazai.osmium.ui.reader.toLocator
import org.readium.r2.shared.publication.Locator

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
actual fun NotesAndAnnotationsSubpage(onBack: () -> Unit) {
    val viewModel: NotesAndAnnotationsViewModel = viewModel { NotesAndAnnotationsViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    var selectedBook by remember { mutableStateOf<Book?>(null) }

    val categoryTags = remember { 
        listOf(
            "literature" to Res.string.category_literature,
            "philosophy" to Res.string.category_philosophy,
            "history" to Res.string.category_history,
            "psychology" to Res.string.category_psychology,
        ) 
    }
    val typeTags = remember { 
        listOf(
            "books" to Res.string.type_books,
            "magazines" to Res.string.type_magazines,
            "articles" to Res.string.type_articles
        ) 
    }
    val formatTags = remember { listOf("EPUB", "PDF") }
    val allTags = remember { listOf("all" to Res.string.tag_all) + categoryTags + typeTags }

    if (selectedBook != null) {
        BookAnnotationsDetailSubpage(
            book = selectedBook!!,
            onBack = { selectedBook = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.notes_and_annotations)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                stickyHeader {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().zIndex(10f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 8.dp)
                            ) {
                                items(allTags) { (key, res) ->
                                    val isSelected = uiState.selectedTags.contains(key)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.onTagToggle(key) },
                                        label = { Text(stringResource(res)) },
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                                
                                items(formatTags) { tag ->
                                    val isSelected = uiState.selectedTags.contains(tag)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.onTagToggle(tag) },
                                        label = { Text(tag) },
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                            }

                            ModernSortSwitcher(
                                currentCriteria = uiState.sortCriteria,
                                isAscending = uiState.isAscending,
                                onCriteriaChange = { viewModel.onSortCriteriaChange(it) },
                                onDirectionToggle = { viewModel.toggleSortDirection() },
                                showLabel = false,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    }
                }

                if (uiState.filteredBooks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxHeight(0.6f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(Res.string.no_matching_books), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(uiState.filteredBooks) { stats ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            ManagementBookItem(
                                stats = stats,
                                onClick = { selectedBook = stats.book }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManagementBookItem(stats: BookAnnotationStats, onClick: () -> Unit) {
    val book = stats.book
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BookCover(
            url = book.coverUrl,
            modifier = Modifier
                .width(60.dp)
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(6.dp))
        )
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(2.dp))
            
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (stats.annotationCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.annotation_count_format, stats.annotationCount),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                if (stats.noteCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.note_count_format, stats.noteCount),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                Spacer(Modifier.weight(1f))
                
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${(book.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    LinearProgressIndicator(
                        progress = { book.progress },
                        modifier = Modifier.width(40.dp).height(3.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun BookAnnotationsDetailSubpage(book: Book, onBack: () -> Unit) {
    val annotations by LibraryRepository.getAnnotationsForBook(book.id).collectAsState(emptyList())
    var detailAnnotation by remember { mutableStateOf<Annotation?>(null) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (annotations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.no_annotations))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(annotations, key = { _, annotation -> annotation.id }) { index, annotation ->
                        DetailAnnotationItem(
                            annotation = annotation,
                            isEven = (index % 2 == 0),
                            onClick = { detailAnnotation = annotation },
                            onNavigate = {
                                // TODO: Trigger book navigation from settings
                            }
                        )
                }
            }
        }
    }

    detailAnnotation?.let { annotation ->
        AnnotationDetailDialog(
            annotation = annotation,
            onDismiss = { detailAnnotation = null },
            onNavigate = {
                // TODO: Trigger book navigation from settings
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailAnnotationItem(
    annotation: Annotation,
    isEven: Boolean,
    onClick: () -> Unit,
    onNavigate: () -> Unit
) {
    // 滑动交互状态管理
    val locator = remember(annotation) { annotation.toLocator() }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    LibraryRepository.removeAnnotation(annotation.id)
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onNavigate()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
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
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                } else if (isNavigating) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
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
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            headlineContent = {
                val locatorText = locator?.text?.highlight ?: ""
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
                val chapterPart = annotation.chapter
                val progressionPart = annotation.chapterProgression?.let { "${(it * 100).toInt()}%" }
                val totalPart = (locator?.locations?.totalProgression ?: 0.0).let { "${(it * 100).toInt()}%" }

                val locationText = when {
                    chapterPart != null && progressionPart != null -> "$chapterPart · $progressionPart"
                    chapterPart != null -> chapterPart
                    else -> totalPart
                }

                val typeText = when (annotation.type) {
                    AnnotationType.HIGHLIGHT -> stringResource(Res.string.highlight)
                    AnnotationType.UNDERLINE -> stringResource(Res.string.underline)
                    AnnotationType.NOTE -> stringResource(Res.string.note)
                }
                Text(
                    text = "$typeText · $locationText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = ListItemDefaults.colors(containerColor = containerColor)
        )
    }
}

