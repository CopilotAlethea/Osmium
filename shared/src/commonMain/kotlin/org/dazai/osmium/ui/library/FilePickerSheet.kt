package org.dazai.osmium.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.dazai.osmium.util.FileImporter
import org.dazai.osmium.util.PlatformFile
import org.jetbrains.compose.resources.stringResource
import osmium.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePickerSheet(
    importer: FileImporter,
    onFileSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val rootPath = remember { importer.getRootPath() }
    var currentPath by remember { mutableStateOf(rootPath) }
    var files by remember { mutableStateOf<List<PlatformFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(currentPath) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val allFiles = importer.listFiles(currentPath)
            // 文件夹裁剪逻辑：仅保留包含目标文件(.epub, .pdf)的文件夹或目标文件本身
            val filtered = allFiles.filter { file ->
                if (file.name.startsWith(".")) return@filter false

                if (file.isDirectory) {
                    // 递归检查（或根据需求简单检查一级）
                    hasRelevantFiles(file.path, importer)
                } else {
                    file.extension.lowercase() in listOf("epub", "pdf")
                }
            }
            files = filtered
            isLoading = false
        }
    }

    FilePickerSheetContent(
        currentPath = currentPath,
        rootPath = rootPath,
        files = files,
        isLoading = isLoading,
        onPathChange = { currentPath = it },
        onFileSelected = onFileSelected,
        onDismiss = onDismiss
    )
}

/**
 * 递归检查文件夹是否包含相关书籍文件
 */
private fun hasRelevantFiles(path: String, importer: FileImporter): Boolean {
    val items = try { importer.listFiles(path) } catch (e: Exception) { emptyList() }
    for (item in items) {
        if (item.name.startsWith(".")) continue
        if (item.isDirectory) {
            if (hasRelevantFiles(item.path, importer)) return true
        } else {
            if (item.extension.lowercase() in listOf("epub", "pdf")) return true
        }
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePickerSheetContent(
    currentPath: String,
    rootPath: String,
    files: List<PlatformFile>,
    isLoading: Boolean,
    onPathChange: (String) -> Unit,
    onFileSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
    BoxWithConstraints {
        Column(
            modifier = Modifier
                .heightIn(max = maxHeight * 0.55f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // 固定头部区域
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .height(72.dp) // 统一高度为 72dp
                    .fillMaxWidth()
                    .zIndex(10f)
            ) {
                Column {
                    // 路径导航栏 - 垂直居中于 72dp 空间
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentPath != rootPath) {
                                    val parent = currentPath.substringBeforeLast('/', "")
                                    val nextPath = if (parent.isEmpty() || (parent.length < rootPath.length)) rootPath else parent
                                    onPathChange(nextPath)
                                }
                            },
                            enabled = currentPath != rootPath
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                        }
                        Spacer(Modifier.width(12.dp))

                        val displayPath = if (currentPath == rootPath) {
                            stringResource(Res.string.internal_storage)
                        } else {
                            currentPath.removePrefix(rootPath)
                        }

                        Text(
                            text = displayPath,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(16.dp))
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            // 可滚动列表区域
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .clipToBounds()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                items(files) { file ->
                    val primaryColor = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

                    ListItem(
                        headlineContent = {
                            Text(
                                text = file.name,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        },
                        modifier = Modifier.clickable {
                            if (file.isDirectory) {
                                if (file.path.startsWith(rootPath)) {
                                    onPathChange(file.path)
                                }
                            } else {
                                onFileSelected(file.path)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }

                if (files.isEmpty() && !isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(Res.string.no_books_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
}

