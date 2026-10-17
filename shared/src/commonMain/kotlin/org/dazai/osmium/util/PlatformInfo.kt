package org.dazai.osmium.util

expect val supportsSystemDark: Boolean
expect val isAtLeastS: Boolean
expect val isAtLeastT: Boolean

/**
 * 是否在 Android 6 (API 23) 到 Android 9 (API 28) 之间。
 */
expect val isAndroidBetween6And9: Boolean

