package org.dazai.osmium.ui.reader

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dazai.osmium.domain.repository.LibraryRepository
import org.jetbrains.compose.resources.getString
import osmium.shared.generated.resources.*
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.SearchService
import org.readium.r2.shared.publication.services.search.StringSearchService
import java.io.File
import kotlin.math.abs

@OptIn(ExperimentalReadiumApi::class, kotlinx.coroutines.FlowPreview::class)
@Stable
class BookReaderModel(
    val bookId: String,
    context: Context,
    private val scope: CoroutineScope,
    val state: ReaderState
) {
    var publication by mutableStateOf<Publication?>(null)
        private set
    var allPositions by mutableStateOf<List<Locator>>(emptyList())
        private set
    var fragmentTag by mutableStateOf("")
        private set
    var currentLocator by mutableStateOf<Locator?>(null)
    var readerPrimaryColor by mutableIntStateOf(0)

    private val _currentReadPosition = MutableStateFlow<Locator?>(null)
    private val navigationHistory = mutableListOf<Locator>()
    private var lastSaveTimestamp = 0L
    private var lastStableLocator: Locator? = null
    private var lastTurnTimestamp = 0L
    private var lastJumpTimestamp = 0L
    private var fastPageCount = 0
    private var isFastSkipping = false
    private var skipCheckJob: kotlinx.coroutines.Job? = null
    private var stableUpdateJob: kotlinx.coroutines.Job? = null
    private var autoCommitJob: kotlinx.coroutines.Job? = null
    private var searchIterator: SearchIterator? = null
    private var searchJob: kotlinx.coroutines.Job? = null
    private var lastPrefetchedHref: String? = null

    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
    private val publicationParser = DefaultPublicationParser(context, httpClient, assetRetriever, pdfFactory = null)
    private val publicationOpener = PublicationOpener(
        publicationParser,
        emptyList()
    ) {
        servicesBuilder[SearchService::class] = StringSearchService.createDefaultFactory()
    }

    init {
        // Debounced Persistence
        scope.launch(Dispatchers.IO) {
            _currentReadPosition
                .filterNotNull()
                .debounce(ReaderConstants.AutoSaveIntervalMs)
                .collect { locator ->
                    LibraryRepository.updateBookProgress(bookId, state.progression, locator.toJSON().toString())
                    lastSaveTimestamp = System.currentTimeMillis()
                }
        }
    }

    fun loadBook(path: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val file = File(path)
                if (file.exists()) {
                    val url = file.toUrl(isDirectory = false)
                    val assetResult = assetRetriever.retrieve(url)
                    val asset = assetResult.getOrNull()
                    if (asset != null) {
                        val openResult = publicationOpener.open(asset, allowUserInteraction = false)
                        openResult.onSuccess { pub ->
                            withContext(Dispatchers.Main) {
                                publication = pub
                                fragmentTag = "navigator_${pub.metadata.title.hashCode()}"
                                allPositions = pub.positions()
                            }
                        }.onFailure { 
                            withContext(Dispatchers.Main) { state.error = getString(Res.string.error_open_failed) }
                        }
                    } else { 
                        withContext(Dispatchers.Main) { state.error = getString(Res.string.error_read_failed) }
                    }
                } else { 
                    withContext(Dispatchers.Main) { state.error = getString(Res.string.error_file_not_found, path) }
                }
            } catch (e: Exception) { 
                withContext(Dispatchers.Main) { state.error = e.message }
            } finally { 
                withContext(Dispatchers.Main) { state.isLoading = false }
            }
        }
    }

    fun deleteAnnotation(id: String) {
        scope.launch {
            LibraryRepository.removeAnnotation(id)
        }
    }

    fun saveProgress(force: Boolean = false) {
        val locator = currentLocator ?: return
        if (force) {
            LibraryRepository.updateBookProgress(bookId, state.progression, locator.toJSON().toString())
            lastSaveTimestamp = System.currentTimeMillis()
        } else {
            _currentReadPosition.value = locator
        }
    }

    fun jumpTo(navigator: EpubNavigatorFragment, locator: Locator, isMajor: Boolean = true) {
        lastJumpTimestamp = System.currentTimeMillis()
        if (isMajor) {
            currentLocator?.let {
                if (it.href != locator.href || abs((it.locations.progression ?: 0.0) - (locator.locations.progression ?: 0.0)) > 0.01) {
                    navigationHistory.add(it)
                    state.backLocationName = it.title ?: state.currentHierarchy.lastOrNull()
                    state.canGoBack = true
                }
            }
        }
        navigator.go(locator, animated = true)
        saveProgress(force = true)
    }

    fun goBack(navigator: EpubNavigatorFragment) {
        lastJumpTimestamp = System.currentTimeMillis()
        val prev = navigationHistory.removeLastOrNull()
        if (prev != null) {
            navigator.go(prev, animated = true)
            saveProgress(force = true)
        }
        state.canGoBack = navigationHistory.isNotEmpty()
        
        if (state.canGoBack) {
            state.backLocationName = navigationHistory.lastOrNull()?.title ?: state.currentHierarchy.lastOrNull()
        }
        
        isFastSkipping = false
        fastPageCount = 0
    }

    fun clearHistory() {
        navigationHistory.clear()
        state.canGoBack = false
        lastStableLocator = currentLocator
        fastPageCount = 0
        isFastSkipping = false
        saveProgress(force = true)
    }

    fun startSearch(query: String) {
        searchJob?.cancel()
        val pub = publication ?: return
        if (query.isBlank()) {
            state.searchResults.clear()
            state.isSearching = false
            state.isSearchExhausted = false
            return
        }
        
        searchJob = scope.launch(Dispatchers.IO) {
            delay(500)
            withContext(Dispatchers.Main) { 
                state.isSearching = true 
                state.searchResults.clear()
                state.isSearchExhausted = false
            }
            
            searchIterator?.close()
            val iterator = try { pub.search(query) } catch (e: Exception) { null }
            if (iterator == null) {
                withContext(Dispatchers.Main) { state.isSearching = false }
                return@launch
            }
            searchIterator = iterator
            loadNextSearchResults()
        }
    }

    private suspend fun loadNextSearchResults() {
        val iterator = searchIterator ?: return
        try {
            val collection = iterator.next().getOrNull()
            withContext(Dispatchers.Main) {
                if (collection != null && collection.locators.isNotEmpty()) {
                    val mapped = collection.locators.map { locator ->
                        ReaderSearchResult(
                            before = locator.text.before,
                            highlight = locator.text.highlight,
                            after = locator.text.after,
                            title = locator.title,
                            locatorJson = locator.toJSON().toString()
                        )
                    }
                    state.searchResults.addAll(mapped)
                } else {
                    state.isSearchExhausted = true
                }
            }
        } catch (e: Exception) {
            Log.e("BookReaderModel", "Search error", e)
        } finally {
            withContext(Dispatchers.Main) { state.isSearching = false }
        }
    }

    fun loadMoreSearch() {
        if (state.isSearching) return
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { state.isSearching = true }
            loadNextSearchResults()
        }
    }

    fun closeSearch() {
        scope.launch(Dispatchers.IO) {
            searchIterator?.close()
            searchIterator = null
            withContext(Dispatchers.Main) {
                state.searchResults.clear()
                state.searchQuery = ""
                state.isSearchExhausted = false
                state.activeDialog = ReaderDialogType.NONE
            }
        }
    }

    fun observeNavigator(navigator: EpubNavigatorFragment) {
        val pub = publication ?: return
        val toc = pub.tableOfContents

        val decorationListener = object : DecorableNavigator.Listener {
            override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
                val id = event.decoration.id
                state.activeAnnotationId = if (id.endsWith("-note")) id.removeSuffix("-note") else id
                event.rect?.let { r ->
                    state.activeAnnotationRect = SelectionRect(r.left, r.top, r.right, r.bottom)
                }
                state.showAnnotationActions = true
                return true
            }
        }
        navigator.addDecorationListener("highlights", decorationListener)
        navigator.addDecorationListener("notes", decorationListener)

        scope.launch {
            snapshotFlow { state.activeAnnotationId }
                .combine(LibraryRepository.getAnnotationsForBook(bookId).distinctUntilChanged()) { activeId, annotations ->
                    activeId to annotations
                }
                .conflate()
                .collect { (activeId, annotations) ->
                    withContext(Dispatchers.Main) {
                        val highlights = mutableListOf<Decoration>()
                        val notes = mutableListOf<Decoration>()

                        annotations.forEach { annot ->
                            val locator = annot.toLocator() ?: return@forEach
                            val isActive = (annot.id == activeId)
                            val isNote = !annot.note.isNullOrEmpty() || annot.type == org.dazai.osmium.domain.model.AnnotationType.NOTE
                            
                            val color = if (isNote && readerPrimaryColor != 0) {
                                readerPrimaryColor
                            } else {
                                annot.color ?: 0xFFFFE082.toInt()
                            }

                            // 1. Text highlight/underline decoration
                            val style = if (isNote || annot.type == org.dazai.osmium.domain.model.AnnotationType.UNDERLINE) {
                                Decoration.Style.Underline(tint = color, isActive = isActive)
                            } else {
                                Decoration.Style.Highlight(tint = color, isActive = isActive)
                            }

                            highlights.add(Decoration(
                                id = annot.id,
                                locator = locator,
                                style = style
                            ))

                            // 2. Margin icon decoration if it has a note
                            if (!annot.note.isNullOrEmpty()) {
                                notes.add(Decoration(
                                    id = "${annot.id}-note",
                                    locator = locator,
                                    style = DecorationStyleAnnotationMark(tint = color, isActive = isActive)
                                ))
                            }
                        }
                        
                        navigator.applyDecorations(highlights, group = "highlights")
                        navigator.applyDecorations(notes, group = "notes")
                    }
                }
        }

        scope.launch {
            navigator.currentLocator.collectLatest { locator ->
                val now = System.currentTimeMillis()
                val oldLocator = currentLocator
                currentLocator = locator
                
                // 1. UI Updates
                val pageIdx = allPositions.indexOfFirst { it.href == locator.href && (it.locations.progression ?: 0.0) >= (locator.locations.progression ?: 0.0) }
                val currentPosIdx = if (pageIdx != -1) pageIdx else (locator.locations.position?.minus(1) ?: 0)
                state.currentHierarchy = findHierarchyPath(toc, allPositions, currentPosIdx) ?: emptyList()
                state.progression = locator.locations.totalProgression?.toFloat() ?: 0f

                // 2. Dual State Machine & Quick Back Logic
                if (oldLocator != null) {
                    val timeOnPage = now - lastTurnTimestamp
                    
                    if (timeOnPage < ReaderConstants.FastPageTurnThresholdMs) { 
                        // Skimming Detection
                        fastPageCount++
                        if (fastPageCount >= ReaderConstants.FastSkipCountThreshold) {
                            if (!isFastSkipping) {
                                isFastSkipping = true
                                // Lock the stable anchor by not updating it, and hide existing button
                                state.canGoBack = false
                                stableUpdateJob?.cancel()
                            }
                        }
                    }
                    
                    // a. Update Stable Anchor after pause
                    if (!isFastSkipping) {
                        stableUpdateJob?.cancel()
                        stableUpdateJob = scope.launch {
                            delay(ReaderConstants.StableReadThresholdMs)
                            lastStableLocator = locator
                            fastPageCount = 0
                        }
                    }

                    // b. Stop Check
                    skipCheckJob?.cancel()
                    skipCheckJob = scope.launch {
                        delay(ReaderConstants.PageStopCheckDelayMs)
                        if (isFastSkipping) {
                            lastStableLocator?.let { stable ->
                                val stablePageIndex = findPositionIndex(stable, allPositions)
                                val currentPageIndex = findPositionIndex(locator, allPositions)
                                
                                val pageDistance = if (stablePageIndex != -1 && currentPageIndex != -1) {
                                    abs(stablePageIndex - currentPageIndex)
                                } else {
                                    -1
                                }
                                
                                val shouldShow = if (pageDistance != -1) {
                                    pageDistance >= ReaderConstants.FastSkipPageThreshold || stable.href != locator.href
                                } else {
                                    val dist = abs((stable.locations.totalProgression ?: 0.0) - (locator.locations.totalProgression ?: 0.0))
                                    dist > 0.01 || stable.href != locator.href
                                }

                                if (shouldShow) {
                                    if (navigationHistory.lastOrNull() != stable) {
                                        navigationHistory.add(stable)
                                        state.backLocationName = stable.title ?: ""
                                        state.backLocationChapter = findChapterTitle(toc, allPositions, stablePageIndex)
                                        state.canGoBack = true
                                    }
                                }
                            }
                            fastPageCount = 0
                            isFastSkipping = false
                        }
                    }

                    // c. Auto-Commit (Stay at new place long enough -> hide back button)
                    if (state.canGoBack) {
                        autoCommitJob?.cancel()
                        autoCommitJob = scope.launch {
                            // If the user stays here for 5 seconds without clicking back
                            delay(5000L) 
                            state.canGoBack = false
                            navigationHistory.clear()
                            lastStableLocator = locator
                        }
                    }
                } else {
                    lastStableLocator = locator
                }
                
                lastTurnTimestamp = now

                // 3. Debounced Progress Save
                saveProgress()

                // 4. Prefetch Next Chapter
                val chapterProgression = locator.locations.progression ?: 0.0
                if (chapterProgression > 0.8) {
                    prefetchNextResource(locator)
                }
            }
        }
    }

    private fun prefetchNextResource(locator: Locator) {
        val pub = publication ?: return
        val readingOrder = pub.readingOrder
        val currentIndex = readingOrder.indexOfFirst { it.url().toString() == locator.href.toString() }
        if (currentIndex != -1 && currentIndex < readingOrder.size - 1) {
            val nextLink = readingOrder[currentIndex + 1]
            val nextHref = nextLink.url().toString()
            if (nextHref == lastPrefetchedHref) return
            
            lastPrefetchedHref = nextHref
            scope.launch(Dispatchers.IO) {
                try {
                    pub.get(nextLink)?.read()?.getOrNull()
                } catch (e: Exception) {
                    // Ignore prefetch errors
                }
            }
        }
    }
}

@Composable
fun rememberBookReaderModel(bookId: String, state: ReaderState): BookReaderModel {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(bookId) { BookReaderModel(bookId, context, scope, state) }
}

private fun findHierarchyPath(links: List<Link>, positions: List<Locator>, currentPosIdx: Int): List<String>? {
    val activeLink = links.findLast { link ->
        val entryPosIdx = positions.indexOfFirst { it.href.toString().endsWith(link.href.toString()) }
        entryPosIdx != -1 && entryPosIdx <= currentPosIdx
    }
    if (activeLink != null) {
        val currentTitle = activeLink.title ?: ""
        val subPath = findHierarchyPath(activeLink.children, positions, currentPosIdx)
        return if (subPath != null) listOf(currentTitle) + subPath else listOf(currentTitle)
    }
    return null
}

private fun findChapterTitle(links: List<Link>, positions: List<Locator>, posIdx: Int): String? {
    if (posIdx == -1) return null
    val activeLink = links.findLast { link ->
        val entryPosIdx = positions.indexOfFirst { it.href.toString().endsWith(link.href.toString()) }
        entryPosIdx != -1 && entryPosIdx <= posIdx
    }
    return activeLink?.title
}

private fun findPositionIndex(locator: Locator?, positions: List<Locator>): Int {
    if (locator == null || positions.isEmpty()) return -1
    val hrefPositions = positions.filter { it.href == locator.href }
    if (hrefPositions.isEmpty()) return -1
    
    val targetProgression = locator.locations.progression ?: 0.0
    val matchingPos = hrefPositions.lastOrNull { (it.locations.progression ?: 0.0) <= targetProgression }
        ?: hrefPositions.first()
        
    return positions.indexOf(matchingPos)
}

