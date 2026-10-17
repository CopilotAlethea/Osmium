package org.dazai.osmium

import androidx.compose.ui.window.ComposeUIViewController
import org.dazai.osmium.db.DatabaseDriverFactory
import org.dazai.osmium.ui.App

fun MainViewController() = ComposeUIViewController { App(DatabaseDriverFactory()) }

