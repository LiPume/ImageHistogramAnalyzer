package com.lzx.imagehistogramanalyzer.ui.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeFrameSource
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityCategory
import com.lzx.imagehistogramanalyzer.domain.photo.PhotoCoachResult
import com.lzx.imagehistogramanalyzer.domain.photo.PhotoSceneStatus
import com.lzx.imagehistogramanalyzer.domain.photo.TorchAction
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
    onJudgeCurrentFrame: () -> Unit,
    onFreezePreviewFrame: (Bitmap) -> Unit,
    onResumeRealtimePreview: () -> Unit,
    onSaveFrozenFrame: () -> Unit,
    modifier: Modifier = Modifier,
    previewContent: @Composable () -> Unit = {
        RealtimeCameraPreview(
            onCameraBindingChanged = onCameraBindingChanged,
            onFrameAnalyzed = onFrameAnalyzed,
            onCameraError = onCameraError,
            onPreviewFrozen = onFreezePreviewFrame,
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
                .padding(innerPadding)
                .testTag(CAMERA_SCREEN_LIST_TAG),
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
                        frozenFrame = uiState.frozenFrame,
                        isSavingFrozenFrame = uiState.isSavingFrozenFrame,
                        snapshotMessage = uiState.snapshotMessage,
                        onResumeRealtimePreview = onResumeRealtimePreview,
                        onSaveFrozenFrame = onSaveFrozenFrame,
                        previewContent = previewContent,
                    )
                }
                uiState.errorMessage?.let { message ->
                    item {
                        CameraErrorCard(message)
                    }
                }
                val analysis = uiState.frozenFrame?.analysis ?: uiState.latestAnalysis
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
                    if (uiState.frozenFrame != null || uiState.coachResult != null) item {
                        RealtimePhotoCoachCard(
                            coachResult = uiState.coachResult,
                            onJudgeCurrentFrame = onJudgeCurrentFrame,
                        )
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
    onPreviewFrozen: (Bitmap) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnFrameAnalyzed by rememberUpdatedState(onFrameAnalyzed)
    val latestOnCameraError by rememberUpdatedState(onCameraError)
    val latestOnBindingChanged by rememberUpdatedState(onCameraBindingChanged)
    val latestOnPreviewFrozen by rememberUpdatedState(onPreviewFrozen)
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(MaterialTheme.shapes.large),
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "实时相机预览，点击可定格当前画面"
                },
            factory = { viewContext ->
                PreviewView(viewContext).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    setOnClickListener {
                        capturePreviewBitmap(
                            previewView = this,
                            onPreviewFrozen = latestOnPreviewFrozen,
                            onCameraError = latestOnCameraError,
                        )
                    }
                    previewView = this
                }
            },
        )
        Button(
            onClick = {
                capturePreviewBitmap(
                    previewView = previewView,
                    onPreviewFrozen = latestOnPreviewFrozen,
                    onCameraError = latestOnCameraError,
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppSpacing.small),
        ) {
            Text(stringResource(R.string.freeze_camera_frame))
        }
        AssistChip(
            onClick = {
                capturePreviewBitmap(
                    previewView = previewView,
                    onPreviewFrozen = latestOnPreviewFrozen,
                    onCameraError = latestOnCameraError,
                )
            },
            label = { Text(stringResource(R.string.freeze_camera_hint)) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(AppSpacing.small),
        )
    }

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
    frozenFrame: FrozenCameraFrame?,
    isSavingFrozenFrame: Boolean,
    snapshotMessage: String?,
    onResumeRealtimePreview: () -> Unit,
    onSaveFrozenFrame: () -> Unit,
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
                if (frozenFrame == null) {
                    previewContent()
                } else {
                    FrozenPreviewContent(
                        frozenFrame = frozenFrame,
                        isSavingFrozenFrame = isSavingFrozenFrame,
                        snapshotMessage = snapshotMessage,
                        onResumeRealtimePreview = onResumeRealtimePreview,
                        onSaveFrozenFrame = onSaveFrozenFrame,
                    )
                }
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
private fun FrozenPreviewContent(
    frozenFrame: FrozenCameraFrame,
    isSavingFrozenFrame: Boolean,
    snapshotMessage: String?,
    onResumeRealtimePreview: () -> Unit,
    onSaveFrozenFrame: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(MaterialTheme.shapes.large),
        ) {
            Image(
                bitmap = frozenFrame.bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.frozen_camera_frame_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            AssistChip(
                onClick = onResumeRealtimePreview,
                label = { Text(stringResource(R.string.frozen_camera_frame_badge)) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(AppSpacing.small),
            )
        }
        Text(
            text = snapshotMessage ?: stringResource(R.string.frozen_camera_frame_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = onSaveFrozenFrame,
                enabled = !isSavingFrozenFrame,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    if (isSavingFrozenFrame) {
                        stringResource(R.string.saving_frozen_frame)
                    } else {
                        stringResource(R.string.save_frozen_frame)
                    },
                )
            }
            OutlinedButton(
                onClick = onResumeRealtimePreview,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.resume_realtime_preview))
            }
        }
        frozenFrame.savedUri?.let {
            Text(
                text = stringResource(R.string.frozen_frame_saved),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
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
                text = if (analysis.source == RealtimeFrameSource.PREVIEW_BITMAP) {
                    stringResource(R.string.frozen_histogram_title)
                } else {
                    stringResource(R.string.realtime_histogram_title)
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = if (analysis.source == RealtimeFrameSource.PREVIEW_BITMAP) {
                    stringResource(R.string.frozen_histogram_description)
                } else {
                    stringResource(R.string.realtime_histogram_description)
                },
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
                value = when (analysis.source) {
                    RealtimeFrameSource.Y_PLANE -> stringResource(R.string.camera_frame_source_y_plane)
                    RealtimeFrameSource.PREVIEW_BITMAP -> stringResource(R.string.camera_frame_source_preview_snapshot)
                },
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
private fun RealtimePhotoCoachCard(
    coachResult: PhotoCoachResult?,
    onJudgeCurrentFrame: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
        ) {
            Text(
                text = stringResource(R.string.photo_coach_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.photo_coach_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            if (coachResult == null) {
                Button(
                    onClick = onJudgeCurrentFrame,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.photo_coach_judge_current_frame))
                }
                Text(
                    text = stringResource(R.string.photo_coach_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            } else {
                CameraMetricRow(
                    label = stringResource(R.string.photo_coach_scene_status),
                    value = coachResult.sceneStatus.toDisplayText(),
                )
                CameraMetricRow(
                    label = stringResource(R.string.photo_coach_exposure_delta),
                    value = coachResult.exposureDelta.toExposureDeltaText(),
                )
                CameraMetricRow(
                    label = stringResource(R.string.photo_coach_torch_action),
                    value = coachResult.torchAction.toDisplayText(),
                )
                Text(
                    text = "${stringResource(R.string.photo_coach_reason)}：${coachResult.reason}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = "${stringResource(R.string.photo_coach_advice)}：${coachResult.advice}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
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

private fun PhotoSceneStatus.toDisplayText(): String = when (this) {
    PhotoSceneStatus.SEVERE_UNDEREXPOSED -> "严重欠曝"
    PhotoSceneStatus.DARK -> "偏暗"
    PhotoSceneStatus.SLIGHTLY_DARK -> "轻微偏暗"
    PhotoSceneStatus.SEVERE_OVEREXPOSED -> "严重过曝"
    PhotoSceneStatus.BRIGHT -> "偏亮"
    PhotoSceneStatus.SLIGHTLY_BRIGHT -> "轻微偏亮"
    PhotoSceneStatus.LOW_CONTRAST -> "低对比度"
    PhotoSceneStatus.NORMAL -> "正常"
}

private fun TorchAction.toDisplayText(): String = when (this) {
    TorchAction.KEEP -> "保持不变"
    TorchAction.TURN_ON -> "建议后续开启"
    TorchAction.TURN_OFF -> "建议关闭"
}

private fun Int.toExposureDeltaText(): String = when {
    this > 0 -> "+$this 档"
    this < 0 -> "$this 档"
    else -> "保持不变"
}

private fun Throwable.toCameraUserMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "相机启动或实时分析失败，请返回后重试"

internal const val CAMERA_SCREEN_LIST_TAG = "camera_screen_list"

private fun capturePreviewBitmap(
    previewView: PreviewView?,
    onPreviewFrozen: (Bitmap) -> Unit,
    onCameraError: (String) -> Unit,
) {
    val previewBitmap = previewView?.bitmap
    if (previewBitmap == null) {
        onCameraError("暂时无法定格预览画面，请等待相机画面稳定后再试")
        return
    }
    onPreviewFrozen(previewBitmap)
}
