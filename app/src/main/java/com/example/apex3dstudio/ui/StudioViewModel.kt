package com.example.apex3dstudio.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.apex3dstudio.core.FilamentEngine
import com.example.apex3dstudio.export.VideoRenderer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "StudioViewModel"

    val engine = FilamentEngine(application)
    private val videoRenderer = VideoRenderer(application, engine)

    // States
    private val _cameraMode = MutableStateFlow(CameraMode.TRACK_RIG)
    val cameraMode: StateFlow<CameraMode> = _cameraMode.asStateFlow()

    private val _timeline = MutableStateFlow(TimelineState())
    val timeline: StateFlow<TimelineState> = _timeline.asStateFlow()

    private val _lighting = MutableStateFlow(LightingState())
    val lighting: StateFlow<LightingState> = _lighting.asStateFlow()

    private val _optics = MutableStateFlow(OpticsState())
    val optics: StateFlow<OpticsState> = _optics.asStateFlow()

    private val _exportSettings = MutableStateFlow(ExportSettings())
    val exportSettings: StateFlow<ExportSettings> = _exportSettings.asStateFlow()

    private val _exportProgress = MutableStateFlow(ExportProgress())
    val exportProgress: StateFlow<ExportProgress> = _exportProgress.asStateFlow()

    private val _modelName = MutableStateFlow("Studio Default Scene")
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isExportDialogOpen = MutableStateFlow(false)
    val isExportDialogOpen: StateFlow<Boolean> = _isExportDialogOpen.asStateFlow()

    private var playbackJob: Job? = null
    private var exportJob: Job? = null

    fun setCameraMode(mode: CameraMode) {
        _cameraMode.value = mode
        engine.setCameraMode(mode)
    }

    fun toggleCameraMode() {
        val next = if (_cameraMode.value == CameraMode.TRACK_RIG) CameraMode.FREE_ORBIT else CameraMode.TRACK_RIG
        setCameraMode(next)
    }

    fun setSettingsOpen(open: Boolean) {
        _isSettingsOpen.value = open
    }

    fun setExportDialogOpen(open: Boolean) {
        _isExportDialogOpen.value = open
    }

    // Timeline actions
    fun togglePlay() {
        val next = !_timeline.value.isPlaying
        _timeline.update { it.copy(isPlaying = next) }
        if (next) {
            startPlaybackLoop()
        } else {
            playbackJob?.cancel()
        }
    }

    fun scrub(timeSec: Float) {
        val duration = _timeline.value.durationSec
        val clamped = timeSec.coerceIn(0f, duration)
        updateCurrentTime(clamped)
    }

    fun stepFrame(forward: Boolean) {
        val dt = 1.0f / _timeline.value.fps.toFloat()
        val current = _timeline.value.currentTimeSec
        val next = if (forward) current + dt else current - dt
        scrub(next)
    }

    fun jumpShot(shotIndex: Int) {
        val markers = _timeline.value.shotMarkers
        if (shotIndex in 1..markers.size) {
            val targetSec = markers[shotIndex - 1]
            _timeline.update { it.copy(currentShotIndex = shotIndex) }
            updateCurrentTime(targetSec)
        }
    }

    fun stepShot(forward: Boolean) {
        val current = _timeline.value.currentShotIndex
        val total = _timeline.value.totalShots
        val next = if (forward) {
            if (current < total) current + 1 else 1
        } else {
            if (current > 1) current - 1 else total
        }
        jumpShot(next)
    }

    fun cyclePlaybackSpeed() {
        val current = _timeline.value.playbackSpeed
        val next = when (current) {
            0.5f -> 1.0f
            1.0f -> 2.0f
            else -> 0.5f
        }
        _timeline.update { it.copy(playbackSpeed = next) }
    }

    private fun updateCurrentTime(timeSec: Float) {
        val markers = _timeline.value.shotMarkers
        var shot = 1
        for (i in markers.indices.reversed()) {
            if (timeSec >= markers[i]) {
                shot = i + 1
                break
            }
        }
        _timeline.update { it.copy(currentTimeSec = timeSec, currentShotIndex = shot) }
        engine.applyAnimationTime(timeSec)
    }

    private fun startPlaybackLoop() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            var lastTime = SystemClock.elapsedRealtime()
            while (isActive && _timeline.value.isPlaying) {
                val now = SystemClock.elapsedRealtime()
                val delta = ((now - lastTime) / 1000f) * _timeline.value.playbackSpeed
                lastTime = now

                var current = _timeline.value.currentTimeSec + delta
                val duration = _timeline.value.durationSec

                if (current >= duration) {
                    if (_timeline.value.isLooping) {
                        current = 0f
                    } else {
                        current = duration
                        _timeline.update { it.copy(isPlaying = false, currentTimeSec = duration) }
                        break
                    }
                }

                updateCurrentTime(current)
                delay(16) // ~60fps timeline tick
            }
        }
    }

    // Load External 3D Asset
    fun loadGlbFromUri(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val byteBuffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.LITTLE_ENDIAN)
                    byteBuffer.put(bytes)
                    byteBuffer.rewind()

                    engine.loadGlbBuffer(byteBuffer)

                    val fileName = uri.lastPathSegment?.substringAfterLast("/")?.substringAfterLast(":") ?: "3D Model"
                    _modelName.value = fileName

                    val viewer = engine.modelViewer
                    val animator = viewer?.animator
                    val animCount = animator?.animationCount ?: 0
                    val hasCameras = viewer?.asset?.cameraEntities?.isNotEmpty() == true

                    if (animCount > 0) {
                        val duration = animator?.getAnimationDuration(0) ?: 20.0f
                        _timeline.update {
                            it.copy(
                                currentTimeSec = 0f,
                                durationSec = duration.coerceAtLeast(1.0f)
                            )
                        }
                    }

                    if (hasCameras || animCount > 0) {
                        setCameraMode(CameraMode.TRACK_RIG)
                        updateCurrentTime(0f)
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error loading GLB from uri", t)
            }
        }
    }

    // Optics & Lighting
    fun updateAperture(fStop: Float) {
        _optics.update { it.copy(aperture = fStop) }
        engine.updateOptics(_optics.value)
    }

    fun updateExposureEv(ev: Float) {
        _optics.update { it.copy(exposureEv = ev) }
        engine.updateOptics(_optics.value)
    }

    fun updateLightingPreset(presetName: String) {
        val newLighting = when (presetName) {
            "Studio Clean" -> LightingState(
                sunIntensityLux = 15_000f,
                hdriIntensityLux = 4_000f,
                sunAzimuthDeg = 45f,
                sunElevationDeg = 40f,
                presetName = "Studio Clean"
            )
            "Golden Hour" -> LightingState(
                sunIntensityLux = 12_000f,
                hdriIntensityLux = 3_500f,
                sunAzimuthDeg = 65f,
                sunElevationDeg = 16f,
                presetName = "Golden Hour"
            )
            "High Noon" -> LightingState(
                sunIntensityLux = 35_000f,
                hdriIntensityLux = 8_000f,
                sunAzimuthDeg = 180f,
                sunElevationDeg = 75f,
                presetName = "High Noon"
            )
            "Cyberpunk Night" -> LightingState(
                sunIntensityLux = 5_000f,
                hdriIntensityLux = 2_000f,
                sunAzimuthDeg = 240f,
                sunElevationDeg = 20f,
                presetName = "Cyberpunk Night"
            )
            else -> _lighting.value
        }
        _lighting.value = newLighting
        engine.updateLighting(newLighting)
    }

    fun updateSunIntensity(lux: Float) {
        _lighting.update { it.copy(sunIntensityLux = lux) }
        engine.updateLighting(_lighting.value)
    }

    // Video Export
    fun updateExportSettings(settings: ExportSettings) {
        _exportSettings.value = settings
    }

    fun startExport() {
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _exportProgress.value = ExportProgress(
                isExporting = true,
                statusText = "Initializing hardware encoder..."
            )
            val file = videoRenderer.exportVideo(
                settings = _exportSettings.value,
                durationSec = _timeline.value.durationSec
            ) { progress ->
                _exportProgress.value = progress
            }

            if (file != null && file.exists()) {
                _exportProgress.update {
                    it.copy(
                        isExporting = false,
                        outputFilePath = file.absolutePath,
                        statusText = "Export Complete!"
                    )
                }
            } else {
                _exportProgress.update {
                    it.copy(
                        isExporting = false,
                        statusText = "Export failed or cancelled."
                    )
                }
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        _exportProgress.value = ExportProgress(isExporting = false, statusText = "Cancelled")
    }
}
