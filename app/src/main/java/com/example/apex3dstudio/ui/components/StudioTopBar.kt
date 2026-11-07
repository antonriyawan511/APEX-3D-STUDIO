package com.example.apex3dstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apex3dstudio.state.BottomBarMode
import com.example.apex3dstudio.state.EnvironmentPreset
import com.example.apex3dstudio.ui.theme.AccentCyan
import com.example.apex3dstudio.ui.theme.StudioBorder
import com.example.apex3dstudio.ui.theme.StudioDarkSurface
import com.example.apex3dstudio.ui.theme.TextPrimary

@Composable
fun StudioTopBar(
    onOpenClick: () -> Unit,
    onCameraClick: () -> Unit,
    onSettingsClick: () -> Unit,
    currentPreset: EnvironmentPreset,
    onPresetSelected: (EnvironmentPreset) -> Unit,
    bottomBarMode: BottomBarMode,
    onToggleBottomBarMode: () -> Unit,
    hasCamera: Boolean,
    modifier: Modifier = Modifier
) {
    var envMenuExpanded by remember { mutableStateOf(false) }

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

            Box {
                StudioBarButton(
                    label = "Env",
                    icon = Icons.Default.WbSunny,
                    isActive = currentPreset != EnvironmentPreset.STUDIO_NEUTRAL,
                    onClick = { envMenuExpanded = true }
                )

                DropdownMenu(
                    expanded = envMenuExpanded,
                    onDismissRequest = { envMenuExpanded = false },
                    modifier = Modifier
                        .background(StudioDarkSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                ) {
                    EnvironmentPreset.entries.forEach { preset ->
                        val isSelected = preset == currentPreset
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = preset.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AccentCyan else TextPrimary
                                )
                            },
                            onClick = {
                                onPresetSelected(preset)
                                envMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Toggle Tools Bar vs Timeline Bar
            StudioBarButton(
                label = if (bottomBarMode == BottomBarMode.TIMELINE) "Timeline" else "Timeline",
                icon = Icons.Default.ViewTimeline,
                isActive = bottomBarMode == BottomBarMode.TIMELINE,
                onClick = onToggleBottomBarMode
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
