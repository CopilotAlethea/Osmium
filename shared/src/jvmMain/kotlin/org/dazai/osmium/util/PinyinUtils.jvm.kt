package org.dazai.osmium.util

import com.github.promeg.pinyinhelper.Pinyin

actual fun String.toPinyin(): String {
    return Pinyin.toPinyin(this, "").lowercase()
}

actual fun String.toPinyinInitials(): String {
    val pinyin = Pinyin.toPinyin(this, ",")
    return pinyin.split(",")
        .filter { it.isNotEmpty() }
        .map { it[0].lowercaseChar() }
        .joinToString("")
}

