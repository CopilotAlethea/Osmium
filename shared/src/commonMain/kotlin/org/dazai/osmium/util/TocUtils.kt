package org.dazai.osmium.util

/**
 * 根据书籍路径和 Locator JSON 解析章节名称。
 */
expect object TocUtils {
    suspend fun resolveChapterName(bookPath: String, locatorJson: String): String?
}

