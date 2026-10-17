package org.dazai.osmium.util.ui

import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.SoftwareKeyboardController
import kotlinx.coroutines.delay
import android.os.Build

actual fun clearPlatformFocus(
    focusManager: FocusManager, 
    keyboardController: SoftwareKeyboardController?
) {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        // 对 Android 9 (API 28) 及以下使用更激进的失焦策略
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    } else {
        // Android 10+ 使用标准失焦
        focusManager.clearFocus()
    }
}

actual suspend fun onPageEntryFocusCleanup(
    focusManager: FocusManager, 
    keyboardController: SoftwareKeyboardController?
) {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        // 对 Android 9 及以下增加延迟清理，对抗系统默认聚焦
        delay(300)
        clearPlatformFocus(focusManager, keyboardController)
    } else {
        // Android 10+ 通常不需要启动延迟
        focusManager.clearFocus()
    }
}

actual fun logFocusTrace(tag: String, message: String) {
    // 清理后的正式版不再打印堆栈
}

