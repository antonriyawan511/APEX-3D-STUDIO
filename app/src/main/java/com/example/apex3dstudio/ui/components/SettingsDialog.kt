package com.example.apex3dstudio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.apex3dstudio.state.GestureSensitivity
import kotlin.math.abs

@Composable
fun SettingsDialog(
    sensitivity: GestureSensitivity,
    onOrbitChanged: (Float) -> Unit,
    onPanChanged: (Float) -> Unit,
    onZoomChanged: (Float) -> Unit,
    onTwistOrbitChanged: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Sensitivitas Sentuhan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "Nilai 1.0x adalah rasio 1:1 real-world dengan gerakan jari.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // 1. Orbit (1 Jari)
                val isOrbitOne = abs(sensitivity.orbit - 1.0f) < 0.05f
                SensitivitySlider(
                    label = "Orbit Kamera (1 Jari)",
                    value = sensitivity.orbit,
                    onValueChange = onOrbitChanged,
                    badgeText = if (isOrbitOne) "1:1 " else null
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Pan (2 Jari)
                val isPanOne = abs(sensitivity.pan - 1.0f) < 0.05f
                SensitivitySlider(
                    label = "Pan / Geser (2 Jari)",
                    value = sensitivity.pan,
                    onValueChange = onPanChanged,
                    badgeText = if (isPanOne) "1:1 " else null
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Zoom (Cubit / Pinch)
                val isZoomOne = abs(sensitivity.zoom - 1.0f) < 0.05f
                SensitivitySlider(
                    label = "Zoom / Cubit (Pinch)",
                    value = sensitivity.zoom,
                    onValueChange = onZoomChanged,
                    badgeText = if (isZoomOne) "1:1 " else null
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Putar 2 Jari (Turntable Orbit)
                val isTwistOne = abs(sensitivity.twistOrbit - 1.0f) < 0.05f
                SensitivitySlider(
                    label = "Putar 2 Jari (Turntable Orbit)",
                    value = sensitivity.twistOrbit,
                    onValueChange = onTwistOrbitChanged,
                    badgeText = if (isTwistOne) "1:1 " else null
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset 1:1", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Tutup", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SensitivitySlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    badgeText: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Color.White
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (badgeText != null) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = String.format("%.2fx", value),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.2f..3.0f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
