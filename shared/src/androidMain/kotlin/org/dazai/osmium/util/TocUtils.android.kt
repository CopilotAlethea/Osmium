package org.dazai.osmium.util

import android.content.Context
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import org.json.JSONObject

actual object TocUtils {
    private var cachedBookPath: String? = null
    private var cachedToc: List<Link>? = null
    
    actual suspend fun resolveChapterName(bookPath: String, locatorJson: String): String? {
        val locator = try {
            Locator.fromJSON(JSONObject(locatorJson))
        } catch (e: Exception) {
            null
        } ?: return null

        val context = PlatformContext.context
        val toc = getToc(context, bookPath) ?: return null
        
        return findChapterTitle(toc, locator)
    }

    private suspend fun getToc(context: Context, bookPath: String): List<Link>? {
        if (cachedBookPath == bookPath && cachedToc != null) return cachedToc
        val toc = loadToc(context, bookPath)
        cachedBookPath = bookPath
        cachedToc = toc
        return toc
    }

    private fun findChapterTitle(toc: List<Link>, locator: Locator): String? {
        val flatLinks = flattenToc(toc)
        val locatorHref = locator.href.toString()
        
        // 优先匹配 href 完全相同（含 fragment）的
        flatLinks.find { it.href.toString() == locatorHref }?.title?.let { return it }
        
        // 其次匹配 href 基础路径相同（不含 fragment）的
        val locatorBaseHref = locatorHref.substringBefore("#")
        flatLinks.find { it.href.toString().substringBefore("#") == locatorBaseHref }?.title?.let { return it }
        
        return null
    }

    private fun flattenToc(links: List<Link>): List<Link> {
        val result = mutableListOf<Link>()
        for (link in links) {
            result.add(link)
            result.addAll(flattenToc(link.children))
        }
        return result
    }

    private suspend fun loadToc(context: Context, bookPath: String): List<Link>? {
        val sourceFile = File(bookPath)
        if (!sourceFile.exists()) return null

        val httpClient = DefaultHttpClient()
        val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
        val publicationOpener = PublicationOpener(
            publicationParser = DefaultPublicationParser(context, httpClient, assetRetriever, pdfFactory = null)
        )

        return try {
            val url = sourceFile.toUrl(isDirectory = false)
            val asset = assetRetriever.retrieve(url).getOrNull() ?: return null
            val publication = publicationOpener.open(asset, allowUserInteraction = false).getOrNull() ?: return null
            publication.tableOfContents
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

