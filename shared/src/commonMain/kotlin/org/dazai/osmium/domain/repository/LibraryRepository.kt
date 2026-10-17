package org.dazai.osmium.domain.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dazai.osmium.db.DatabaseHelper
import org.dazai.osmium.domain.model.Book
import org.dazai.osmium.db.BookEntity
import org.dazai.osmium.db.AnnotationEntity
import org.dazai.osmium.db.CountAnnotationsByType
import org.dazai.osmium.domain.model.Annotation
import org.dazai.osmium.domain.model.AnnotationType
import org.dazai.osmium.util.toPinyin
import org.dazai.osmium.util.toPinyinInitials

object LibraryRepository {
    private var databaseHelper: DatabaseHelper? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val internalBooks = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = internalBooks.asStateFlow()

    fun initialize(helper: DatabaseHelper) {
        databaseHelper = helper
        scope.launch {
            helper.selectAllBooks().collect { entities ->
                // 将繁重的映射工作移至 Default 线程
                val books = withContext(Dispatchers.Default) {
                    entities.map { entity ->
                        Book(
                            id = entity.id,
                            title = entity.title,
                            author = entity.author,
                            path = entity.path,
                            coverUrl = entity.coverUrl,
                            progress = entity.progress.toFloat(),
                            tags = if (entity.tags.isEmpty()) emptyList() else entity.tags.split(","),
                            importTimestamp = entity.importTimestamp,
                            lastReadTimestamp = entity.lastReadTimestamp,
                            lastLocation = entity.lastLocation,
                            titlePinyin = entity.titlePinyin,
                            titleInitials = entity.titleInitials
                        )
                    }
                }
                internalBooks.value = books
                
                // 批量处理拼音更新，避免协程爆炸
                val booksToUpdate = books.filter { it.titlePinyin.isEmpty() && it.title.isNotEmpty() }
                if (booksToUpdate.isNotEmpty()) {
                    scope.launch {
                        booksToUpdate.forEach { book ->
                            val pinyin = book.title.toPinyin()
                            val initials = book.title.toPinyinInitials()
                            databaseHelper?.updateBookInfo(book.id, book.title, book.author, pinyin, initials)
                        }
                    }
                }
            }
        }
    }

    fun addBook(book: Book) {
        scope.launch {
            databaseHelper?.insertBook(book)
        }
    }

    fun getBookByIdFlow(id: String): Flow<Book?> {
        return internalBooks.map { list -> list.find { it.id == id } }
            .distinctUntilChanged()
    }

    fun removeBook(book: Book) {
        scope.launch {
            databaseHelper?.deleteBook(book.id)
        }
    }

    fun updateBookProgress(id: String, progress: Float, lastLocation: String?) {
        scope.launch {
            databaseHelper?.updateBookProgress(id, progress, lastLocation)
        }
    }

    fun updateBookInfo(id: String, title: String, author: String, titlePinyin: String? = null, titleInitials: String? = null) {
        scope.launch {
            val pinyin = titlePinyin ?: title.toPinyin()
            val initials = titleInitials ?: title.toPinyinInitials()
            databaseHelper?.updateBookInfo(id, title, author, pinyin, initials)
        }
    }

    fun updateBookTags(id: String, tags: List<String>) {
        scope.launch {
            databaseHelper?.updateBookTags(id, tags.joinToString(","))
        }
    }

    suspend fun getSetting(key: String): String? = withContext(Dispatchers.IO) {
        databaseHelper?.getSetting(key)
    }

    suspend fun getAllSettings(): Map<String, String> = withContext(Dispatchers.IO) {
        databaseHelper?.getAllSettings() ?: emptyMap()
    }

    fun insertSetting(key: String, value: String) {
        scope.launch {
            databaseHelper?.insertSetting(key, value)
        }
    }

    fun getAnnotationsForBook(bookId: String): Flow<List<Annotation>> {
        return databaseHelper?.getAnnotationsForBook(bookId)?.map { entities ->
            entities.map { entity ->
                Annotation(
                    id = entity.id,
                    bookId = entity.bookId,
                    href = entity.href,
                    mediaType = entity.mediaType,
                    locations = entity.locations,
                    text = entity.text,
                    title = entity.title,
                    totalProgression = entity.totalProgression,
                    type = AnnotationType.valueOf(entity.type),
                    style = entity.style,
                    color = entity.color?.toInt(),
                    note = entity.note,
                    chapter = entity.chapter,
                    chapterProgression = entity.chapterProgression,
                    timestamp = entity.timestamp
                )
            }
        } ?: flowOf(emptyList())
    }

    fun addAnnotation(annotation: Annotation) {
        scope.launch {
            databaseHelper?.insertAnnotation(
                id = annotation.id,
                bookId = annotation.bookId,
                href = annotation.href,
                mediaType = annotation.mediaType,
                locations = annotation.locations,
                text = annotation.text,
                title = annotation.title,
                totalProgression = annotation.totalProgression,
                type = annotation.type.name,
                style = annotation.style,
                color = annotation.color,
                note = annotation.note,
                chapter = annotation.chapter,
                chapterProgression = annotation.chapterProgression
            )
        }
    }

    fun removeAnnotation(id: String) {
        scope.launch {
            databaseHelper?.deleteAnnotation(id)
        }
    }

    fun removeAnnotationsByLocator(bookId: String, href: String, locations: String) {
        scope.launch {
            // This is a simplified version. Ideally we'd find overlapping locators.
            // For now, we find annotations with the exact same locator or overlapping ranges.
            // Since complex overlap detection is hard in SQL, we'll fetch all and filter or just match exact.
            databaseHelper?.deleteAnnotationsByLocator(bookId, href, locations)
        }
    }

    fun getAnnotationStats(): Flow<Map<String, Map<String, Int>>> {
        return databaseHelper?.getAnnotationStats()?.map { list ->
            list.groupBy { it.bookId }.mapValues { entry ->
                entry.value.associate { it.type to it.total.toInt() }
            }
        } ?: flowOf(emptyMap())
    }
}

