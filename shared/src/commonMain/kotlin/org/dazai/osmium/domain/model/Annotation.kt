package org.dazai.osmium.domain.model

data class Annotation(
    val id: String,
    val bookId: String,
    val href: String,
    val mediaType: String,
    val locations: String, // JSON
    val text: String, // JSON
    val title: String? = null,
    val totalProgression: Double = 0.0,
    val type: AnnotationType,
    val style: String? = null, // marker, straight, wavy
    val color: Int? = null,
    val note: String? = null,
    val chapter: String? = null,
    val chapterProgression: Double? = null,
    val timestamp: Long
)

enum class AnnotationType {
    HIGHLIGHT, UNDERLINE, NOTE
}

