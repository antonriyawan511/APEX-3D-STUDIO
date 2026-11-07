package com.example.apex3dstudio.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apex3dstudio.ui.TimelineState
import com.example.apex3dstudio.ui.theme.AccentGold
import com.example.apex3dstudio.ui.theme.StudioBorder
import com.example.apex3dstudio.ui.theme.StudioDarkSurface
import com.example.apex3dstudio.ui.theme.StudioElevated
import com.example.apex3dstudio.ui.theme.TextPrimary
import com.example.apex3dstudio.ui.theme.TextSecondary

@Composable
fun StudioTransportBar(
    timeline: TimelineState,
    onTogglePlay: () -> Unit,
    onScrub: (Float) -> Unit,
    onStepShot: (Boolean) -> Unit,
    onCycleSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSec = timeline.currentTimeSec
    val totalSec = timeline.durationSec
    val fps = timeline.fps
    val currentFrame = (currentSec * fps).toInt()

    val currentMin = (currentSec / 60).toInt()
    val currentRemSec = (currentSec % 60).toInt()
    val currentFrac = ((currentSec - currentMin * 60 - currentRemSec) * 100).toInt()

    val timecode = String.format("%02d:%02d.%02d", currentMin, currentRemSec, currentFrac)
    val durationCode = String.format("%02d:%02d", (totalSec / 60).toInt(), (totalSec % 60).toInt())

    Surface(
        color = StudioDarkSurface.copy(alpha = 0.90f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(0.8.dp, StudioBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            // Scrubber Slider
            Slider(
                value = timeline.currentTimeSec.coerceIn(0f, timeline.durationSec),
                onValueChange = onScrub,
                valueRange = 0f..timeline.durationSec.coerceAtLeast(0.1f),
                colors = SliderDefaults.colors(
                    thumbColor = AccentGold,
                    activeTrackColor = AccentGold,
                    inactiveTrackColor = StudioElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )

            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Timecode + Shot Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timecode,
                        color = AccentGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = " / $durationCode",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = StudioElevated,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Shot ${timeline.currentShotIndex}/${timeline.totalShots}",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Center: Shot Steps & Play/Pause Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Prev Shot
                    IconButton(
                        onClick = { onStepShot(false) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Shot",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play / Pause Circle
                    Surface(
                        color = AccentGold,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { onTogglePlay() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (timeline.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (timeline.isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Next Shot
                    IconButton(
                        onClick = { onStepShot(true) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Shot",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Right: Speed Pill
                Surface(
                    color = StudioElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(0.6.dp, StudioBorder, RoundedCornerShape(12.dp))
                        .clickable { onCycleSpeed() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${timeline.playbackSpeed}x",
                        color = AccentGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
