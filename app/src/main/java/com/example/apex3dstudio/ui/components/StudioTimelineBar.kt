package com.example.apex3dstudio.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apex3dstudio.ui.theme.AccentCyan
import com.example.apex3dstudio.ui.theme.StudioBorder
import com.example.apex3dstudio.ui.theme.StudioDarkSurface
import com.example.apex3dstudio.ui.theme.TextMuted
import com.example.apex3dstudio.ui.theme.TextPrimary
import com.example.apex3dstudio.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.ceil

@Composable
fun StudioTimelineBar(
    isPlaying: Boolean,
    currentTime: Float,
    duration: Float,
    onPlayPause: () -> Unit,
    onReset: () -> Unit,
    onForward1s: () -> Unit,
    onBackward1s: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val basePixelsPerSec = with(density) { 60.dp.toPx() }

    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    val pixelsPerSec = basePixelsPerSec * zoomScale

    var scrollOffset by remember { mutableFloatStateOf(0f) }
    var viewportWidthPx by remember { mutableFloatStateOf(1f) }
    var isUserScrubbing by remember { mutableStateOf(false) }

    val safeDuration = duration.coerceAtLeast(0.1f)
    val totalWidthPx = safeDuration * pixelsPerSec
    val maxScroll = (totalWidthPx - viewportWidthPx).coerceAtLeast(0f)

    // Keep playhead in view during normal playback (auto-scroll)
    LaunchedEffect(currentTime, isPlaying, isUserScrubbing) {
        if (!isUserScrubbing && duration > 0f) {
            val playheadX = currentTime * pixelsPerSec
            if (playheadX < scrollOffset || playheadX > scrollOffset + viewportWidthPx) {
                scrollOffset = (playheadX - viewportWidthPx / 2f).coerceIn(0f, maxScroll)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Informasi Current Time / Total Duration di LUAR bar timeline (Center)
        Box(
            modifier = Modifier
                .background(
                    color = StudioDarkSurface.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(14.dp)
                )
                .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatTime(currentTime),
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "/",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = formatTime(duration),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 2. Bar Timeline Utama
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = StudioDarkSurface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row Atas: Tombol (-) Kiri, Playback Controls Tengah, Tombol (+) Kanan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Zoom Out (-)
                IconButton(
                    onClick = {
                        val newZoom = (zoomScale / 1.25f).coerceAtLeast(0.4f)
                        val newPixels = basePixelsPerSec * newZoom
                        val newMax = (safeDuration * newPixels - viewportWidthPx).coerceAtLeast(0f)
                        scrollOffset = (currentTime * newPixels - viewportWidthPx / 2f).coerceIn(0f, newMax)
                        zoomScale = newZoom
                    },
                    enabled = duration > 0f && zoomScale > 0.4f,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out (-)",
                        tint = if (duration > 0f && zoomScale > 0.4f) TextPrimary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Transport Playback Controls (Reset, -1s, Play/Pause, +1s)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onReset,
                        enabled = duration > 0f,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Reset",
                            tint = if (duration > 0f) TextPrimary else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onBackward1s,
                        enabled = duration > 0f,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Backward 1s",
                            tint = if (duration > 0f) TextPrimary else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = onPlayPause,
                        enabled = duration > 0f,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isPlaying) AccentCyan else AccentCyan.copy(alpha = 0.2f),
                            contentColor = if (isPlaying) Color.Black else AccentCyan
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onForward1s,
                        enabled = duration > 0f,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Forward 1s",
                            tint = if (duration > 0f) TextPrimary else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Tombol Zoom In (+)
                IconButton(
                    onClick = {
                        val newZoom = (zoomScale * 1.25f).coerceAtMost(4.0f)
                        val newPixels = basePixelsPerSec * newZoom
                        val newMax = (safeDuration * newPixels - viewportWidthPx).coerceAtLeast(0f)
                        scrollOffset = (currentTime * newPixels - viewportWidthPx / 2f).coerceIn(0f, newMax)
                        zoomScale = newZoom
                    },
                    enabled = duration > 0f && zoomScale < 4.0f,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In (+)",
                        tint = if (duration > 0f && zoomScale < 4.0f) TextPrimary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Row Bawah: Timeline Canvas Ruler & Playhead (Hanya Scrubbing, TANPA Pinch Zoom)
            val tickPaint = remember {
                Paint().apply {
                    color = 0xFF5A6275.toInt()
                    textSize = 22f
                    isAntiAlias = true
                    typeface = Typeface.MONOSPACE
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0C0E14))
                    .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                    .onSizeChanged { size ->
                        viewportWidthPx = size.width.toFloat().coerceAtLeast(1f)
                    }
                    .pointerInput(safeDuration, pixelsPerSec, scrollOffset) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isUserScrubbing = true

                            // Initial touch seek
                            val touchX = down.position.x
                            val targetTime = ((touchX + scrollOffset) / pixelsPerSec).coerceIn(0f, safeDuration)
                            onSeek(targetTime)

                            while (true) {
                                val event = awaitPointerEvent()
                                val active = event.changes.filter { it.pressed }
                                if (active.isEmpty()) break

                                // Hanya pointer pertama yang menggerakkan scrubbing. Tidak ada logika pinch zoom.
                                val pointer = active[0]
                                val x = pointer.position.x
                                val scrubTime = ((x + scrollOffset) / pixelsPerSec).coerceIn(0f, safeDuration)
                                onSeek(scrubTime)

                                // Edge auto-scrolling
                                if (x < 40f && scrollOffset > 0f) {
                                    scrollOffset = (scrollOffset - 6f).coerceAtLeast(0f)
                                } else if (x > viewportWidthPx - 40f && scrollOffset < maxScroll) {
                                    scrollOffset = (scrollOffset + 6f).coerceAtMost(maxScroll)
                                }

                                // Consume semua touch agar tidak tembus ke kamera 3D
                                event.changes.forEach { it.consume() }
                            }

                            isUserScrubbing = false
                        }
                    }
            ) {
                if (duration <= 0f) {
                    Text(
                        text = "Tidak ada track animasi pada model ini",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val totalSeconds = ceil(safeDuration).toInt()
                        val minorStep = if (pixelsPerSec > 100f) 0.2f else 0.5f

                        // Draw minor ticks
                        var t = 0f
                        while (t <= safeDuration) {
                            val x = t * pixelsPerSec - scrollOffset
                            if (x in -10f..size.width + 10f) {
                                drawLine(
                                    color = Color(0xFF1E2330),
                                    start = Offset(x, 0f),
                                    end = Offset(x, 10f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            t += minorStep
                        }

                        // Draw major second ticks and labels
                        for (s in 0..totalSeconds) {
                            val x = s * pixelsPerSec - scrollOffset
                            if (x in -20f..size.width + 20f) {
                                drawLine(
                                    color = Color(0xFF384055),
                                    start = Offset(x, 0f),
                                    end = Offset(x, 22f),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                                drawIntoCanvas { canvas ->
                                    canvas.nativeCanvas.drawText("${s}s", x + 5f, 34f, tickPaint)
                                }
                            }
                        }

                        // Draw Playhead
                        val playheadX = currentTime * pixelsPerSec - scrollOffset
                        if (playheadX in -10f..size.width + 10f) {
                            // Vertical playhead line
                            drawLine(
                                color = AccentCyan,
                                start = Offset(playheadX, 0f),
                                end = Offset(playheadX, size.height),
                                strokeWidth = 2.dp.toPx()
                            )

                            // Playhead top cap indicator
                            drawRect(
                                color = AccentCyan,
                                topLeft = Offset(playheadX - 4.dp.toPx(), 0f),
                                size = Size(8.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Float): String {
    val totalSeconds = seconds.coerceAtLeast(0f)
    val m = (totalSeconds / 60).toInt()
    val s = (totalSeconds % 60).toInt()
    val tenths = ((totalSeconds % 1f) * 10).toInt()
    return String.format(Locale.US, "%02d:%02d.%d", m, s, tenths)
}
