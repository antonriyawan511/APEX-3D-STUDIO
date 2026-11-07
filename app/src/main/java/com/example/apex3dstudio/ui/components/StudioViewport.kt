package com.example.apex3dstudio.ui.components

import android.annotation.SuppressLint
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.apex3dstudio.core.FilamentEngine
import com.example.apex3dstudio.ui.theme.StudioBlack

@SuppressLint("ClickableViewAccessibility")
@Composable
fun StudioViewport(
    engine: FilamentEngine,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBlack)
    ) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    engine.attachSurfaceView(this)
                    setOnTouchListener { _, event ->
                        engine.handleTouchEvent(event)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        DisposableEffect(Unit) {
            onDispose {
                engine.detachSurfaceView()
            }
        }
    }
}
