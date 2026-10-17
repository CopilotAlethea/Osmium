package org.dazai.osmium.util.ui

import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.SoftwareKeyboardController

/**
 * 平台特有的焦点管理工具。
 */
expect fun clearPlatformFocus(
    focusManager: FocusManager, 
    keyboardController: SoftwareKeyboardController?
)

/**
 * 页面进入时的焦点清理逻辑，针对不同平台可能有不同的延迟或处理。
 */
expect suspend fun onPageEntryFocusCleanup(
    focusManager: FocusManager, 
    keyboardController: SoftwareKeyboardController?
)

/**
 * 记录焦点变化的堆栈跟踪，用于排查自动聚焦的原因。
 */
expect fun logFocusTrace(tag: String, message: String)

