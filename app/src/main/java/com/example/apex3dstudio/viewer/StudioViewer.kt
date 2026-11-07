package com.example.apex3dstudio.viewer

import android.content.Context
import android.net.Uri
import android.view.MotionEvent
import android.view.SurfaceView
import com.example.apex3dstudio.state.EnvironmentPreset
import com.google.android.filament.Camera
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.Skybox
import com.google.android.filament.utils.ModelViewer
import java.io.File
import java.io.FileInputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class StudioViewer(val surfaceView: SurfaceView) {

    val modelViewer: ModelViewer = ModelViewer(surfaceView)
    val orbitController: StudioOrbitController = StudioOrbitController(surfaceView, surfaceView.context)

    private val orbitCameraEntity: Int = EntityManager.get().create()
    private val orbitCamera: Camera = modelViewer.engine.createCamera(orbitCameraEntity)

    var animTime: Float = 0f
        private set
    var isBlenderCamera: Boolean = true
        private set
    private var lastNanos: Long = 0L
    private val lights = mutableListOf<Int>()
    private var currentSkybox: Skybox? = null
    var currentEnvironment: EnvironmentPreset = EnvironmentPreset.STUDIO_NEUTRAL
        private set

    init {
        setupStudioLighting()
    }

    private fun setupStudioLighting() {
        val engine = modelViewer.engine
        val scene = modelViewer.scene

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
        // Ground bounce light (soft upward bounce)
        addLight(0.0f, 1.0f, 0.0f, 30_000f, 0.9f, 0.95f, 1.0f)

        applyEnvironment(EnvironmentPreset.STUDIO_NEUTRAL)
    }

    fun applyEnvironment(preset: EnvironmentPreset) {
        currentEnvironment = preset
        val engine = modelViewer.engine
        val scene = modelViewer.scene
        val lm = engine.lightManager

        val (sr, sg, sb) = when (preset) {
            EnvironmentPreset.FILE_DEFAULT -> Triple(0.12f, 0.13f, 0.16f)
            EnvironmentPreset.STUDIO_NEUTRAL -> Triple(0.12f, 0.13f, 0.16f)
            EnvironmentPreset.DAYLIGHT -> Triple(0.20f, 0.24f, 0.32f)
            EnvironmentPreset.GOLDEN_SUNSET -> Triple(0.18f, 0.10f, 0.15f)
            EnvironmentPreset.NIGHT_CITY -> Triple(0.05f, 0.06f, 0.10f)
            EnvironmentPreset.OLED_BLACK -> Triple(0.0f, 0.0f, 0.0f)
        }

        currentSkybox?.let { runCatching { engine.destroySkybox(it) } }
        val newSkybox = Skybox.Builder().color(sr, sg, sb, 1.0f).build(engine)
        scene.skybox = newSkybox
        currentSkybox = newSkybox

        if (lights.size < 4) return

        fun updateLight(
            index: Int,
            intensity: Float,
            r: Float, g: Float, b: Float,
            dirX: Float, dirY: Float, dirZ: Float
        ) {
            val instance = lm.getInstance(lights[index])
            if (instance != 0) {
                lm.setIntensity(instance, intensity)
                lm.setColor(instance, r, g, b)
                lm.setDirection(instance, dirX, dirY, dirZ)
            }
        }

        when (preset) {
            EnvironmentPreset.FILE_DEFAULT -> {
                updateLight(0, 0f, 1f, 1f, 1f, 0.6f, -1.0f, -0.7f)
                updateLight(1, 0f, 1f, 1f, 1f, -0.8f, -0.5f, -0.4f)
                updateLight(2, 0f, 1f, 1f, 1f, 0.0f, -0.7f, 1.0f)
                updateLight(3, 0f, 1f, 1f, 1f, 0.0f, 1.0f, 0.0f)
            }
            EnvironmentPreset.STUDIO_NEUTRAL -> {
                updateLight(0, 120_000f, 1.0f, 1.0f, 1.0f, 0.6f, -1.0f, -0.7f)
                updateLight(1, 65_000f, 0.95f, 0.95f, 1.0f, -0.8f, -0.5f, -0.4f)
                updateLight(2, 50_000f, 1.0f, 1.0f, 1.0f, 0.0f, -0.7f, 1.0f)
                updateLight(3, 30_000f, 0.9f, 0.95f, 1.0f, 0.0f, 1.0f, 0.0f)
            }
            EnvironmentPreset.DAYLIGHT -> {
                updateLight(0, 160_000f, 1.0f, 0.98f, 0.92f, 0.4f, -1.2f, -0.6f)
                updateLight(1, 80_000f, 0.75f, 0.85f, 1.0f, -0.6f, -0.8f, 0.2f)
                updateLight(2, 40_000f, 1.0f, 1.0f, 1.0f, 0.0f, -0.5f, 1.0f)
                updateLight(3, 40_000f, 0.85f, 0.9f, 0.8f, 0.0f, 1.0f, 0.0f)
            }
            EnvironmentPreset.GOLDEN_SUNSET -> {
                updateLight(0, 150_000f, 1.0f, 0.65f, 0.30f, 1.2f, -0.3f, -0.4f)
                updateLight(1, 50_000f, 0.45f, 0.40f, 0.75f, -1.0f, -0.4f, 0.2f)
                updateLight(2, 70_000f, 1.0f, 0.45f, 0.20f, -0.4f, -0.6f, 1.0f)
                updateLight(3, 25_000f, 0.40f, 0.25f, 0.30f, 0.0f, 1.0f, 0.0f)
            }
            EnvironmentPreset.NIGHT_CITY -> {
                updateLight(0, 90_000f, 0.25f, 0.85f, 1.0f, 0.8f, -0.8f, -0.5f)
                updateLight(1, 40_000f, 0.15f, 0.20f, 0.60f, -0.7f, -0.6f, -0.3f)
                updateLight(2, 95_000f, 1.0f, 0.15f, 0.65f, 0.0f, -0.8f, 1.0f)
                updateLight(3, 15_000f, 0.10f, 0.10f, 0.25f, 0.0f, 1.0f, 0.0f)
            }
            EnvironmentPreset.OLED_BLACK -> {
                updateLight(0, 130_000f, 1.0f, 1.0f, 1.0f, 0.5f, -1.0f, -0.5f)
                updateLight(1, 35_000f, 0.80f, 0.85f, 0.95f, -0.7f, -0.4f, -0.3f)
                updateLight(2, 80_000f, 1.0f, 1.0f, 1.0f, 0.0f, -0.5f, 1.0f)
                updateLight(3, 10_000f, 0.50f, 0.50f, 0.60f, 0.0f, 1.0f, 0.0f)
            }
        }
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

        // Drive camera
        if (!isBlenderCamera) {
            orbitController.updateCamera(orbitCamera)
            modelViewer.view.camera = orbitCamera
        } else {
            val cameras = modelViewer.asset?.cameraEntities
            if (cameras != null && cameras.isNotEmpty()) {
                modelViewer.view.camera = modelViewer.engine.getCameraComponent(cameras[0])
            }
        }

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

        modelViewer.asset?.boundingBox?.let { box ->
            orbitController.resetToModel(box.center, box.halfExtent)
        }

        val cameras = modelViewer.asset?.cameraEntities
        val hasCamera = cameras != null && cameras.isNotEmpty()
        isBlenderCamera = hasCamera

        if (!hasCamera) {
            orbitController.updateCamera(orbitCamera)
            modelViewer.view.camera = orbitCamera
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
            orbitController.updateCamera(orbitCamera)
            modelViewer.view.camera = orbitCamera
        }
        return isBlenderCamera
    }

    fun seekTo(seconds: Float) {
        val animator = modelViewer.animator
        if (animator != null && animator.animationCount > 0) {
            val duration = animator.getAnimationDuration(0).coerceAtLeast(0.1f)
            animTime = seconds.coerceIn(0f, duration)
            animator.applyAnimation(0, animTime)
            animator.updateBoneMatrices()
        } else {
            animTime = 0f
        }
    }

    fun resetTime() {
        seekTo(0f)
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        return orbitController.onTouchEvent(event)
    }

    fun destroy() {
        for (light in lights) {
            runCatching { modelViewer.engine.destroyEntity(light) }
        }
        lights.clear()
        currentSkybox?.let { runCatching { modelViewer.engine.destroySkybox(it) } }
        currentSkybox = null
        runCatching { modelViewer.engine.destroyCameraComponent(orbitCameraEntity) }
        runCatching { EntityManager.get().destroy(orbitCameraEntity) }
        modelViewer.destroyModel()
    }
}
