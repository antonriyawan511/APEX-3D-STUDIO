package com.example.apex3dstudio.state

enum class BottomBarMode {
    TOOLS,
    TIMELINE
}

data class StudioState(
    val isPlaying: Boolean = true,
    val currentTime: Float = 0f,
    val duration: Float = 0f,
    val modelName: String = "",
    val hasCamera: Boolean = false,
    val environmentPreset: EnvironmentPreset = EnvironmentPreset.STUDIO_NEUTRAL,
    val bottomBarMode: BottomBarMode = BottomBarMode.TOOLS
)
