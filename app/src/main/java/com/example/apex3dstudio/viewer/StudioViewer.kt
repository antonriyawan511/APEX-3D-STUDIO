package com.example.apex3dstudio.viewer

import android.content.Context
import android.net.Uri
import android.view.MotionEvent
import android.view.SurfaceView
import com.google.android.filament.utils.ModelViewer
import java.io.File
import java.io.FileInputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class StudioViewer(surfaceView: SurfaceView) {

    val modelViewer: ModelViewer = ModelViewer(surfaceView)
    var animTime: Float = 0f
        private set
    var isBlenderCamera: Boolean = true
        private set
    private var lastNanos: Long = 0L

    fun step(nanos: Long, isPlaying: Boolean, onProgress: (Float) -> Unit) {
        if (isPlaying && lastNanos != 0L) {
            val dt = ((nanos - lastNanos) / 1_000_000_000.0).toFloat()
            val animator = modelViewer.animator
            if (animator != null && animator.animationCount > 0) {
                val duration = animator.getAnimationDuration(0).coerceAtLeast(0.1f)
                animTime = (animTime + dt) % duration
                animator.applyAnimation(0, animTime)
                animator.updateBoneMatrices()
                onProgress(animTime)
            }
        }
        lastNanos = nanos
        modelViewer.render(nanos)
    }

    fun loadGlbFile(file: File, onLoaded: (duration: Float, hasCamera: Boolean) -> Unit = { _, _ -> }) {
        runCatching {
            FileInputStream(file).use { fis ->
                val channel = fis.channel
                val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
                loadGlbBuffer(buffer, onLoaded)
            }
        }
    }

    fun loadGlbUri(context: Context, uri: Uri, onLoaded: (duration: Float, hasCamera: Boolean) -> Unit = { _, _ -> }) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.LITTLE_ENDIAN)
                buffer.put(bytes)
                buffer.rewind()
                loadGlbBuffer(buffer, onLoaded)
            }
        }
    }

    fun loadGlbBuffer(buffer: Buffer, onLoaded: (duration: Float, hasCamera: Boolean) -> Unit = { _, _ -> }) {
        modelViewer.destroyModel()
        modelViewer.loadModelGlb(buffer)

        val cameras = modelViewer.asset?.cameraEntities
        val hasCamera = cameras != null && cameras.isNotEmpty()
        isBlenderCamera = hasCamera

        if (!hasCamera) {
            modelViewer.transformToUnitCube()
        } else {
            modelViewer.view.camera = modelViewer.engine.getCameraComponent(cameras[0])
        }

        animTime = 0f
        val duration = modelViewer.animator?.let {
            if (it.animationCount > 0) it.getAnimationDuration(0) else 0f
        } ?: 0f

        onLoaded(duration, hasCamera)
    }

    fun toggleCamera(): Boolean {
        val cameras = modelViewer.asset?.cameraEntities
        if (cameras == null || cameras.isEmpty()) return false

        isBlenderCamera = !isBlenderCamera
        if (isBlenderCamera) {
            modelViewer.view.camera = modelViewer.engine.getCameraComponent(cameras[0])
        } else {
            modelViewer.view.camera = modelViewer.camera
            modelViewer.transformToUnitCube()
        }
        return isBlenderCamera
    }

    fun resetTime() {
        animTime = 0f
        modelViewer.animator?.let {
            if (it.animationCount > 0) {
                it.applyAnimation(0, 0f)
                it.updateBoneMatrices()
            }
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        modelViewer.onTouchEvent(event)
        return true
    }

    fun destroy() {
        modelViewer.destroyModel()
    }
}
