package com.example.apex3dstudio

import android.net.Uri
import android.os.Bundle
import android.view.Choreographer
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.io.File
import java.io.FileInputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class MainActivity : ComponentActivity() {

    private var modelViewer: ModelViewer? = null
    private var isPlaying by mutableStateOf(true)
    private var animTime = 0f

    private val choreographer = Choreographer.getInstance()
    private var lastNanos = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(nanos: Long) {
            choreographer.postFrameCallback(this)
            val viewer = modelViewer ?: return

            if (isPlaying && lastNanos != 0L) {
                val dt = ((nanos - lastNanos) / 1_000_000_000.0).toFloat()
                val animator = viewer.animator
                if (animator != null && animator.animationCount > 0) {
                    val duration = animator.getAnimationDuration(0).coerceAtLeast(0.1f)
                    animTime = (animTime + dt) % duration
                    animator.applyAnimation(0, animTime)
                    animator.updateBoneMatrices()
                }
            }
            lastNanos = nanos
            viewer.render(nanos)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.init()

        setContent {
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) loadGlbFromUri(uri)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            val viewer = ModelViewer(this)
                            modelViewer = viewer
                            setOnTouchListener { _, event -> viewer.onTouchEvent(event); true }
                            choreographer.postFrameCallback(frameCallback)

                            // Try direct load test asset if available
                            val defaultFile = File("/sdcard/Download/bmw_s1000rr_cinematic.glb")
                            if (defaultFile.exists() && defaultFile.canRead()) {
                                loadGlbFile(defaultFile)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Minimal Controls (Only 2 buttons)
                Button(
                    onClick = { launcher.launch(arrayOf("*/*")) },
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                ) {
                    Text("Open GLB")
                }

                Button(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
                ) {
                    Text(if (isPlaying) "Pause" else "Play")
                }
            }
        }
    }

    private fun loadGlbFile(file: File) {
        runCatching {
            FileInputStream(file).use { fis ->
                val channel = fis.channel
                val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
                loadGlbBuffer(buffer)
            }
        }
    }

    private fun loadGlbFromUri(uri: Uri) {
        runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.LITTLE_ENDIAN)
                buffer.put(bytes)
                buffer.rewind()
                loadGlbBuffer(buffer)
            }
        }
    }

    private fun loadGlbBuffer(buffer: Buffer) {
        val viewer = modelViewer ?: return
        viewer.destroyModel()
        viewer.loadModelGlb(buffer)

        val cameras = viewer.asset?.cameraEntities
        if (cameras == null || cameras.isEmpty()) {
            viewer.transformToUnitCube()
        } else {
            // Use Blender camera directly from GLB
            viewer.view.camera = viewer.engine.getCameraComponent(cameras[0])
        }
        animTime = 0f
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.removeFrameCallback(frameCallback)
        modelViewer?.destroyModel()
    }
}
