package org.dazai.osmium.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.dazai.osmium.util.getHourOfDay
import org.dazai.osmium.manager.OsmiumThemeType
import org.dazai.osmium.manager.DarkFollowMode
import org.dazai.osmium.manager.ThemeManager

@Composable
expect fun dynamicColorScheme(isDark: Boolean): ColorScheme?

@Composable
expect fun UpdateStatusBarConfig(isDark: Boolean)

@Composable
expect fun UpdateSystemBarsVisibility(visible: Boolean)

@Composable
fun OsmiumTheme(
    themeType: OsmiumThemeType,
    darkFollowMode: DarkFollowMode,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (darkFollowMode) {
        DarkFollowMode.System -> isSystemDark
        DarkFollowMode.Time -> ThemeManager.isCurrentlyInDarkTime(getHourOfDay())
        DarkFollowMode.Environment -> false // TODO
        DarkFollowMode.Always -> true
        DarkFollowMode.None -> false
    }

    val dynamicColors = if (themeType == OsmiumThemeType.System) dynamicColorScheme(isDark) else null

    var colorScheme = dynamicColors ?: when (themeType) {
        OsmiumThemeType.System -> if (isDark) darkColorScheme() else lightColorScheme()
        OsmiumThemeType.Matcha -> if (isDark) AppThemeSchemes.MatchaDarkColorScheme else AppThemeSchemes.MatchaLightColorScheme
        OsmiumThemeType.Azuki -> if (isDark) AppThemeSchemes.AzukiDarkColorScheme else AppThemeSchemes.AzukiLightColorScheme
        OsmiumThemeType.Cocoa -> if (isDark) AppThemeSchemes.CocoaDarkColors else AppThemeSchemes.CocoaLightColors
        OsmiumThemeType.Metal -> if (isDark) AppThemeSchemes.MetalColorScheme else AppThemeSchemes.MetalLightColorScheme
    }

    // 实现 TrueBlack (纯黑模式)
    if (isDark && ThemeManager.trueBlack) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )

    UpdateStatusBarConfig(isDark)
}

