package org.dazai.osmium.ui

import androidx.compose.runtime.*
import org.dazai.osmium.db.DatabaseDriverFactory
import org.dazai.osmium.db.DatabaseHelper
import org.dazai.osmium.domain.model.Book
import org.dazai.osmium.domain.repository.LibraryRepository
import org.dazai.osmium.ui.library.LibraryScreen
import org.dazai.osmium.ui.settings.SettingsScreen
import org.dazai.osmium.ui.theme.OsmiumTheme
import org.dazai.osmium.manager.OsmiumThemeType
import org.dazai.osmium.manager.ThemeManager
import org.dazai.osmium.manager.LanguageManager
import org.dazai.osmium.ui.reader.BookReader

@Composable
fun App(databaseDriverFactory: DatabaseDriverFactory) {
    // 初始化数据库
    LaunchedEffect(databaseDriverFactory) {
        val helper = DatabaseHelper(databaseDriverFactory)
        LibraryRepository.initialize(helper)
        
        // 批量加载设置并同步初始化管理器
        val allSettings = LibraryRepository.getAllSettings()
        ThemeManager.initialize(allSettings)
        LanguageManager.initialize(allSettings)
    }

    OsmiumTheme(
        themeType = ThemeManager.currentTheme,
        darkFollowMode = ThemeManager.darkFollowMode
    ) {
        var currentBookId by remember { mutableStateOf<String?>(null) }
        var showSettings by remember { mutableStateOf(false) }

        if (showSettings) {
            SettingsScreen(onBack = { showSettings = false })
        } else if (currentBookId == null) {
            LibraryScreen(
                onBookClick = { 
                    println("App: Opening book ${it.id}")
                    currentBookId = it.id 
                },
                onSettingsClick = {
                    showSettings = true
                }
            )
        } else {
            BookReader(
                bookId = currentBookId!!,
                onClose = { 
                    println("App: Closing book reader")
                    currentBookId = null 
                }
            )
        }
    }
}

