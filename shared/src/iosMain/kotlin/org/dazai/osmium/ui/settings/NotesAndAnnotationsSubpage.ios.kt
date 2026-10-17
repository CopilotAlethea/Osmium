package org.dazai.osmium.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import osmium.shared.generated.resources.*

@Composable
actual fun NotesAndAnnotationsSubpage(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Notes and Annotations ${stringResource(Res.string.not_implemented)}")
    }
}

