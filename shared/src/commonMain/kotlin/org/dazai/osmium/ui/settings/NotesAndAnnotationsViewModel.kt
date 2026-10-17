package org.dazai.osmium.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import org.dazai.osmium.domain.model.Book
import org.dazai.osmium.domain.repository.LibraryRepository
import org.dazai.osmium.ui.library.LibrarySortCriteria

data class BookAnnotationStats(
    val book: Book,
    val annotationCount: Int,
    val noteCount: Int
)

data class NotesAndAnnotationsUiState(
    val booksWithStats: List<BookAnnotationStats> = emptyList(),
    val filteredBooks: List<BookAnnotationStats> = emptyList(),
    val sortCriteria: LibrarySortCriteria = LibrarySortCriteria.LastRead,
    val isAscending: Boolean = false,
    val filterType: AnnotationFilterType = AnnotationFilterType.ALL,
    val selectedTags: Set<String> = setOf("all")
)

enum class AnnotationFilterType {
    ALL, WITH_ANNOTATIONS, WITHOUT_ANNOTATIONS
}

class NotesAndAnnotationsViewModel : ViewModel() {
    private val internalSortCriteria = MutableStateFlow(LibrarySortCriteria.LastRead)
    private val internalIsAscending = MutableStateFlow(false)
    private val internalFilterType = MutableStateFlow(AnnotationFilterType.ALL)
    private val internalSelectedTags = MutableStateFlow(setOf("all"))

    val uiState: StateFlow<NotesAndAnnotationsUiState> = combine(
        LibraryRepository.books,
        LibraryRepository.getAnnotationStats(),
        internalSortCriteria,
        internalIsAscending,
        internalFilterType,
        internalSelectedTags
    ) { params ->
        val books = params[0] as List<Book>
        val stats = params[1] as Map<String, Map<String, Int>>
        val sort = params[2] as LibrarySortCriteria
        val asc = params[3] as Boolean
        val filter = params[4] as AnnotationFilterType
        val tags = params[5] as Set<String>

        val booksWithStats = books.map { book ->
            val bookStats = stats[book.id] ?: emptyMap()
            val annCount = (bookStats["HIGHLIGHT"] ?: 0) + (bookStats["UNDERLINE"] ?: 0)
            val noteCount = bookStats["NOTE"] ?: 0
            BookAnnotationStats(book, annCount, noteCount)
        }

        val filtered = filterBooks(booksWithStats, filter, tags)
        val sorted = sortBooks(filtered, sort, asc)

        NotesAndAnnotationsUiState(
            booksWithStats = booksWithStats,
            filteredBooks = sorted,
            sortCriteria = sort,
            isAscending = asc,
            filterType = filter,
            selectedTags = tags
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotesAndAnnotationsUiState())

    fun onSortCriteriaChange(criteria: LibrarySortCriteria) {
        internalSortCriteria.value = criteria
    }

    fun toggleSortDirection() {
        internalIsAscending.value = !internalIsAscending.value
    }

    fun onFilterTypeChange(type: AnnotationFilterType) {
        internalFilterType.value = type
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

    private fun filterBooks(list: List<BookAnnotationStats>, filter: AnnotationFilterType, tags: Set<String>): List<BookAnnotationStats> {
        // 核心要求：此界面只显示有“笔记”或“标注”的图书
        val baseFiltered = list.filter { it.annotationCount > 0 || it.noteCount > 0 }

        val filteredByType = when (filter) {
            AnnotationFilterType.ALL -> baseFiltered
            AnnotationFilterType.WITH_ANNOTATIONS -> baseFiltered.filter { it.annotationCount > 0 }
            AnnotationFilterType.WITHOUT_ANNOTATIONS -> baseFiltered.filter { it.annotationCount == 0 } // 虽然名字叫 WITHOUT，但在 baseFiltered 下实际上是只有 note 没有 highlight 的
        }

        if (tags.contains("all")) return filteredByType

        return filteredByType.filter { stats ->
            val book = stats.book
            val categoryTags = listOf("literature", "philosophy", "history", "psychology")
            val typeTags = listOf("books", "magazines", "articles")
            val formatTags = listOf("EPUB", "PDF")
            val relevantTags = tags.intersect((categoryTags + typeTags + formatTags).toSet())
            
            if (relevantTags.isEmpty()) true 
            else book.tags.any { it in relevantTags } || 
                 (relevantTags.contains("EPUB") && book.path.lowercase().endsWith(".epub")) ||
                 (relevantTags.contains("PDF") && book.path.lowercase().endsWith(".pdf"))
        }
    }

    private fun sortBooks(list: List<BookAnnotationStats>, criteria: LibrarySortCriteria, ascending: Boolean): List<BookAnnotationStats> {
        val comparator = when (criteria) {
            LibrarySortCriteria.Name -> compareBy<BookAnnotationStats> { it.book.title }
            LibrarySortCriteria.ImportTime -> compareBy<BookAnnotationStats> { it.book.importTimestamp }
            LibrarySortCriteria.Progress -> compareBy<BookAnnotationStats> { it.book.progress }
            LibrarySortCriteria.LastRead -> compareBy<BookAnnotationStats> { it.book.lastReadTimestamp }
            LibrarySortCriteria.Author -> compareBy<BookAnnotationStats> { it.book.author }
        }
        return if (ascending) list.sortedWith(comparator) else list.sortedWith(comparator.reversed())
    }
}

