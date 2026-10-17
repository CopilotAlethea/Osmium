package org.dazai.osmium.util

import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.json.JSONObject

actual object TocUtils {
    actual suspend fun resolveChapterName(bookPath: String, locatorJson: String): String? {
        val locator = try {
            JSONObject(locatorJson)
        } catch (e: Exception) {
            null
        } ?: return null

        val href = locator.optString("href") ?: return null
        val baseHref = href.substringBefore("#")

        return try {
            val toc = getToc(bookPath)
            findChapterTitle(toc, baseHref)
        } catch (e: Exception) {
            null
        }
    }

    private data class TocEntry(val title: String, val href: String, val children: List<TocEntry> = emptyList())

    private fun getToc(bookPath: String): List<TocEntry> {
        val file = File(bookPath)
        if (!file.exists() || !file.extension.equals("epub", ignoreCase = true)) return emptyList()

        ZipFile(file).use { zip ->
            // 1. Find rootfile
            val containerEntry = zip.getEntry("META-INF/container.xml") ?: return emptyList()
            val containerDoc = parseXml(zip.getInputStream(containerEntry))
            val opfPath = containerDoc.getElementsByTagName("rootfile").item(0)
                ?.attributes?.getNamedItem("full-path")?.nodeValue ?: return emptyList()

            // 2. Find TOC in OPF
            val opfEntry = zip.getEntry(opfPath) ?: return emptyList()
            val opfDoc = parseXml(zip.getInputStream(opfEntry))
            
            val manifestItems = opfDoc.getElementsByTagName("item")
            var ncxPath: String? = null
            var navPath: String? = null

            for (i in 0 until manifestItems.length) {
                val item = manifestItems.item(i)
                val mediaType = item.attributes?.getNamedItem("media-type")?.nodeValue
                val properties = item.attributes?.getNamedItem("properties")?.nodeValue
                val href = item.attributes?.getNamedItem("href")?.nodeValue

                if (mediaType == "application/x-dtbncx+xml") {
                    ncxPath = resolveRelativePath(opfPath, href ?: "")
                } else if (properties?.contains("nav") == true) {
                    navPath = resolveRelativePath(opfPath, href ?: "")
                }
            }

            // 3. Parse NCX (legacy) or NAV (EPUB 3)
            return if (navPath != null) {
                parseNav(zip, navPath)
            } else if (ncxPath != null) {
                parseNcx(zip, ncxPath)
            } else {
                emptyList()
            }
        }
    }

    private fun parseNcx(zip: ZipFile, ncxPath: String): List<TocEntry> {
        val entry = zip.getEntry(ncxPath) ?: return emptyList()
        val doc = parseXml(zip.getInputStream(entry))
        val navMap = doc.getElementsByTagName("navMap").item(0) ?: return emptyList()
        
        fun parseNavPoints(parent: org.w3c.dom.Node): List<TocEntry> {
            val entries = mutableListOf<TocEntry>()
            val children = parent.childNodes
            for (i in 0 until children.length) {
                val child = children.item(i)
                if (child.nodeName == "navPoint") {
                    val label = (child as org.w3c.dom.Element).getElementsByTagName("navLabel").item(0)
                        ?.textContent?.trim() ?: ""
                    val content = child.getElementsByTagName("content").item(0)
                        ?.attributes?.getNamedItem("src")?.nodeValue ?: ""
                    val subEntries = parseNavPoints(child)
                    entries.add(TocEntry(label, resolveRelativePath(ncxPath, content).substringBefore("#"), subEntries))
                }
            }
            return entries
        }

        return parseNavPoints(navMap)
    }

    private fun parseNav(zip: ZipFile, navPath: String): List<TocEntry> {
        val entry = zip.getEntry(navPath) ?: return emptyList()
        val doc = parseXml(zip.getInputStream(entry))
        val nav = doc.getElementsByTagName("nav").item(0) ?: return emptyList()
        val ol = (nav as org.w3c.dom.Element).getElementsByTagName("ol").item(0) ?: return emptyList()

        fun parseOl(parentOl: org.w3c.dom.Node): List<TocEntry> {
            val entries = mutableListOf<TocEntry>()
            val lis = parentOl.childNodes
            for (i in 0 until lis.length) {
                val li = lis.item(i)
                if (li.nodeName == "li") {
                    val a = (li as org.w3c.dom.Element).getElementsByTagName("a").item(0)
                    if (a != null) {
                        val label = a.textContent.trim()
                        val href = a.attributes?.getNamedItem("href")?.nodeValue ?: ""
                        val subOl = li.getElementsByTagName("ol").item(0)
                        val subEntries = if (subOl != null) parseOl(subOl) else emptyList()
                        entries.add(TocEntry(label, resolveRelativePath(navPath, href).substringBefore("#"), subEntries))
                    }
                }
            }
            return entries
        }

        return parseOl(ol)
    }

    private fun findChapterTitle(toc: List<TocEntry>, baseHref: String): String? {
        for (entry in toc) {
            if (entry.href == baseHref) return entry.title
            val childTitle = findChapterTitle(entry.children, baseHref)
            if (childTitle != null) return childTitle
        }
        return null
    }

    private fun parseXml(inputStream: InputStream): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        val builder = factory.newDocumentBuilder()
        return builder.parse(inputStream)
    }

    private fun resolveRelativePath(basePath: String, relativePath: String): String {
        if (relativePath.startsWith("/")) return relativePath.drop(1)
        val parent = File(basePath).parent?.replace("\\", "/") ?: ""
        return if (parent.isEmpty()) relativePath else "$parent/$relativePath"
    }
}

