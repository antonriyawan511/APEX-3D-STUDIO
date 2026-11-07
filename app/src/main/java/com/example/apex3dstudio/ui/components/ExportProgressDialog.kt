package com.example.apex3dstudio.ui.components

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.apex3dstudio.ui.ExportProgress
import com.example.apex3dstudio.ui.theme.AccentGold
import com.example.apex3dstudio.ui.theme.AccentGreen
import com.example.apex3dstudio.ui.theme.AccentRed
import com.example.apex3dstudio.ui.theme.StudioBorder
import com.example.apex3dstudio.ui.theme.StudioDarkSurface
import com.example.apex3dstudio.ui.theme.StudioElevated
import com.example.apex3dstudio.ui.theme.TextPrimary
import com.example.apex3dstudio.ui.theme.TextSecondary
import java.io.File

@Composable
fun ExportProgressDialog(
    isOpen: Boolean,
    progress: ExportProgress,
    onDismiss: () -> Unit,
    onCancel: () -> Unit
) {
    if (!isOpen) return
    val context = LocalContext.current
    val isFinished = progress.outputFilePath != null

    Dialog(onDismissRequest = {
        if (!progress.isExporting) onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioDarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(0.8.dp, StudioBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isFinished) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            tint = AccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            color = AccentGold,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isFinished) "Render Complete" else "Rendering Video",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isFinished) {
                    val file = File(progress.outputFilePath!!)
                    Surface(
                        color = StudioElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Output File:", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = file.name,
                                color = AccentGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val sizeMb = file.length().toFloat() / (1024f * 1024f)
                            Text(
                                text = "${String.format("%.2f", sizeMb)} MB (H.264 High Profile)",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Close", color = TextPrimary)
                        }

                        Button(
                            onClick = {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share MP4 Video"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Share", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // In-progress View
                    LinearProgressIndicator(
                        progress = { (progress.progressPercent / 100f).coerceIn(0f, 1f) },
                        color = AccentGold,
                        trackColor = StudioElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = progress.statusText, color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = "${progress.progressPercent.toInt()}%",
                            color = AccentGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Cancel", color = AccentRed)
                        }
                    }
                }
            }
        }
    }
}
