package org.dazai.osmium.domain.model

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val path: String,
    val coverUrl: String? = null,
    val progress: Float = 0f,
    val tags: List<String> = emptyList(),
    val importTimestamp: Long = 0,
    val lastReadTimestamp: Long = 0,
    val lastLocation: String? = null,
    val titlePinyin: String = "",
    val titleInitials: String = ""
)

