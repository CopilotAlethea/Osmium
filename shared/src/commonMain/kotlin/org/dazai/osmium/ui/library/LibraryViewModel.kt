package org.dazai.osmium.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import org.dazai.osmium.domain.model.Book
import org.dazai.osmium.domain.repository.LibraryRepository
import org.dazai.osmium.util.normalize
import org.dazai.osmium.util.subsequenceMatch
import org.jetbrains.compose.resources.StringResource
import osmium.shared.generated.resources.*

enum class LibrarySortCriteria(val labelRes: StringResource) {
    Name(Res.string.book_title),
    Author(Res.string.book_author),
    LastRead(Res.string.sort_last_read),
    Progress(Res.string.sort_progress),
    ImportTime(Res.string.sort_import_time)
}

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val filteredBooks: List<Book> = emptyList(),
    val recentBooks: List<Book> = emptyList(),
    val searchQuery: String = "",
    val selectedTags: Set<String> = setOf("all"),
    val sortCriteria: LibrarySortCriteria = LibrarySortCriteria.Name,
    val isAscending: Boolean = true,
    val pendingNavigationBookId: String? = null,
    val highlightedBookId: String? = null
)

class LibraryViewModel : ViewModel() {
    private val internalSearchQuery = MutableStateFlow("")
    private val internalSelectedTags = MutableStateFlow(setOf("all"))
    private val internalSortCriteria = MutableStateFlow(LibrarySortCriteria.Name)
    private val internalIsAscending = MutableStateFlow(true)
    private val internalPendingNavigationBookId = MutableStateFlow<String?>(null)
    private val internalHighlightedBookId = MutableStateFlow<String?>(null)

    private val knownTagSet = setOf("literature", "philosophy", "history", "psychology", "books", "magazines", "articles", "EPUB", "PDF")

    @OptIn(FlowPreview::class)
    val uiState: StateFlow<LibraryUiState> = combine(
        LibraryRepository.books,
        internalSearchQuery.debounce(200),
        internalSelectedTags,
        internalSortCriteria,
        internalIsAscending,
        internalPendingNavigationBookId,
        internalHighlightedBookId
    ) { params: Array<Any?> ->
        val books = params[0] as List<Book>
        val query = params[1] as String
        val tags = params[2] as Set<String>
        val criteria = params[3] as LibrarySortCriteria
        val ascending = params[4] as Boolean
        val pendingId = params[5] as String?
        val highlightedId = params[6] as String?

        val filtered = filterBooks(books, query, tags)
        val sorted = sortBooks(filtered, criteria, ascending)
        val recent = books.asSequence()
            .filter { it.progress > 0 || it.lastReadTimestamp > 0 }
            .sortedByDescending { it.lastReadTimestamp }
            .take(5)
            .toList()

        LibraryUiState(
            books = books,
            filteredBooks = sorted,
            recentBooks = recent,
            searchQuery = query,
            selectedTags = tags,
            sortCriteria = criteria,
            isAscending = ascending,
            pendingNavigationBookId = pendingId,
            highlightedBookId = highlightedId
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    fun onSearchQueryChange(query: String) {
        internalSearchQuery.value = query
    }

    fun onTagToggle(tag: String) {
        val current = internalSelectedTags.value
        if (tag == "all") {
            internalSelectedTags.value = setOf("all")
        } else {
            val newSet = current.toMutableSet()
            if (newSet.contains("all")) newSet.remove("all")
            if (current.contains(tag)) newSet.remove(tag) else newSet.add(tag)
            if (newSet.isEmpty()) newSet.add("all")
            internalSelectedTags.value = newSet
        }
    }

    fun onSortCriteriaChange(criteria: LibrarySortCriteria) {
        internalSortCriteria.value = criteria
    }

    fun toggleSortDirection() {
        internalIsAscending.value = !internalIsAscending.value
    }

    fun navigateToBook(bookId: String) {
        internalPendingNavigationBookId.value = bookId
    }

    fun onNavigationConsumed() {
        internalPendingNavigationBookId.value = null
    }

    fun triggerHighlight(bookId: String) {
        internalHighlightedBookId.value = bookId
    }

    fun clearHighlight() {
        internalHighlightedBookId.value = null
    }

    private fun filterBooks(books: List<Book>, query: String, tags: Set<String>): List<Book> {
        val filteredByTags = books.filter { book ->
            if (tags.contains("all")) {
                true
            } else {
                val relevantTags = tags.intersect(knownTagSet)
                
                if (relevantTags.isEmpty()) true 
                else book.tags.any { it in relevantTags } || 
                     (relevantTags.contains("EPUB") && book.path.lowercase().endsWith(".epub")) ||
                     (relevantTags.contains("PDF") && book.path.lowercase().endsWith(".pdf"))
            }
        }

        if (query.isEmpty()) return filteredByTags

        val normalizedQuery = query.normalize()

        return filteredByTags.filter { book ->
            val normalizedTitle = book.title.normalize()
            val normalizedAuthor = book.author.normalize()
            
            normalizedTitle.contains(normalizedQuery) || 
            normalizedAuthor.contains(normalizedQuery) ||
            book.titlePinyin.contains(normalizedQuery) ||
            book.titleInitials.contains(normalizedQuery) ||
            book.title.subsequenceMatch(query)
        }.sortedByDescending { calculateScore(it, query) }
    }

    private fun calculateScore(book: Book, query: String): Int {
        val q = query.normalize()
        val title = book.title.normalize()
        val author = book.author.normalize()
        val pinyin = book.titlePinyin.lowercase()
        val initials = book.titleInitials.lowercase()

        var score = 0
        if (title == q) score += 1000
        else if (title.startsWith(q)) score += 500
        else if (title.contains(q)) score += 300

        if (author == q) score += 800
        else if (author.startsWith(q)) score += 400
        else if (author.contains(q)) score += 200

        if (pinyin == q) score += 250
        else if (pinyin.startsWith(q)) score += 150
        
        if (initials == q) score += 200
        else if (initials.startsWith(q)) score += 100
        
        if (book.title.subsequenceMatch(query)) score += 50

        return score
    }

    private fun sortBooks(books: List<Book>, criteria: LibrarySortCriteria, ascending: Boolean): List<Book> {
        val comparator = when (criteria) {
            LibrarySortCriteria.Name -> compareBy<Book> { it.title }
            LibrarySortCriteria.ImportTime -> compareBy<Book> { it.importTimestamp }
            LibrarySortCriteria.Progress -> compareBy<Book> { it.progress }
            LibrarySortCriteria.LastRead -> compareBy<Book> { it.lastReadTimestamp }
            LibrarySortCriteria.Author -> compareBy<Book> { it.author }
        }
        return if (ascending) books.sortedWith(comparator) else books.sortedWith(comparator.reversed())
    }
}

