package org.dazai.osmium.util

import platform.Foundation.NSUserDefaults

actual fun changeLanguage(languageCode: String) {
    if (languageCode == "system") {
        NSUserDefaults.standardUserDefaults.removeObjectForKey("AppleLanguages")
    } else {
        NSUserDefaults.standardUserDefaults.setObject(listOf(languageCode), "AppleLanguages")
    }
    NSUserDefaults.standardUserDefaults.synchronize()
}

