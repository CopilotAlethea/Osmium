package org.dazai.osmium

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dazai.osmium.db.DatabaseDriverFactory
import org.dazai.osmium.ui.App
import java.io.File

fun main() = application {
    var initialized by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(-1f) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            KCEF.init(
                builder = {
                    installDir(File("kcef-bundle"))
                    args("--no-sandbox", "--disable-dev-shm-usage")
                    progress {
                        onDownloading {
                            downloadProgress = it
                        }
                        onInitialized {
                            initialized = true
                        }
                    }
                },
                onError = { it?.printStackTrace() },
                onRestartRequired = { exitApplication() }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            KCEF.disposeBlocking()
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Osmium",
    ) {
        if (initialized) {
            App(DatabaseDriverFactory())
        } else {
            Surface {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        val text = if (downloadProgress >= 0) {
                            "Downloading Chromium... ${(downloadProgress * 100).toInt()}%"
                        } else {
                            "Initializing WebView engine..."
                        }
                        Text(text)
                    }
                }
            }
        }
    }
}

