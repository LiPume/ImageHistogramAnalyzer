package com.lzx.imagehistogramanalyzer.ui.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lzx.imagehistogramanalyzer.R
import com.lzx.imagehistogramanalyzer.data.camera.RealtimeFrameAnalyzer
import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityCategory
import com.lzx.imagehistogramanalyzer.ui.component.HistogramCanvas
import com.lzx.imagehistogramanalyzer.ui.theme.AppSpacing
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    uiState: CameraUiState,
    onBackHome: () -> Unit,
    onRequestPermission: () -> Unit,
    onCameraBindingChanged: (Boolean) -> Unit,
    onFrameAnalyzed: (RealtimeCameraAnalysis) -> Unit,
    onCameraError: (String) -> Unit,
    modifier: Modifier = Modifier,
    previewContent: @Composable () -> Unit = {
        RealtimeCameraPreview(
            onCameraBindingChanged = onCameraBindingChanged,
            onFrameAnalyzed = onFrameAnalyzed,
            onCameraError = onCameraError,
        )
    },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.camera_title)) },
                navigationIcon = {
                    TextButton(onClick = onBackHome) {
                        Text(stringResource(R.string.back_home))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = AppSpacing.medium,
                vertical = AppSpacing.small,
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            item {
                CameraIntroCard()
            }

            if (!uiState.hasCameraPermission) {
                item {
                    CameraPermissionCard(onRequestPermission = onRequestPermission)
                }
            } else {
                item {
                    CameraPreviewCard(
                        isBindingCamera = uiState.isBindingCamera,
                        previewContent = previewContent,
                    )
                }
                uiState.errorMessage?.let { message ->
                    item {
                        CameraErrorCard(message)
                    }
                }
                val analysis = uiState.latestAnalysis
                if (analysis == null) {
                    item {
                        WaitingForFrameCard()
                    }
                } else {
                    item {
                        RealtimeHistogramCard(analysis)
                    }
                    item {
                        RealtimeQualityCard(analysis)
                    }
                }
            }
        }
    }
}

@Composable
private fun RealtimeCameraPreview(
    onCameraBindingChanged: (Boolean) -> Unit,
    onFrameAnalyzed: (RealtimeCameraAnalysis) -> Unit,
    onCameraError: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnFrameAnalyzed by rememberUpdatedState(onFrameAnalyzed)
    val latestOnCameraError by rememberUpdatedState(onCameraError)
    val latestOnBindingChanged by rememberUpdatedState(onCameraBindingChanged)
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(MaterialTheme.shapes.large),
        factory = { viewContext ->
            PreviewView(viewContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                previewView = this
            }
        },
    )

    DisposableEffect(context, lifecycleOwner, previewView) {
        val currentPreviewView = previewView ?: return@DisposableEffect onDispose {}
        val cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        latestOnBindingChanged(true)

        cameraProviderFuture.addListener(
            {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { previewUseCase ->
                        previewUseCase.setSurfaceProvider(currentPreviewView.surfaceProvider)
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysisUseCase ->
                            analysisUseCase.setAnalyzer(
                                cameraExecutor,
                                RealtimeFrameAnalyzer(
                                    onAnalyzed = latestOnFrameAnalyzed,
                                    onError = { error ->
                                        latestOnCameraError(error.toCameraUserMessage())
                                    },
                                ),
                            )
                        }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                    latestOnBindingChanged(false)
                } catch (error: Throwable) {
                    latestOnBindingChanged(false)
                    latestOnCameraError(error.toCameraUserMessage())
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
            cameraExecutor.shutdown()
        }
    }
}

@Composable
private fun CameraIntroCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall),
        ) {
            Text(
                text = stringResource(R.string.camera_intro_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.camera_intro_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun CameraPermissionCard(onRequestPermission: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            Text(
                text = stringResource(R.string.camera_permission_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.camera_permission_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.request_camera_permission))
            }
        }
    }
}

@Composable
private fun CameraPreviewCard(
    isBindingCamera: Boolean,
    previewContent: @Composable () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
        ) {
            Text(
                text = stringResource(R.string.camera_preview_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                previewContent()
                if (isBindingCamera) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitingForFrameCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.waiting_camera_frame),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CameraErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall),
        ) {
            Text(
                text = stringResource(R.string.camera_error_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun RealtimeHistogramCard(analysis: RealtimeCameraAnalysis) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
        ) {
            Text(
                text = stringResource(R.string.realtime_histogram_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.realtime_histogram_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HistogramCanvas(analysis.histogram.normalizedHeights)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("0", style = MaterialTheme.typography.labelSmall)
                Text("128", style = MaterialTheme.typography.labelSmall)
                Text("255", style = MaterialTheme.typography.labelSmall)
            }
            CameraMetricRow(
                label = stringResource(R.string.camera_frame_size),
                value = "${analysis.frameWidth} × ${analysis.frameHeight}",
            )
            CameraMetricRow(
                label = stringResource(R.string.camera_frame_source),
                value = stringResource(R.string.camera_frame_source_y_plane),
            )
            CameraMetricRow(
                label = stringResource(R.string.histogram_max_count_label),
                value = analysis.histogram.maxCount.toString(),
            )
        }
    }
}

@Composable
private fun RealtimeQualityCard(analysis: RealtimeCameraAnalysis) {
    val quality = analysis.qualityResult
    val darkPercent = (quality.darkRatio * 100).roundToInt()
    val brightPercent = (quality.brightRatio * 100).roundToInt()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
        ) {
            Text(
                text = stringResource(R.string.realtime_quality_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.realtime_quality_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            LinearProgressIndicator(
                progress = { quality.meanGray.toFloat() / 255f },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "实时平均亮度 ${"%.1f".format(quality.meanGray)}"
                    },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            )
            CameraMetricRow(
                label = stringResource(R.string.quality_category),
                value = quality.category.toDisplayText(),
            )
            CameraMetricRow(
                label = stringResource(R.string.mean_gray),
                value = "%.2f".format(quality.meanGray),
            )
            CameraMetricRow(
                label = stringResource(R.string.dark_ratio),
                value = "$darkPercent%",
            )
            CameraMetricRow(
                label = stringResource(R.string.bright_ratio),
                value = "$brightPercent%",
            )
            CameraMetricRow(
                label = stringResource(R.string.gray_standard_deviation),
                value = "%.2f".format(quality.standardDeviation),
            )
        }
    }
}

@Composable
private fun CameraMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

private fun ImageQualityCategory.toDisplayText(): String = when (this) {
    ImageQualityCategory.DARK -> "偏暗"
    ImageQualityCategory.BRIGHT -> "偏亮"
    ImageQualityCategory.LOW_CONTRAST -> "低对比度"
    ImageQualityCategory.NORMAL -> "正常"
}

private fun Throwable.toCameraUserMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "相机启动或实时分析失败，请返回后重试"
