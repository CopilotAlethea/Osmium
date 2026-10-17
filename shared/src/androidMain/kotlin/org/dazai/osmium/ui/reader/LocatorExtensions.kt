package org.dazai.osmium.ui.reader

import org.json.JSONObject
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.dazai.osmium.domain.model.Annotation

/**
 * 将 JSON 字符串解析为 Locator 对象的扩展函数。
 * 集中处理解析异常并记录日志。
 */
fun String.toLocator(): Locator? {
    return try {
        Locator.fromJSON(JSONObject(this))
    } catch (e: Exception) {
        println("Error parsing Locator JSON: ${e.message}")
        null
    }
}

/**
 * 将 Locator 对象序列化为 JSON 字符串。
 */
fun Locator.toJsonString(): String {
    return this.toJSON().toString()
}

/**
 * 从结构化的 Annotation 对象重建 Locator。
 * locations 和 text 字段仍存储为 JSON 字符串以保证解析兼容性。
 */
fun Annotation.toLocator(): Locator? {
    return try {
        Locator(
            href = Url(href)!!,
            mediaType = MediaType(mediaType) ?: MediaType.BINARY,
            title = title,
            locations = Locator.Locations.fromJSON(JSONObject(locations)),
            text = Locator.Text.fromJSON(JSONObject(text))
        )
    } catch (e: Exception) {
        println("Error reconstructing Locator from Annotation: ${e.message}")
        null
    }
}

