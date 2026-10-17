package org.dazai.osmium.util

import com.darkrockstudios.libraries.mpfilepicker.MPFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.dazai.osmium.domain.model.Book

@Composable
actual fun rememberFileImporter(): FileImporter = remember { FileImporter() }

actual class FileImporter {
    actual suspend fun importFile(file: MPFile<*>): String? = null
    actual suspend fun importFileFromPath(path: String): String? = null
    actual suspend fun processBookImport(path: String): Book? = null
    actual fun getRootPath(): String = ""
    actual fun listFiles(path: String): List<PlatformFile> = emptyList()
}

