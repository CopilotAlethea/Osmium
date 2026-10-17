package org.dazai.osmium.util.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class NotificationType {
    INFO, ERROR
}

class NotificationState(private val scope: CoroutineScope) {
    var isVisible by mutableStateOf(false)
    var message by mutableStateOf("")
    var type by mutableStateOf(NotificationType.INFO)
    private var dismissJob: Job? = null

    fun show(msg: String, notificationType: NotificationType = NotificationType.INFO) {
        dismissJob?.cancel()
        message = msg
        type = notificationType
        isVisible = true
        dismissJob = scope.launch {
            delay(3000)
            isVisible = false
        }
    }
}

@Composable
fun rememberNotificationState(scope: CoroutineScope = rememberCoroutineScope()) = 
    remember(scope) { NotificationState(scope) }

@Composable
fun CapsuleNotification(state: NotificationState) {
    Box(
        modifier = Modifier.fillMaxSize().padding(bottom = 64.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = state.isVisible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            val backgroundColor = when (state.type) {
                NotificationType.INFO -> MaterialTheme.colorScheme.primary
                NotificationType.ERROR -> MaterialTheme.colorScheme.error
            }
            val contentColor = when (state.type) {
                NotificationType.INFO -> MaterialTheme.colorScheme.onPrimary
                NotificationType.ERROR -> MaterialTheme.colorScheme.onError
            }
            val icon = when (state.type) {
                NotificationType.INFO -> Icons.Default.CheckCircle
                NotificationType.ERROR -> Icons.Default.Error
            }

            Surface(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .clip(CircleShape),
                color = backgroundColor,
                contentColor = contentColor,
                tonalElevation = 4.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

