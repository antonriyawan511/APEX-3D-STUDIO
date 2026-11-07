package com.example.apex3dstudio

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.apex3dstudio.ui.StudioViewModel
import com.example.apex3dstudio.ui.components.ExportProgressDialog
import com.example.apex3dstudio.ui.components.StudioSettingsSheet
import com.example.apex3dstudio.ui.components.StudioTopBar
import com.example.apex3dstudio.ui.components.StudioTransportBar
import com.example.apex3dstudio.ui.components.StudioViewport
import com.example.apex3dstudio.ui.theme.APEX3DSTUDIOTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep screen on during 3D playback and video rendering
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        setContent {
            APEX3DSTUDIOTheme {
                StudioScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun StudioScreen(viewModel: StudioViewModel) {
    val context = LocalContext.current

    val cameraMode by viewModel.cameraMode.collectAsState()
    val timeline by viewModel.timeline.collectAsState()
    val lighting by viewModel.lighting.collectAsState()
    val optics by viewModel.optics.collectAsState()
    val exportSettings by viewModel.exportSettings.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val modelName by viewModel.modelName.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isExportDialogOpen by viewModel.isExportDialogOpen.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadGlbFromUri(uri, context)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Fullscreen 3D Viewport (100% Edge-to-Edge)
        StudioViewport(
            engine = viewModel.engine,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Minimalist Floating Overlay
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Floating Island
            StudioTopBar(
                modelName = modelName,
                cameraMode = cameraMode,
                onToggleCameraMode = { viewModel.toggleCameraMode() },
                onOpenFilePicker = {
                    filePickerLauncher.launch(arrayOf("*/*", "model/*", "application/octet-stream"))
                },
                onOpenSettings = { viewModel.setSettingsOpen(true) },
                onOpenExport = {
                    viewModel.setExportDialogOpen(true)
                    viewModel.startExport()
                },
                modifier = Modifier.statusBarsPadding()
            )

            // Flexible Viewport Area
            Box(modifier = Modifier.weight(1f))

            // Bottom Transport Bar
            StudioTransportBar(
                timeline = timeline,
                onTogglePlay = { viewModel.togglePlay() },
                onScrub = { viewModel.scrub(it) },
                onStepShot = { viewModel.stepShot(it) },
                onCycleSpeed = { viewModel.cyclePlaybackSpeed() },
                modifier = Modifier.navigationBarsPadding()
            )
        }

        // 3. Settings Modal Bottom Sheet
        StudioSettingsSheet(
            isOpen = isSettingsOpen,
            onDismiss = { viewModel.setSettingsOpen(false) },
            optics = optics,
            onApertureChange = { viewModel.updateAperture(it) },
            onExposureChange = { viewModel.updateExposureEv(it) },
            lighting = lighting,
            onLightingPresetChange = { viewModel.updateLightingPreset(it) },
            onSunIntensityChange = { viewModel.updateSunIntensity(it) },
            exportSettings = exportSettings,
            onExportSettingsChange = { viewModel.updateExportSettings(it) }
        )

        // 4. Video Render Progress Modal
        ExportProgressDialog(
            isOpen = isExportDialogOpen,
            progress = exportProgress,
            onDismiss = { viewModel.setExportDialogOpen(false) },
            onCancel = { viewModel.cancelExport() }
        )
    }
}
