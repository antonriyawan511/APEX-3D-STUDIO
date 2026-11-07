package com.example.apex3dstudio.core

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceView
import com.example.apex3dstudio.ui.CameraMode
import com.example.apex3dstudio.ui.LightingState
import com.example.apex3dstudio.ui.OpticsState
import com.google.android.filament.Camera
import com.google.android.filament.ColorGrading
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Skybox
import com.google.android.filament.Texture
import com.google.android.filament.View
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

class FilamentEngine(
    private val context: Context
) {
    companion object {
        private const val TAG = "FilamentEngine"

        init {
            try {
                Utils.init()
            } catch (t: Throwable) {
                Log.e(TAG, "Utils.init error", t)
                try {
                    Filament.init()
                } catch (t2: Throwable) {
                    Log.e(TAG, "Filament.init error", t2)
                }
            }
        }
    }

    var modelViewer: ModelViewer? = null
        private set

    var engine: Engine? = null
        private set

    // Independent Cinematic Camera (bypasses ModelViewer's touch manipulator override)
    private var cinematicCamera: Camera? = null
    private var cinematicCameraEntity: Int = 0

    // Scene Environment & Lights
    private var sunEntity: Int = 0
    private var indirectLight: IndirectLight? = null
    private var skybox: Skybox? = null
    private var colorGrading: ColorGrading? = null

    // State caches
    private var currentCameraMode = CameraMode.TRACK_RIG
    private var currentLighting = LightingState()
    private var currentOptics = OpticsState()

    // 60 FPS Render Loop
    private val choreographer = Choreographer.getInstance()
    private var isLoopActive = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isLoopActive) return
            choreographer.postFrameCallback(this)
            modelViewer?.render(frameTimeNanos)
        }
    }

    fun attachSurfaceView(surfaceView: SurfaceView) {
        try {
            val eng = try {
                Log.i(TAG, "Initializing Vulkan 1.3 engine backend")
                Engine.create(Engine.Backend.VULKAN)
            } catch (t: Throwable) {
                Log.w(TAG, "Vulkan backend failed, fallback to OpenGL ES", t)
                Engine.create(Engine.Backend.OPENGL)
            }
            engine = eng

            val viewer = ModelViewer(surfaceView = surfaceView, engine = eng)
            modelViewer = viewer

            // Create dedicated cinematic camera entity
            val camEntity = EntityManager.get().create()
            cinematicCameraEntity = camEntity
            cinematicCamera = eng.createCamera(camEntity)

            setupEnvironment(eng, viewer)
            setupSunLight(eng, viewer)
            setupPostProcessing(eng, viewer)
            setupCameras(viewer)

            resumeRendering()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to attach SurfaceView", t)
        }
    }

    fun detachSurfaceView() {
        pauseRendering()
        val eng = engine ?: return
        val viewer = modelViewer ?: return

        try {
            colorGrading?.let { eng.destroyColorGrading(it) }
        } catch (_: Throwable) {}
        colorGrading = null

        try {
            indirectLight?.let { eng.destroyIndirectLight(it) }
            skybox?.let { eng.destroySkybox(it) }
            if (sunEntity != 0) {
                eng.destroyEntity(sunEntity)
                EntityManager.get().destroy(sunEntity)
                sunEntity = 0
            }
            if (cinematicCameraEntity != 0) {
                eng.destroyCameraComponent(cinematicCameraEntity)
                EntityManager.get().destroy(cinematicCameraEntity)
                cinematicCameraEntity = 0
                cinematicCamera = null
            }
        } catch (_: Throwable) {}

        try {
            viewer.destroyModel()
        } catch (_: Throwable) {}
        modelViewer = null
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        if (currentCameraMode == CameraMode.FREE_ORBIT) {
            modelViewer?.onTouchEvent(event)
            return true
        }
        return false
    }

    fun resumeRendering() {
        if (!isLoopActive) {
            isLoopActive = true
            choreographer.postFrameCallback(frameCallback)
        }
    }

    fun pauseRendering() {
        isLoopActive = false
        choreographer.removeFrameCallback(frameCallback)
    }

    private fun setupEnvironment(eng: Engine, viewer: ModelViewer) {
        // Dark Obsidian Studio Background
        val sb = Skybox.Builder()
            .color(0.04f, 0.05f, 0.08f, 1.0f)
            .build(eng)
        viewer.scene.skybox = sb
        skybox = sb

        // Ambient Spherical Harmonics
        val sh = floatArrayOf(0.282095f, 0.282095f, 0.282095f)
        val ibl = IndirectLight.Builder()
            .irradiance(1, sh)
            .intensity(currentLighting.hdriIntensityLux)
            .build(eng)
        viewer.scene.indirectLight = ibl
        indirectLight = ibl
    }

    private fun setupSunLight(eng: Engine, viewer: ModelViewer) {
        val sun = EntityManager.get().create()
        val radAz = Math.toRadians(currentLighting.sunAzimuthDeg.toDouble())
        val radEl = Math.toRadians(currentLighting.sunElevationDeg.toDouble())
        val dx = (cos(radEl) * sin(radAz)).toFloat()
        val dy = (-sin(radEl)).toFloat()
        val dz = (cos(radEl) * cos(radAz)).toFloat()

        LightManager.Builder(LightManager.Type.SUN)
            .color(1.0f, 0.96f, 0.90f)
            .intensity(currentLighting.sunIntensityLux)
            .direction(dx, dy, dz)
            .castShadows(true)
            .sunAngularRadius(0.54f)
            .shadowOptions(LightManager.ShadowOptions().apply {
                screenSpaceContactShadows = true
            })
            .build(eng, sun)

        viewer.scene.addEntity(sun)
        sunEntity = sun
    }

    private fun setupPostProcessing(eng: Engine, viewer: ModelViewer) {
        val view = viewer.view

        view.antiAliasing = View.AntiAliasing.FXAA
        view.temporalAntiAliasingOptions = view.temporalAntiAliasingOptions.apply {
            enabled = true
            feedback = 0.90f
            filterHistory = true
        }

        // GTAO Ambient Occlusion
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
            enabled = true
            radius = 0.45f
            power = 1.2f
            bias = 0.005f
        }

        // Crisp, controlled Bloom (NO blowout)
        view.bloomOptions = view.bloomOptions.apply {
            enabled = true
            strength = 0.05f
            threshold = true
            highlight = 800.0f
        }

        view.screenSpaceReflectionsOptions = view.screenSpaceReflectionsOptions.apply {
            enabled = true
        }

        // ACES Filmic Tone Mapping
        updateColorGrading(eng, view)
    }

    private fun updateColorGrading(eng: Engine, view: View) {
        val old = colorGrading
        val newCg = ColorGrading.Builder()
            .toneMapping(ColorGrading.ToneMapping.ACES)
            .build(eng)
        view.colorGrading = newCg
        colorGrading = newCg
        old?.let { eng.destroyColorGrading(it) }
    }

    private fun setupCameras(viewer: ModelViewer) {
        val aperture = currentOptics.aperture.coerceIn(1.2f, 22.0f)
        val shutter = 1.0f / 125.0f
        val evFactor = Math.pow(2.0, (-currentOptics.exposureEv).toDouble()).toFloat()
        val iso = (100.0f * evFactor).coerceIn(25.0f, 6400.0f)

        viewer.camera.setExposure(aperture, shutter, iso)
        viewer.camera.setFocusDistance(currentOptics.focusDistance)

        cinematicCamera?.setExposure(aperture, shutter, iso)
        cinematicCamera?.setFocusDistance(currentOptics.focusDistance)

        syncActiveCamera(viewer)
    }

    private fun syncActiveCamera(viewer: ModelViewer) {
        val asset = viewer.asset
        val hasCameras = asset != null && asset.cameraEntities.isNotEmpty()

        if (currentCameraMode == CameraMode.TRACK_RIG && hasCameras && cinematicCamera != null) {
            viewer.view.camera = cinematicCamera
            updateCameraFromGlb(viewer)
        } else {
            viewer.view.camera = viewer.camera
        }
    }

    fun loadGlbBuffer(buffer: ByteBuffer) {
        val viewer = modelViewer ?: return
        try {
            viewer.destroyModel()
            viewer.loadModelGlb(buffer)

            val hasCameras = viewer.asset?.cameraEntities?.isNotEmpty() == true
            // If asset already contains baked Blender cameras, preserve real 1:1 meter coordinates
            if (!hasCameras) {
                viewer.transformToUnitCube()
            }

            syncActiveCamera(viewer)
        } catch (t: Throwable) {
            Log.e(TAG, "Error loading GLB buffer", t)
        }
    }

    fun setCameraMode(mode: CameraMode) {
        currentCameraMode = mode
        modelViewer?.let { syncActiveCamera(it) }
    }

    fun updateOptics(optics: OpticsState) {
        currentOptics = optics
        modelViewer?.let { setupCameras(it) }
    }

    fun updateLighting(lighting: LightingState) {
        currentLighting = lighting
        val eng = engine ?: return

        if (sunEntity != 0) {
            val radAz = Math.toRadians(lighting.sunAzimuthDeg.toDouble())
            val radEl = Math.toRadians(lighting.sunElevationDeg.toDouble())
            val dx = (cos(radEl) * sin(radAz)).toFloat()
            val dy = (-sin(radEl)).toFloat()
            val dz = (cos(radEl) * cos(radAz)).toFloat()

            eng.lightManager.setDirection(
                eng.lightManager.getInstance(sunEntity),
                dx, dy, dz
            )
            eng.lightManager.setIntensity(
                eng.lightManager.getInstance(sunEntity),
                lighting.sunIntensityLux
            )
        }

        indirectLight?.intensity = lighting.hdriIntensityLux
    }

    fun applyAnimationTime(timeSec: Float) {
        val viewer = modelViewer ?: return
        val animator = viewer.animator ?: return

        if (animator.animationCount > 0) {
            animator.applyAnimation(0, timeSec)
            animator.updateBoneMatrices()
            if (currentCameraMode == CameraMode.TRACK_RIG) {
                updateCameraFromGlb(viewer)
            }
        }
    }

    fun updateCameraFromGlb(viewer: ModelViewer) {
        val asset = viewer.asset ?: return
        val eng = engine ?: return
        val cameraEntities = asset.cameraEntities
        if (cameraEntities.isNotEmpty()) {
            val camEntity = cameraEntities[0]
            val tm = eng.transformManager
            val inst = tm.getInstance(camEntity)
            if (inst != 0) {
                val worldMat = FloatArray(16)
                tm.getWorldTransform(inst, worldMat)
                val cinCam = cinematicCamera
                if (cinCam != null) {
                    cinCam.setModelMatrix(worldMat)
                    // Synchronize exact projection & aspect ratio from the view
                    val projMat = DoubleArray(16)
                    viewer.camera.getProjectionMatrix(projMat)
                    cinCam.setCustomProjection(
                        projMat,
                        viewer.camera.near.toDouble(),
                        viewer.camera.cullingFar.toDouble()
                    )
                } else {
                    viewer.camera.setModelMatrix(worldMat)
                }
            }
        }
    }

    /**
     * Reads pixel buffer from Filament renderer for hardware MP4 export
     */
    fun captureCurrentFrame(width: Int, height: Int, onResult: (Bitmap?) -> Unit) {
        val viewer = modelViewer ?: run {
            onResult(null)
            return
        }

        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val byteBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN)

            val pbd = Texture.PixelBufferDescriptor(
                byteBuffer,
                Texture.Format.RGBA,
                Texture.Type.UBYTE,
                1, 0, 0, 0,
                mainHandler,
                Runnable {
                    byteBuffer.rewind()
                    bitmap.copyPixelsFromBuffer(byteBuffer)
                    onResult(bitmap)
                }
            )
            viewer.renderer.readPixels(0, 0, width, height, pbd)
        } catch (t: Throwable) {
            Log.e(TAG, "Frame capture error", t)
            onResult(null)
        }
    }
}
