package org.dazai.osmium.ui.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dazai.osmium.domain.model.Book

@Composable
expect fun BookReader(
    bookId: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
)

