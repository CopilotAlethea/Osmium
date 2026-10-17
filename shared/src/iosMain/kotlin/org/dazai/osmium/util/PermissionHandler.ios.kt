package org.dazai.osmium.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPermissionHandler(onPermissionResult: (Boolean) -> Unit): PermissionHandler {
    return remember {
        object : PermissionHandler {
            override fun requestStoragePermission() {
                onPermissionResult(true)
            }

            override fun isStoragePermissionGranted(): Boolean = true
        }
    }
}

