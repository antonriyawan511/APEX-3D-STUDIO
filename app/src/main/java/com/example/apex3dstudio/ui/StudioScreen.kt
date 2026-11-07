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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.apex3dstudio.state.StudioState
import com.example.apex3dstudio.ui.components.ViewerControls
import com.example.apex3dstudio.viewer.StudioViewer
import java.io.File

@Composable
fun StudioScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var studioViewer by remember { mutableStateOf<StudioViewer?>(null) }
    var state by remember { mutableStateOf(StudioState()) }

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

        ViewerControls(
            isPlaying = state.isPlaying,
            onOpenGlbClick = { launcher.launch(arrayOf("*/*")) },
            onPlayPauseClick = { state = state.copy(isPlaying = !state.isPlaying) }
        )
    }
}
