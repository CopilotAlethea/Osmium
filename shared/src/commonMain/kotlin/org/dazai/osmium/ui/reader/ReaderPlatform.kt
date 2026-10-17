package org.dazai.osmium.ui.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
expect fun getReaderPlatformSettings(primaryColor: Color): ReaderPlatformSettings

data class ReaderPlatformSettings(
    val injectScript: String,
    val atDocumentStart: Boolean = false,
    val needsManualBackgroundTransparency: Boolean = false,
    val needsManualNavigationSync: Boolean = false
)

