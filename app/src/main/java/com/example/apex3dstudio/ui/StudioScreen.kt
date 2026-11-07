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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.apex3dstudio.state.BottomBarMode
import com.example.apex3dstudio.state.GestureSensitivity
import com.example.apex3dstudio.state.StudioState
import com.example.apex3dstudio.ui.components.SettingsDialog
import com.example.apex3dstudio.ui.components.StudioBottomBar
import com.example.apex3dstudio.ui.components.StudioTimelineBar
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

    val currentPlaying by rememberUpdatedState(state.isPlaying)
    val choreographer = remember { Choreographer.getInstance() }
    val frameCallback = remember {
        object : Choreographer.FrameCallback {
            override fun doFrame(nanos: Long) {
                choreographer.postFrameCallback(this)
                studioViewer?.step(nanos, currentPlaying) { progress ->
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
                    modelName = uri.lastPathSegment ?: "Custom Model",
                    currentTime = 0f,
                    isPlaying = false
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
                                modelName = defaultFile.name,
                                currentTime = 0f,
                                isPlaying = false
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
            bottomBarMode = state.bottomBarMode,
            onToggleBottomBarMode = {
                state = state.copy(
                    bottomBarMode = if (state.bottomBarMode == BottomBarMode.TOOLS) BottomBarMode.TIMELINE else BottomBarMode.TOOLS
                )
            },
            hasCamera = state.hasCamera,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (state.bottomBarMode == BottomBarMode.TIMELINE) {
            StudioTimelineBar(
                isPlaying = state.isPlaying,
                currentTime = state.currentTime,
                duration = state.duration,
                onPlayPause = {
                    val next = !state.isPlaying
                    studioViewer?.isPlaying = next
                    state = state.copy(isPlaying = next)
                },
                onReset = {
                    studioViewer?.seekTo(0f)
                    state = state.copy(currentTime = 0f, isPlaying = false)
                },
                onForward1s = {
                    val target = (state.currentTime + 1.0f).coerceAtMost(state.duration)
                    studioViewer?.seekTo(target)
                    state = state.copy(currentTime = target, isPlaying = false)
                },
                onBackward1s = {
                    val target = (state.currentTime - 1.0f).coerceAtLeast(0f)
                    studioViewer?.seekTo(target)
                    state = state.copy(currentTime = target, isPlaying = false)
                },
                onSkipToEnd = {
                    val end = state.duration
                    studioViewer?.seekTo(end)
                    state = state.copy(currentTime = end, isPlaying = false)
                },
                onSeek = { targetTime ->
                    studioViewer?.seekTo(targetTime)
                    state = state.copy(currentTime = targetTime, isPlaying = false)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            StudioBottomBar(
                isPlaying = state.isPlaying,
                onPlayPauseClick = {
                    val next = !state.isPlaying
                    studioViewer?.isPlaying = next
                    state = state.copy(isPlaying = next)
                },
                onResetClick = {
                    studioViewer?.resetTime()
                    state = state.copy(currentTime = 0f, isPlaying = false)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

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
