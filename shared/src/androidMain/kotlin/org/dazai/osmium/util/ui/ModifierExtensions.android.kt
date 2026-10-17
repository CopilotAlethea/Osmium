package org.dazai.osmium.util.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

actual fun Modifier.osmiumClickable(
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: ((Offset) -> Unit)?
): Modifier = if (enabled) {
    this.pointerInput(Unit) {
        detectTapGestures(
            onTap = { onClick() },
            onLongPress = { offset -> onLongClick?.invoke(offset) }
        )
    }
} else {
    this
}

