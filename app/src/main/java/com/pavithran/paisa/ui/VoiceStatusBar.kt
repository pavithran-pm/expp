package com.pavithran.paisa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pavithran.paisa.voice.VoiceState

@Composable
fun VoiceStatusBar(state: VoiceState) {
    val label = when (state) {
        VoiceState.Idle -> null
        VoiceState.Listening -> "Listening…"
        is VoiceState.Hearing -> "\"${state.partial}\""
        VoiceState.Processing -> "Saving…"
        is VoiceState.Failed -> state.message
    } ?: return

    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
