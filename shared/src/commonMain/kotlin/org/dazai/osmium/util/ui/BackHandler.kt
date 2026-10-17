package org.dazai.osmium.util.ui

import androidx.compose.runtime.Composable

/**
 * A cross-platform back button handler.
 * Maps to androidx.activity.compose.BackHandler on Android.
 * No-op on other platforms.
 */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)

