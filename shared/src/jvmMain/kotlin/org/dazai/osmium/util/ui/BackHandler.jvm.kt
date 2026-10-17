package org.dazai.osmium.util.ui

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // JVM 平台（桌面端）暂时不处理物理返回键
}

