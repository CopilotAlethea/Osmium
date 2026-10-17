package org.dazai.osmium.util

import android.os.Build

actual val supportsSystemDark: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
actual val isAtLeastS: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
actual val isAtLeastT: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

actual val isAndroidBetween6And9: Boolean = 
    Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.P

