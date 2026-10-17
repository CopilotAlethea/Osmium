package org.dazai.osmium.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 阅读器顶层布局容器，定义了各个 UI 层级的堆叠顺序（Z-Order）。
 * 通过插槽（Slot）模式实现 UI 结构与具体组件实现的解耦。
 */
@Composable
fun ReaderLayout(
    modifier: Modifier = Modifier,
    // 底层：阅读器核心（如 Readium Navigator）
    navigator: @Composable () -> Unit,
    // 中层：常驻遮罩（如底部进度文字、交互感应层、书签指示器）
    persistentOverlays: @Composable BoxScope.() -> Unit = {},
    // 中高层：上下文相关的浮层（如选择菜单、快速回退、笔记输入）
    contextualOverlays: @Composable BoxScope.() -> Unit = {},
    // 高层：功能面板（如主题设置、目录面板）
    sheets: @Composable BoxScope.() -> Unit = {},
    // 顶层：全局加载遮罩（涵盖初始化和渲染阶段）
    loadingOverlay: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 1. 核心内容
        navigator()
        
        // 2. 常驻反馈与交互
        persistentOverlays()
        
        // 3. 上下文弹窗
        contextualOverlays()
        
        // 4. 功能面板
        sheets()
        
        // 5. 顶层加载
        loadingOverlay()
    }
}

