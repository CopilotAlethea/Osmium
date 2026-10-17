package org.dazai.osmium.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object PlatformContext {
    lateinit var context: Context
}

actual fun changeLanguage(languageCode: String) {
    val appLocale: LocaleListCompat = if (languageCode == "system") {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(languageCode)
    }
    AppCompatDelegate.setApplicationLocales(appLocale)
}

