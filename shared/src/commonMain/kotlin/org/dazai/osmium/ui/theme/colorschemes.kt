package org.dazai.osmium.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object AppThemeSchemes {

    // ==========================================
    // 1. 抹茶主题 (Matcha - #4C662B)
    // ==========================================
    val MatchaLightColorScheme = lightColorScheme(
        primary = Color(0xFF4C662B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCDEDA3),
        onPrimaryContainer = Color(0xFF122000),
        secondary = Color(0xFF586249),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDCEDC8),
        onSecondaryContainer = Color(0xFF151E0B),
        tertiary = Color(0xFF386663),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFBCECE8),
        onTertiaryContainer = Color(0xFF00201E),
        background = Color(0xFFF2F5E9),
        onBackground = Color(0xFF1A1C18),
        surface = Color(0xFFF2F5E9),
        onSurface = Color(0xFF1A1C18),
        surfaceVariant = Color(0xFFE1E4D5),
        onSurfaceVariant = Color(0xFF44483E),
        outline = Color(0xFF75796C),
        outlineVariant = Color(0xFFC5C8BA)
    )

    val MatchaDarkColorScheme = darkColorScheme(
        primary = Color(0xFFB1D18A),
        onPrimary = Color(0xFF1F3701),
        primaryContainer = Color(0xFF354E16),
        onPrimaryContainer = Color(0xFFCDEDA3),
        secondary = Color(0xFFBFCBAD),
        onSecondary = Color(0xFF2A331E),
        secondaryContainer = Color(0xFF404A33),
        onSecondaryContainer = Color(0xFFDCEDC8),
        tertiary = Color(0xFFA0CFCC),
        onTertiary = Color(0xFF003735),
        tertiaryContainer = Color(0xFF1F4E4C),
        onTertiaryContainer = Color(0xFFBCECE8),
        background = Color(0xFF1A1C18),
        onBackground = Color(0xFFE3E3DC),
        surface = Color(0xFF1A1C18),
        onSurface = Color(0xFFE3E3DC),
        surfaceVariant = Color(0xFF44483E),
        onSurfaceVariant = Color(0xFFC5C8BA),
        outline = Color(0xFF8F9285),
        outlineVariant = Color(0xFF44483E)
    )

    // ==========================================
    // 2. 红豆主题 (Azuki - #90414F)
    // ==========================================
    val AzukiLightColorScheme = lightColorScheme(
        primary = Color(0xFF90414F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD9DD),
        onPrimaryContainer = Color(0xFF3B0011),
        secondary = Color(0xFF75565A),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFA9B4),
        onSecondaryContainer = Color(0xFF2C1518),
        tertiary = Color(0xFF775930),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDEB7),
        onTertiaryContainer = Color(0xFF2C1600),
        background = Color(0xFFF9F0F1),
        onBackground = Color(0xFF221A1A),
        surface = Color(0xFFF9F0F1),
        onSurface = Color(0xFF221A1A),
        surfaceVariant = Color(0xFFF3DDDF),
        onSurfaceVariant = Color(0xFF524345),
        outline = Color(0xFF847375),
        outlineVariant = Color(0xFFD7C2C4)
    )

    val AzukiDarkColorScheme = darkColorScheme(
        primary = Color(0xFFFFB2BB),
        onPrimary = Color(0xFF571424),
        primaryContainer = Color(0xFF732A39),
        onPrimaryContainer = Color(0xFFFFD9DD),
        secondary = Color(0xFFE5BDC0),
        onSecondary = Color(0xFF43292C),
        secondaryContainer = Color(0xFF5C3F42),
        onSecondaryContainer = Color(0xFFFFA9B4),
        tertiary = Color(0xFFE8C08E),
        onTertiary = Color(0xFF432C06),
        tertiaryContainer = Color(0xFF5D421B),
        onTertiaryContainer = Color(0xFFFFDEB7),
        background = Color(0xFF221A1A),
        onBackground = Color(0xFFECE0E0),
        surface = Color(0xFF221A1A),
        onSurface = Color(0xFFECE0E0),
        surfaceVariant = Color(0xFF524345),
        onSurfaceVariant = Color(0xFFD7C2C4),
        outline = Color(0xFF9F8C8E),
        outlineVariant = Color(0xFF524345)
    )

    // ==========================================
    // 3. 可可主题 (Cocoa / Chocolate Tokens)
    // ==========================================
    val CocoaLightColors = lightColorScheme(
        primary = Color(0xFF7D5341),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDBCE),
        onPrimaryContainer = Color(0xFF321408),
        secondary = Color(0xFF6D5C55),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF6E0D7),
        onSecondaryContainer = Color(0xFF261A14),
        tertiary = Color(0xFF626031),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE9E5AA),
        onTertiaryContainer = Color(0xFF1D1D00),
        background = Color(0xFFF5F0EE), // 带有淡淡奶香的极浅米褐
        surface = Color(0xFFF5F0EE),
        onBackground = Color(0xFF211A17),
        onSurface = Color(0xFF211A17),
        surfaceVariant = Color(0xFFF2DFD7),
        onSurfaceVariant = Color(0xFF51443F),
        outline = Color(0xFF83746E)
    )

    val CocoaDarkColors = darkColorScheme(
        primary = Color(0xFFEFB9A2),
        onPrimary = Color(0xFF492617),
        primaryContainer = Color(0xFF633C2C),
        onPrimaryContainer = Color(0xFFFFDBCE),
        secondary = Color(0xFFD9C4BB),
        onSecondary = Color(0xFF3C2E28),
        secondaryContainer = Color(0xFF54443E),
        onSecondaryContainer = Color(0xFFF6E0D7),
        tertiary = Color(0xFFCCC990),
        onTertiary = Color(0xFF333207),
        tertiaryContainer = Color(0xFF4A491C),
        onTertiaryContainer = Color(0xFFE9E5AA),
        background = Color(0xFF1A120E), // 纯正黑巧克力深色背景
        surface = Color(0xFF1A120E),
        onBackground = Color(0xFFEDE0DB),
        onSurface = Color(0xFFEDE0DB),
        surfaceVariant = Color(0xFF51443F),
        onSurfaceVariant = Color(0xFFD5C3BB),
        outline = Color(0xFF9D8E87)
    )

    // ==========================================
    // 4. 金属主题 (Metal - #7A5900)
    // ==========================================
    val MetalLightColorScheme = lightColorScheme(
        primary = Color(0xFF7A5900),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDEA4),
        onPrimaryContainer = Color(0xFF261900),
        secondary = Color(0xFF6B5D46),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF4E0BB),
        onSecondaryContainer = Color(0xFF241A08),
        tertiary = Color(0xFF4F6547),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFD1EABD),
        onTertiaryContainer = Color(0xFF0E2009),
        background = Color(0xFFF6F1E5),
        onBackground = Color(0xFF1E1B16),
        surface = Color(0xFFF6F1E5),
        onSurface = Color(0xFF1E1B16),
        surfaceVariant = Color(0xFFECE1CF),
        onSurfaceVariant = Color(0xFF4D4639),
        outline = Color(0xFF7E7667),
        outlineVariant = Color(0xFFD0C5B4)
    )

    val MetalColorScheme = darkColorScheme(
        primary = Color(0xFFEFC148),
        onPrimary = Color(0xFF402D00),
        primaryContainer = Color(0xFF5C4200),
        onPrimaryContainer = Color(0xFFFFDEA4),
        secondary = Color(0xFFD7C4A1),
        onSecondary = Color(0xFF3B2F1B),
        secondaryContainer = Color(0xFF52452E),
        onSecondaryContainer = Color(0xFFF4E0BB),
        tertiary = Color(0xFFB5CDA3),
        onTertiary = Color(0xFF22361C),
        tertiaryContainer = Color(0xFF384D31),
        onTertiaryContainer = Color(0xFFD1EABD),
        background = Color(0xFF1E1B16),
        onBackground = Color(0xFFEAE1D9),
        surface = Color(0xFF1E1B16),
        onSurface = Color(0xFFEAE1D9),
        surfaceVariant = Color(0xFF4D4639),
        onSurfaceVariant = Color(0xFFD0C5B4),
        outline = Color(0xFF999080),
        outlineVariant = Color(0xFF4D4639)
    )
}

