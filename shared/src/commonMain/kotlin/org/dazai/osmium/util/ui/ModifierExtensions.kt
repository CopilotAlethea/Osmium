package org.dazai.osmium.util.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

expect fun Modifier.osmiumClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: ((Offset) -> Unit)? = null
): Modifier

