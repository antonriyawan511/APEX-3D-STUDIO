package com.example.apex3dstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apex3dstudio.ui.CameraMode
import com.example.apex3dstudio.ui.theme.AccentCyan
import com.example.apex3dstudio.ui.theme.AccentGold
import com.example.apex3dstudio.ui.theme.AccentRed
import com.example.apex3dstudio.ui.theme.StudioBorder
import com.example.apex3dstudio.ui.theme.StudioDarkSurface
import com.example.apex3dstudio.ui.theme.StudioElevated
import com.example.apex3dstudio.ui.theme.TextPrimary
import com.example.apex3dstudio.ui.theme.TextSecondary

@Composable
fun StudioTopBar(
    modelName: String,
    cameraMode: CameraMode,
    onToggleCameraMode: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioDarkSurface.copy(alpha = 0.90f),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(0.8.dp, StudioBorder, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Open File + Model Name Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioElevated)
                    .clickable { onOpenFilePicker() }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileOpen,
                    contentDescription = "Open 3D File",
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = modelName,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(110.dp)
                )
            }

            // Center: 1-Tap Camera Mode Toggle Pill
            val isTrack = cameraMode == CameraMode.TRACK_RIG
            Surface(
                color = if (isTrack) AccentGold.copy(alpha = 0.2f) else StudioElevated,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .border(
                        0.8.dp,
                        if (isTrack) AccentGold else StudioBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onToggleCameraMode() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isTrack) Icons.Default.Videocam else Icons.Default.PanTool,
                        contentDescription = "Camera Mode",
                        tint = if (isTrack) AccentGold else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isTrack) "Track Rig" else "Free Orbit",
                        color = if (isTrack) AccentGold else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right: Settings & Render MP4
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Settings",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Button(
                    onClick = onOpenExport,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = "Render MP4",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RENDER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
