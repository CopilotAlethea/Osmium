package org.dazai.osmium.ui.reader

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dazai.osmium.ui.theme.AppThemeSchemes
import org.dazai.osmium.manager.ThemeManager
import org.dazai.osmium.manager.OsmiumThemeType
import org.dazai.osmium.manager.DarkFollowMode
import org.dazai.osmium.util.supportsSystemDark
import org.dazai.osmium.util.isAtLeastS
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import osmium.shared.generated.resources.*

import org.dazai.osmium.domain.model.Annotation
import org.dazai.osmium.domain.model.AnnotationType

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.foundation.lazy.items

@Composable
fun FullscreenImageViewer(
    imageUrl: String?,
    imageData: ByteArray?,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageData ?: imageUrl,
            contentDescription = "Fullscreen Image",
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        
                        val maxTranslationX = (scale - 1) * size.width / 2
                        val maxTranslationY = (scale - 1) * size.height / 2
                        
                        val newOffsetX = offset.x + pan.x * scale
                        val newOffsetY = offset.y + pan.y * scale
                        
                        offset = Offset(
                            x = newOffsetX.coerceIn(-maxTranslationX, maxTranslationX),
                            y = newOffsetY.coerceIn(-maxTranslationY, maxTranslationY)
                        )
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun ReaderLoadingOverlay(
    isVisible: Boolean,
    coverUrl: String?,
) {
    val colorScheme = MaterialTheme.colorScheme
    
    val scaleAnim = remember { Animatable(0.25f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            scaleAnim.snapTo(0.25f)
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = ReaderConstants.WebviewRenderDelayMs.toInt(),
                    easing = LinearEasing
                )
            )
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(1200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                },
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .graphicsLayer {
                            scaleX = scaleAnim.value
                            scaleY = scaleAnim.value
                        },
                    contentScale = ContentScale.Crop
                )
            }

            CircularProgressIndicator(
                color = colorScheme.primary.copy(alpha = 0.7f),
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun ReaderThemeDialog(
    anchorBounds: Rect?,
    onDismiss: () -> Unit
) {
    var isTimedExpanded by remember { mutableStateOf(false) }

    ReaderBubbleDialog(
        anchorBounds = anchorBounds,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            ThemeSettingsPanel(
                isTimedExpanded = isTimedExpanded,
                onTimedExpandChange = { isTimedExpanded = it }
            )
        }
    }
}

@Composable
fun ReaderSettingsDialog(
    anchorBounds: Rect?,
    onDismiss: () -> Unit
) {
    ReaderBubbleDialog(
        anchorBounds = anchorBounds,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            TextSettingsPanel()
        }
    }
}

@Composable
fun TextSettingsPanel() {
    val isUserControlEnabled = !ThemeManager.publisherStyles

    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(Res.string.publisher_styles), style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = ThemeManager.publisherStyles,
                onCheckedChange = { ThemeManager.updatePublisherStyles(it) }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        CompositionLocalProvider(
            LocalContentColor provides if (isUserControlEnabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.alpha(if (isUserControlEnabled) 1f else 0.6f)
            ) {
                FontSettingsSection(enabled = isUserControlEnabled)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(Res.string.first_line_indent), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = ThemeManager.firstLineIndent,
                        enabled = isUserControlEnabled,
                        onCheckedChange = { ThemeManager.updateFirstLineIndent(it) }
                    )
                }

                StepperSettingRow(
                    label = stringResource(Res.string.font_size),
                    value = ThemeManager.fontSizeLevel,
                    options = listOf(1, 2, 3, 4, 5),
                    enabled = isUserControlEnabled,
                    onOptionClick = { ThemeManager.updateFontSizeLevel(it) },
                    displayMapper = { 
                        when(it) {
                            1 -> stringResource(Res.string.size_small)
                            2 -> stringResource(Res.string.size_smaller)
                            3 -> stringResource(Res.string.size_default)
                            4 -> stringResource(Res.string.size_larger)
                            5 -> stringResource(Res.string.size_large)
                            else -> ""
                        }
                    }
                )

                StepperSettingRow(
                    label = stringResource(Res.string.letter_spacing),
                    value = ThemeManager.letterSpacingLevel,
                    options = listOf(1, 2, 3, 4, 5),
                    enabled = isUserControlEnabled,
                    onOptionClick = { ThemeManager.updateLetterSpacingLevel(it) },
                    displayMapper = {
                        when(it) {
                            1 -> stringResource(Res.string.spacing_narrow)
                            2 -> stringResource(Res.string.spacing_narrower)
                            3 -> stringResource(Res.string.size_default)
                            4 -> stringResource(Res.string.spacing_wider)
                            5 -> stringResource(Res.string.spacing_wide)
                            else -> ""
                        }
                    }
                )

                StepperSettingRow(
                    label = stringResource(Res.string.line_height),
                    value = ThemeManager.lineHeightLevel,
                    options = listOf(1, 2, 3, 4, 5),
                    enabled = isUserControlEnabled,
                    onOptionClick = { ThemeManager.updateLineHeightLevel(it) },
                    displayMapper = {
                        when(it) {
                            1 -> stringResource(Res.string.spacing_narrow)
                            2 -> stringResource(Res.string.spacing_narrower)
                            3 -> stringResource(Res.string.size_default)
                            4 -> stringResource(Res.string.spacing_wider)
                            5 -> stringResource(Res.string.spacing_wide)
                            else -> ""
                        }
                    }
                )

                StepperSettingRow(
                    label = stringResource(Res.string.para_spacing),
                    value = ThemeManager.paraSpacingLevel,
                    options = listOf(1, 2, 3, 4, 5),
                    enabled = isUserControlEnabled,
                    onOptionClick = { ThemeManager.updateParaSpacingLevel(it) },
                    displayMapper = {
                        when(it) {
                            1 -> stringResource(Res.string.spacing_narrow)
                            2 -> stringResource(Res.string.spacing_narrower)
                            3 -> stringResource(Res.string.size_default)
                            4 -> stringResource(Res.string.spacing_wider)
                            5 -> stringResource(Res.string.spacing_wide)
                            else -> ""
                        }
                    }
                )

                StepperSettingRow(
                    label = stringResource(Res.string.page_margin),
                    value = ThemeManager.marginLevel,
                    options = listOf(1, 2, 3, 4, 5),
                    enabled = isUserControlEnabled,
                    onOptionClick = { ThemeManager.updateMarginLevel(it) },
                    displayMapper = {
                        when(it) {
                            1 -> stringResource(Res.string.spacing_narrow)
                            2 -> stringResource(Res.string.spacing_narrower)
                            3 -> stringResource(Res.string.size_default)
                            4 -> stringResource(Res.string.spacing_wider)
                            5 -> stringResource(Res.string.spacing_wide)
                            else -> ""
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FontSettingsSection(enabled: Boolean = true) {
    val fonts = listOf(
        Res.string.font_system to "system",
        Res.string.font_serif to "serif",
        Res.string.font_kaiti to "kaiti",
        Res.string.font_fangsong to "fangsong"
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(Res.string.font),
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            fonts.forEachIndexed { index, (res, key) ->
                val isSelected = ThemeManager.fontFamily == key
                SegmentedButton(
                    selected = isSelected,
                    enabled = enabled,
                    onClick = { ThemeManager.updateFontFamily(key) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = fonts.size),
                    label = { Text(stringResource(res)) }
                )
            }
        }
    }
}

@Composable
fun <T> StepperSettingRow(
    label: String,
    value: T,
    options: List<T>,
    enabled: Boolean = true,
    onOptionClick: (T) -> Unit,
    displayMapper: @Composable (T) -> String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label, 
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.5f else 0.2f))
        ) {
            val currentIndex = options.indexOf(value)
            
            IconButton(
                onClick = { if (currentIndex > 0) onOptionClick(options[currentIndex - 1]) },
                enabled = enabled && currentIndex > 0
            ) {
                Icon(
                    Icons.Default.Remove, 
                    contentDescription = stringResource(Res.string.decrease), 
                    modifier = Modifier.size(20.dp),
                    tint = if (enabled && currentIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
            
            Text(
                text = displayMapper(value),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.widthIn(min = 48.dp),
                textAlign = TextAlign.Center,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
            
            IconButton(
                onClick = { if (currentIndex < options.size - 1) onOptionClick(options[currentIndex + 1]) },
                enabled = enabled && currentIndex < options.size - 1
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = stringResource(Res.string.increase), 
                    modifier = Modifier.size(20.dp),
                    tint = if (enabled && currentIndex < options.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
    }
}

@Composable
fun ThemeSettingsPanel(
    isTimedExpanded: Boolean,
    onTimedExpandChange: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(Res.string.theme_color),
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                val availableThemes = OsmiumThemeType.entries.filter { 
                    it != OsmiumThemeType.System || isAtLeastS
                }
                items(availableThemes.size) { index ->
                    val type = availableThemes[index]
                    ThemeOption(
                        type = type,
                        isSelected = ThemeManager.currentTheme == type,
                        onClick = { ThemeManager.updateTheme(type) }
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(Res.string.auto_dark),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DarkFollowOption(
                        title = stringResource(Res.string.mode_none),
                        isSelected = ThemeManager.darkFollowMode == DarkFollowMode.None,
                        onClick = { 
                            ThemeManager.updateDarkFollow(DarkFollowMode.None)
                            onTimedExpandChange(false)
                        }
                    )

                    DarkFollowOption(
                        title = stringResource(Res.string.mode_always),
                        isSelected = ThemeManager.darkFollowMode == DarkFollowMode.Always,
                        onClick = {
                            ThemeManager.updateDarkFollow(DarkFollowMode.Always)
                            onTimedExpandChange(false)
                        }
                    )

                    if (supportsSystemDark) {
                        DarkFollowOption(
                            title = stringResource(Res.string.mode_system),
                            isSelected = ThemeManager.darkFollowMode == DarkFollowMode.System,
                            onClick = { 
                                ThemeManager.updateDarkFollow(DarkFollowMode.System)
                                onTimedExpandChange(false)
                            }
                        )
                    }

                    DarkFollowOption(
                        title = stringResource(Res.string.mode_time),
                        isSelected = ThemeManager.darkFollowMode == DarkFollowMode.Time,
                        onClick = { 
                            if (ThemeManager.darkFollowMode == DarkFollowMode.Time) {
                                onTimedExpandChange(!isTimedExpanded)
                            } else {
                                ThemeManager.updateDarkFollow(DarkFollowMode.Time)
                                onTimedExpandChange(true)
                            }
                        }
                    )

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isTimedExpanded,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .graphicsLayer {
                                    scaleX = 0.85f
                                    scaleY = 0.85f
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.start),
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurface.copy(alpha = 0.5f),
                                softWrap = false
                            )
                            
                            Spacer(Modifier.width(8.dp))
                            
                            CompactTimeSelector(
                                hour = ThemeManager.darkTimeStart,
                                minute = 0,
                                onTimeChange = { h, _ -> ThemeManager.updateDarkTimeStart(h) }
                            )
                            
                            Text(
                                text = stringResource(Res.string.to),
                                modifier = Modifier.padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                softWrap = false
                            )
                            
                            CompactTimeSelector(
                                hour = ThemeManager.darkTimeEnd,
                                minute = 0,
                                onTimeChange = { h, _ -> ThemeManager.updateDarkTimeEnd(h) }
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = stringResource(Res.string.end),
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurface.copy(alpha = 0.5f),
                                softWrap = false
                            )
                        }
                    }

                    DarkFollowOption(
                        title = stringResource(Res.string.env_unavailable),
                        isSelected = ThemeManager.darkFollowMode == DarkFollowMode.Environment,
                        enabled = false,
                        onClick = { ThemeManager.updateDarkFollow(DarkFollowMode.Environment) }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            
            // 3. 为深色模式启用 trueBlack
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("trueBlack", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(Res.string.true_black_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Switch(
                    checked = ThemeManager.trueBlack,
                    onCheckedChange = { ThemeManager.updateTrueBlack(it) }
                )
            }
        }
    }
}

@Composable
fun CompactTimeSelector(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WheelTimeSelector(
            value = hour,
            max = 23,
            onValueChange = { onTimeChange(it, minute) }
        )
        Text(":", modifier = Modifier.padding(horizontal = 2.dp), fontWeight = FontWeight.Bold)
        WheelTimeSelector(
            value = minute,
            max = 59,
            onValueChange = { onTimeChange(hour, it) }
        )
    }
}

@Composable
fun WheelTimeSelector(
    value: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    val itemHeight = 24.dp
    val visibleItems = 3
    val containerHeight = itemHeight * visibleItems

    Box(
        modifier = Modifier
            .width(40.dp)
            .height(containerHeight)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = value + 1000 * (max + 1))

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = itemHeight)
        ) {
            items(Int.MAX_VALUE) { index ->
                val v = index % (max + 1)
                val isSelected = v == value
                Box(
                    modifier = Modifier.height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = v.toString().padStart(2, '0'),
                        style = if (isSelected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
        )

        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .collect { isScrolling ->
                    if (!isScrolling && listState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                        val centerIndex = listState.firstVisibleItemIndex
                        val currentVal = centerIndex % (max + 1)
                        if (currentVal != value) {
                            onValueChange(currentVal)
                        }
                        if (listState.firstVisibleItemScrollOffset != 0) {
                            listState.animateScrollToItem(centerIndex)
                        }
                    }
                }
        }
    }
}

@Composable
private fun DarkFollowOption(
    title: String,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        RadioButton(selected = isSelected, onClick = null, enabled = enabled)
    }
}

@Composable
private fun ThemeOption(
    type: OsmiumThemeType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val themeColors = when (type) {
        OsmiumThemeType.System -> null
        OsmiumThemeType.Matcha -> AppThemeSchemes.MatchaLightColorScheme
        OsmiumThemeType.Azuki -> AppThemeSchemes.AzukiLightColorScheme
        OsmiumThemeType.Cocoa -> AppThemeSchemes.CocoaLightColors
        OsmiumThemeType.Metal -> AppThemeSchemes.MetalLightColorScheme
    }

    val bgColor = themeColors?.primary ?: MaterialTheme.colorScheme.primary
    
    val outerBorderColor = if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.Transparent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .border(2.5.dp, outerBorderColor, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(bgColor)
                .clickable { onClick() }
        )
        Text(
            text = stringResource(type.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ReaderIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    shape: Shape = CircleShape,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = LocalContentColor.current,
    iconAlignment: Alignment = Alignment.Center
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (isSelected) colorScheme.primaryContainer else color,
        contentColor = if (isSelected) colorScheme.primary else contentColor,
        modifier = modifier.height(40.dp)
    ) {
        Box(
            modifier = Modifier.widthIn(min = 40.dp).fillMaxHeight(),
            contentAlignment = iconAlignment
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ReaderIconGroup(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        shape = CircleShape,
        color = color,
        modifier = modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((-4).dp),
            content = content
        )
    }
}

@Composable
fun ReaderBubbleDialog(
    anchorBounds: Rect?,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val anchor = anchorBounds ?: return
    val density = LocalDensity.current
    
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val maxPopupHeight = screenHeight * 0.52f
        
        val anchorTop = with(density) { anchor.top.toDp() }
        val anchorBottom = with(density) { anchor.bottom.toDp() }
        val anchorCenter = with(density) { (anchor.left + anchor.right).toDp() / 2 }
        
        val isBottomPopup = anchorTop < screenHeight / 2
        
        val yOffset = if (isBottomPopup) anchorBottom + 8.dp else 0.dp

        Popup(
            alignment = if (isBottomPopup) Alignment.TopCenter else Alignment.BottomCenter,
            offset = IntOffset(
                x = with(density) { (anchorCenter - maxWidth / 2).toPx().toInt() },
                y = with(density) { (if (isBottomPopup) yOffset else -(screenHeight - anchorTop + 8.dp)).toPx().toInt() }
            ),
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true)
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = maxWidth - 32.dp)
                    .width(400.dp)
                    .heightIn(max = maxPopupHeight)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectionButtonGroup(
    isTopPopup: Boolean,
    lastUnderlineColor: Int,
    lastHighlightColor: Int,
    lastStyle: String?,
    onAction: (AnnotationType, String?, Int) -> Unit,
    onToolUpdate: (AnnotationType, Int, String?) -> Unit,
    onNote: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showUnderlinePicker by remember { mutableStateOf(false) }
    var showHighlightPicker by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current

    val popupAlignment = if (isTopPopup) Alignment.TopCenter else Alignment.BottomCenter
    val popupOffset = if (isTopPopup) {
        IntOffset(0, with(density) { (-60).dp.toPx().toInt() })
    } else {
        IntOffset(0, with(density) { 60.dp.toPx().toInt() })
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.wrapContentSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                color = colorScheme.primaryContainer,
                contentColor = colorScheme.onPrimaryContainer,
                tonalElevation = 6.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier.combinedClickable(
                        onClick = { 
                            onAction(AnnotationType.UNDERLINE, lastStyle ?: "straight", lastUnderlineColor)
                        },
                        onLongClick = { 
                            showHighlightPicker = false
                            showUnderlinePicker = !showUnderlinePicker
                        }
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                       Icons.Default.HorizontalRule,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(lastUnderlineColor)
                    )
                }
            }

            if (showUnderlinePicker) {
                Popup(
                    alignment = popupAlignment,
                    offset = popupOffset,
                    onDismissRequest = { showUnderlinePicker = false }
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colorScheme.primaryContainer,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val underlineColors = listOf(0xFF4C662B.toInt(), 0xFF90414F.toInt(), 0xFF7D5341.toInt())
                                underlineColors.forEach { colorInt ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(Color(colorInt), CircleShape)
                                                .border(
                                                    width = if (lastUnderlineColor == colorInt) 2.dp else 0.dp,
                                                    color = colorScheme.surface,
                                                    shape = CircleShape
                                                )
                                                .clickable { 
                                                    onToolUpdate(AnnotationType.UNDERLINE, colorInt, lastStyle)
                                                    onAction(AnnotationType.UNDERLINE, lastStyle ?: "straight", colorInt)
                                                    showUnderlinePicker = false
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                color = colorScheme.surface,
                contentColor = colorScheme.onPrimaryContainer,
                tonalElevation = 6.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier.combinedClickable(
                        onClick = { 
                            onAction(AnnotationType.HIGHLIGHT, "marker", lastHighlightColor)
                        },
                        onLongClick = { 
                            showUnderlinePicker = false
                            showHighlightPicker = !showHighlightPicker 
                        }
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Circle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp).border(
                            width = 2.dp,
                            color = colorScheme.primary,
                            shape = CircleShape
                        ),
                        tint = Color(lastHighlightColor)
                    )
                }
            }

            if (showHighlightPicker) {
                Popup(
                    alignment = popupAlignment,
                    offset = popupOffset,
                    onDismissRequest = { showHighlightPicker = false }
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val highlighterColors = listOf(0xFFFFE082.toInt(), 0xFFCDEDA3.toInt(), 0xFFF8BBD0.toInt())
                            highlighterColors.forEach { colorInt ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(colorInt), CircleShape)
                                            .border(
                                                width = if (lastHighlightColor == colorInt) 2.dp else 0.dp,
                                                color = colorScheme.primary,
                                                shape = CircleShape
                                            )
                                            .clickable { 
                                                onToolUpdate(AnnotationType.HIGHLIGHT, colorInt, "marker")
                                                onAction(AnnotationType.HIGHLIGHT, "marker", colorInt)
                                                showHighlightPicker = false
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            shape = CircleShape,
            color = colorScheme.primaryContainer,
            contentColor = colorScheme.onPrimaryContainer,
            tonalElevation = 6.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.height(48.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNote) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(22.dp))
                }
                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 2.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(22.dp), tint = colorScheme.error)
                }
            }
        }

        Surface(
            onClick = onDismiss,
            shape = CircleShape,
            color = colorScheme.surfaceVariant,
            contentColor = colorScheme.onSurfaceVariant,
            tonalElevation = 6.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSearchDialog(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearching: Boolean,
    isSearchExhausted: Boolean,
    searchResults: List<ReaderSearchResult>,
    anchorBounds: Rect?,
    onSearch: (String) -> Unit,
    onResultClick: (ReaderSearchResult) -> Unit,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 3
        }
        .distinctUntilChanged()
        .filter { it }
        .collect {
            if (!isSearching && !isSearchExhausted && searchResults.isNotEmpty()) {
                onLoadMore()
            }
        }
    }

    ReaderBubbleDialog(
        anchorBounds = anchorBounds,
        onDismiss = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { 
                        onSearchQueryChange(it)
                        onSearch(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    interactionSource = interactionSource,
                    decorationBox = { innerTextField ->
                        OutlinedTextFieldDefaults.DecorationBox(
                            value = searchQuery,
                            innerTextField = innerTextField,
                            enabled = true,
                            singleLine = true,
                            visualTransformation = VisualTransformation.None,
                            interactionSource = interactionSource,
                            isError = false,
                            placeholder = { Text(stringResource(Res.string.search_book_placeholder)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                if (isSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange(""); onSearch("") }) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            container = {
                                OutlinedTextFieldDefaults.ContainerBox(
                                    enabled = true,
                                    isError = false,
                                    interactionSource = interactionSource,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
                            }
                        )
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(modifier = Modifier.weight(1f, fill = false) .background(MaterialTheme.colorScheme.surface)) {
                if (searchResults.isEmpty() && !isSearching) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (searchQuery.isEmpty()) stringResource(Res.string.search_book_placeholder)
                            else stringResource(Res.string.no_results),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(searchResults) { result ->
                            SearchResultItem(result, onResultClick)
                        }
                        
                        if (isSearching && searchResults.isNotEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        } else if (isSearchExhausted && searchResults.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), 
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(Res.string.no_more_results),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(result: ReaderSearchResult, onClick: (ReaderSearchResult) -> Unit) {
    val annotatedString = buildAnnotatedString {
        val maxBeforeLength = 40
        val rawBefore = result.before ?: ""
        val displayBefore = if (rawBefore.length > maxBeforeLength) {
            "..." + rawBefore.takeLast(maxBeforeLength)
        } else {
            rawBefore
        }

        append(displayBefore)
        withStyle(style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )) {
            append(result.highlight ?: "")
        }
        append(result.after ?: "")
    }

    ListItem(
        headlineContent = {
            Text(
                text = annotatedString,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        supportingContent = {
            if (result.title != null) {
                Text(result.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        modifier = Modifier.clickable { onClick(result) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun NoteInputDialog(
    initialText: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialText.isEmpty()) stringResource(Res.string.add_note) else stringResource(Res.string.edit_note)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                placeholder = { Text(stringResource(Res.string.note_placeholder))
                }
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        }
    )
}

@Composable
expect fun ReaderAnnotationsDialog(
    anchorBounds: Rect?,
    annotations: List<Annotation>,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    onDeleteAnnotation: (String) -> Unit
)

fun formatDynamicPath(title: String, tocPath: List<String>, maxChars: Int): String {
    val items = listOf(title) + tocPath
    val fullPath = items.filter { it.isNotEmpty() }.joinToString(" › ")
    return if (fullPath.length > maxChars) {
        "..." + fullPath.takeLast(maxChars - 3)
    } else {
        fullPath
    }
}

