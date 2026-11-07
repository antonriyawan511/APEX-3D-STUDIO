package com.example.apex3dstudio.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.ViewerControls(
    isPlaying: Boolean,
    onOpenGlbClick: () -> Unit,
    onPlayPauseClick: () -> Unit
) {
    Button(
        onClick = onOpenGlbClick,
        modifier = Modifier
            .align(Alignment.TopStart)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text("Open GLB")
    }

    Button(
        onClick = onPlayPauseClick,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
    ) {
        Text(if (isPlaying) "Pause" else "Play")
    }
}
