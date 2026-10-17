package org.dazai.osmium.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun dynamicColorScheme(isDark: Boolean): ColorScheme? {
    return null
}

@Composable
actual fun UpdateStatusBarConfig(isDark: Boolean) {
    // No-op for Desktop
}

@Composable
actual fun UpdateSystemBarsVisibility(visible: Boolean) {
    // No-op for Desktop
}

