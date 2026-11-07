package com.example.apex3dstudio.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apex3dstudio.ui.ExportAspectRatio
import com.example.apex3dstudio.ui.ExportResolution
import com.example.apex3dstudio.ui.ExportSettings
import com.example.apex3dstudio.ui.LightingState
import com.example.apex3dstudio.ui.OpticsState
import com.example.apex3dstudio.ui.theme.AccentGold
import com.example.apex3dstudio.ui.theme.StudioBorder
import com.example.apex3dstudio.ui.theme.StudioDarkSurface
import com.example.apex3dstudio.ui.theme.StudioElevated
import com.example.apex3dstudio.ui.theme.TextPrimary
import com.example.apex3dstudio.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioSettingsSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    optics: OpticsState,
    onApertureChange: (Float) -> Unit,
    onExposureChange: (Float) -> Unit,
    lighting: LightingState,
    onLightingPresetChange: (String) -> Unit,
    onSunIntensityChange: (Float) -> Unit,
    exportSettings: ExportSettings,
    onExportSettingsChange: (ExportSettings) -> Unit
) {
    if (!isOpen) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StudioDarkSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Studio Settings",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Camera & Lens Optics
            Text(
                text = "CAMERA & OPTICS",
                color = AccentGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            SliderRow(
                title = "Aperture (Bokeh Blur)",
                displayValue = "f/${String.format("%.1f", optics.aperture)}",
                value = optics.aperture,
                range = 1.2f..22f,
                onValueChange = onApertureChange
            )

            Spacer(modifier = Modifier.height(10.dp))

            SliderRow(
                title = "Exposure Compensation",
                displayValue = "${if (optics.exposureEv >= 0) "+" else ""}${String.format("%.1f", optics.exposureEv)} EV",
                value = optics.exposureEv,
                range = -3.0f..3.0f,
                onValueChange = onExposureChange
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Lighting Presets
            Text(
                text = "LIGHTING PRESETS",
                color = AccentGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Studio Clean", "Golden Hour", "High Noon", "Cyberpunk Night").forEach { preset ->
                    val isSel = lighting.presetName == preset
                    Surface(
                        color = if (isSel) AccentGold else StudioElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onLightingPresetChange(preset) }
                            .border(0.6.dp, if (isSel) AccentGold else StudioBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = preset.substringBefore(" "),
                            color = if (isSel) Color.Black else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SliderRow(
                title = "Sun Intensity",
                displayValue = "${(lighting.sunIntensityLux / 1000f).toInt()} kLux",
                value = lighting.sunIntensityLux,
                range = 2_000f..40_000f,
                onValueChange = onSunIntensityChange
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Export Configuration
            Text(
                text = "EXPORT CONFIGURATION",
                color = AccentGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Aspect Ratio", color = TextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExportAspectRatio.values().forEach { ratio ->
                    val isSel = exportSettings.aspectRatio == ratio
                    Surface(
                        color = if (isSel) AccentGold else StudioElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onExportSettingsChange(exportSettings.copy(aspectRatio = ratio)) }
                            .border(0.6.dp, if (isSel) AccentGold else StudioBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = ratio.label.substringBefore(" "),
                            color = if (isSel) Color.Black else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "Resolution", color = TextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExportResolution.values().forEach { res ->
                    val isSel = exportSettings.resolution == res
                    Surface(
                        color = if (isSel) AccentGold else StudioElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onExportSettingsChange(exportSettings.copy(resolution = res)) }
                            .border(0.6.dp, if (isSel) AccentGold else StudioBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = res.label.substringBefore(" "),
                            color = if (isSel) Color.Black else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    displayValue: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, color = TextPrimary, fontSize = 12.sp)
            Text(
                text = displayValue,
                color = AccentGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = AccentGold,
                activeTrackColor = AccentGold,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier.height(26.dp)
        )
    }
}
