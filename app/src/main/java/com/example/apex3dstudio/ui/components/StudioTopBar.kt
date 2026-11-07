package com.example.apex3dstudio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StudioTopBar(
    onOpenClick: () -> Unit,
    onCameraClick: () -> Unit,
    onSettingsClick: () -> Unit,
    hasCamera: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StudioBarButton(
                label = "Open",
                icon = Icons.Default.FolderOpen,
                onClick = onOpenClick
            )

            StudioBarButton(
                label = "Settings",
                icon = Icons.Default.Settings,
                onClick = onSettingsClick
            )
        }

        StudioBarButton(
            label = if (hasCamera) "Camera" else "Orbit",
            icon = Icons.Default.Videocam,
            isActive = hasCamera,
            onClick = onCameraClick
        )
    }
}
