package org.dazai.osmium.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dazai.osmium.ui.reader.ThemeSettingsPanel
import org.dazai.osmium.util.ui.BackHandler
import org.dazai.osmium.manager.LanguageManager
import org.jetbrains.compose.resources.stringResource
import osmium.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var showThemeSubpage by remember { mutableStateOf(false) }
    var showLanguageSubpage by remember { mutableStateOf(false) }
    var showNotesSubpage by remember { mutableStateOf(false) }

    // 处理系统返回按键/手势
    BackHandler(onBack = {
        when {
            showThemeSubpage -> showThemeSubpage = false
            showLanguageSubpage -> showLanguageSubpage = false
            showNotesSubpage -> showNotesSubpage = false
            else -> onBack()
        }
    })

    if (showThemeSubpage) {
        ThemeSettingsSubpage(onBack = { showThemeSubpage = false })
    } else if (showLanguageSubpage) {
        LanguageSettingsSubpage(onBack = { showLanguageSubpage = false })
    } else if (showNotesSubpage) {
        NotesAndAnnotationsSubpage(onBack = { showNotesSubpage = false })
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.settings)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 用户头像区域
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("D", style = MaterialTheme.typography.headlineLarge)
                    }
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text("Dazai", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("dazai@example.com", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                SettingsSection(title = stringResource(Res.string.user_preferences)) {
                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = stringResource(Res.string.language),
                        subtitle = LanguageManager.getLanguageName(LanguageManager.currentLanguage),
                        onClick = { showLanguageSubpage = true }
                    )
                    SettingsItem(icon = Icons.Default.Palette, title = stringResource(Res.string.theme_settings), onClick = { showThemeSubpage = true })
                }
                
                SettingsSection(title = stringResource(Res.string.reading_data)) {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.StickyNote2,
                        title = stringResource(Res.string.notes_and_annotations),
                        onClick = { showNotesSubpage = true }
                    )
                    SettingsItem(icon = Icons.Default.CloudSync, title = stringResource(Res.string.sync_settings), subtitle = stringResource(Res.string.sync_settings_desc))
                }
                
                SettingsSection(title = stringResource(Res.string.personal_settings)) {
                    SettingsItem(icon = Icons.Default.AccountCircle, title = stringResource(Res.string.account_settings), subtitle = stringResource(Res.string.account_settings_desc))
                    SettingsItem(icon = Icons.Default.Lock, title = stringResource(Res.string.change_password))
                }

                SettingsSection(title = stringResource(Res.string.other)) {
                    SettingsItem(icon = Icons.Default.Update, title = stringResource(Res.string.check_updates))
                    SettingsItem(icon = Icons.Default.Info, title = stringResource(Res.string.about))
                    SettingsItem(icon = Icons.AutoMirrored.Filled.Logout, title = stringResource(Res.string.logout), textColor = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsSubpage(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.language)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            LanguageSettingsPanel()
        }
    }
}

@Composable
fun LanguageSettingsPanel() {
    val colorScheme = MaterialTheme.colorScheme
    val languages = LanguageManager.supportedLanguages

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(Res.string.language),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            languages.forEach { info ->
                LanguageOption(
                    title = info.nativeName,
                    subtitle = if (info.name != info.nativeName) info.name else null,
                    isSelected = LanguageManager.currentLanguage == info.code,
                    onClick = { LanguageManager.updateLanguage(info.code) }
                )
            }
        }
    }
}

@Composable
private fun LanguageOption(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        RadioButton(selected = isSelected, onClick = null)
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (textColor == MaterialTheme.colorScheme.error) textColor else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = textColor)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsSubpage(onBack: () -> Unit) {
    var isTimedExpanded by remember { mutableStateOf(false) }

    // 处理系统返回按键/手势，返回到设置主页而非直接退出设置
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.theme_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp), // 移除 horizontal padding，让内容自行贴边
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            ThemeSettingsPanel(
                isTimedExpanded = isTimedExpanded,
                onTimedExpandChange = { isTimedExpanded = it }
            )
        }
    }
}

