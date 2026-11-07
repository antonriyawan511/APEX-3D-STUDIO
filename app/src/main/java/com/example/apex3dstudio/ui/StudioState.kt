package com.example.apex3dstudio.ui

enum class CameraMode(val label: String) {
    TRACK_RIG("Cinematic Track"),
    FREE_ORBIT("Free Orbit")
}

enum class ExportResolution(val label: String, val width: Int, val height: Int) {
    HD_720P("720p HD", 1280, 720),
    FHD_1080P("1080p FHD", 1920, 1080),
    QHD_1440P("1440p 2K", 2560, 1440)
}

enum class ExportAspectRatio(val label: String, val ratio: Float) {
    RATIO_16_9("16:9 Landscape", 16f / 9f),
    RATIO_9_16("9:16 Vertical Reel", 9f / 16f),
    RATIO_21_9("21:9 Cinemascope", 2.39f)
}

data class TimelineState(
    val currentTimeSec: Float = 0f,
    val durationSec: Float = 20.0f,
    val fps: Int = 24,
    val playbackSpeed: Float = 1.0f,
    val isPlaying: Boolean = false,
    val isLooping: Boolean = true,
    val currentShotIndex: Int = 1,
    val totalShots: Int = 6,
    val shotMarkers: List<Float> = listOf(0f, 3.33f, 6.67f, 10.0f, 13.33f, 16.67f)
)

data class LightingState(
    val sunIntensityLux: Float = 15_000f,
    val hdriIntensityLux: Float = 4_000f,
    val sunAzimuthDeg: Float = 45f,
    val sunElevationDeg: Float = 40f,
    val presetName: String = "Studio Clean"
)

data class OpticsState(
    val aperture: Float = 2.8f,
    val exposureEv: Float = 0.0f,
    val focusDistance: Float = 3.5f
)

data class ExportSettings(
    val resolution: ExportResolution = ExportResolution.FHD_1080P,
    val aspectRatio: ExportAspectRatio = ExportAspectRatio.RATIO_9_16,
    val fps: Int = 24,
    val bitrateMbps: Int = 35
)

data class ExportProgress(
    val isExporting: Boolean = false,
    val currentFrame: Int = 0,
    val totalFrames: Int = 0,
    val progressPercent: Float = 0f,
    val statusText: String = "",
    val outputFilePath: String? = null
)
