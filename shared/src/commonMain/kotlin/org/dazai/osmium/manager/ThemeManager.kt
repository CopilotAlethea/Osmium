package org.dazai.osmium.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.dazai.osmium.domain.repository.LibraryRepository
import org.dazai.osmium.util.isAtLeastS
import org.dazai.osmium.util.supportsSystemDark
import org.jetbrains.compose.resources.StringResource
import osmium.shared.generated.resources.*

enum class OsmiumThemeType(val labelRes: StringResource) {
    System(Res.string.theme_system),
    Matcha(Res.string.theme_matcha),
    Azuki(Res.string.theme_azuki),
    Cocoa(Res.string.theme_cocoa),
    Metal(Res.string.theme_metal)
}

enum class DarkFollowMode(val labelRes: StringResource) {
    None(Res.string.mode_none),
    System(Res.string.mode_system),
    Time(Res.string.mode_time),
    Environment(Res.string.env_unavailable),
    Always(Res.string.mode_always)
}

// 全局主题状态管理器
object ThemeManager {
    private const val KeyBaseTheme = "baseTheme"
    private const val KeyDarkFollow = "darkFollow"
    private const val KeyDarkTimeStart = "darkTimeStart"
    private const val KeyDarkTimeEnd = "darkTimeEnd"

    // 设置项的键名
    private const val KeyFontFamily = "fontFamily"
    private const val KeyFontSizeLevel = "fontSizeLevel"
    private const val KeyLineHeightLevel = "lineHeightLevel"
    private const val KeyLetterSpacingLevel = "letterSpacingLevel"
    private const val KeyParaSpacingLevel = "paraSpacingLevel"
    private const val KeyMarginLevel = "marginLevel"
    private const val KeyFirstLineIndent = "firstLineIndent"
    private const val KeyPublisherStyles = "publisherStyles"
    private const val KeyTrueBlack = "trueBlack"

    var currentTheme by mutableStateOf(if (isAtLeastS) OsmiumThemeType.System else OsmiumThemeType.Azuki)
    var darkFollowMode by mutableStateOf(if (supportsSystemDark) DarkFollowMode.System else DarkFollowMode.Time)
    
    // 默认 19:00 开始，07:00 结束
    var darkTimeStart by mutableStateOf(19)
    var darkTimeStartMin by mutableStateOf(0)
    var darkTimeEnd by mutableStateOf(7)
    var darkTimeEndMin by mutableStateOf(0)

    // 阅读样式设置 - 统一使用 1-5 档位 (3 为默认/出版商)
    var fontFamily by mutableStateOf("system")
    var fontSizeLevel by mutableStateOf(3)
    var lineHeightLevel by mutableStateOf(3)
    var letterSpacingLevel by mutableStateOf(3)
    var paraSpacingLevel by mutableStateOf(3)
    var marginLevel by mutableStateOf(3)
    var firstLineIndent by mutableStateOf(true)
    var publisherStyles by mutableStateOf(true)
    var trueBlack by mutableStateOf(false)

    // 数值映射表
    val fontSizeOptions = listOf(80f, 90f, 100f, 120f, 140f)
    val letterSpacingOptions = listOf(0f, 0.02f, 0.05f, 0.1f, 0.2f)
    val lineHeightOptions = listOf(1.25f, 1.35f, 1.5f, 1.75f, 2.0f)
    val paraSpacingOptions = listOf(0.5f, 0.75f, 1.0f, 1.5f, 2.0f)
    val marginOptions = listOf(0.5f, 0.75f, 1.0f, 1.5f, 2.0f)

    // 获取真实数值逻辑
    fun getFontSize(): Float = fontSizeOptions[fontSizeLevel - 1]
    fun getLetterSpacing(): Float = letterSpacingOptions[letterSpacingLevel - 1]
    fun getLineHeight(): Float = lineHeightOptions[lineHeightLevel - 1]
    fun getParaSpacing(): Float = paraSpacingOptions[paraSpacingLevel - 1]
    fun getMargin(): Float = marginOptions[marginLevel - 1]

    fun initialize(settings: Map<String, String>) {
        settings[KeyFontFamily]?.let { fontFamily = it }
        settings[KeyFontSizeLevel]?.toIntOrNull()?.let { fontSizeLevel = it }
        settings[KeyLineHeightLevel]?.toIntOrNull()?.let { lineHeightLevel = it }
        settings[KeyLetterSpacingLevel]?.toIntOrNull()?.let { letterSpacingLevel = it }
        settings[KeyParaSpacingLevel]?.toIntOrNull()?.let { paraSpacingLevel = it }
        settings[KeyMarginLevel]?.toIntOrNull()?.let { marginLevel = it }
        settings[KeyFirstLineIndent]?.let { firstLineIndent = it == "true" }
        settings[KeyPublisherStyles]?.let { publisherStyles = it == "true" } ?: run { publisherStyles = true }
        settings[KeyTrueBlack]?.let { trueBlack = it == "true" }

        settings[KeyBaseTheme]?.let { name ->
            try { 
                currentTheme = when (name) {
                    "Light" -> OsmiumThemeType.System
                    "Dark" -> OsmiumThemeType.System
                    "Oatmeal" -> OsmiumThemeType.Azuki
                    "Mint" -> OsmiumThemeType.Matcha
                    else -> OsmiumThemeType.valueOf(name)
                }
                if (currentTheme == OsmiumThemeType.System && !isAtLeastS) {
                    currentTheme = OsmiumThemeType.Azuki
                }
            } catch (e: Exception) {
                currentTheme = if (isAtLeastS) OsmiumThemeType.System else OsmiumThemeType.Azuki
            }
        } ?: run {
            currentTheme = if (isAtLeastS) OsmiumThemeType.System else OsmiumThemeType.Azuki
        }
        
        settings[KeyDarkTimeStart]?.toIntOrNull()?.let { darkTimeStart = it }
        settings[KeyDarkTimeEnd]?.toIntOrNull()?.let { darkTimeEnd = it }
        
        val savedFollow = settings[KeyDarkFollow]
        if (savedFollow != null) {
            try { 
                val mode = DarkFollowMode.valueOf(savedFollow)
                darkFollowMode = if (mode == DarkFollowMode.System && !supportsSystemDark) {
                    DarkFollowMode.Time
                } else {
                    mode
                }
            } catch (e: Exception) {
                darkFollowMode = if (supportsSystemDark) DarkFollowMode.System else DarkFollowMode.Time
            }
        } else {
            darkFollowMode = if (supportsSystemDark) DarkFollowMode.System else DarkFollowMode.Time
        }
    }

    fun updateTheme(type: OsmiumThemeType) {
        currentTheme = type
        LibraryRepository.insertSetting(KeyBaseTheme, type.name)
    }

    fun updateDarkFollow(mode: DarkFollowMode) {
        darkFollowMode = mode
        LibraryRepository.insertSetting(KeyDarkFollow, mode.name)
    }

    fun updateDarkTime(startHour: Int, endHour: Int) {
        darkTimeStart = startHour
        darkTimeEnd = endHour
        LibraryRepository.insertSetting(KeyDarkTimeStart, startHour.toString())
        LibraryRepository.insertSetting(KeyDarkTimeEnd, endHour.toString())
    }

    fun updateDarkTimeStart(hour: Int) {
        darkTimeStart = hour
        LibraryRepository.insertSetting(KeyDarkTimeStart, hour.toString())
    }

    fun updateDarkTimeEnd(hour: Int) {
        darkTimeEnd = hour
        LibraryRepository.insertSetting(KeyDarkTimeEnd, hour.toString())
    }

    fun updateFontFamily(font: String) {
        fontFamily = font
        LibraryRepository.insertSetting(KeyFontFamily, font)
    }

    fun updateFontSizeLevel(level: Int) {
        fontSizeLevel = level
        LibraryRepository.insertSetting(KeyFontSizeLevel, level.toString())
    }

    fun updateLineHeightLevel(level: Int) {
        lineHeightLevel = level
        LibraryRepository.insertSetting(KeyLineHeightLevel, level.toString())
    }

    fun updateLetterSpacingLevel(level: Int) {
        letterSpacingLevel = level
        LibraryRepository.insertSetting(KeyLetterSpacingLevel, level.toString())
    }

    fun updateParaSpacingLevel(level: Int) {
        paraSpacingLevel = level
        LibraryRepository.insertSetting(KeyParaSpacingLevel, level.toString())
    }

    fun updateMarginLevel(level: Int) {
        marginLevel = level
        LibraryRepository.insertSetting(KeyMarginLevel, level.toString())
    }

    fun updateFirstLineIndent(indent: Boolean) {
        firstLineIndent = indent
        LibraryRepository.insertSetting(KeyFirstLineIndent, indent.toString())
    }

    fun updatePublisherStyles(enabled: Boolean) {
        publisherStyles = enabled
        LibraryRepository.insertSetting(KeyPublisherStyles, enabled.toString())
    }

    fun updateTrueBlack(enabled: Boolean) {
        trueBlack = enabled
        LibraryRepository.insertSetting(KeyTrueBlack, enabled.toString())
    }

    // 核心判定逻辑：处理跨夜与不跨夜
    fun isCurrentlyInDarkTime(currentHour: Int): Boolean {
        return if (darkTimeStart < darkTimeEnd) {
            // 情况 A：不跨夜 (例如 18:00 - 22:00)
            currentHour in darkTimeStart until darkTimeEnd
        } else if (darkTimeStart > darkTimeEnd) {
            // 情况 B：跨夜 (例如 22:00 - 06:00)
            currentHour >= darkTimeStart || currentHour < darkTimeEnd
        } else {
            // 起始和结束时间相同，视为全天或从不
            currentHour == darkTimeStart
        }
    }
}

