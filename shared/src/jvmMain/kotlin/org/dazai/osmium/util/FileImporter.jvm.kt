package org.dazai.osmium.util

import com.darkrockstudios.libraries.mpfilepicker.MPFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.dazai.osmium.domain.model.Book
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Document

@Composable
actual fun rememberFileImporter(): FileImporter = remember { FileImporter() }

actual class FileImporter {
    private val appDir = File(System.getProperty("user.home"), ".osmium")

    actual suspend fun importFile(file: MPFile<*>): String? = withContext(Dispatchers.IO) {
        val sourceFile = File(file.path)
        if (!sourceFile.exists()) return@withContext null
        saveFile(sourceFile.inputStream(), sourceFile.extension)
    }

    actual suspend fun importFileFromPath(path: String): String? = withContext(Dispatchers.IO) {
        val sourceFile = File(path)
        if (!sourceFile.exists()) return@withContext null
        saveFile(sourceFile.inputStream(), sourceFile.extension)
    }

    actual suspend fun processBookImport(path: String): Book? = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(path)
            if (!sourceFile.exists()) return@withContext null

            val fileHash = calculateMD5(sourceFile)
            val existingBooks = org.dazai.osmium.domain.repository.LibraryRepository.books.value
            val duplicateBook = existingBooks.find { it.id == fileHash }
            if (duplicateBook != null) {
                throw IllegalStateException("BookAlreadyExists:${duplicateBook.id}:${duplicateBook.title}")
            }

            // Extract metadata from EPUB
            val metadata = extractEpubMetadata(sourceFile)
            val bookId = fileHash
            val title = metadata.title ?: sourceFile.nameWithoutExtension
            val author = metadata.author ?: "未知作者"

            val extension = sourceFile.extension
            val destFile = File(appDir, "books/$bookId.$extension")
            destFile.parentFile?.mkdirs()
            sourceFile.copyTo(destFile, overwrite = true)

            var coverPath: String? = null
            if (metadata.coverPathInZip != null) {
                try {
                    val coverFile = File(appDir, "covers/$bookId.jpg")
                    coverFile.parentFile?.mkdirs()
                    ZipFile(sourceFile).use { zip ->
                        val entry = zip.getEntry(metadata.coverPathInZip)
                        if (entry != null) {
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(coverFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            coverPath = coverFile.absolutePath
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Book(
                id = bookId,
                title = title,
                author = author,
                path = destFile.absolutePath,
                coverUrl = coverPath,
                titlePinyin = title.toPinyin(),
                titleInitials = title.toPinyinInitials(),
                importTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            if (e.message?.startsWith("BookAlreadyExists") == true) throw e
            e.printStackTrace()
            null
        }
    }

    private fun calculateMD5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read = input.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun saveFile(inputStream: InputStream, extension: String): String? {
        val fileName = "${UUID.randomUUID()}.$extension"
        val destFile = File(appDir, "books/$fileName")
        destFile.parentFile?.mkdirs()
        return try {
            inputStream.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    actual fun getRootPath(): String = System.getProperty("user.home")

    actual fun listFiles(path: String): List<PlatformFile> {
        val root = File(path)
        val supportedExtensions = listOf("epub", "pdf")

        fun hasSupportedFiles(dir: File): Boolean {
            val items = dir.listFiles() ?: return false
            for (item in items) {
                if (item.name.startsWith(".")) continue
                if (item.isDirectory) {
                    if (hasSupportedFiles(item)) return true
                } else {
                    if (supportedExtensions.contains(item.extension.lowercase())) return true
                }
            }
            return false
        }

        return root.listFiles()?.filter {
            if (it.name.startsWith(".")) return@filter false
            if (it.isDirectory) {
                hasSupportedFiles(it)
            } else {
                supportedExtensions.contains(it.extension.lowercase())
            }
        }?.map {
            PlatformFile(it.name, it.absolutePath, it.isDirectory, it.extension)
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
    }

    private data class EpubMetadata(
        val title: String? = null,
        val author: String? = null,
        val coverPathInZip: String? = null
    )

    private fun extractEpubMetadata(file: File): EpubMetadata {
        if (!file.extension.equals("epub", ignoreCase = true)) return EpubMetadata()

        try {
            ZipFile(file).use { zip ->
                // 1. Find rootfile in container.xml
                val containerEntry = zip.getEntry("META-INF/container.xml") ?: return EpubMetadata()
                val containerDoc = parseXml(zip.getInputStream(containerEntry))
                val opfPath = containerDoc.getElementsByTagName("rootfile").item(0)
                    ?.attributes?.getNamedItem("full-path")?.nodeValue ?: return EpubMetadata()

                // 2. Parse OPF for metadata
                val opfEntry = zip.getEntry(opfPath) ?: return EpubMetadata()
                val opfDoc = parseXml(zip.getInputStream(opfEntry))
                
                val title = opfDoc.getElementsByTagName("dc:title").item(0)?.textContent
                val author = opfDoc.getElementsByTagName("dc:creator").item(0)?.textContent

                // 3. Find cover image
                var coverPath: String? = null
                val metaTags = opfDoc.getElementsByTagName("meta")
                var coverId: String? = null
                for (i in 0 until metaTags.length) {
                    val meta = metaTags.item(i)
                    if (meta.attributes?.getNamedItem("name")?.nodeValue == "cover") {
                        coverId = meta.attributes?.getNamedItem("content")?.nodeValue
                        break
                    }
                }

                val items = opfDoc.getElementsByTagName("item")
                if (coverId != null) {
                    for (i in 0 until items.length) {
                        val item = items.item(i)
                        if (item.attributes?.getNamedItem("id")?.nodeValue == coverId) {
                            val href = item.attributes?.getNamedItem("href")?.nodeValue
                            if (href != null) {
                                coverPath = resolveRelativePath(opfPath, href)
                            }
                            break
                        }
                    }
                } else {
                    // Fallback: look for item with properties="cover-image" (EPUB 3)
                    for (i in 0 until items.length) {
                        val item = items.item(i)
                        if (item.attributes?.getNamedItem("properties")?.nodeValue == "cover-image") {
                            val href = item.attributes?.getNamedItem("href")?.nodeValue
                            if (href != null) {
                                coverPath = resolveRelativePath(opfPath, href)
                            }
                            break
                        }
                    }
                }

                return EpubMetadata(title, author, coverPath)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return EpubMetadata()
    }

    private fun parseXml(inputStream: InputStream): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false // Simplified parsing
        val builder = factory.newDocumentBuilder()
        return builder.parse(inputStream)
    }

    private fun resolveRelativePath(basePath: String, relativePath: String): String {
        if (relativePath.startsWith("/")) return relativePath.drop(1)
        val parent = File(basePath).parent?.replace("\\", "/") ?: ""
        return if (parent.isEmpty()) relativePath else "$parent/$relativePath"
    }
}

