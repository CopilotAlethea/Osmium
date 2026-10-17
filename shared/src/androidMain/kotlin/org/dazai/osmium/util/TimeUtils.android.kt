package org.dazai.osmium.util

import java.util.Calendar

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun getHourOfDay(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

actual fun generateUUID(): String = java.util.UUID.randomUUID().toString()

