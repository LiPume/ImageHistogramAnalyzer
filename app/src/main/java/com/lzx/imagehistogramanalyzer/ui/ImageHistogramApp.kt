package com.lzx.imagehistogramanalyzer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculationStrategy
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerScreen
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerUiState
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerViewModel
import com.lzx.imagehistogramanalyzer.ui.home.HomeScreen

@Composable
fun ImageHistogramApp(viewModel: AnalyzerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.selectImage(uri)
            destination = AppDestination.ANALYZER
        }
    }

    BackHandler(enabled = destination == AppDestination.ANALYZER) {
        destination = AppDestination.HOME
    }

    ImageHistogramContent(
        destination = destination,
        uiState = uiState,
        onPickImage = {
            photoPicker.launch(PickVisualMediaRequest(ImageOnly))
        },
        onOpenAnalyzer = { destination = AppDestination.ANALYZER },
        onBackHome = { destination = AppDestination.HOME },
        onSelectStrategy = viewModel::selectStrategy,
        onCalculate = viewModel::calculateHistogram,
    )
}

@Composable
internal fun ImageHistogramContent(
    destination: AppDestination,
    uiState: AnalyzerUiState,
    onPickImage: () -> Unit,
    onOpenAnalyzer: () -> Unit,
    onBackHome: () -> Unit,
    onSelectStrategy: (HistogramCalculationStrategy) -> Unit,
    onCalculate: () -> Unit,
) {
    when (destination) {
        AppDestination.HOME -> HomeScreen(
            hasSelectedImage = uiState.image != null,
            onPickImage = onPickImage,
            onResumeAnalysis = onOpenAnalyzer,
        )

        AppDestination.ANALYZER -> AnalyzerScreen(
            uiState = uiState,
            onBackHome = onBackHome,
            onPickImage = onPickImage,
            onSelectStrategy = onSelectStrategy,
            onCalculate = onCalculate,
        )
    }
}

internal enum class AppDestination {
    HOME,
    ANALYZER,
}
