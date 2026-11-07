package com.example.apex3dstudio.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.apex3dstudio.ui.theme.APEX3DSTUDIOTheme
import com.google.android.filament.utils.Utils

class StudioActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Utils.init()

        setContent {
            APEX3DSTUDIOTheme {
                StudioScreen()
            }
        }
    }
}
