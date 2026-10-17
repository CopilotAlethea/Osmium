package org.dazai.osmium.ui.reader

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.ExperimentalComposeUiApi
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dazai.osmium.domain.repository.LibraryRepository
import org.dazai.osmium.util.EpubServer
import org.jetbrains.compose.resources.stringResource
import osmium.shared.generated.resources.*
import java.io.File
import java.util.zip.ZipFile
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
actual fun BookReader(
    bookId: String,
    onClose: () -> Unit,
    modifier: Modifier
) {
    val books by LibraryRepository.books.collectAsState()
    val book = remember(books, bookId) { books.find { it.id == bookId } } ?: return

    val state = rememberReaderState()
    val scope = rememberCoroutineScope()
    
    val annotations by LibraryRepository.getAnnotationsForBook(bookId).collectAsState(emptyList())

    // Epub Server
    val server = remember(book.path) { EpubServer(book.path) }
    var port by remember { mutableStateOf(0) }
    var toc by remember { mutableStateOf<List<ChapterLink>>(emptyList()) }
    var opfPath by remember { mutableStateOf("") }
    var currentHref by remember { mutableStateOf<String?>(null) }

    DisposableEffect(server) {
        port = server.start()
        onDispose {
            server.stop()
        }
    }

    LaunchedEffect(book.path) {
        state.isLoading = true
        withContext(Dispatchers.IO) {
            val epubInfo = loadEpubInfo(book.path)
            toc = epubInfo.toc
            opfPath = epubInfo.opfPath
            
            // 尝试恢复进度
            val savedLoc = book.lastLocation?.let { try { JSONObject(it) } catch (e: Exception) { null } }
            var restoredHref = savedLoc?.optString("href") ?: toc.firstOrNull()?.href
            
            // 自愈逻辑：修复已经损坏的递归路径 (EPUB/EPUB/...)
            val parent = File(opfPath).parent?.replace("\\", "/") ?: ""
            if (parent.isNotEmpty() && restoredHref != null) {
                val prefix = "$parent/"
                while (restoredHref!!.startsWith(prefix + prefix)) {
                    restoredHref = restoredHref!!.substring(prefix.length)
                }
            }
            
            currentHref = restoredHref
            state.isLoading = false
        }
    }

    if (currentHref == null || port == 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val baseUrl = "http://localhost:$port/"
    val fullInitialUrl = if (currentHref!!.contains("://")) currentHref!! else baseUrl + currentHref!!
    
    val webViewState = rememberWebViewState(fullInitialUrl)
    val webViewNavigator = rememberWebViewNavigator()

    // 监听 URL 变化以更新进度和当前 Href
    LaunchedEffect(webViewState.lastLoadedUrl) {
        webViewState.lastLoadedUrl?.let { url ->
            if (url.startsWith(baseUrl)) {
                val relativeHref = url.removePrefix(baseUrl)
                currentHref = relativeHref
                
                // 这里我们简化处理，只保存 href
                val locator = JSONObject().apply {
                    put("href", relativeHref)
                    put("type", "application/xhtml+xml")
                }
                LibraryRepository.updateBookProgress(bookId, 0f, locator.toString())
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(remember { FocusRequester() })
            .focusable()
            .onKeyEvent { event ->
                if (state.activeDialog != ReaderDialogType.NONE) return@onKeyEvent false
                
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionRight, Key.Spacebar -> {
                            // 尝试下一章
                            val currentIndex = toc.indexOfFirst { it.href == currentHref }
                            if (currentIndex != -1 && currentIndex < toc.size - 1) {
                                val nextHref = toc[currentIndex + 1].href
                                val targetUrl = baseUrl + nextHref
                                webViewNavigator.loadUrl(targetUrl)
                            }
                            true
                        }
                        Key.DirectionLeft -> {
                            // 尝试上一章
                            val currentIndex = toc.indexOfFirst { it.href == currentHref }
                            if (currentIndex > 0) {
                                val prevHref = toc[currentIndex - 1].href
                                val targetUrl = baseUrl + prevHref
                                webViewNavigator.loadUrl(targetUrl)
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            },
        color = MaterialTheme.colorScheme.background
    ) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        ReaderLayout(
            navigator = {
                Box(modifier = Modifier.fillMaxSize()) {
                    WebView(
                        state = webViewState,
                        navigator = webViewNavigator,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 透明交互层
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.6f) // 增大中心交互区域
                            .align(Alignment.Center)
                            .onPointerEvent(PointerEventType.Press) { event ->
                                if (event.buttons.isSecondaryPressed) {
                                    // 右键：直接呼出目录 (替换长按)
                                    state.activeDialog = ReaderDialogType.TOC
                                    if (!state.showPopups) state.showPopups = true
                                } else if (event.buttons.isPrimaryPressed) {
                                    // 左键：切换菜单显示
                                    state.togglePopups()
                                }
                            }
                    )
                }
            },
            persistentOverlays = {
                ReaderPopups(
                    state = state,
                    onBack = onClose,
                    onSettings = { state.activeDialog = ReaderDialogType.SETTINGS },
                    onTheme = { state.activeDialog = ReaderDialogType.THEME },
                    onMore = {},
                    onToc = { state.activeDialog = ReaderDialogType.TOC },
                    onAnnotations = { state.activeDialog = ReaderDialogType.ANNOTATIONS }
                )
            },
            contextualOverlays = {
                // TODO: Selection and Annotation Actions
            },
            sheets = {
                when (state.activeDialog) {
                    ReaderDialogType.TOC -> {
                        ReaderTocDialog(
                            anchorBounds = state.anchorBounds,
                            toc = toc,
                            onDismiss = { state.activeDialog = ReaderDialogType.NONE },
                            onNavigate = { href ->
                                val targetUrl = baseUrl + href
                                webViewNavigator.loadUrl(targetUrl)
                            }
                        )
                    }
                    ReaderDialogType.ANNOTATIONS -> {
                        ReaderAnnotationsDialog(
                            anchorBounds = state.anchorBounds,
                            annotations = annotations,
                            onDismiss = { state.activeDialog = ReaderDialogType.NONE },
                            onNavigate = { locJson ->
                                try {
                                    val loc = JSONObject(locJson)
                                    val href = loc.optString("href")
                                    val targetUrl = if (href.startsWith("http")) href else baseUrl + href
                                    webViewNavigator.loadUrl(targetUrl)
                                } catch (e: Exception) {}
                            },
                            onDeleteAnnotation = { LibraryRepository.removeAnnotation(it) }
                        )
                    }
                    ReaderDialogType.THEME -> {
                        ReaderThemeDialog(
                            anchorBounds = state.anchorBounds,
                            onDismiss = { state.activeDialog = ReaderDialogType.NONE }
                        )
                    }
                    ReaderDialogType.SETTINGS -> {
                        ReaderSettingsDialog(
                            anchorBounds = state.anchorBounds,
                            onDismiss = { state.activeDialog = ReaderDialogType.NONE }
                        )
                    }
                    else -> {}
                }
            },
            loadingOverlay = {
                ReaderLoadingOverlay(isVisible = state.isLoading, coverUrl = book.coverUrl)
            }
        )
    }
}

private data class ChapterLink(val title: String, val href: String)
private data class EpubInfo(val toc: List<ChapterLink>, val opfPath: String)

@Composable
private fun ReaderTocDialog(
    anchorBounds: androidx.compose.ui.geometry.Rect?,
    toc: List<ChapterLink>,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    ReaderBubbleDialog(anchorBounds = anchorBounds, onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.toc),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            HorizontalDivider()
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(toc) { link ->
                    ListItem(
                        headlineContent = { Text(link.title) },
                        modifier = Modifier.clickable { 
                            onNavigate(link.href)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

private fun loadEpubInfo(path: String): EpubInfo {
    val toc = mutableListOf<ChapterLink>()
    var opfPath = ""
    try {
        val file = File(path)
        ZipFile(file).use { zip ->
            val containerEntry = zip.getEntry("META-INF/container.xml") ?: return EpubInfo(emptyList(), "")
            val containerDoc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(zip.getInputStream(containerEntry))
            opfPath = containerDoc.getElementsByTagName("rootfile").item(0)?.attributes?.getNamedItem("full-path")?.nodeValue ?: ""
            
            if (opfPath.isNotEmpty()) {
                val opfDoc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(zip.getInputStream(zip.getEntry(opfPath)))
                val manifestItems = opfDoc.getElementsByTagName("item")
                val manifest = mutableMapOf<String, String>()
                for (i in 0 until manifestItems.length) {
                    val item = manifestItems.item(i)
                    val id = item.attributes.getNamedItem("id")?.nodeValue
                    val href = item.attributes.getNamedItem("href")?.nodeValue
                    if (id != null && href != null) manifest[id] = href
                }
                
                val spineItems = opfDoc.getElementsByTagName("itemref")
                for (i in 0 until spineItems.length) {
                    val idref = spineItems.item(i).attributes.getNamedItem("idref")?.nodeValue
                    val href = manifest[idref]
                    if (href != null) {
                        val resolvedHref = resolveRelativePath(opfPath, href)
                        toc.add(ChapterLink(href, resolvedHref)) // 这里简化了标题，可以进一步解析 NCX/NAV
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return EpubInfo(toc, opfPath)
}

private fun resolveRelativePath(basePath: String, relativePath: String): String {
    if (relativePath.startsWith("/")) return relativePath.drop(1)
    val parent = File(basePath).parent?.replace("\\", "/") ?: ""
    return if (parent.isEmpty()) relativePath else "$parent/$relativePath"
}

