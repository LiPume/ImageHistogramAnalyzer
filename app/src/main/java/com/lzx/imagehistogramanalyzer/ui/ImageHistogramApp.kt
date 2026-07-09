package com.lzx.imagehistogramanalyzer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculationStrategy
import com.lzx.imagehistogramanalyzer.domain.roi.PreviewImageLayout
import com.lzx.imagehistogramanalyzer.domain.roi.PreviewRect
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerScreen
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerUiState
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerViewModel
import com.lzx.imagehistogramanalyzer.ui.camera.CameraScreen
import com.lzx.imagehistogramanalyzer.ui.camera.CameraUiState
import com.lzx.imagehistogramanalyzer.ui.camera.CameraViewModel
import com.lzx.imagehistogramanalyzer.ui.home.HomeScreen

@Composable
fun ImageHistogramApp(
    viewModel: AnalyzerViewModel,
    cameraViewModel: CameraViewModel,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraUiState by cameraViewModel.uiState.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        handlePhotoPickerResult(
            uri = uri,
            onImageSelected = viewModel::selectImage,
            onOpenAnalyzer = { destination = AppDestination.ANALYZER },
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        cameraViewModel.setCameraPermission(granted)
        if (granted) destination = AppDestination.CAMERA
    }

    LaunchedEffect(destination) {
        if (destination == AppDestination.CAMERA) {
            cameraViewModel.setCameraPermission(context.hasCameraPermission())
        }
    }

    BackHandler(enabled = destination != AppDestination.HOME) {
        destination = AppDestination.HOME
    }

    ImageHistogramContent(
        destination = destination,
        uiState = uiState,
        cameraUiState = cameraUiState,
        onPickImage = {
            photoPicker.launch(PickVisualMediaRequest(ImageOnly))
        },
        onOpenAnalyzer = { destination = AppDestination.ANALYZER },
        onOpenCamera = { destination = AppDestination.CAMERA },
        onBackHome = { destination = AppDestination.HOME },
        onSelectStrategy = viewModel::selectStrategy,
        onCalculate = viewModel::calculateHistogram,
        onStartRoiSelection = viewModel::startRoiSelection,
        onCancelRoiSelection = viewModel::cancelRoiSelection,
        onConfirmRoiSelection = viewModel::confirmRoiSelection,
        onRestoreFullImage = viewModel::restoreFullImageAnalysis,
        onRequestCameraPermission = {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onCameraBindingChanged = cameraViewModel::setBindingCamera,
        onFrameAnalyzed = cameraViewModel::onFrameAnalyzed,
        onCameraError = cameraViewModel::onCameraError,
        onJudgeCurrentFrame = cameraViewModel::judgeCurrentFrame,
    )
}

@Composable
internal fun ImageHistogramContent(
    destination: AppDestination,
    uiState: AnalyzerUiState,
    cameraUiState: CameraUiState,
    onPickImage: () -> Unit,
    onOpenAnalyzer: () -> Unit,
    onOpenCamera: () -> Unit,
    onBackHome: () -> Unit,
    onSelectStrategy: (HistogramCalculationStrategy) -> Unit,
    onCalculate: () -> Unit,
    onStartRoiSelection: () -> Unit,
    onCancelRoiSelection: () -> Unit,
    onConfirmRoiSelection: (PreviewRect, PreviewImageLayout) -> Unit,
    onRestoreFullImage: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onCameraBindingChanged: (Boolean) -> Unit,
    onFrameAnalyzed: (RealtimeCameraAnalysis) -> Unit,
    onCameraError: (String) -> Unit,
    onJudgeCurrentFrame: () -> Unit,
) {
    when (destination) {
        AppDestination.HOME -> HomeScreen(
            hasSelectedImage = uiState.image != null,
            onPickImage = onPickImage,
            onResumeAnalysis = onOpenAnalyzer,
            onOpenCamera = onOpenCamera,
        )

        AppDestination.ANALYZER -> AnalyzerScreen(
            uiState = uiState,
            onBackHome = onBackHome,
            onPickImage = onPickImage,
            onSelectStrategy = onSelectStrategy,
            onCalculate = onCalculate,
            onStartRoiSelection = onStartRoiSelection,
            onCancelRoiSelection = onCancelRoiSelection,
            onConfirmRoiSelection = onConfirmRoiSelection,
            onRestoreFullImage = onRestoreFullImage,
        )

        AppDestination.CAMERA -> CameraScreen(
            uiState = cameraUiState,
            onBackHome = onBackHome,
            onRequestPermission = onRequestCameraPermission,
            onCameraBindingChanged = onCameraBindingChanged,
            onFrameAnalyzed = onFrameAnalyzed,
            onCameraError = onCameraError,
            onJudgeCurrentFrame = onJudgeCurrentFrame,
        )
    }
}

internal enum class AppDestination {
    HOME,
    ANALYZER,
    CAMERA,
}

/** 系统选择器取消时不改变当前页面或已完成的分析结果。 */
internal fun handlePhotoPickerResult(
    uri: Uri?,
    onImageSelected: (Uri) -> Unit,
    onOpenAnalyzer: () -> Unit,
) {
    if (uri == null) return
    onImageSelected(uri)
    onOpenAnalyzer()
}

private fun android.content.Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
