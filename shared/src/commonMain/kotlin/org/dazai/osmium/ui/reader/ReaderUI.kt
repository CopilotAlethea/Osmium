package org.dazai.osmium.ui.reader

import org.jetbrains.compose.resources.stringResource
import osmium.shared.generated.resources.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import org.dazai.osmium.domain.model.Annotation
import org.dazai.osmium.domain.model.AnnotationType
import org.dazai.osmium.domain.repository.LibraryRepository
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import kotlinx.coroutines.launch
import org.dazai.osmium.util.currentTimeMillis
import org.dazai.osmium.util.generateUUID

@Composable
fun ReaderPersistentBottomText(
    title: String,
    hierarchy: List<String>,
    modifier: Modifier = Modifier
) {
    val pathText = formatDynamicPath(title, hierarchy, 30)
    val colorScheme = MaterialTheme.colorScheme
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = pathText,
            style = TextStyle(
                fontSize = 11.sp,
                color = colorScheme.onSurface.copy(alpha = 0.45f),
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ReaderInteractionLayer(
    state: ReaderState,
    onPageForward: () -> Unit,
    onPageBackward: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val width = size.width
                        val x = offset.x
                        val leftBound = width * ReaderConstants.PageTurnEdgeRatio
                        val rightBound = width * (1f - ReaderConstants.PageTurnEdgeRatio)

                        when {
                            x in leftBound..rightBound -> state.togglePopups()
                            x < leftBound -> { onPageBackward(); state.resetKeepScreenOn() }
                            x > rightBound -> { onPageForward(); state.resetKeepScreenOn() }
                        }
                    }
                )
            }
    ) {
        content?.invoke()
    }
}

@Composable
fun QuickBackOverlay(
    state: ReaderState,
    onBack: () -> Unit,
    onIgnore: () -> Unit
) {
    AnimatedVisibility(
        visible = state.canGoBack,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(animationSpec = tween(600, easing = EaseOutCubic)),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(animationSpec = tween(500, easing = EaseInCubic)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onIgnore() })
                }
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .pointerInput(Unit) {
                        detectTapGestures { /* consume */ }
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    tonalElevation = 8.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .height(52.dp)
                        .weight(1f, fill = false)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when {
                                state.backLocationChapter != null -> stringResource(Res.string.back_to_previous, state.backLocationChapter!!)
                                state.backLocationName != null -> stringResource(Res.string.back_to_previous, state.backLocationName!!)
                                else -> stringResource(Res.string.back_to_previous, stringResource(Res.string.previous_page))
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    onClick = onIgnore,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    tonalElevation = 8.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(Res.string.ignore),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderPopups(
    state: ReaderState,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onTheme: () -> Unit,
    onMore: () -> Unit,
    onToc: () -> Unit,
    onAnnotations: () -> Unit,
    modifier: Modifier = Modifier
) {
    val show = state.showPopups
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(state.showPopups, state.lastInteractionTime, state.activeDialog) {
        if (state.showPopups && state.activeDialog == ReaderDialogType.NONE) {
            val now = currentTimeMillis()
            val elapsed = now - state.lastInteractionTime
            val remaining = ReaderConstants.PopupAutoHideDelayMs - elapsed
            if (remaining > 0) {
                delay(remaining)
            }
            if (currentTimeMillis() - state.lastInteractionTime >= ReaderConstants.PopupAutoHideDelayMs && state.activeDialog == ReaderDialogType.NONE) {
                state.showPopups = false
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = show,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 0.dp, end = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReaderIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = { state.updateInteractionTime(); onBack() },
                    shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                    modifier = Modifier.width(55.dp),
                    iconAlignment = Alignment.CenterEnd
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val searchSelected = state.activeDialog == ReaderDialogType.SEARCH
                    ReaderIconButton(
                        icon = Icons.Default.Search,
                        isSelected = searchSelected,
                        onClick = { 
                            state.updateInteractionTime()
                            if (searchSelected) {
                                state.activeDialog = ReaderDialogType.NONE
                            } else {
                                state.activeDialog = ReaderDialogType.SEARCH
                            }
                        },
                        color = colorScheme.surfaceVariant,
                        modifier = Modifier.onGloballyPositioned { 
                            if (searchSelected) state.anchorBounds = it.boundsInWindow() 
                        }
                    )

                    ReaderIconGroup(
                        color = colorScheme.secondaryContainer
                    ) {
                        val settingsSelected = state.activeDialog == ReaderDialogType.SETTINGS
                        IconButton(
                            modifier = Modifier.onGloballyPositioned { 
                                if (settingsSelected) state.anchorBounds = it.boundsInWindow() 
                            },
                            onClick = { 
                                state.updateInteractionTime()
                                if (settingsSelected) {
                                    state.activeDialog = ReaderDialogType.NONE
                                } else {
                                    onSettings()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Settings, 
                                contentDescription = stringResource(Res.string.settings), 
                                modifier = Modifier.size(20.dp),
                                tint = if (settingsSelected) colorScheme.primary else LocalContentColor.current
                            )
                        }

                        val themeSelected = state.activeDialog == ReaderDialogType.THEME
                        IconButton(
                            modifier = Modifier.onGloballyPositioned { 
                                if (themeSelected) state.anchorBounds = it.boundsInWindow() 
                            },
                            onClick = { 
                                state.updateInteractionTime()
                                if (themeSelected) {
                                    state.activeDialog = ReaderDialogType.NONE
                                } else {
                                    onTheme()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Palette, 
                                contentDescription = stringResource(Res.string.theme),
                                modifier = Modifier.size(20.dp),
                                tint = if (themeSelected) colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }

                    ReaderIconButton(
                        icon = Icons.Default.MoreVert,
                        onClick = { state.updateInteractionTime(); onMore() },
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                        modifier = Modifier.width(55.dp),
                        iconAlignment = Alignment.CenterStart
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = show,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
            ) {
                val tocSelected = state.activeDialog == ReaderDialogType.TOC
                ReaderIconButton(
                    icon = Icons.AutoMirrored.Filled.List,
                    isSelected = tocSelected,
                    onClick = { 
                        state.updateInteractionTime()
                        if (tocSelected) {
                            state.activeDialog = ReaderDialogType.NONE
                        } else {
                            onToc()
                        }
                    },
                    color = colorScheme.secondaryContainer,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .onGloballyPositioned { 
                            if (tocSelected) state.anchorBounds = it.boundsInWindow() 
                        }
                )

                val annoSelected = state.activeDialog == ReaderDialogType.ANNOTATIONS
                ReaderIconButton(
                    icon = Icons.AutoMirrored.Filled.StickyNote2,
                    isSelected = annoSelected,
                    onClick = { 
                        state.updateInteractionTime()
                        if (annoSelected) {
                            state.activeDialog = ReaderDialogType.NONE
                        } else {
                            onAnnotations()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .onGloballyPositioned { 
                            if (annoSelected) state.anchorBounds = it.boundsInWindow() 
                        }
                )
            }
        }
    }
}

