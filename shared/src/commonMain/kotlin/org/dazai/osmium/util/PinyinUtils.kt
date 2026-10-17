package org.dazai.osmium.util

/**
 * 将字符串转换为拼音。
 */
expect fun String.toPinyin(): String

/**
 * 将字符串转换为拼音首字母。
 */
expect fun String.toPinyinInitials(): String

/**
 * 字符串标准化：去除首尾空格、所有内部空白字符（含换行、全角空格），并转为小写。
 */
fun String.normalize(): String {
    return this.trim()
        .replace("\\s+".toRegex(), "")
        .lowercase()
}

/**
 * 子序列匹配：判断 query 中的字符是否按顺序出现在 text 中。
 * 例如 "斗苍" 匹配 "斗破苍穹"。
 * 匹配前会自动对 query 和 text 进行标准化处理。
 */
fun String.subsequenceMatch(query: String): Boolean {
    val targetQuery = query.normalize()
    if (targetQuery.isEmpty()) return true
    
    val targetText = this.normalize()
    var queryIdx = 0
    
    for (char in targetText) {
        if (char == targetQuery[queryIdx]) {
            queryIdx++
            if (queryIdx == targetQuery.length) return true
        }
    }
    return false
}

