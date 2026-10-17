package org.dazai.osmium.util

import platform.Foundation.*
import platform.CoreFoundation.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual fun String.toPinyin(): String {
    val mutableString = CFStringCreateMutableCopy(null, 0, this as CFStringRef) ?: return this
    CFStringTransform(mutableString, null, kCFStringTransformToLatin, false)
    CFStringTransform(mutableString, null, kCFStringTransformStripDiacritics, false)
    val result = (mutableString as NSString).toString().replace(" ", "").lowercase()
    CFRelease(mutableString)
    return result
}

@OptIn(ExperimentalForeignApi::class)
actual fun String.toPinyinInitials(): String {
    val mutableString = CFStringCreateMutableCopy(null, 0, this as CFStringRef) ?: return this
    CFStringTransform(mutableString, null, kCFStringTransformToLatin, false)
    CFStringTransform(mutableString, null, kCFStringTransformStripDiacritics, false)
    val pinyin = (mutableString as NSString).toString()
    val result = pinyin.split(" ")
        .filter { it.isNotEmpty() }
        .map { it[0].lowercaseChar() }
        .joinToString("")
    CFRelease(mutableString)
    return result
}

