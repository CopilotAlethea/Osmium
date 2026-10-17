package org.dazai.osmium.util.ui

import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.SoftwareKeyboardController

actual fun clearPlatformFocus(focusManager: FocusManager, keyboardController: SoftwareKeyboardController?) {
    focusManager.clearFocus()
}

actual suspend fun onPageEntryFocusCleanup(focusManager: FocusManager, keyboardController: SoftwareKeyboardController?) {
    focusManager.clearFocus()
}

actual fun logFocusTrace(tag: String, message: String) {
    println("$tag: $message")
}

