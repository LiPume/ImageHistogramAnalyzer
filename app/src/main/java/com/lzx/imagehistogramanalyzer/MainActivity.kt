package com.lzx.imagehistogramanalyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.lzx.imagehistogramanalyzer.ui.ImageHistogramApp
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerViewModel
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerViewModelFactory
import com.lzx.imagehistogramanalyzer.ui.theme.ImageHistogramAnalyzerTheme

class MainActivity : ComponentActivity() {
    private val analyzerViewModel: AnalyzerViewModel by viewModels {
        AnalyzerViewModelFactory(contentResolver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageHistogramAnalyzerTheme {
                ImageHistogramApp(viewModel = analyzerViewModel)
            }
        }
    }
}
