package org.dazai.osmium.ui.library

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.dazai.osmium.domain.model.Book
import org.dazai.osmium.domain.repository.LibraryRepository
import org.dazai.osmium.ui.reader.rememberReaderState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource
import osmium.shared.generated.resources.*
import org.dazai.osmium.util.rememberFileImporter
import org.dazai.osmium.util.rememberPermissionHandler
import org.dazai.osmium.util.isAtLeastS
import org.dazai.osmium.util.isAndroidBetween6And9
import org.dazai.osmium.util.ui.*
import org.dazai.osmium.ui.reader.ReaderState
import org.dazai.osmium.ui.reader.ReaderDialogType
import org.dazai.osmium.ui.reader.rememberReaderState
import org.dazai.osmium.ui.reader.ReaderAnnotationsDialog
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow

/**
 * 辅助 Modifier，用于抵消父容器（如 Grid）的 16.dp 水平内边距，
 * 使组件能够横跨整个屏幕宽度。
 */
private fun Modifier.offsetGridPadding(): Modifier = this.layout { measurable, constraints ->
    val paddingPx = 16.dp.roundToPx()
    val placeable = measurable.measure(constraints.copy(
        maxWidth = constraints.maxWidth + (2 * paddingPx)
    ))
    layout(constraints.maxWidth, placeable.height) {
        placeable.placeRelative(-paddingPx, 0)
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onBookClick: (Book) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: LibraryViewModel = viewModel { LibraryViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyGridState()
    val recentBooksState = rememberLazyListState()

    // 追踪是否已经执行过首次进入时的最近阅读滚动重置
    var hasResetRecentBooksScroll by remember { mutableStateOf(false) }

    // 当最近阅读数据加载完成时，执行一次重置滚动到开始
    LaunchedEffect(uiState.recentBooks) {
        if (!hasResetRecentBooksScroll && uiState.recentBooks.isNotEmpty()) {
            recentBooksState.scrollToItem(0)
            hasResetRecentBooksScroll = true
        }
    }

    // 监测是否有挂起的跳转请求
    val pendingNavigationBookId = uiState.pendingNavigationBookId
    LaunchedEffect(pendingNavigationBookId) {
        if (pendingNavigationBookId != null) {
            val indexInFiltered = uiState.filteredBooks.indexOfFirst { it.id == pendingNavigationBookId }
            if (indexInFiltered != -1) {
                // 计算在 LazyVerticalGrid 中的目标 Item 索引
                // 1. 最近阅读区域占用 1 个 item (span = maxLineSpan)
                val recentBooksOffset = if (uiState.recentBooks.isNotEmpty()) 1 else 0
                // 2. “全部书籍” Header 占用 1 个 item (span = maxLineSpan)
                val headerOffset = 1
                // 3. 目标书在网格中的索引
                val targetIndex = recentBooksOffset + headerOffset + indexInFiltered
                
                // 执行平滑滚动，并根据视口高度自动居中定位
                val viewportHeight = listState.layoutInfo.viewportSize.height
                val offset = if (viewportHeight > 0) -(viewportHeight / 3) else -200
                listState.animateScrollToItem(targetIndex, scrollOffset = offset)
                
                // 等滚动停稳后（缓冲200ms）再触发高亮，确保闪烁不被滚动遮盖
                delay(200)
                viewModel.triggerHighlight(pendingNavigationBookId)
                viewModel.onNavigationConsumed() // 清除指令
            } else {
                // 如果在当前过滤条件下没找到，尝试清除过滤（可选，根据需求决定）
                viewModel.onNavigationConsumed()
            }
        }
    }
    
    var isSearchFocused by remember { mutableStateOf(false) }
    var isSearchConfirmed by remember { mutableStateOf(false) }
    
    // 使用 TextFieldValue 接管输入框状态，解决低版本安卓输入法争夺与光标跳变问题
    var searchTextFieldValue by remember { 
        mutableStateOf(TextFieldValue(uiState.searchQuery)) 
    }

    // 同步外部状态变更（例如清除搜索）
    LaunchedEffect(uiState.searchQuery) {
        if (searchTextFieldValue.text != uiState.searchQuery) {
            searchTextFieldValue = searchTextFieldValue.copy(text = uiState.searchQuery)
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dummyFocusRequester = remember { FocusRequester() }
    
    val closeSearchAndClearQuery = {
        if (isAndroidBetween6And9) {
            try { dummyFocusRequester.requestFocus() } catch (e: Exception) {}
        }
        clearPlatformFocus(focusManager, keyboardController)
        viewModel.onSearchQueryChange("")
    }

    // 确保进入首页时不自动聚焦搜索框（采用平台特有延迟清理）
    LaunchedEffect(Unit) {
        onPageEntryFocusCleanup(focusManager, keyboardController)
    }

    // 当检测到列表在滚动时，如果搜索框有焦点或有输入内容，自动清除焦点
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && (isSearchFocused || uiState.searchQuery.isNotEmpty())) {
            closeSearchAndClearQuery()
        }
    }

    val categoryTags = remember { 
        listOf(
            "literature" to Res.string.category_literature,
            "philosophy" to Res.string.category_philosophy,
            "history" to Res.string.category_history,
            "psychology" to Res.string.category_psychology
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

    var showFilePicker by remember { mutableStateOf(false) }
    val fileImporter = rememberFileImporter()
    val scope = rememberCoroutineScope()
    val notificationState = rememberNotificationState()
    var showPermissionRationale by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf<Book?>(null) }
    var showTagDialog by remember { mutableStateOf<Book?>(null) }
    
    val readerState = rememberReaderState()
    var annotationBook by remember { mutableStateOf<Book?>(null) }
    val annotations by if (annotationBook != null) {
        LibraryRepository.getAnnotationsForBook(annotationBook!!.id).collectAsState(emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val showScrollToTopState = remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    val permissionHandler = rememberPermissionHandler { isGranted ->
        if (isGranted) {
            showFilePicker = true
        } else {
            scope.launch {
                val deniedMsg = org.jetbrains.compose.resources.getString(Res.string.permission_denied_storage)
                notificationState.show(deniedMsg, NotificationType.ERROR)
            }
        }
    }

    if (showPermissionRationale) {
        UnifiedPermissionDialog(
            description = stringResource(Res.string.permission_rationale_storage),
            onConfirm = {
                showPermissionRationale = false
                permissionHandler.requestStoragePermission()
            },
            onDismiss = { showPermissionRationale = false }
        )
    }

    if (showFilePicker) {
        FilePickerSheet(
            importer = fileImporter,
            onDismiss = { showFilePicker = false },
            onFileSelected = { path ->
                showFilePicker = false
                scope.launch {
                    val startingMsg = org.jetbrains.compose.resources.getString(Res.string.import_starting)
                    notificationState.show(startingMsg, NotificationType.INFO)
                    try {
                        val importedBook = fileImporter.processBookImport(path)
                        if (importedBook != null) {
                            LibraryRepository.addBook(importedBook)
                            val successMsg = org.jetbrains.compose.resources.getString(Res.string.import_success)
                            notificationState.show(successMsg)
                        } else {
                            val failedMsg = org.jetbrains.compose.resources.getString(Res.string.import_failed)
                            notificationState.show(failedMsg, NotificationType.ERROR)
                        }
                    } catch (e: Exception) {
                        val message = e.message ?: ""
                        if (message.startsWith("BookAlreadyExists")) {
                            val parts = message.split(":")
                            if (parts.size >= 2) {
                                val bookId = parts[1]
                                val existsMsg = org.jetbrains.compose.resources.getString(Res.string.import_failed_exists)
                                notificationState.show(existsMsg, NotificationType.ERROR)
                                viewModel.navigateToBook(bookId)
                            } else {
                                val existsMsg = org.jetbrains.compose.resources.getString(Res.string.import_failed_exists)
                                notificationState.show(existsMsg, NotificationType.ERROR)
                            }
                        } else {
                            val errorMsg = org.jetbrains.compose.resources.getString(Res.string.import_error, e.message ?: "")
                            notificationState.show(errorMsg, NotificationType.ERROR)
                        }
                    }
                }
            }
        )
    }


    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // 点击屏幕任意空白或非消费事件区域时，清除输入框焦点，隐藏光标与键盘
                detectTapGestures(onTap = {
                    closeSearchAndClearQuery()
                })
            }
    ) {
        if (isAndroidBetween6And9) {
            // 不可见的焦点“诱饵”组件，用于打破 Android 6-9 的自动回弹循环
            Box(
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(dummyFocusRequester)
                    .focusable()
            )
        }

        val screenWidthPx = with(LocalDensity.current) { maxWidth.toPx() }

        Scaffold(
            snackbarHost = { CapsuleNotification(notificationState) },
            topBar = {
                val verticalPadding = 12.dp

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = verticalPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isAndroidBetween6And9) {
                            OutlinedTextField(
                                value = searchTextFieldValue,
                                onValueChange = { 
                                    searchTextFieldValue = it
                                    if (uiState.searchQuery != it.text) {
                                        viewModel.onSearchQueryChange(it.text)
                                    }
                                },
                                placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .onFocusChanged { 
                                        isSearchFocused = it.isFocused 
                                        if (it.isFocused) isSearchConfirmed = false
                                    },
                                textStyle = LocalTextStyle.current.copy(
                                    color = if (isSearchFocused || isSearchConfirmed) MaterialTheme.colorScheme.onSurface 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(26.dp),
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                trailingIcon = {
                                    Surface(
                                        modifier = Modifier.padding(end = 6.dp).size(40.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        onClick = onSettingsClick
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "D",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { 
                                    isSearchConfirmed = true
                                    keyboardController?.hide()
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        } else {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .onFocusChanged { 
                                        isSearchFocused = it.isFocused 
                                        if (it.isFocused) isSearchConfirmed = false
                                    },
                                textStyle = LocalTextStyle.current.copy(
                                    color = if (isSearchFocused || isSearchConfirmed) MaterialTheme.colorScheme.onSurface 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(26.dp),
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                trailingIcon = {
                                    Surface(
                                        modifier = Modifier.padding(end = 6.dp).size(40.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        onClick = onSettingsClick
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "D",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { 
                                    isSearchConfirmed = true
                                    keyboardController?.hide()
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AnimatedVisibility(
                        visible = showScrollToTopState.value,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                closeSearchAndClearQuery()
                                scope.launch { listState.animateScrollToItem(0) }
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(Res.string.back_to_top))
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            closeSearchAndClearQuery()
                            if (permissionHandler.isStoragePermissionGranted()) {
                                showFilePicker = true
                            } else {
                                showPermissionRationale = true
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_book))
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                if (uiState.recentBooks.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(Res.string.continue_reading),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            LazyRow(
                                state = recentBooksState,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 12.dp),
                                modifier = Modifier.fillMaxWidth().offsetGridPadding() // 使用辅助 Modifier 抵消网格边距
                            ) {
                                items(
                                    items = uiState.recentBooks,
                                    key = { it.id }
                                ) { book ->
                                    RecentBookItemWrapper(
                                        modifier = Modifier.animateItem(),
                                        book = book,
                                        onBookClick = {
                                            closeSearchAndClearQuery()
                                            onBookClick(book)
                                        },
                                        onRemove = { LibraryRepository.removeBook(it) },
                                        onEdit = { showEditDialog = it },
                                        onTags = { showTagDialog = it },
                                        onNotes = { noteBook, rect ->
                                            annotationBook = noteBook
                                            readerState.activeDialog = ReaderDialogType.ANNOTATIONS
                                            readerState.anchorBounds = rect
                                        },
                                        screenWidthPx = screenWidthPx
                                    )
                                }
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(10f)
                            .offsetGridPadding() // 抵消网格边距
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    stringResource(Res.string.all_books),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                ModernSortSwitcher(
                                    currentCriteria = uiState.sortCriteria,
                                    isAscending = uiState.isAscending,
                                    onCriteriaChange = { 
                                        closeSearchAndClearQuery()
                                        viewModel.onSortCriteriaChange(it) 
                                    },
                                    onDirectionToggle = { 
                                        closeSearchAndClearQuery()
                                        viewModel.toggleSortDirection() 
                                    }
                                )
                            }

                            LazyRow(
                                modifier = Modifier.padding(bottom = 12.dp).offsetGridPadding(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(allTags) { (key, res) ->
                                    val isSelected = uiState.selectedTags.contains(key)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { 
                                            closeSearchAndClearQuery()
                                            viewModel.onTagToggle(key) 
                                        },
                                        label = { Text(stringResource(res)) },
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                                
                                items(formatTags) { tag ->
                                    val isSelected = uiState.selectedTags.contains(tag)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { 
                                            closeSearchAndClearQuery()
                                            viewModel.onTagToggle(tag) 
                                        },
                                        label = { Text(tag) },
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.filteredBooks.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .height(300.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(Res.string.no_books_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(
                        items = uiState.filteredBooks,
                        key = { it.id }
                    ) { book ->
                        val isHighlighted = uiState.highlightedBookId == book.id
                        
                        BookGridItemWrapper(
                            book = book,
                            isHighlighted = isHighlighted,
                            onBookClick = { 
                                closeSearchAndClearQuery()
                                onBookClick(it) 
                            },
                            onRemove = { LibraryRepository.removeBook(it) },
                            onEdit = { showEditDialog = it },
                            onTags = { showTagDialog = it },
                            onNotes = { noteBook, rect ->
                                annotationBook = noteBook
                                readerState.activeDialog = ReaderDialogType.ANNOTATIONS
                                readerState.anchorBounds = rect
                            },
                            screenWidthPx = screenWidthPx,
                            onHighlightEnd = { viewModel.clearHighlight() }
                        )
                    }
                }
            }


            // 搜索结果下拉面板
            AnimatedVisibility(
                visible = isSearchFocused,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                modifier = Modifier.zIndex(100f)
            ) {
                SearchDropdownPanel(
                    query = uiState.searchQuery,
                    results = uiState.filteredBooks,
                    onBookClick = { book ->
                        closeSearchAndClearQuery()
                        onBookClick(book)
                    }
                )
            }
        }
    }

    if (showEditDialog != null) {
        BookEditDialog(
            book = showEditDialog!!,
            onDismiss = { showEditDialog = null },
            onConfirm = { title, author ->
                LibraryRepository.updateBookInfo(showEditDialog!!.id, title, author)
                showEditDialog = null
            }
        )
    }

    if (showTagDialog != null) {
            TagEditDialog(
                book = showTagDialog!!,
                allAvailableTags = categoryTags + typeTags,
                onDismiss = { showTagDialog = null },
                onConfirm = { newTags ->
                    LibraryRepository.updateBookTags(showTagDialog!!.id, newTags)
                    showTagDialog = null
                }
            )
        }

        if (readerState.activeDialog == ReaderDialogType.ANNOTATIONS && annotationBook != null) {
            ReaderAnnotationsDialog(
                anchorBounds = readerState.anchorBounds,
                annotations = annotations,
                onDismiss = { readerState.activeDialog = ReaderDialogType.NONE },
                onNavigate = { _ -> onBookClick(annotationBook!!) },
                onDeleteAnnotation = { LibraryRepository.removeAnnotation(it) }
            )
        }
    }
}

@Composable
private fun BookGridItemWrapper(
    book: Book,
    isHighlighted: Boolean,
    onBookClick: (Book) -> Unit,
    onRemove: (Book) -> Unit,
    onEdit: (Book) -> Unit,
    onTags: (Book) -> Unit,
    onNotes: (Book, Rect) -> Unit,
    screenWidthPx: Float,
    onHighlightEnd: () -> Unit
) {
    var layoutCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var touchOffset by remember { mutableStateOf(Offset.Zero) }
    var menuPosition by remember { mutableStateOf(Offset.Zero) }

    val alpha = if (isHighlighted) {
        val strobeAlpha = remember { Animatable(1f) }
        LaunchedEffect(Unit) {
            repeat(3) {
                strobeAlpha.animateTo(0.3f, tween(125))
                strobeAlpha.animateTo(1f, tween(125))
            }
            onHighlightEnd()
        }
        strobeAlpha.value
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .onGloballyPositioned { layoutCoords = it }
            .graphicsLayer {
                this.alpha = alpha
                if (isHighlighted) {
                    val scale = 1f + (1f - alpha) * 0.33f
                    scaleX = scale.coerceAtMost(1.1f)
                    scaleY = scale.coerceAtMost(1.1f)
                }
            }
    ) {
        BookItem(
            book = book,
            onClick = { onBookClick(book) },
            onLongClick = { offset ->
                touchOffset = offset
                layoutCoords?.let { menuPosition = it.localToWindow(offset) }
                showMenu = true
            }
        )

        if (showMenu) {
            val density = LocalDensity.current
            ActionMenuPopup(
                isRightSide = menuPosition.x > screenWidthPx / 2,
                isBottomSide = menuPosition.y > with(density) { 600.dp.toPx() },
                touchOffset = touchOffset,
                onEdit = { onEdit(book) },
                onTags = { onTags(book) },
                onNotes = { 
                    layoutCoords?.let { onNotes(book, it.boundsInWindow()) }
                },
                onRemove = { onRemove(book) },
                onDismiss = { showMenu = false }
            )
        }
    }
}

@Composable
private fun RecentBookItemWrapper(
    book: Book,
    onBookClick: (Book) -> Unit,
    onRemove: (Book) -> Unit,
    onEdit: (Book) -> Unit,
    onTags: (Book) -> Unit,
    onNotes: (Book, Rect) -> Unit,
    screenWidthPx: Float,
    modifier: Modifier = Modifier
) {
    var layoutCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var touchOffset by remember { mutableStateOf(Offset.Zero) }
    var menuPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { layoutCoords = it }
    ) {
        ContinueReadingCard(
            book = book,
            onClick = { onBookClick(book) },
            onLongClick = { offset ->
                touchOffset = offset
                layoutCoords?.let { menuPosition = it.localToWindow(offset) }
                showMenu = true
            }
        )

        if (showMenu) {
            val density = LocalDensity.current
            ActionMenuPopup(
                isRightSide = menuPosition.x > screenWidthPx / 2,
                isBottomSide = menuPosition.y > with(density) { 400.dp.toPx() },
                touchOffset = touchOffset,
                onEdit = { onEdit(book) },
                onTags = { onTags(book) },
                onNotes = { 
                    layoutCoords?.let { onNotes(book, it.boundsInWindow()) }
                },
                onRemove = { onRemove(book) },
                onDismiss = { showMenu = false }
            )
        }
    }
}

