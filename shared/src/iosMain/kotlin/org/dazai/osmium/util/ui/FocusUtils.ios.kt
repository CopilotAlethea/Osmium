package org.dazai.osmium.util.ui

import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.SoftwareKeyboardController

actual fun clearPlatformFocus(
    focusManager: FocusManager, 
    keyboardController: SoftwareKeyboardController?
) {
    focusManager.clearFocus()
    keyboardController?.hide()
}

actual suspend fun onPageEntryFocusCleanup(
    focusManager: FocusManager, 
    keyboardController: SoftwareKeyboardController?
) {
    // iOS 平台通常不需要特殊的初始清理延迟
    clearPlatformFocus(focusManager, keyboardController)
}

actual fun logFocusTrace(tag: String, message: String) {
    println("$tag: $message")
}

