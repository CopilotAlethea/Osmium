package org.dazai.osmium.ui.reader

import androidx.compose.runtime.Composable

@Composable
actual fun getReaderPlatformSettings(primaryColor: androidx.compose.ui.graphics.Color): ReaderPlatformSettings {
    return ReaderPlatformSettings(
        injectScript = "",
        needsManualBackgroundTransparency = false,
        needsManualNavigationSync = false
    )
}

