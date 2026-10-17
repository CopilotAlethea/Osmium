package org.dazai.osmium.util

import androidx.compose.runtime.Composable
import com.darkrockstudios.libraries.mpfilepicker.MPFile
import org.dazai.osmium.domain.model.Book

@Composable
expect fun rememberFileImporter(): FileImporter

expect class FileImporter {
    suspend fun importFile(file: MPFile<*>): String?
    suspend fun importFileFromPath(path: String): String?
    suspend fun processBookImport(path: String): Book?
    fun getRootPath(): String
    fun listFiles(path: String): List<PlatformFile>
}

data class PlatformFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val extension: String = ""
)

