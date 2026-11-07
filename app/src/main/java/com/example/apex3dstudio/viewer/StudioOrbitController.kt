package com.example.apex3dstudio.viewer

import android.view.MotionEvent
import android.view.View
import com.google.android.filament.Camera
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class StudioOrbitController(private val view: View) {

    var targetX: Double = 0.0
    var targetY: Double = 0.0
    var targetZ: Double = 0.0

    var distance: Double = 4.0
    var minDistance: Double = 0.1
    var maxDistance: Double = 1000.0

    var yaw: Double = 0.0    // degrees around Y
    var pitch: Double = 15.0 // degrees above horizon

    private var prevX = 0f
    private var prevY = 0f
    private var prevDistance = 0f
    private var isDragging = false
    private var isPinching = false

    fun resetToModel(center: FloatArray, halfExtent: FloatArray) {
        targetX = center[0].toDouble()
        targetY = center[1].toDouble()
        targetZ = center[2].toDouble()

        val maxExt = maxOf(halfExtent[0], halfExtent[1], halfExtent[2]).toDouble().coerceAtLeast(0.5)
        distance = maxExt * 2.5
        minDistance = maxExt * 0.1
        maxDistance = maxExt * 25.0
        yaw = 0.0
        pitch = 15.0
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.x
                prevY = event.y
                isDragging = true
                isPinching = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    val x0 = event.getX(0)
                    val y0 = event.getY(0)
                    val x1 = event.getX(1)
                    val y1 = event.getY(1)

                    prevX = (x0 + x1) / 2f
                    prevY = (y0 + y1) / 2f
                    prevDistance = hypot(x1 - x0, y1 - y0)
                    isPinching = true
                    isDragging = false
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isPinching && event.pointerCount >= 2) {
                    val x0 = event.getX(0)
                    val y0 = event.getY(0)
                    val x1 = event.getX(1)
                    val y1 = event.getY(1)

                    val midX = (x0 + x1) / 2f
                    val midY = (y0 + y1) / 2f
                    val currDistance = hypot(x1 - x0, y1 - y0)

                    // 1. Simultaneous Zoom (Multiplicative Pinch scaling)
                    if (prevDistance > 0f && currDistance > 0f) {
                        val scale = (currDistance / prevDistance).toDouble()
                        if (scale in 0.1..10.0) {
                            distance = (distance / scale).coerceIn(minDistance, maxDistance)
                        }
                    }

                    // 2. Simultaneous Pan (Translates target along camera screen plane)
                    val dx = (midX - prevX).toDouble()
                    val dy = (midY - prevY).toDouble()

                    val radYaw = yaw * (PI / 180.0)
                    val radPitch = pitch * (PI / 180.0)

                    // Camera Right Vector (on screen plane)
                    val rightX = cos(radYaw)
                    val rightZ = -sin(radYaw)

                    // Camera Up Vector (on screen plane)
                    val upX = -sin(radPitch) * sin(radYaw)
                    val upY = cos(radPitch)
                    val upZ = -sin(radPitch) * cos(radYaw)

                    val panSpeed = (distance * 0.0015).coerceAtLeast(0.001)
                    targetX -= (rightX * dx - upX * dy) * panSpeed
                    targetY -= (-upY * dy) * panSpeed
                    targetZ -= (rightZ * dx - upZ * dy) * panSpeed

                    prevX = midX
                    prevY = midY
                    prevDistance = currDistance
                } else if (isDragging && event.pointerCount == 1) {
                    // 1-Finger Orbit (Rotate yaw and pitch around target)
                    val dx = (event.x - prevX).toDouble()
                    val dy = (event.y - prevY).toDouble()

                    yaw = (yaw - dx * 0.4) % 360.0
                    pitch = (pitch + dy * 0.4).coerceIn(-85.0, 85.0)

                    prevX = event.x
                    prevY = event.y
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 2) {
                    isPinching = false
                    isDragging = false
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                isPinching = false
            }
        }
        return true
    }

    fun updateCamera(camera: Camera) {
        val radYaw = yaw * (PI / 180.0)
        val radPitch = pitch * (PI / 180.0)

        val eyeX = targetX + distance * cos(radPitch) * sin(radYaw)
        val eyeY = targetY + distance * sin(radPitch)
        val eyeZ = targetZ + distance * cos(radPitch) * cos(radYaw)

        camera.lookAt(eyeX, eyeY, eyeZ, targetX, targetY, targetZ, 0.0, 1.0, 0.0)

        val aspect = view.width.toDouble() / view.height.coerceAtLeast(1)
        camera.setLensProjection(28.0, aspect, 0.05, 2000.0)
    }
}
