package com.lzx.imagehistogramanalyzer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerScreen
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerViewModel

@Composable
fun ImageHistogramApp(viewModel: AnalyzerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) viewModel.selectImage(uri)
    }

    AnalyzerScreen(
        uiState = uiState,
        onPickImage = {
            photoPicker.launch(PickVisualMediaRequest(ImageOnly))
        },
        onSelectStrategy = viewModel::selectStrategy,
        onCalculate = viewModel::calculateHistogram,
    )
}
