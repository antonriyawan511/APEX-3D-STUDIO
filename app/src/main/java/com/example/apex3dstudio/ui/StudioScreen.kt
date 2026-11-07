package com.example.apex3dstudio.ui

import android.net.Uri
import android.view.Choreographer
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.apex3dstudio.state.GestureSensitivity
import com.example.apex3dstudio.state.StudioState
import com.example.apex3dstudio.ui.components.SettingsDialog
import com.example.apex3dstudio.ui.components.StudioBottomBar
import com.example.apex3dstudio.ui.components.StudioTopBar
import com.example.apex3dstudio.viewer.StudioViewer
import java.io.File

@Composable
fun StudioScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var studioViewer by remember { mutableStateOf<StudioViewer?>(null) }
    var state by remember { mutableStateOf(StudioState()) }
    var sensitivity by remember { mutableStateOf(GestureSensitivity()) }
    var showSettings by remember { mutableStateOf(false) }

    val choreographer = remember { Choreographer.getInstance() }
    val frameCallback = remember {
        object : Choreographer.FrameCallback {
            override fun doFrame(nanos: Long) {
                choreographer.postFrameCallback(this)
                studioViewer?.step(nanos, state.isPlaying) { progress ->
                    state = state.copy(currentTime = progress)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        choreographer.postFrameCallback(frameCallback)
        onDispose {
            choreographer.removeFrameCallback(frameCallback)
            studioViewer?.destroy()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            studioViewer?.loadGlbUri(context, uri) { duration, hasCamera ->
                state = state.copy(
                    duration = duration,
                    hasCamera = hasCamera,
                    modelName = uri.lastPathSegment ?: "Custom Model"
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    val viewer = StudioViewer(this)
                    viewer.orbitController.sensitivity = sensitivity
                    studioViewer = viewer
                    setOnTouchListener { _, event -> viewer.onTouchEvent(event) }

                    // Auto-load default benchmark model if available
                    val defaultFile = File("/sdcard/Download/bmw_s1000rr_cinematic.glb")
                    if (defaultFile.exists() && defaultFile.canRead()) {
                        viewer.loadGlbFile(defaultFile) { duration, hasCamera ->
                            state = state.copy(
                                duration = duration,
                                hasCamera = hasCamera,
                                modelName = defaultFile.name
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        StudioTopBar(
            onOpenClick = { launcher.launch(arrayOf("*/*")) },
            onCameraClick = {
                val isBlender = studioViewer?.toggleCamera() ?: false
                state = state.copy(hasCamera = isBlender)
            },
            onSettingsClick = { showSettings = true },
            currentPreset = state.environmentPreset,
            onPresetSelected = { preset ->
                state = state.copy(environmentPreset = preset)
                studioViewer?.applyEnvironment(preset)
            },
            hasCamera = state.hasCamera,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        StudioBottomBar(
            isPlaying = state.isPlaying,
            onPlayPauseClick = { state = state.copy(isPlaying = !state.isPlaying) },
            onResetClick = {
                studioViewer?.resetTime()
                state = state.copy(currentTime = 0f)
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showSettings) {
            SettingsDialog(
                sensitivity = sensitivity,
                onOrbitChanged = {
                    sensitivity = sensitivity.copy(orbit = it)
                    studioViewer?.orbitController?.sensitivity = sensitivity
                },
                onPanChanged = {
                    sensitivity = sensitivity.copy(pan = it)
                    studioViewer?.orbitController?.sensitivity = sensitivity
                },
                onZoomChanged = {
                    sensitivity = sensitivity.copy(zoom = it)
                    studioViewer?.orbitController?.sensitivity = sensitivity
                },
                onTwistOrbitChanged = {
                    sensitivity = sensitivity.copy(twistOrbit = it)
                    studioViewer?.orbitController?.sensitivity = sensitivity
                },
                onReset = {
                    sensitivity = GestureSensitivity()
                    studioViewer?.orbitController?.sensitivity = sensitivity
                },
                onDismiss = { showSettings = false }
            )
        }
    }
}
