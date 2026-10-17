package org.dazai.osmium.ui.reader

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.CoroutineScope
import org.dazai.osmium.domain.model.AnnotationType

enum class ReaderDialogType {
    NONE, TOC, THEME, SETTINGS, ANNOTATIONS, SEARCH
}

@Stable
data class ReaderSearchResult(
    val before: String?,
    val highlight: String?,
    val after: String?,
    val title: String?,
    val locatorJson: String
)

// 抽象阅读器状态
@Stable
class ReaderState(private val scope: CoroutineScope) {
    var showThemeSheet by mutableStateOf(false)
    var showTocSheet by mutableStateOf(false)
    
    var activeDialog by mutableStateOf(ReaderDialogType.NONE)
    var anchorBounds by mutableStateOf<Rect?>(null)

    var showPopups by mutableStateOf(false)
    var lastInteractionTime by mutableLongStateOf(0L)
    var currentHierarchy by mutableStateOf<List<String>>(emptyList())
    var isLoading by mutableStateOf(true)
    var isClosing by mutableStateOf(false)
    var loadingCoverUrl by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    // 全屏图片查看
    var fullscreenImageUrl by mutableStateOf<String?>(null)
    var fullscreenImageData by mutableStateOf<ByteArray?>(null)
    
    // 进度 (0.0 - 1.0)
    var progression by mutableFloatStateOf(0f)
    
    // 手势状态
    var totalDragY by mutableFloatStateOf(0f)

    // 选择与菜单
    var showSelectionMenu by mutableStateOf(false)
    var selectionLocatorJson by mutableStateOf<String?>(null)
    var selectionProgression by mutableStateOf<Double?>(null)
    var selectionRect by mutableStateOf<SelectionRect?>(null)
    
    // 笔记输入
    var showNoteInput by mutableStateOf(false)
    
    // 手势状态 (拖动翻页等)
    var isDragging by mutableStateOf(false)

    // 快速回退
    var canGoBack by mutableStateOf(false)
    var backLocationName by mutableStateOf<String?>(null)
    var backLocationChapter by mutableStateOf<String?>(null)

    // 被点击的标注状态
    var activeAnnotationId by mutableStateOf<String?>(null)
    var activeAnnotationRect by mutableStateOf<SelectionRect?>(null)
    var showAnnotationActions by mutableStateOf(false)

    // 上次选择的工具类型
    var lastToolType by mutableStateOf(AnnotationType.HIGHLIGHT)
    var lastStyle by mutableStateOf<String?>("marker")
    var lastHighlightColor by mutableIntStateOf(0xFFFFE082.toInt())
    var lastUnderlineColor by mutableIntStateOf(0xFF4C662B.toInt())

    // 搜索相关
    var searchQuery by mutableStateOf("")
    var searchResults = mutableStateListOf<ReaderSearchResult>()
    var isSearching by mutableStateOf(false)
    var searchAnchorBounds by mutableStateOf<Rect?>(null)
    var isSearchExhausted by mutableStateOf(false)

    var keepScreenOnUntil by mutableLongStateOf(0L)

    fun togglePopups() {
        showPopups = !showPopups
        if (showPopups) {
            updateInteractionTime()
        } else {
            activeDialog = ReaderDialogType.NONE
            searchQuery = "" // 关闭时清空搜索
            searchResults.clear()
        }
        resetKeepScreenOn()
    }

    fun updateInteractionTime() {
        lastInteractionTime = org.dazai.osmium.util.currentTimeMillis()
        resetKeepScreenOn()
    }

    fun resetKeepScreenOn() {
        keepScreenOnUntil = org.dazai.osmium.util.currentTimeMillis() + ReaderConstants.KeepScreenOnDelayMs
    }
}



data class SelectionRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Composable
fun rememberReaderState(): ReaderState {
    val scope = rememberCoroutineScope()
    return remember { ReaderState(scope) }
}

