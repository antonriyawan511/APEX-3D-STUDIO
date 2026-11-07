package com.example.apex3dstudio.viewer

import android.content.Context
import android.net.Uri
import android.view.MotionEvent
import android.view.SurfaceView
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.Skybox
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.ModelViewer
import java.io.File
import java.io.FileInputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max

class StudioViewer(val surfaceView: SurfaceView) {

    var manipulator: Manipulator = Manipulator.Builder()
        .viewport(surfaceView.width.coerceAtLeast(1), surfaceView.height.coerceAtLeast(1))
        .build(Manipulator.Mode.ORBIT)

    val modelViewer: ModelViewer = ModelViewer(surfaceView, manipulator = manipulator)
    private val gestureDetector = StudioGestureDetector(surfaceView, manipulator)

    var animTime: Float = 0f
        private set
    var isBlenderCamera: Boolean = true
        private set
    private var lastNanos: Long = 0L
    private val lights = mutableListOf<Int>()

    init {
        setupStudioLighting()
    }

    private fun setupStudioLighting() {
        val engine = modelViewer.engine
        val scene = modelViewer.scene

        // 1. Studio Skybox Background (Dark neutral slate)
        val skybox = Skybox.Builder()
            .color(0.12f, 0.13f, 0.16f, 1.0f)
            .build(engine)
        scene.skybox = skybox

        fun addLight(dirX: Float, dirY: Float, dirZ: Float, intensity: Float, r: Float = 1f, g: Float = 1f, b: Float = 1f) {
            val light = EntityManager.get().create()
            LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(r, g, b)
                .intensity(intensity)
                .direction(dirX, dirY, dirZ)
                .castShadows(true)
                .build(engine, light)
            scene.addEntity(light)
            lights.add(light)
        }

        // Key light (top-front-right)
        addLight(0.6f, -1.0f, -0.7f, 120_000f)
        // Fill light (front-left)
        addLight(-0.8f, -0.5f, -0.4f, 65_000f)
        // Back / Rim light (top-back)
        addLight(0.0f, -0.7f, 1.0f, 50_000f)
        // Ground bounce light (soft upward bounce so underside isn't pitch black)
        addLight(0.0f, 1.0f, 0.0f, 30_000f, 0.9f, 0.95f, 1.0f)
    }

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

        surfaceView.post {
            setupOrbitManipulator()
        }

        val cameras = modelViewer.asset?.cameraEntities
        val hasCamera = cameras != null && cameras.isNotEmpty()
        isBlenderCamera = hasCamera

        if (!hasCamera) {
            modelViewer.view.camera = modelViewer.camera
        } else {
            modelViewer.view.camera = modelViewer.engine.getCameraComponent(cameras[0])
        }

        animTime = 0f
        val duration = modelViewer.animator?.let {
            if (it.animationCount > 0) it.getAnimationDuration(0) else 0f
        } ?: 0f

        onLoaded(duration, hasCamera)
    }

    private fun setModelViewerManipulator(newManipulator: Manipulator) {
        manipulator = newManipulator
        gestureDetector.manipulator = newManipulator

        runCatching {
            val manipField = ModelViewer::class.java.getDeclaredField("cameraManipulator")
            manipField.isAccessible = true
            manipField.set(modelViewer, newManipulator)

            val gestureField = ModelViewer::class.java.getDeclaredField("gestureDetector")
            gestureField.isAccessible = true
            gestureField.set(modelViewer, com.google.android.filament.utils.GestureDetector(surfaceView, newManipulator))
        }
    }

    fun setupOrbitManipulator() {
        val asset = modelViewer.asset ?: return
        val box = asset.boundingBox
        val center = box.center
        val half = box.halfExtent
        val maxExtent = max(half[0], max(half[1], half[2])).coerceAtLeast(0.5f)
        val distance = maxExtent * 2.5f

        val viewWidth = surfaceView.width.coerceAtLeast(1)
        val viewHeight = surfaceView.height.coerceAtLeast(1)

        val newManipulator = Manipulator.Builder()
            .targetPosition(center[0], center[1], center[2])
            .orbitHomePosition(center[0], center[1] + maxExtent * 0.2f, center[2] + distance)
            .viewport(viewWidth, viewHeight)
            .build(Manipulator.Mode.ORBIT)

        setModelViewerManipulator(newManipulator)
    }

    fun toggleCamera(): Boolean {
        val cameras = modelViewer.asset?.cameraEntities
        if (cameras == null || cameras.isEmpty()) return false

        isBlenderCamera = !isBlenderCamera
        if (isBlenderCamera) {
            modelViewer.view.camera = modelViewer.engine.getCameraComponent(cameras[0])
        } else {
            setupOrbitManipulator()
            modelViewer.view.camera = modelViewer.camera
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
        return gestureDetector.onTouchEvent(event)
    }

    fun destroy() {
        for (light in lights) {
            runCatching { modelViewer.engine.destroyEntity(light) }
        }
        lights.clear()
        modelViewer.destroyModel()
    }
}
