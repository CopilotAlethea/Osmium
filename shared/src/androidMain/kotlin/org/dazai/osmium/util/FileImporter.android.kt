package org.dazai.osmium.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.darkrockstudios.libraries.mpfilepicker.MPFile
import org.dazai.osmium.domain.model.Book
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.readium.r2.shared.publication.services.cover
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dazai.osmium.util.toPinyin
import org.dazai.osmium.util.toPinyinInitials

@Composable
actual fun rememberFileImporter(): FileImporter {
    val context = LocalContext.current
    return remember(context) { FileImporter(context) }
}

actual class FileImporter(private val context: Context) {
    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(context, httpClient, assetRetriever, pdfFactory = null)
    )

    actual suspend fun importFile(file: MPFile<*>): String? {
        val uri = file.platformFile as? android.net.Uri ?: return null
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            saveFile(inputStream, file.path.substringAfterLast('.', "epub"))
        } catch (e: Exception) { null }
    }

    actual suspend fun importFileFromPath(path: String): String? = null

    /**
     * [终极优化版] 基于 MD5 严格防重的书籍导入。
     * 采用“先验明正身，再放行拷贝”策略，遇到重复书籍实现 0 拷贝极速拦截。
     */
    actual suspend fun processBookImport(path: String): Book? = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(path)
            if (!sourceFile.exists()) return@withContext null

            // 1. [核心优化] 仅读取原文件流计算 MD5，绝不提前产生临时文件拷贝
            val fileHash = calculateMD5(sourceFile)

            // 2. 【防重校验】严格比对 MD5 指纹
            val existingBooks = org.dazai.osmium.domain.repository.LibraryRepository.books.value
            val duplicateBook = existingBooks.find { it.id == fileHash }
            if (duplicateBook != null) {
                throw IllegalStateException("BookAlreadyExists:${duplicateBook.id}:${duplicateBook.title}")
            }

            // 3. 用 Readium 直接解析外部原文件，提取元数据
            val url = sourceFile.toUrl(isDirectory = false)
            val asset = assetRetriever.retrieve(url).getOrNull() ?: return@withContext null
            val publication = publicationOpener.open(asset, allowUserInteraction = false).getOrNull() ?: return@withContext null
            
            // 4. [修复核心 Bug] 强制使用 MD5 作为书籍的主键 ID，彻底抛弃可能重复/混乱的 metadata.identifier
            val bookId = fileHash
            val title = publication.metadata.title?.ifBlank { null } ?: sourceFile.nameWithoutExtension
            val author = publication.metadata.authors.firstOrNull()?.name?.ifBlank { null } ?: "未知作者"
            
            // 5. 确认是新书后，执行全程唯一的一次干净文件拷贝
            val extension = sourceFile.extension
            val destFile = File(context.filesDir, "books/$bookId.$extension")
            destFile.parentFile?.mkdirs()
            sourceFile.copyTo(destFile, overwrite = true)

            // 6. 提取并保存封面
            var coverPath: String? = null
            try {
                val bitmap = publication.cover()
                if (bitmap != null) {
                    val coverFile = File(context.filesDir, "covers/$bookId.jpg")
                    coverFile.parentFile?.mkdirs()
                    java.io.FileOutputStream(coverFile).use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                    }
                    coverPath = coverFile.absolutePath
                }
            } catch (e: Exception) { e.printStackTrace() }

            Book(
                id = bookId,
                title = title,
                author = author,
                path = destFile.absolutePath,
                coverUrl = coverPath,
                titlePinyin = title.toPinyin(),
                titleInitials = title.toPinyinInitials()
            )
        } catch (e: Exception) {
            if (e.message?.startsWith("BookAlreadyExists") == true) throw e
            e.printStackTrace()
            null
        }
    }

    /**
     * 高效计算文件 MD5 的辅助函数 (替换掉原来的 copyAndGetHash)
     */
    private fun calculateMD5(file: File): String {
        val digest = java.security.MessageDigest.getInstance("MD5")
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

    private fun saveFile(inputStream: java.io.InputStream, extension: String): String? {
        val fileName = "${UUID.randomUUID()}.$extension"
        val destFile = File(context.filesDir, "books/$fileName")
        destFile.parentFile?.mkdirs()
        return try {
            inputStream.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
            destFile.absolutePath
        } catch (e: Exception) { null }
    }

    actual fun getRootPath(): String = Environment.getExternalStorageDirectory().absolutePath

    actual fun listFiles(path: String): List<PlatformFile> {
        val root = File(path)
        val supportedExtensions = listOf("epub", "pdf")

        // 递归检查文件夹是否包含支持的文件
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
}

