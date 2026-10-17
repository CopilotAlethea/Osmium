package org.dazai.osmium.util.ui

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS 平台侧滑返回由系统处理，Compose 层暂时不拦截
}

