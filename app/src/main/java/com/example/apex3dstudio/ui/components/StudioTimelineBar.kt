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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Shadow
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
    onSkipToEnd: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val basePixelsPerSec = with(density) { 60.dp.toPx() }

    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    val pixelsPerSec = basePixelsPerSec * zoomScale

    var viewportWidthPx by remember { mutableFloatStateOf(1f) }
    val safeDuration = duration.coerceAtLeast(0.1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Informasi Current Time / Total Duration: Transparan, tanpa container/background, center
        Row(
            modifier = Modifier.padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val textShadow = Shadow(color = Color.Black.copy(alpha = 0.85f), blurRadius = 6f)
            Text(
                text = formatTime(currentTime),
                color = AccentCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelMedium.copy(shadow = textShadow)
            )
            Text(
                text = "/",
                color = TextMuted,
                fontSize = 11.sp,
                style = MaterialTheme.typography.labelMedium.copy(shadow = textShadow)
            )
            Text(
                text = formatTime(duration),
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelMedium.copy(shadow = textShadow)
            )
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
            // Row 1: Tombol (-) Kiri, 5 Tombol Playback Simetris (Play sebagai Center Anchor), Tombol (+) Kanan
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Tombol Zoom Out (-)
                IconButton(
                    onClick = { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.4f) },
                    enabled = duration > 0f && zoomScale > 0.4f,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out (-)",
                        tint = if (duration > 0f && zoomScale > 0.4f) TextPrimary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 5 Tombol Playback Simetris: 2 tombol di kiri Play, Play tepat di tengah, 2 tombol di kanan Play
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tombol 1: Reset (00:00)
                    IconButton(
                        onClick = onReset,
                        enabled = duration > 0f,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Reset",
                            tint = if (duration > 0f) TextPrimary else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Tombol 2: Backward 1s (-1s)
                    IconButton(
                        onClick = onBackward1s,
                        enabled = duration > 0f,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Backward 1s",
                            tint = if (duration > 0f) TextPrimary else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Tombol 3: Play / Stop (Titik Pusat / Center Anchor)
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

                    // Tombol 4: Forward 1s (+1s)
                    IconButton(
                        onClick = onForward1s,
                        enabled = duration > 0f,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Forward 1s",
                            tint = if (duration > 0f) TextPrimary else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Tombol 5: Ke Akhir (Skip to End)
                    IconButton(
                        onClick = onSkipToEnd,
                        enabled = duration > 0f,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "To End",
                            tint = if (duration > 0f) TextPrimary else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Tombol Zoom In (+)
                IconButton(
                    onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(4.0f) },
                    enabled = duration > 0f && zoomScale < 4.0f,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In (+)",
                        tint = if (duration > 0f && zoomScale < 4.0f) TextPrimary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Row 2: Timeline Canvas Ruler (Center Playhead & Moving Track Scrubbing)
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
                    .pointerInput(safeDuration, pixelsPerSec) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startX = down.position.x
                            val startTime = currentTime

                            while (true) {
                                val event = awaitPointerEvent()
                                val active = event.changes.filter { it.pressed }
                                if (active.isEmpty()) break

                                val currentX = active[0].position.x
                                val dx = currentX - startX
                                // Geser track ke kiri (dx < 0) = maju waktu; geser ke kanan (dx > 0) = mundur waktu
                                val scrubTime = (startTime - dx / pixelsPerSec).coerceIn(0f, safeDuration)
                                onSeek(scrubTime)

                                event.changes.forEach { it.consume() }
                            }
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
                        val centerX = size.width / 2f
                        val totalSeconds = ceil(safeDuration).toInt()
                        val minorStep = if (pixelsPerSec > 100f) 0.2f else 0.5f

                        // 1. Shaded Track Background (Range 0s..duration)
                        val xTrackStart = centerX - currentTime * pixelsPerSec
                        val xTrackEnd = centerX + (safeDuration - currentTime) * pixelsPerSec
                        val clipLeft = xTrackStart.coerceAtLeast(0f)
                        val clipRight = xTrackEnd.coerceAtMost(size.width)
                        if (clipRight > clipLeft) {
                            drawRect(
                                color = Color(0xFF131722),
                                topLeft = Offset(clipLeft, 0f),
                                size = Size(clipRight - clipLeft, size.height)
                            )
                        }

                        // 2. Minor ticks
                        var t = 0f
                        while (t <= safeDuration) {
                            val x = centerX + (t - currentTime) * pixelsPerSec
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

                        // 3. Major second ticks and labels
                        for (s in 0..totalSeconds) {
                            val x = centerX + (s - currentTime) * pixelsPerSec
                            if (x in -30f..size.width + 30f) {
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

                        // 4. Center Playhead (SELALU DIAM DI TENGAH)
                        drawLine(
                            color = AccentCyan,
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, size.height),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Playhead top cap indicator
                        drawRect(
                            color = AccentCyan,
                            topLeft = Offset(centerX - 4.dp.toPx(), 0f),
                            size = Size(8.dp.toPx(), 7.dp.toPx())
                        )
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
