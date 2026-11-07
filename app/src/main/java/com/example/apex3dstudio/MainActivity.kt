package com.example.apex3dstudio

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.apex3dstudio.ui.theme.APEX3DSTUDIOTheme
import com.google.android.filament.Camera
import com.google.android.filament.ColorGrading
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : ComponentActivity() {

    private val TAG = "ApexViewer"

    private var modelViewer: ModelViewer? = null
    private var gltfCamera: Camera? = null
    private var useGltfCamera = true

    // Playback state
    private var isPlaying = false
    private var currentTimeSec = 0f
    private var durationSec = 20f

    private val choreographer = Choreographer.getInstance()
    private var lastFrameTimeNanos = 0L

    // UI state observers
    private val uiCurrentTime = mutableFloatStateOf(0f)
    private val uiDuration = mutableFloatStateOf(20f)
    private val uiIsPlaying = mutableStateOf(false)
    private val uiUseTrackCamera = mutableStateOf(true)
    private val uiModelName = mutableStateOf("No Model Loaded")

    init {
        try {
            Utils.init()
        } catch (_: Throwable) {
            Filament.init()
        }
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            choreographer.postFrameCallback(this)
            val viewer = modelViewer ?: return

            if (lastFrameTimeNanos != 0L && isPlaying) {
                val dt = ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000.0).toFloat()
                val anim = viewer.animator
                if (anim != null && anim.animationCount > 0) {
                    currentTimeSec = (currentTimeSec + dt) % durationSec
                    anim.applyAnimation(0, currentTimeSec)
                    anim.updateBoneMatrices()
                    uiCurrentTime.floatValue = currentTimeSec
                }
            }
            lastFrameTimeNanos = frameTimeNanos
            viewer.render(frameTimeNanos)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        setContent {
            APEX3DSTUDIOTheme {
                ViewerScreen(
                    modelName = uiModelName.value,
                    currentTime = uiCurrentTime.floatValue,
                    duration = uiDuration.floatValue,
                    isPlaying = uiIsPlaying.value,
                    useTrackCamera = uiUseTrackCamera.value,
                    onAttachSurface = { sv -> setupFilament(sv) },
                    onDetachSurface = { teardownFilament() },
                    onLoadFile = { uri -> loadModelFromUri(uri) },
                    onTogglePlay = { togglePlay() },
                    onScrub = { sec -> seekTo(sec) },
                    onToggleCamera = { toggleCameraMode() }
                )
            }
        }
    }

    private fun setupFilament(surfaceView: SurfaceView) {
        val eng = try {
            Engine.create(Engine.Backend.VULKAN)
        } catch (_: Throwable) {
            Engine.create(Engine.Backend.OPENGL)
        }

        val viewer = ModelViewer(surfaceView = surfaceView, engine = eng)
        modelViewer = viewer

        // Setup clean studio lighting (No blowout)
        val sb = Skybox.Builder().color(0.05f, 0.05f, 0.08f, 1.0f).build(eng)
        viewer.scene.skybox = sb

        val sh = floatArrayOf(0.282095f, 0.282095f, 0.282095f)
        val ibl = IndirectLight.Builder().irradiance(1, sh).intensity(4_000f).build(eng)
        viewer.scene.indirectLight = ibl

        val sun = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.SUN)
            .color(1.0f, 0.96f, 0.90f)
            .intensity(16_000f)
            .direction(0.6f, -0.8f, 0.5f)
            .castShadows(true)
            .sunAngularRadius(0.54f)
            .build(eng, sun)
        viewer.scene.addEntity(sun)

        // ACES Filmic Tone Mapping + FXAA + subtle bloom
        val cg = ColorGrading.Builder().toneMapping(ColorGrading.ToneMapping.ACES).build(eng)
        viewer.view.colorGrading = cg
        viewer.view.antiAliasing = View.AntiAliasing.FXAA
        viewer.view.bloomOptions = viewer.view.bloomOptions.apply {
            enabled = true
            strength = 0.04f
        }

        // Touch listener for free orbit
        surfaceView.setOnTouchListener { _, event ->
            if (!useGltfCamera) {
                viewer.onTouchEvent(event)
            }
            true
        }

        choreographer.postFrameCallback(frameCallback)

        // Auto-load pre-exported BMW or Castle scene if exists in Download folder
        val bmw = File("/sdcard/Download/bmw_s1000rr_cinematic.glb")
        val castle = File("/sdcard/Download/castle_sunset_animated.glb")
        if (bmw.exists()) {
            loadModelFile(bmw)
        } else if (castle.exists()) {
            loadModelFile(castle)
        }
    }

    private fun teardownFilament() {
        choreographer.removeFrameCallback(frameCallback)
        try {
            modelViewer?.destroyModel()
        } catch (_: Throwable) {}
        modelViewer = null
    }

    private fun loadModelFile(file: File) {
        try {
            val bytes = file.readBytes()
            applyGlbBytes(bytes, file.name)
        } catch (t: Throwable) {
            Log.e(TAG, "Error reading file", t)
        }
    }

    private fun loadModelFromUri(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val name = uri.lastPathSegment?.substringAfterLast("/")?.substringAfterLast(":") ?: "Model"
                applyGlbBytes(bytes, name)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error loading uri", t)
        }
    }

    private fun applyGlbBytes(bytes: ByteArray, name: String) {
        val viewer = modelViewer ?: return
        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(bytes)
        buffer.rewind()

        viewer.destroyModel()
        viewer.loadModelGlb(buffer)

        val asset = viewer.asset
        val cameras = asset?.cameraEntities

        if (cameras == null || cameras.isEmpty()) {
            // No camera in GLB -> use free orbit and center unit cube
            viewer.transformToUnitCube()
            gltfCamera = null
            useGltfCamera = false
            viewer.view.camera = viewer.camera
        } else {
            // Has baked Blender camera -> use glTF camera directly (1:1 Blender coordinates)
            val cam = viewer.engine.getCameraComponent(cameras[0])
            gltfCamera = cam
            useGltfCamera = true
            viewer.view.camera = cam
        }

        val anim = viewer.animator
        if (anim != null && anim.animationCount > 0) {
            durationSec = anim.getAnimationDuration(0).coerceAtLeast(1.0f)
            uiDuration.floatValue = durationSec
            currentTimeSec = 0f
            uiCurrentTime.floatValue = 0f
            anim.applyAnimation(0, 0f)
            anim.updateBoneMatrices()
        }

        uiModelName.value = name
        uiUseTrackCamera.value = useGltfCamera
    }

    private fun togglePlay() {
        isPlaying = !isPlaying
        uiIsPlaying.value = isPlaying
    }

    private fun seekTo(sec: Float) {
        currentTimeSec = sec.coerceIn(0f, durationSec)
        uiCurrentTime.floatValue = currentTimeSec
        val anim = modelViewer?.animator
        if (anim != null && anim.animationCount > 0) {
            anim.applyAnimation(0, currentTimeSec)
            anim.updateBoneMatrices()
        }
    }

    private fun toggleCameraMode() {
        val viewer = modelViewer ?: return
        if (gltfCamera == null) return

        useGltfCamera = !useGltfCamera
        uiUseTrackCamera.value = useGltfCamera

        if (useGltfCamera) {
            viewer.view.camera = gltfCamera
            val anim = viewer.animator
            if (anim != null && anim.animationCount > 0) {
                anim.applyAnimation(0, currentTimeSec)
                anim.updateBoneMatrices()
            }
        } else {
            viewer.view.camera = viewer.camera
        }
    }
}

@Composable
fun ViewerScreen(
    modelName: String,
    currentTime: Float,
    duration: Float,
    isPlaying: Boolean,
    useTrackCamera: Boolean,
    onAttachSurface: (SurfaceView) -> Unit,
    onDetachSurface: () -> Unit,
    onLoadFile: (Uri) -> Unit,
    onTogglePlay: () -> Unit,
    onScrub: (Float) -> Unit,
    onToggleCamera: () -> Unit
) {
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onLoadFile(uri)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF090B10))) {
        // 1. 3D Viewport (Full Screen)
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    onAttachSurface(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        DisposableEffect(Unit) {
            onDispose { onDetachSurface() }
        }

        // 2. Floating Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Open File Button + Model Name
            Surface(
                color = Color(0xFF141722).copy(alpha = 0.88f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF282E40)),
                modifier = Modifier.clickable { filePicker.launch(arrayOf("*/*")) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = "Open",
                        tint = Color(0xFF00D2C4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = modelName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Camera Mode Toggle Pill
            Surface(
                color = if (useTrackCamera) Color(0xFFF5A623).copy(alpha = 0.2f) else Color(0xFF141722).copy(alpha = 0.88f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    0.6.dp,
                    if (useTrackCamera) Color(0xFFF5A623) else Color(0xFF282E40)
                ),
                modifier = Modifier.clickable { onToggleCamera() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Camera",
                        tint = if (useTrackCamera) Color(0xFFF5A623) else Color(0xFF8B92A4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (useTrackCamera) "Track Rig" else "Free Orbit",
                        color = if (useTrackCamera) Color(0xFFF5A623) else Color(0xFF8B92A4),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Floating Bottom Transport Bar
        Surface(
            color = Color(0xFF141722).copy(alpha = 0.90f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF282E40)),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Scrubber
                Slider(
                    value = currentTime.coerceIn(0f, duration),
                    onValueChange = onScrub,
                    valueRange = 0f..duration.coerceAtLeast(0.1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFF5A623),
                        activeTrackColor = Color(0xFFF5A623),
                        inactiveTrackColor = Color(0xFF202636)
                    ),
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                )

                // Transport Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val curSec = currentTime.toInt()
                    val totalSec = duration.toInt()
                    val timecode = String.format("%02d:%02d / %02d:%02d", curSec / 60, curSec % 60, totalSec / 60, totalSec % 60)

                    Text(
                        text = timecode,
                        color = Color(0xFFF5A623),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    // Play/Pause button
                    Surface(
                        color = Color(0xFFF5A623),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { onTogglePlay() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
