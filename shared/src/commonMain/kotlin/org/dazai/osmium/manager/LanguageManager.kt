package org.dazai.osmium.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.dazai.osmium.domain.repository.LibraryRepository
import org.dazai.osmium.util.changeLanguage

object LanguageManager {
    private const val KeyLanguage = "appLanguage"

    var currentLanguage by mutableStateOf("system")
        private set

    val supportedLanguages = listOf(
        LanguageInfo("system", "Follow System", "跟随系统"),
        LanguageInfo("en", "English", "English"),
        LanguageInfo("zh", "Chinese", "中文 (简体)"),
        LanguageInfo("ja", "Japanese", "日本語"),
        LanguageInfo("de", "German", "Deutsch"),
        LanguageInfo("fr", "French", "Français"),
        LanguageInfo("es", "Spanish", "Español")
    )

    fun initialize(settings: Map<String, String>) {
        val savedLanguage = settings[KeyLanguage]
        if (savedLanguage != null) {
            currentLanguage = savedLanguage
        }
    }

    fun getLanguageName(code: String): String {
        return supportedLanguages.find { it.code == code }?.nativeName ?: "English"
    }

    fun updateLanguage(code: String) {
        currentLanguage = code
        LibraryRepository.insertSetting(KeyLanguage, code)
        changeLanguage(code)
    }

    data class LanguageInfo(
        val code: String,
        val name: String,
        val nativeName: String
    )
}

