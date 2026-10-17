package org.dazai.osmium.ui.reader

import android.os.Build
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dazai.osmium.domain.model.Annotation
import org.dazai.osmium.domain.model.AnnotationType
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.html.toCss
import org.readium.r2.navigator.html.HtmlDecorationTemplate
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import android.graphics.Color as AndroidColor
import org.jetbrains.compose.resources.stringResource
import osmium.shared.generated.resources.*
import org.dazai.osmium.domain.repository.LibraryRepository
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import androidx.viewpager2.widget.ViewPager2
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.*
import org.dazai.osmium.manager.ThemeManager
import org.dazai.osmium.manager.OsmiumThemeType
import org.dazai.osmium.manager.DarkFollowMode
import org.dazai.osmium.ui.theme.UpdateSystemBarsVisibility
import org.dazai.osmium.ui.theme.setSystemBarsVisibility
import org.dazai.osmium.util.getHourOfDay
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.navigator.HyperlinkNavigator
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.resource.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class, ExperimentalReadiumApi::class)
@Composable
actual fun BookReader(
    bookId: String,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    val books by LibraryRepository.books.collectAsState()
    val book = remember(books, bookId) { books.find { it.id == bookId } } ?: return

    val annotations by LibraryRepository.getAnnotationsForBook(bookId).collectAsState(emptyList())

    val state = rememberReaderState()
    val colorScheme = MaterialTheme.colorScheme
    val model = rememberBookReaderModel(bookId, state)

    LaunchedEffect(colorScheme.primary) {
        model.readerPrimaryColor = colorScheme.primary.toArgb()
    }

    // 控制系统栏显示/隐藏
    UpdateSystemBarsVisibility(visible = state.showPopups)

    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current

    // 退出时强制保存进度并恢复系统栏
    DisposableEffect(Unit) {
        onDispose {
            model.saveProgress(force = true)
            setSystemBarsVisibility(context, view, visible = true)
            view.keepScreenOn = false
        }
    }

    // 屏幕常亮逻辑：交互后保持 1 分钟
    LaunchedEffect(state.keepScreenOnUntil) {
        val now = org.dazai.osmium.util.currentTimeMillis()
        val delayTime = state.keepScreenOnUntil - now
        if (delayTime > 0) {
            view.keepScreenOn = true
            delay(delayTime)
            view.keepScreenOn = false
        } else {
            view.keepScreenOn = false
        }
    }

    LaunchedEffect(book.path) {
        state.loadingCoverUrl = book.coverUrl
        state.isLoading = true
        model.loadBook(book.path)
    }

    val activity = LocalContext.current as AppCompatActivity

    // 增加延时加载状态，确保 WebView 有时间渲染
    LaunchedEffect(state.isLoading, model.publication) {
        // 已在 onPageLoaded 中通过 state.isLoading = false 处理
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.background
    ) {
        if (state.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = colorScheme.error)
            }
        } else {
            ReaderLayout(
                navigator = {
                    if (model.publication != null) {
                        ReadiumNavigatorView(
                            pub = model.publication!!,
                            fragmentTag = model.fragmentTag,
                            book = book,
                            annotations = annotations,
                            state = state,
                            onNavigatorReady = { model.observeNavigator(it) }
                        )
                    }
                },
                persistentOverlays = {
                    if (model.publication != null) {
                        ReaderPersistentBottomText(
                            title = model.publication!!.metadata.title ?: "",
                            hierarchy = state.currentHierarchy,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        ReaderPopups(
                            state = state,
                            onBack = {
                                model.saveProgress(force = true)
                                onClose()
                            },
                            onSettings = { state.activeDialog = ReaderDialogType.SETTINGS },
                            onTheme = { state.activeDialog = ReaderDialogType.THEME },
                            onMore = { /* TODO: More */ },
                            onToc = { state.activeDialog = ReaderDialogType.TOC },
                            onAnnotations = { state.activeDialog = ReaderDialogType.ANNOTATIONS }
                        )
                    }
                },
                contextualOverlays = {
                    SelectionMenuOverlay(state, bookId) {
                        val fm = activity.supportFragmentManager
                        (fm.findFragmentByTag(model.fragmentTag) as? EpubNavigatorFragment)?.clearSelection()
                    }

                    QuickBackOverlay(
                        state = state,
                        onBack = {
                            (activity.supportFragmentManager.findFragmentByTag(model.fragmentTag) as? EpubNavigatorFragment)?.let {
                                model.goBack(it)
                            }
                        },
                        onIgnore = { model.clearHistory() }
                    )
                    
                    NoteInputOverlay(state, bookId, annotations) {
                        val fm = activity.supportFragmentManager
                        (fm.findFragmentByTag(model.fragmentTag) as? EpubNavigatorFragment)?.clearSelection()
                    }

                    AnnotationActionsOverlay(
                        showAnnotationActions = state.showAnnotationActions,
                        activeAnnotationId = state.activeAnnotationId,
                        activeAnnotationRect = state.activeAnnotationRect,
                        annotations = annotations,
                        lastUnderlineColor = state.lastUnderlineColor,
                        lastHighlightColor = state.lastHighlightColor,
                        lastStyle = state.lastStyle,
                        onToolUpdate = { type, color, style ->
                            state.lastToolType = type
                            if (type == AnnotationType.UNDERLINE) {
                                state.lastUnderlineColor = color
                            } else {
                                state.lastHighlightColor = color
                            }
                            state.lastStyle = style
                        },
                        onDelete = { model.deleteAnnotation(it) },
                        onEdit = { annot ->
                            val locator = annot.toLocator()
                            state.selectionLocatorJson = locator?.toJSON()?.toString()
                            state.showNoteInput = true
                        },
                        onDismiss = { 
                            state.showAnnotationActions = false 
                            state.activeAnnotationId = null
                        }
                    )

                    if (state.activeDialog == ReaderDialogType.SEARCH) {
                        ReaderSearchDialog(
                            searchQuery = state.searchQuery,
                            onSearchQueryChange = { state.searchQuery = it },
                            isSearching = state.isSearching,
                            isSearchExhausted = state.isSearchExhausted,
                            searchResults = state.searchResults,
                            anchorBounds = state.anchorBounds,
                            onSearch = { query -> model.startSearch(query) },
                            onResultClick = { result ->
                                result.locatorJson.toLocator()?.let { locator ->
                                    (activity.supportFragmentManager.findFragmentByTag(model.fragmentTag) as? EpubNavigatorFragment)?.let {
                                        model.jumpTo(it, locator)
                                        state.activeDialog = ReaderDialogType.NONE
                                    }
                                }
                            },
                            onLoadMore = { model.loadMoreSearch() },
                            onDismiss = { model.closeSearch() }
                        )
                    }
                },
                sheets = {
                    when (state.activeDialog) {
                        ReaderDialogType.THEME -> ReaderThemeDialog(
                            anchorBounds = state.anchorBounds,
                            onDismiss = { state.activeDialog = ReaderDialogType.NONE }
                        )
                        ReaderDialogType.SETTINGS -> ReaderSettingsDialog(
                            anchorBounds = state.anchorBounds,
                            onDismiss = { state.activeDialog = ReaderDialogType.NONE }
                        )
                        ReaderDialogType.TOC -> if (model.publication != null) {
                            ReaderTocDialog(
                            anchorBounds = state.anchorBounds,
                            toc = model.publication!!.tableOfContents,
                            onDismiss = { state.activeDialog = ReaderDialogType.NONE },
                            onNavigate = { locator ->
                                (activity.supportFragmentManager.findFragmentByTag(model.fragmentTag) as? EpubNavigatorFragment)?.let {
                                    model.jumpTo(it, locator)
                                }
                            }
                        )
                    }
                    ReaderDialogType.ANNOTATIONS -> if (model.publication != null) {
                        ReaderAnnotationsDialog(
                            anchorBounds = state.anchorBounds,
                            annotations = annotations,
                            onDismiss = { state.activeDialog = ReaderDialogType.NONE },
                            onNavigate = { locatorJson ->
                                locatorJson.toLocator()?.let { locator ->
                                    (activity.supportFragmentManager.findFragmentByTag(model.fragmentTag) as? EpubNavigatorFragment)?.let {
                                        model.jumpTo(it, locator)
                                    }
                                }
                            },
                            onDeleteAnnotation = { model.deleteAnnotation(it) }
                        )
                        }
                        else -> {}
                    }
                },
                loadingOverlay = {
                    ReaderLoadingOverlay(
                        isVisible = state.isLoading,
                        coverUrl = state.loadingCoverUrl
                    )
                }
            )
        }

        // 全屏图片查看
        if (state.fullscreenImageUrl != null || state.fullscreenImageData != null) {
            FullscreenImageViewer(
                imageUrl = state.fullscreenImageUrl,
                imageData = state.fullscreenImageData,
                onDismiss = { 
                    state.fullscreenImageUrl = null
                    state.fullscreenImageData = null
                }
            )
        }
    }

    val handleClose = {
        if (state.fullscreenImageUrl != null || state.fullscreenImageData != null) {
            state.fullscreenImageUrl = null
            state.fullscreenImageData = null
        } else {
            model.saveProgress(force = true)
            onClose()
        }
    }

    BackHandler {
        when {
            state.fullscreenImageUrl != null || state.fullscreenImageData != null -> {
                state.fullscreenImageUrl = null
                state.fullscreenImageData = null
            }
            state.activeDialog != ReaderDialogType.NONE -> state.activeDialog = ReaderDialogType.NONE
            state.showPopups -> state.showPopups = false
            else -> handleClose()
        }
    }
}

@OptIn(ExperimentalReadiumApi::class)
@Composable
private fun ReadiumNavigatorView(
    pub: Publication,
    fragmentTag: String,
    book: org.dazai.osmium.domain.model.Book,
    annotations: List<Annotation>,
    state: ReaderState,
    onNavigatorReady: (EpubNavigatorFragment) -> Unit
) {
    val activity = LocalContext.current as AppCompatActivity
    val coroutineScope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    val viewId = remember { android.view.View.generateViewId() }
    val bgColorInt = colorScheme.background.toArgb()

    val baseTheme = ThemeManager.currentTheme
    val darkFollow = ThemeManager.darkFollowMode
    val isSystemDark = isSystemInDarkTheme()

    val isDark = when (darkFollow) {
        DarkFollowMode.System -> isSystemDark
        DarkFollowMode.Time -> ThemeManager.isCurrentlyInDarkTime(getHourOfDay())
        DarkFollowMode.Always -> true
        else -> false
    }

    val platformSettings = getReaderPlatformSettings(colorScheme.primary)

    val readiumTheme = when (baseTheme) {
        OsmiumThemeType.System -> if (isDark) Theme.DARK else Theme.LIGHT
        OsmiumThemeType.Matcha -> if (isDark) Theme.DARK else Theme.LIGHT
        OsmiumThemeType.Azuki -> if (isDark) Theme.DARK else Theme.LIGHT
        OsmiumThemeType.Cocoa -> if (isDark) Theme.DARK else Theme.LIGHT
        OsmiumThemeType.Metal -> if (isDark) Theme.DARK else Theme.LIGHT
    }

    DisposableEffect(fragmentTag) {
        onDispose {
            if (fragmentTag.isNotEmpty()) {
                val fm = activity.supportFragmentManager
                val existingFragment = fm.findFragmentByTag(fragmentTag)
                if (existingFragment != null) {
                    fm.beginTransaction()
                        .remove(existingFragment)
                        .commitAllowingStateLoss()
                }
            }
        }
    }

    // 使用 Lifecycle 回调获取 Navigator，消除脆弱的轮询延时
    val currentOnNavigatorReady by rememberUpdatedState(onNavigatorReady)
    var cachedViewPager by remember { mutableStateOf<ViewPager2?>(null) }
    var cachedRecyclerView by remember { mutableStateOf<RecyclerView?>(null) }

    DisposableEffect(fragmentTag) {
        if (fragmentTag.isEmpty()) return@DisposableEffect onDispose {}
        
        val fm = activity.supportFragmentManager
        val callback = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentResumed(f: FragmentManager, f1: Fragment) {
                if (f1.tag == fragmentTag && f1 is EpubNavigatorFragment) {
                    currentOnNavigatorReady(f1)
                    
                    // 性能优化：在 Resume 时设置一次 offscreenPageLimit
                    f1.view?.let { view ->
                        val vp = cachedViewPager ?: findViewPager2(view).also { cachedViewPager = it }
                        vp?.offscreenPageLimit = 1
                    }
                }
            }
        }
        
        fm.registerFragmentLifecycleCallbacks(callback, false)
        
        // 检查是否已经存在且 Resume 了
        (fm.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment)?.let {
            if (it.isResumed) currentOnNavigatorReady(it)
        }
        
        onDispose {
            fm.unregisterFragmentLifecycleCallbacks(callback)
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> 
            FragmentContainerView(ctx).apply { 
                id = viewId
                setBackgroundColor(bgColorInt)
            } 
        }
    ) {
        val fm = activity.supportFragmentManager
        val existing = fm.findFragmentByTag(fragmentTag)
        if (existing == null || existing !is EpubNavigatorFragment) {
            val factory = EpubNavigatorFactory(pub)
            val initialLocator = book.lastLocation?.toLocator()
            
            val initialPrefs = EpubPreferences(
                theme = readiumTheme,
                backgroundColor = ReadiumColor(colorScheme.background.toArgb()),
                textColor = ReadiumColor(colorScheme.onBackground.toArgb()),
                publisherStyles = ThemeManager.publisherStyles,
                fontSize = ThemeManager.getFontSize() / 100.0,
                lineHeight = ThemeManager.getLineHeight().toDouble(),
                letterSpacing = ThemeManager.getLetterSpacing().toDouble(),
                paragraphSpacing = ThemeManager.getParaSpacing().toDouble(),
                pageMargins = ThemeManager.getMargin().toDouble()
            )

            val config = EpubNavigatorFragment.Configuration {
                registerJavascriptInterface("OsmiumBridge") { _ ->
                    object {
                        @JavascriptInterface
                        fun onImageClicked(url: String) {
                            coroutineScope.launch {
                                val cleanPath = try {
                                    val raw = URI(url).path  // 形如 /readium_package/OEBPS/images/cover.jpg
                                    when {
                                        raw.startsWith("/readium_package/") -> raw.removePrefix("/readium_package/")
                                        raw.startsWith("readium_package/")  -> raw.removePrefix("readium_package/")
                                        raw.startsWith("/")                 -> raw.drop(1)
                                        else                                -> raw
                                    }
                                } catch (e: Exception) {
                                    url.substringAfter("readium_package/")
                                }

                                withContext(Dispatchers.IO) {
                                    val resource = Url(cleanPath)?.let { pub.get(it) }
                                    val data = resource?.read()?.getOrNull()
                                    withContext(Dispatchers.Main) {
                                        if (data != null && data.isNotEmpty()) {
                                            state.fullscreenImageData = data
                                            state.fullscreenImageUrl = null
                                        } else {
                                            state.fullscreenImageUrl = url
                                            state.fullscreenImageData = null
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                selectionActionModeCallback = object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                        coroutineScope.launch {
                            state.activeAnnotationId = null
                            val navigator = fm.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment
                            val selection = navigator?.currentSelection()
                            if (selection != null) {
                                state.selectionLocatorJson = selection.locator.toJSON().toString()
                                state.selectionProgression = selection.locator.locations.progression
                                selection.rect?.let { rect ->
                                    state.selectionRect = SelectionRect(rect.left, rect.top, rect.right, rect.bottom)
                                }
                                state.showSelectionMenu = true
                            }
                        }
                        return true
                    }
                    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
                    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = false
                    override fun onDestroyActionMode(mode: ActionMode?) {
                        if (!state.showNoteInput) state.showSelectionMenu = false
                    }
                }

                servedAssets = listOf("annotation-icon.svg")

                decorationTemplates = HtmlDecorationTemplates.defaultTemplates(
                    alpha = 1.0,
                    experimentalPositioning = true
                ).apply {
                    set(DecorationStyleAnnotationMark::class, annotationMarkTemplate())
                    set(Decoration.Style.Highlight::class, highlightTemplate())
                    set(Decoration.Style.Underline::class, underlineTemplate())
                }
            }

    fm.fragmentFactory = factory.createFragmentFactory(
        initialLocator = initialLocator,
        initialPreferences = initialPrefs,
        listener = object : EpubNavigatorFragment.Listener {
            override fun onExternalLinkActivated(url: AbsoluteUrl) {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url.toString()))
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        },
        paginationListener = object : EpubNavigatorFragment.PaginationListener {
            override fun onPageLoaded() {
                state.isLoading = false
                state.resetKeepScreenOn()

                val fragment = fm.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment
                fragment?.view?.let { view ->
                    // 1. 获取 ViewPager2 和 RecyclerView (优先 from cache)
                    val vp = cachedViewPager ?: findViewPager2(view).also { cachedViewPager = it }
                    cachedRecyclerView = cachedRecyclerView ?: (vp?.getChildAt(0) as? RecyclerView)

                    // 2. 注入脚本
                    findAllWebViews(view).forEach { webView ->
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                        }
                        webView.evaluateJavascript(platformSettings.injectScript, null)
                    }
                }
            }
        },
        configuration = config
    )
    fm.beginTransaction()
                .replace(viewId, EpubNavigatorFragment::class.java, null, fragmentTag)
                .commitAllowingStateLoss()
            
            // 监听外链
            fm.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                    if (f.tag == fragmentTag && f is EpubNavigatorFragment) {
                        f.addInputListener(object : InputListener {}) // 确保 listener 生效
                    }
                }
            }, false)
        } else if (existing.id != viewId) {
            fm.beginTransaction().replace(viewId, existing, fragmentTag).commitAllowingStateLoss()
        }
    }

    LaunchedEffect(
        readiumTheme, colorScheme, fragmentTag,
        ThemeManager.publisherStyles,
        ThemeManager.fontSizeLevel, ThemeManager.lineHeightLevel, ThemeManager.letterSpacingLevel,
        ThemeManager.paraSpacingLevel, ThemeManager.marginLevel
    ) {
        if (fragmentTag.isEmpty()) return@LaunchedEffect
        val fm = activity.supportFragmentManager
        (fm.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment)?.let { navigator ->
            val prefs = EpubPreferences(
                theme = readiumTheme,
                backgroundColor = ReadiumColor(colorScheme.background.toArgb()),
                textColor = ReadiumColor(colorScheme.onBackground.toArgb()),
                publisherStyles = ThemeManager.publisherStyles,
                fontSize = ThemeManager.getFontSize() / 100.0,
                lineHeight = ThemeManager.getLineHeight().toDouble(),
                letterSpacing = ThemeManager.getLetterSpacing().toDouble(),
                paragraphSpacing = ThemeManager.getParaSpacing().toDouble(),
                pageMargins = ThemeManager.getMargin().toDouble()
            )
            navigator.submitPreferences(prefs)
        }
    }

    LaunchedEffect(platformSettings.injectScript, fragmentTag) {
        if (fragmentTag.isEmpty()) return@LaunchedEffect
        val fm = activity.supportFragmentManager
        (fm.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment)?.view?.let { view ->
            findAllWebViews(view).forEach { webView ->
                webView.evaluateJavascript(platformSettings.injectScript, null)
            }
        }
    }

     LaunchedEffect(fragmentTag) {
        if (fragmentTag.isEmpty()) return@LaunchedEffect
        val fm = activity.supportFragmentManager
        
        var navigator: EpubNavigatorFragment? = null
        repeat(ReaderConstants.FragmentPollingRetryCount) {
            navigator = fm.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment
            if (navigator?.view != null) return@repeat
            delay(ReaderConstants.FragmentPollingIntervalMs)
        }
        
        navigator?.addInputListener(object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                val point = event.point
                val view = navigator.view
                val viewWidth = view?.width?.toFloat() ?: 0f
                val viewHeight = view?.height?.toFloat() ?: 0f
                if (viewWidth == 0f || viewHeight == 0f) return false

                val leftBound = viewWidth * ReaderConstants.PageTurnEdgeRatio
                val rightBound = viewWidth * (1f - ReaderConstants.PageTurnEdgeRatio)
                
                if (point.x in leftBound..rightBound) {
                    state.togglePopups()
                    return true
                }

                if (point.x < leftBound) {
                    navigator.goBackward(animated = true)
                    state.resetKeepScreenOn()
                    return true
                }
                if (point.x > rightBound) {
                    navigator.goForward(animated = true)
                    state.resetKeepScreenOn()
                    return true
                }
                return false
            }
        })
    }
}

private fun annotationMarkTemplate(@androidx.annotation.ColorInt defaultTint: Int = AndroidColor.YELLOW): HtmlDecorationTemplate {
    val className = "Osmium-annotation-mark"
    val iconUrl = checkNotNull(EpubNavigatorFragment.assetUrl("annotation-icon.svg"))
    return HtmlDecorationTemplate(
        layout = HtmlDecorationTemplate.Layout.BOUNDS,
        width = HtmlDecorationTemplate.Width.PAGE,
        element = { decoration ->
            val style = decoration.style as? DecorationStyleAnnotationMark
            val tint = style?.tint ?: defaultTint
            val opacity = if (style?.isActive == true) "1.0" else "0.8"
            """
            <div><div data-activable="1" class="$className" style="background-color: ${tint.toCss()} !important; opacity: $opacity !important;"/></div>
            """
        },
        stylesheet = """
            .$className {
                float: left;
                margin-left: 8px;
                width: 30px;
                height: 30px;
                border-radius: 50%;
                background: url('$iconUrl') no-repeat center;
                background-size: auto 50%;
                opacity: 0.8;
            }
            """
    )
}

private fun highlightTemplate(): HtmlDecorationTemplate = HtmlDecorationTemplate(
    layout = HtmlDecorationTemplate.Layout.BOXES,
    element = { decoration ->
        val style = decoration.style as? Decoration.Style.Highlight
        val color = style?.tint?.toCss() ?: "yellow"
        val isActive = style?.isActive ?: false
        
        val opacity = if (isActive) "0.5" else "0.3"
        val outline = if (isActive) "outline: 2px solid $color; outline-offset: -1px;" else ""
        
        """
        <div style="background-color: $color !important; opacity: $opacity !important; $outline width: 100%; height: 100%;"></div>
        """
    }
)

private fun underlineTemplate(): HtmlDecorationTemplate = HtmlDecorationTemplate(
    layout = HtmlDecorationTemplate.Layout.BOXES,
    element = { decoration ->
        val style = decoration.style as? Decoration.Style.Underline
        val color = style?.tint?.toCss() ?: "red"
        val isActive = style?.isActive ?: false
        
        val borderBottom = if (isActive) "3px solid $color" else "2px solid $color"
        val backgroundColor = if (isActive) "background-color: ${style?.tint?.toCss()}22 !important;" else ""
        
        """
        <div style="border-bottom: $borderBottom !important; $backgroundColor width: 100%; height: 100%;"></div>
        """
    }
)

private fun findAllWebViews(view: View): List<WebView> {
    val webViews = mutableListOf<WebView>()
    if (view is WebView) {
        webViews.add(view)
    } else if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            webViews.addAll(findAllWebViews(view.getChildAt(i)))
        }
    }
    return webViews
}

private fun findWebView(view: View): WebView? {
    if (view is WebView) return view
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val child = findWebView(view.getChildAt(i))
            if (child != null) return child
        }
    }
    return null
}

private fun findViewPager2(view: View): ViewPager2? {
    if (view is ViewPager2) return view
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val child = findViewPager2(view.getChildAt(i))
            if (child != null) return child
        }
    }
    return null
}

