package org.dazai.osmium.ui.library

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.crossfade
import kotlinx.coroutines.delay
import org.dazai.osmium.domain.model.Book
import org.dazai.osmium.util.ui.osmiumClickable
import org.jetbrains.compose.resources.stringResource
import osmium.shared.generated.resources.*

@Composable
fun ContinueReadingCard(
    book: Book, 
    onClick: (Book) -> Unit,
    onLongClick: (Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        // 1. 底层卡片：略微下移，为封面顶部留出空间
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(start=30.dp,bottom=7.dp)
                .osmiumClickable(
                    onClick = { onClick(book) },
                    onLongClick = { offset -> onLongClick(offset) }
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 100.dp, end = 25.dp, top = 12.dp, bottom = 12.dp), // start padding 增加以避开封面
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    val authorText = if (book.author == "未知作者" || book.author == "Unknown Author") {
                        stringResource(Res.string.unknown_author)
                    } else {
                        book.author
                    }
                    Text(
                        authorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(Res.string.read_progress, (book.progress * 100).toInt()),
                        style = MaterialTheme.typography.labelSmall
                    )
                    LinearProgressIndicator(
                        progress = { book.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .height(4.dp)
                            .clip(CircleShape),
                        trackColor = MaterialTheme.colorScheme.surface,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 2. 顶层封面：覆盖在卡片左侧，且底部伸出
        BookCover(
            url = book.coverUrl,
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
            modifier = Modifier
                .padding(start = 16.dp) // 左侧边距
                .width(110.dp)
                .height(150.dp) // 封面总高度
                .osmiumClickable(
                    onClick = { onClick(book) },
                    onLongClick = { offset -> onLongClick(offset) }
                ),
            targetSize = 350
        )
    }
}

@Composable
fun BookItem(book: Book, onClick: () -> Unit, onLongClick: (Offset) -> Unit) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .osmiumClickable(
                onClick = { onClick() },
                onLongClick = { offset -> onLongClick(offset) }
            )
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.707f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            BookCover(
                url = book.coverUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
                targetSize = 300
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            book.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        val authorText = if (book.author == "未知作者" || book.author == "Unknown Author") {
            stringResource(Res.string.unknown_author)
        } else {
            book.author
        }
        Text(
            authorText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BookCover(
    url: String?, 
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.BottomCenter,
    targetSize: Int = 400 // 【性能优化】默认400，但允许小图覆盖
) {
    if (url != null) {
        AsyncImage(
            model = coil3.request.ImageRequest.Builder(LocalPlatformContext.current)
                .data(url)
                // 移除 crossfade 以节省低端机 GPU/内存
                .crossfade(false)
                // 强制限制采样尺寸，封面通常很小
                .size(targetSize) // 使用动态参数控制内存采样
                .memoryCacheKey(url)
                .diskCacheKey(url)
                .build(),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
            alignment = alignment
        )
    } else {
        Box(
            modifier = modifier.background(Color.LightGray),
            contentAlignment = Alignment.BottomCenter
        ) {
            Icon(
                Icons.Default.Description, 
                contentDescription = null, 
                tint = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
fun ActionMenuPopup(
    isRightSide: Boolean,
    isBottomSide: Boolean,
    touchOffset: Offset,
    onEdit: () -> Unit,
    onTags: () -> Unit,
    onNotes: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(
            x = with(density) { 
                if (isRightSide) (touchOffset.x.toDp() - 160.dp).roundToPx() 
                else (touchOffset.x.toDp() + 20.dp).roundToPx() 
            },
            y = with(density) { 
                if (isBottomSide) (touchOffset.y.toDp() - 280.dp).roundToPx() 
                else (touchOffset.y.toDp() - 120.dp).roundToPx() 
            }
        ),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier.width(IntrinsicSize.Max),
            horizontalAlignment = if (isRightSide) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MenuContent(isRightSide, onEdit, onTags, onNotes, onRemove, onDismiss)
            CloseButton(onDismiss)
        }
    }
}

@Composable
private fun MenuContent(
    isRightSide: Boolean,
    onEdit: () -> Unit,
    onTags: () -> Unit,
    onNotes: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    SmartMenuButton(
        icon = Icons.Default.Edit,
        text = stringResource(Res.string.edit_info),
        isRightSide = isRightSide,
        onClick = { onEdit(); onDismiss() }
    )
    SmartMenuButton(
        icon = Icons.Default.Description,
        text = stringResource(Res.string.edit_tags),
        isRightSide = isRightSide,
        onClick = { onTags(); onDismiss() }
    )
    SmartMenuButton(
        icon = Icons.AutoMirrored.Filled.MenuBook,
        text = stringResource(Res.string.view_notes),
        isRightSide = isRightSide,
        onClick = { onNotes(); onDismiss() }
    )
    SmartMenuButton(
        icon = Icons.Default.Delete,
        text = stringResource(Res.string.remove_book),
        isRightSide = isRightSide,
        onClick = { onRemove(); onDismiss() }
    )
}

@Composable
private fun CloseButton(onDismiss: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        onClick = onDismiss,
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.collapse), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SmartMenuButton(
    icon: ImageVector,
    text: String,
    isRightSide: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isRightSide) Arrangement.End else Arrangement.Start
        ) {
            if (!isRightSide) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(text, style = MaterialTheme.typography.labelLarge)
            } else {
                Text(text, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(12.dp))
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ModernSortSwitcher(
    currentCriteria: LibrarySortCriteria,
    isAscending: Boolean,
    onCriteriaChange: (LibrarySortCriteria) -> Unit,
    onDirectionToggle: () -> Unit,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isExpanded, currentCriteria, isAscending) {
        if (isExpanded) {
            delay(3000)
            isExpanded = false
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.CenterEnd) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null 
                ) { isExpanded = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = { isExpanded = true },
                modifier = Modifier.height(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = if (showLabel) 16.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val icon = getSortIcon(currentCriteria)
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    if (showLabel) {
                        Text(stringResource(currentCriteria.labelRes), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Surface(
                onClick = { onDirectionToggle() },
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SortDirectionTriangles(isAscending)
                }
            }
        }

        if (isExpanded) {
            val entries = LibrarySortCriteria.entries
            val currentIndex = entries.indexOf(currentCriteria)
            val itemHeight = 42
            val spacing = 8
            val totalItems = entries.size
            
            val itemFullHeight = itemHeight + spacing
            val centerIndex = (totalItems - 1) / 2.0
            
            val targetYOffset = ((centerIndex - currentIndex) * itemFullHeight).dp
            val animatedYOffset by animateDpAsState(
                targetValue = targetYOffset,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow 
                )
            )

            Popup(
                alignment = Alignment.CenterEnd,
                offset = IntOffset(0, 0),
                onDismissRequest = { isExpanded = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true,
                    clippingEnabled = false 
                )
            ) {
                val containerHeight = itemFullHeight * 9
                
                Row(
                    modifier = Modifier
                        .height(containerHeight.dp)
                        .width(IntrinsicSize.Max),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.weight(1f, fill = false),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Column(
                            modifier = Modifier.offset(y = animatedYOffset),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(spacing.dp)
                        ) {
                            entries.forEach { criteria ->
                                val isSelected = currentCriteria == criteria
                                
                                Surface(
                                    onClick = { onCriteriaChange(criteria) },
                                    shape = CircleShape,
                                    color = if (isSelected) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                    contentColor = if (isSelected) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.height(itemHeight.dp),
                                    shadowElevation = 8.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(getSortIcon(criteria), contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = stringResource(criteria.labelRes),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        onClick = { onDirectionToggle() },
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            SortDirectionTriangles(isAscending)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortDirectionTriangles(isAscending: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.ArrowDropUp,
            contentDescription = null,
            modifier = Modifier.size(14.dp).offset(y = 2.dp),
            tint = if (isAscending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Icon(
            Icons.Default.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier.size(14.dp).offset(y = (-2).dp),
            tint = if (!isAscending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

internal fun getSortIcon(criteria: LibrarySortCriteria): ImageVector {
    return when (criteria) {
        LibrarySortCriteria.Name -> Icons.AutoMirrored.Filled.Sort
        LibrarySortCriteria.Author -> Icons.Default.Person
        LibrarySortCriteria.LastRead -> Icons.Default.History
        LibrarySortCriteria.Progress -> Icons.AutoMirrored.Filled.TrendingUp
        LibrarySortCriteria.ImportTime -> Icons.Default.Add
    }
}

@Composable
fun SearchDropdownPanel(
    query: String,
    results: List<Book>,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp),
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        if (query.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(Res.string.search_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(Res.string.no_matching_books),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(results, key = { it.id }) { book ->
                    SearchResultItem(book, onClick = { onBookClick(book) })
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.no_more_results),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(book: Book, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .osmiumClickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(
                url = book.coverUrl,
                modifier = Modifier
                    .width(52.dp)
                    .height(72.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
                targetSize = 150 // 【性能优化】小列表仅需小分辨率采样
            )
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                val authorText = if (book.author == "未知作者" || book.author == "Unknown Author") {
                    stringResource(Res.string.unknown_author)
                } else {
                    book.author
                }
                Text(
                    text = authorText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

