package com.example.videodownaloder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.videodownaloder.ui.download.DownloadScreen
import com.example.videodownaloder.ui.theme.VideodownaloderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideodownaloderTheme {
                DownloadScreen()
            }
        }
    }
}
