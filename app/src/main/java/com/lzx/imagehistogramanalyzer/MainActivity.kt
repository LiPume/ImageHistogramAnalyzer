package com.lzx.imagehistogramanalyzer

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.lzx.imagehistogramanalyzer.ui.ImageHistogramApp
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerViewModel
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerViewModelFactory
import com.lzx.imagehistogramanalyzer.ui.camera.CameraViewModel
import com.lzx.imagehistogramanalyzer.ui.theme.ImageHistogramAnalyzerTheme

class MainActivity : ComponentActivity() {
    private val analyzerViewModel: AnalyzerViewModel by viewModels {
        AnalyzerViewModelFactory(contentResolver)
    }
    private val cameraViewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            ImageHistogramAnalyzerTheme {
                ImageHistogramApp(
                    viewModel = analyzerViewModel,
                    cameraViewModel = cameraViewModel,
                )
            }
        }
    }
}
