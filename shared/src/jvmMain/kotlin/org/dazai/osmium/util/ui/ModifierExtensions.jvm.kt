package org.dazai.osmium.util.ui

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.ExperimentalComposeUiApi

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.osmiumClickable(
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: ((Offset) -> Unit)?
): Modifier = if (enabled) {
    this.onPointerEvent(PointerEventType.Press) { event ->
        if (event.buttons.isSecondaryPressed) {
            onLongClick?.invoke(event.changes.first().position)
        }
    }.clickable { onClick() }
} else {
    this
}

