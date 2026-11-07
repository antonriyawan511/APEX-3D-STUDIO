package com.example.apex3dstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.apex3dstudio.ui.StudioScreen
import com.example.apex3dstudio.ui.theme.APEX3DSTUDIOTheme
import com.google.android.filament.utils.Utils

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.init()

        setContent {
            APEX3DSTUDIOTheme {
                StudioScreen()
            }
        }
    }
}
