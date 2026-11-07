package com.example.apex3dstudio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StudioTopBar(
    onOpenClick: () -> Unit,
    onCameraClick: () -> Unit,
    hasCamera: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudioButton(
            icon = Icons.Default.FolderOpen,
            text = "Open",
            onClick = onOpenClick
        )

        StudioButton(
            icon = Icons.Default.Videocam,
            text = if (hasCamera) "Camera" else "Orbit",
            onClick = onCameraClick
        )
    }
}
