package com.example.apex3dstudio.state

data class GestureSensitivity(
    val orbit: Float = 1.0f,      // Multiplier orbit kamera 1 jari (1.0x = 1:1)
    val pan: Float = 1.0f,        // Multiplier geser kamera 2 jari (1.0x = 1:1)
    val zoom: Float = 1.0f,       // Multiplier perbesaran cubit 2 jari (1.0x = 1:1)
    val twistOrbit: Float = 1.0f  // Multiplier putar 2 jari turntable (1.0x = 1:1)
)
