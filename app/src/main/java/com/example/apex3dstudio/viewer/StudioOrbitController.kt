package com.example.apex3dstudio.viewer

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.apex3dstudio.state.GestureSensitivity
import com.google.android.filament.Camera
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class StudioOrbitController(
    private val view: View,
    context: Context
) {
    var sensitivity = GestureSensitivity()

    var targetX: Double = 0.0
    var targetY: Double = 0.0
    var targetZ: Double = 0.0

    var distance: Double = 4.0
    var minDistance: Double = 0.1
    var maxDistance: Double = 1000.0

    var yaw: Double = 0.0    // degrees
    var pitch: Double = 15.0 // degrees

    private enum class TouchMode { NONE, ORBIT, MULTI_TOUCH }
    private var touchMode = TouchMode.NONE

    private var previousX = 0f
    private var previousY = 0f
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var hasMovedSignificantly = false

    private var previousPanX = 0f
    private var previousPanY = 0f
    private var multiTouchStartMidX = 0f
    private var multiTouchStartMidY = 0f
    private var isMultiPanActive = false

    private var previousAngleDeg = 0f
    private var multiTouchStartAngleDeg = 0f
    private var isMultiTwistActive = false

    private val scaleDetector: ScaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val rawDelta = detector.scaleFactor - 1.0f
            val adjustedScale = 1.0f + rawDelta * sensitivity.zoom
            zoom(adjustedScale)
            return true
        }
    })

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

    fun orbit(deltaYaw: Float, deltaPitch: Float) {
        yaw = (yaw + deltaYaw) % 360.0
        if (yaw < 0) yaw += 360.0
        pitch = (pitch + deltaPitch).coerceIn(-85.0, 85.0)
    }

    fun zoom(scaleFactor: Float) {
        if (scaleFactor > 0.001f) {
            distance = (distance / scaleFactor).coerceIn(minDistance, maxDistance)
        }
    }

    fun pan(deltaScreenX: Float, deltaScreenY: Float) {
        val h = view.height.coerceAtLeast(1).toFloat()
        // 1-to-1 pixel to world factor based on distance and FOV
        val factor = (distance * 2.0 * kotlin.math.tan((28.0 * PI / 180.0) / 2.0)).toFloat() / h

        val radYaw = (yaw * PI / 180.0).toFloat()
        val radPitch = (pitch * PI / 180.0).toFloat()

        // Right vector on camera view plane
        val rightX = cos(radYaw)
        val rightZ = -sin(radYaw)

        // Up vector on camera view plane
        val upX = -sin(radPitch) * sin(radYaw)
        val upY = cos(radPitch)
        val upZ = -sin(radPitch) * cos(radYaw)

        targetX -= (rightX * deltaScreenX - upX * deltaScreenY) * factor
        targetY -= (-upY * deltaScreenY) * factor
        targetZ -= (rightZ * deltaScreenX - upZ * deltaScreenY) * factor
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchMode = TouchMode.NONE
                previousX = x
                previousY = y
                touchDownX = x
                touchDownY = y
                hasMovedSignificantly = false
                previousPanX = 0f
                previousPanY = 0f
                multiTouchStartMidX = 0f
                multiTouchStartMidY = 0f
                isMultiPanActive = false
                previousAngleDeg = 0f
                multiTouchStartAngleDeg = 0f
                isMultiTwistActive = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                touchMode = TouchMode.MULTI_TOUCH
                if (event.pointerCount >= 2) {
                    val midX = (event.getX(0) + event.getX(1)) / 2f
                    val midY = (event.getY(0) + event.getY(1)) / 2f
                    previousPanX = midX
                    previousPanY = midY
                    multiTouchStartMidX = midX
                    multiTouchStartMidY = midY
                    isMultiPanActive = false

                    val pDx = event.getX(1) - event.getX(0)
                    val pDy = event.getY(1) - event.getY(0)
                    val angle = Math.toDegrees(atan2(pDy.toDouble(), pDx.toDouble())).toFloat()
                    previousAngleDeg = angle
                    multiTouchStartAngleDeg = angle
                    isMultiTwistActive = false
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val moveDist = hypot((x - touchDownX).toDouble(), (y - touchDownY).toDouble()).toFloat()
                if (moveDist > 10f) {
                    hasMovedSignificantly = true
                }

                when (touchMode) {
                    TouchMode.MULTI_TOUCH -> {
                        if (event.pointerCount >= 2) {
                            val midX = (event.getX(0) + event.getX(1)) / 2f
                            val midY = (event.getY(0) + event.getY(1)) / 2f

                            // 1. Two-finger Pan
                            if (!isMultiPanActive) {
                                val panDist = hypot((midX - multiTouchStartMidX).toDouble(), (midY - multiTouchStartMidY).toDouble()).toFloat()
                                if (panDist > 10f) {
                                    isMultiPanActive = true
                                    previousPanX = midX
                                    previousPanY = midY
                                }
                            } else {
                                val panDx = (midX - previousPanX) * sensitivity.pan
                                val panDy = (midY - previousPanY) * sensitivity.pan
                                pan(panDx, panDy)
                                previousPanX = midX
                                previousPanY = midY
                            }

                            // 2. Two-finger Twist (Turntable Orbit)
                            val pDx = event.getX(1) - event.getX(0)
                            val pDy = event.getY(1) - event.getY(0)
                            val currentAngleDeg = Math.toDegrees(atan2(pDy.toDouble(), pDx.toDouble())).toFloat()

                            if (!isMultiTwistActive) {
                                var totalAngleDiff = currentAngleDeg - multiTouchStartAngleDeg
                                if (totalAngleDiff > 180f) totalAngleDiff -= 360f
                                else if (totalAngleDiff < -180f) totalAngleDiff += 360f

                                if (abs(totalAngleDiff) > 2.5f) {
                                    isMultiTwistActive = true
                                    previousAngleDeg = currentAngleDeg
                                }
                            } else {
                                var deltaAngle = currentAngleDeg - previousAngleDeg
                                if (deltaAngle > 180f) deltaAngle -= 360f
                                else if (deltaAngle < -180f) deltaAngle += 360f

                                orbit(deltaAngle * sensitivity.twistOrbit, 0f)
                                previousAngleDeg = currentAngleDeg
                            }
                        }
                    }

                    TouchMode.NONE, TouchMode.ORBIT -> {
                        if (event.pointerCount == 1 && !scaleDetector.isInProgress) {
                            if (hasMovedSignificantly) {
                                touchMode = TouchMode.ORBIT
                                val dx = x - previousX
                                val dy = y - previousY
                                val orbitSpeed = 0.35f * sensitivity.orbit
                                orbit(-dx * orbitSpeed, dy * orbitSpeed)
                            }
                            previousX = x
                            previousY = y
                        }
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (touchMode == TouchMode.MULTI_TOUCH) {
                    isMultiPanActive = false
                    isMultiTwistActive = false
                    previousPanX = 0f
                    previousPanY = 0f
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchMode = TouchMode.NONE
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
