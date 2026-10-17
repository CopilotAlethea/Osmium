package org.dazai.osmium.ui.reader

object ReaderConstants {
    /** 开启书籍时，为了掩盖 WebView 渲染瞬间的人工延时 (毫秒) */
    const val WebviewRenderDelayMs = 300L

    /** 边缘点击区域比例 (0.35 表示屏幕左右各 35% 为翻页区) */
    const val PageTurnEdgeRatio = 0.35f
    
    /** 弹出菜单自动淡出延迟 (毫秒) */
    const val PopupAutoHideDelayMs = 5000L
    
    /** 轮询检查 Fragment 是否挂载的最大重试次数 */
    const val FragmentPollingRetryCount = 20
    const val FragmentPollingIntervalMs = 100L
    
    /** 判定为“快速翻页”的时间阈值 (毫秒) */
    const val FastPageTurnThresholdMs = 1000L
    
    /** 触发“快速跳转回退”的连续翻页次数 */
    const val FastSkipCountThreshold = 5
    
    /** 判定为“稳定阅读”的时间阈值 (毫秒) */
    const val StableReadThresholdMs = 2000L

    /** 检测翻页停顿的延迟时间 (毫秒) */
    const val PageStopCheckDelayMs = 1200L

    /** 触发快速回退的模拟页数阈值 */
    const val FastSkipPageThreshold = 3

    /** 快速回退按钮自动隐去延迟 (毫秒) */
    const val QuickBackAutoHideDelayMs = 5000L
    
    /** 自动保存进度的最小时间间隔 (毫秒) */
    const val AutoSaveIntervalMs = 3000L

    /** 阅读时保持屏幕开启的时间 (毫秒) */
    const val KeepScreenOnDelayMs = 300000L
}
