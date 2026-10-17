package org.dazai.osmium.db

import kotlinx.coroutines.flow.Flow
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.dazai.osmium.domain.model.Book
import org.dazai.osmium.db.OsmiumDatabase
import org.dazai.osmium.db.BookEntity
import org.dazai.osmium.util.currentTimeMillis

class DatabaseHelper(databaseDriverFactory: DatabaseDriverFactory) {
    private val driver = databaseDriverFactory.createDriver()
    private val database = OsmiumDatabase(driver)
    private val queries = database.osmiumDatabaseQueries

    fun selectAllBooks(): Flow<List<BookEntity>> {
        return queries.selectAllBooks().asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun insertBook(book: Book) {
        queries.insertBook(
            id = book.id,
            title = book.title,
            author = book.author,
            path = book.path,
            coverUrl = book.coverUrl,
            progress = book.progress.toDouble(),
            tags = book.tags.joinToString(","),
            importTimestamp = book.importTimestamp,
            lastReadTimestamp = book.lastReadTimestamp,
            lastLocation = book.lastLocation,
            titlePinyin = book.titlePinyin,
            titleInitials = book.titleInitials
        )
    }

    suspend fun updateBookProgress(id: String, progress: Float, lastLocation: String?) {
        queries.updateProgress(
            progress = progress.toDouble(),
            lastReadTimestamp = currentTimeMillis(),
            lastLocation = lastLocation,
            id = id
        )
    }

    suspend fun updateBookInfo(id: String, title: String, author: String, titlePinyin: String, titleInitials: String) {
        queries.updateBookInfo(title, author, titlePinyin, titleInitials, id)
    }

    suspend fun updateBookTags(id: String, tags: String) {
        queries.updateBookTags(tags, id)
    }

    suspend fun deleteBook(id: String) {
        queries.deleteBook(id)
    }

    fun getSetting(key: String): String? {
        return queries.getSetting(key).executeAsOneOrNull()
    }

    fun getAllSettings(): Map<String, String> {
        return queries.selectAllSettings().executeAsList().associate { it.settingKey to it.settingValue }
    }

    fun insertSetting(key: String, value: String) {
        queries.insertSetting(key, value)
    }

    fun getAnnotationsForBook(bookId: String): Flow<List<AnnotationEntity>> {
        return queries.getAnnotationsForBook(bookId).asFlow().mapToList(Dispatchers.IO)
    }

    fun insertAnnotation(
        id: String,
        bookId: String,
        href: String,
        mediaType: String,
        locations: String,
        text: String,
        title: String?,
        totalProgression: Double,
        type: String,
        style: String?,
        color: Int?,
        note: String?,
        chapter: String?,
        chapterProgression: Double?
    ) {
        queries.insertAnnotation(
            id = id,
            bookId = bookId,
            href = href,
            mediaType = mediaType,
            locations = locations,
            text = text,
            title = title,
            totalProgression = totalProgression,
            type = type,
            style = style,
            color = color?.toLong(),
            note = note,
            chapter = chapter,
            chapterProgression = chapterProgression,
            timestamp = currentTimeMillis()
        )
    }

    fun deleteAnnotation(id: String) {
        queries.deleteAnnotation(id)
    }

    fun deleteAnnotationsByLocator(bookId: String, href: String, locations: String) {
        queries.deleteAnnotationsByLocator(bookId, href, locations)
    }

    fun getAnnotationStats(): Flow<List<CountAnnotationsByType>> {
        return queries.countAnnotationsByType().asFlow().mapToList(Dispatchers.IO)
    }
}

