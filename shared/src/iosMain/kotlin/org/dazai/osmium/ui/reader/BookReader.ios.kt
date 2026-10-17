package org.dazai.osmium.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import org.jetbrains.compose.resources.stringResource
import osmium.shared.generated.resources.*

@Composable
actual fun BookReader(
    bookId: String,
    onClose: () -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("iOS Reader (Readium Swift) ${stringResource(Res.string.not_implemented)}")
    }
}

