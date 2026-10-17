package org.dazai.osmium.util

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPermissionHandler(onPermissionResult: (Boolean) -> Unit): PermissionHandler

interface PermissionHandler {
    fun requestStoragePermission()
    fun isStoragePermissionGranted(): Boolean
}

