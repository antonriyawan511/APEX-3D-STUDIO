package com.example.apex3dstudio.viewer

import android.view.MotionEvent
import android.view.View
import com.google.android.filament.utils.Manipulator
import kotlin.math.abs
import kotlin.math.hypot

class StudioGestureDetector(
    private val view: View,
    var manipulator: Manipulator
) {
    private enum class State { NONE, ORBIT, PINCH_PAN_ZOOM }
    private var state = State.NONE

    private var prevX = 0
    private var prevY = 0
    private var prevDistance = 0f

    fun onTouchEvent(event: MotionEvent): Boolean {
        val height = view.height.coerceAtLeast(1)
        manipulator.setViewport(view.width.coerceAtLeast(1), height)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x.toInt()
                val y = height - event.y.toInt()
                prevX = x
                prevY = y
                manipulator.grabBegin(x, y, false) // 1-finger orbit grab
                state = State.ORBIT
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    if (state == State.ORBIT) {
                        manipulator.grabEnd()
                    }

                    val x0 = event.getX(0)
                    val y0 = event.getY(0)
                    val x1 = event.getX(1)
                    val y1 = event.getY(1)

                    val midX = ((x0 + x1) / 2f).toInt()
                    val midY = height - ((y0 + y1) / 2f).toInt()

                    prevX = midX
                    prevY = midY
                    prevDistance = hypot(x1 - x0, y1 - y0)
                    state = State.PINCH_PAN_ZOOM
                }
            }

            MotionEvent.ACTION_MOVE -> {
                when (state) {
                    State.ORBIT -> {
                        if (event.pointerCount == 1) {
                            val x = event.x.toInt()
                            val y = height - event.y.toInt()
                            manipulator.grabUpdate(x, y)
                            prevX = x
                            prevY = y
                        }
                    }

                    State.PINCH_PAN_ZOOM -> {
                        if (event.pointerCount >= 2) {
                            val x0 = event.getX(0)
                            val y0 = event.getY(0)
                            val x1 = event.getX(1)
                            val y1 = event.getY(1)

                            val midX = ((x0 + x1) / 2f).toInt()
                            val midY = height - ((y0 + y1) / 2f).toInt()
                            val distance = hypot(x1 - x0, y1 - y0)

                            if (prevDistance > 0f) {
                                // 1. Zoom (Negative delta = Zoom In, Positive = Zoom Out)
                                val deltaZoom = (prevDistance - distance) * 0.08f
                                if (abs(distance - prevDistance) > 0.5f) {
                                    manipulator.scroll(midX, midY, deltaZoom)
                                }

                                // 2. Pan (Simultaneous incremental strafe)
                                if (abs(midX - prevX) > 0 || abs(midY - prevY) > 0) {
                                    manipulator.grabBegin(prevX, prevY, true)
                                    manipulator.grabUpdate(midX, midY)
                                    manipulator.grabEnd()
                                }
                            }

                            prevX = midX
                            prevY = midY
                            prevDistance = distance
                        }
                    }

                    State.NONE -> {}
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (state == State.PINCH_PAN_ZOOM) {
                    state = State.NONE
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (state == State.ORBIT) {
                    manipulator.grabEnd()
                }
                state = State.NONE
            }
        }
        return true
    }
}
