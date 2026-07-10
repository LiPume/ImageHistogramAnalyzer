package com.lzx.imagehistogramanalyzer.ui.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.lzx.imagehistogramanalyzer.data.camera.CameraXAdjustmentController
import com.lzx.imagehistogramanalyzer.data.camera.RealtimeFrameAnalyzer
import com.lzx.imagehistogramanalyzer.domain.camera.CameraAdjustmentController
import com.lzx.imagehistogramanalyzer.domain.camera.CameraAdjustmentState
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    uiState: CameraUiState,
    onBackHome: () -> Unit,
    onRequestPermission: () -> Unit,
    onCameraBindingChanged: (Boolean) -> Unit,
    onCameraAdjustmentControllerChanged: (CameraAdjustmentController?) -> Unit,
    onFrameAnalyzed: (RealtimeCameraAnalysis) -> Unit,
    onCameraError: (String) -> Unit,
    onJudgeCurrentFrame: () -> Unit,
    onDecreaseExposure: () -> Unit,
    onIncreaseExposure: () -> Unit,
    onToggleTorch: () -> Unit,
    onApplySuggestedAdjustment: () -> Unit,
    onFreezePreviewFrame: (Bitmap) -> Unit,
    onResumeRealtimePreview: () -> Unit,
    onSaveFrozenFrame: () -> Unit,
    modifier: Modifier = Modifier,
    previewContent: @Composable (Int) -> Unit = { freezeRequestId ->
        RealtimeCameraPreview(
            freezeRequestId = freezeRequestId,
            onCameraBindingChanged = onCameraBindingChanged,
            onCameraAdjustmentControllerChanged = onCameraAdjustmentControllerChanged,
            onFrameAnalyzed = onFrameAnalyzed,
            onCameraError = onCameraError,
            onPreviewFrozen = onFreezePreviewFrame,
        )
    },
) {
    var freezeRequestId by remember { mutableStateOf(0) }
    var controlsExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.retakeRequestId) {
        if (uiState.retakeRequestId > 0) {
            delay(RETAKE_CAPTURE_DELAY_MS)
            freezeRequestId += 1
        }
    }

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
                val analysis = uiState.frozenFrame?.analysis ?: uiState.latestAnalysis
                item {
                    CameraPreviewCard(
                        isBindingCamera = uiState.isBindingCamera,
                        frameAnalysis = analysis,
                        frozenFrame = uiState.frozenFrame,
                        adjustmentState = uiState.adjustmentState,
                        coachResult = uiState.coachResult,
                        isSavingFrozenFrame = uiState.isSavingFrozenFrame,
                        snapshotMessage = uiState.snapshotMessage,
                        onDecreaseExposure = onDecreaseExposure,
                        onIncreaseExposure = onIncreaseExposure,
                        onToggleTorch = onToggleTorch,
                        onApplySuggestedAdjustment = onApplySuggestedAdjustment,
                        onFreezePreviewRequest = { freezeRequestId += 1 },
                        onResumeRealtimePreview = onResumeRealtimePreview,
                        onSaveFrozenFrame = onSaveFrozenFrame,
                        controlsExpanded = controlsExpanded,
                        onControlsExpandedChange = { controlsExpanded = it },
                        previewContent = { previewContent(freezeRequestId) },
                    )
                }
                uiState.errorMessage?.let { message ->
                    item {
                        CameraErrorCard(message)
                    }
                }
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
    freezeRequestId: Int,
    onCameraBindingChanged: (Boolean) -> Unit,
    onCameraAdjustmentControllerChanged: (CameraAdjustmentController?) -> Unit,
    onFrameAnalyzed: (RealtimeCameraAnalysis) -> Unit,
    onCameraError: (String) -> Unit,
    onPreviewFrozen: (Bitmap) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnFrameAnalyzed by rememberUpdatedState(onFrameAnalyzed)
    val latestOnCameraError by rememberUpdatedState(onCameraError)
    val latestOnBindingChanged by rememberUpdatedState(onCameraBindingChanged)
    val latestOnControllerChanged by rememberUpdatedState(onCameraAdjustmentControllerChanged)
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
    }

    LaunchedEffect(freezeRequestId, previewView) {
        if (freezeRequestId > 0) {
            capturePreviewBitmap(
                previewView = previewView,
                onPreviewFrozen = latestOnPreviewFrozen,
                onCameraError = latestOnCameraError,
            )
        }
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
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                    latestOnControllerChanged(CameraXAdjustmentController(camera))
                    latestOnBindingChanged(false)
                } catch (error: Throwable) {
                    latestOnBindingChanged(false)
                    latestOnControllerChanged(null)
                    latestOnCameraError(error.toCameraUserMessage())
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
            latestOnControllerChanged(null)
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
    frameAnalysis: RealtimeCameraAnalysis?,
    frozenFrame: FrozenCameraFrame?,
    adjustmentState: CameraAdjustmentState,
    coachResult: PhotoCoachResult?,
    isSavingFrozenFrame: Boolean,
    snapshotMessage: String?,
    onDecreaseExposure: () -> Unit,
    onIncreaseExposure: () -> Unit,
    onToggleTorch: () -> Unit,
    onApplySuggestedAdjustment: () -> Unit,
    onFreezePreviewRequest: () -> Unit,
    onResumeRealtimePreview: () -> Unit,
    onSaveFrozenFrame: () -> Unit,
    controlsExpanded: Boolean,
    onControlsExpandedChange: (Boolean) -> Unit,
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
                if (frozenFrame != null) {
                    FrozenPreviewImage(frozenFrame)
                }
                frameAnalysis?.let { analysis ->
                    CameraFrameStatusBadge(
                        status = analysis.toFrameStatus(
                            coachResult = coachResult.takeIf { frozenFrame != null },
                        ),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(AppSpacing.small),
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
            CameraAdjustmentPanel(
                isFrozen = frozenFrame != null,
                expanded = controlsExpanded,
                adjustmentState = adjustmentState,
                coachResult = coachResult,
                onFreezePreviewRequest = onFreezePreviewRequest,
                onDecreaseExposure = onDecreaseExposure,
                onIncreaseExposure = onIncreaseExposure,
                onToggleTorch = onToggleTorch,
                onApplySuggestedAdjustment = onApplySuggestedAdjustment,
                onExpandedChange = onControlsExpandedChange,
            )
            frozenFrame?.let {
                FrozenFrameActions(
                    snapshotMessage = snapshotMessage,
                    isSavingFrozenFrame = isSavingFrozenFrame,
                    hasSavedFrame = it.savedUri != null,
                    onResumeRealtimePreview = onResumeRealtimePreview,
                    onSaveFrozenFrame = onSaveFrozenFrame,
                )
            }
        }
    }
}

@Composable
private fun CameraAdjustmentPanel(
    isFrozen: Boolean,
    expanded: Boolean,
    adjustmentState: CameraAdjustmentState,
    coachResult: PhotoCoachResult?,
    onFreezePreviewRequest: () -> Unit,
    onDecreaseExposure: () -> Unit,
    onIncreaseExposure: () -> Unit,
    onToggleTorch: () -> Unit,
    onApplySuggestedAdjustment: () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
) {
    val hasActionableSuggestion = coachResult?.hasActionableCameraSuggestion() == true
    val hasSupportedSuggestion = coachResult?.hasSupportedCameraSuggestion(adjustmentState) == true
    val retakeActionText = retakeActionText(coachResult)
    val canRetakeWithSuggestion = isFrozen &&
        hasSupportedSuggestion &&
        !adjustmentState.isAdjusting
    val helperText = when {
        !isFrozen -> stringResource(R.string.camera_retake_live_hint)
        coachResult == null -> stringResource(R.string.camera_retake_waiting_hint)
        coachResult.isGoodFrame() -> stringResource(R.string.camera_retake_not_needed_hint)
        !hasActionableSuggestion -> stringResource(R.string.camera_retake_review_advice_hint)
        !hasSupportedSuggestion -> stringResource(R.string.camera_retake_unavailable_hint)
        else -> stringResource(R.string.camera_retake_ready_hint)
    }
    val compactStatusText = when {
        !isFrozen -> exposureStatusText(adjustmentState)
        coachResult?.isGoodFrame() == true -> stringResource(R.string.camera_retake_good_compact_hint)
        coachResult == null -> stringResource(R.string.camera_retake_waiting_hint)
        coachResult.toFrameStatus() == CameraFrameStatus.BAD ->
            stringResource(R.string.camera_retake_bad_compact_hint)
        else -> stringResource(R.string.camera_retake_compact_hint)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(AppSpacing.small),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall),
        ) {
            if (!expanded) {
                CompactCameraControlRow(
                    isFrozen = isFrozen,
                    canRetakeWithSuggestion = canRetakeWithSuggestion,
                    isAdjusting = adjustmentState.isAdjusting,
                    statusText = compactStatusText,
                    retakeActionText = retakeActionText,
                    onFreezePreviewRequest = onFreezePreviewRequest,
                    onApplySuggestedAdjustment = onApplySuggestedAdjustment,
                    onExpand = { onExpandedChange(true) },
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.camera_control_title),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    TextButton(onClick = { onExpandedChange(false) }) {
                        Text(stringResource(R.string.camera_control_collapse))
                    }
                }
                AssistChip(
                    onClick = {},
                    label = { Text(exposureStatusText(adjustmentState)) },
                )
                adjustmentState.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = onFreezePreviewRequest,
                        enabled = !isFrozen && !adjustmentState.isAdjusting,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.freeze_camera_frame))
                    }
                    Button(
                        onClick = onApplySuggestedAdjustment,
                        enabled = canRetakeWithSuggestion,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(retakeActionText)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = onDecreaseExposure,
                        enabled = adjustmentState.canDecreaseExposure,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.camera_exposure_down))
                    }
                    OutlinedButton(
                        onClick = onIncreaseExposure,
                        enabled = adjustmentState.canIncreaseExposure,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.camera_exposure_up))
                    }
                }
                OutlinedButton(
                    onClick = onToggleTorch,
                    enabled = adjustmentState.canToggleTorch,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (!adjustmentState.hasFlashUnit) {
                            stringResource(R.string.camera_torch_unavailable)
                        } else if (adjustmentState.isTorchOn) {
                            stringResource(R.string.camera_torch_off)
                        } else {
                            stringResource(R.string.camera_torch_on)
                        },
                    )
                }
                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CompactCameraControlRow(
    isFrozen: Boolean,
    canRetakeWithSuggestion: Boolean,
    isAdjusting: Boolean,
    statusText: String,
    retakeActionText: String,
    onFreezePreviewRequest: () -> Unit,
    onApplySuggestedAdjustment: () -> Unit,
    onExpand: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.camera_control_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isFrozen) {
            Button(
                onClick = onApplySuggestedAdjustment,
                enabled = canRetakeWithSuggestion,
            ) {
                Text(retakeActionText)
            }
        } else {
            OutlinedButton(
                onClick = onFreezePreviewRequest,
                enabled = !isAdjusting,
            ) {
                Text(stringResource(R.string.freeze_camera_frame))
            }
        }
        TextButton(onClick = onExpand) {
            Text(stringResource(R.string.camera_control_expand))
        }
    }
}

@Composable
private fun CameraFrameStatusBadge(
    status: CameraFrameStatus,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(status.labelRes)
    val description = stringResource(R.string.camera_frame_status_description, label)
    Surface(
        modifier = modifier.semantics {
            contentDescription = description
        },
        shape = MaterialTheme.shapes.small,
        color = when (status) {
            CameraFrameStatus.GOOD -> MaterialTheme.colorScheme.secondaryContainer
            CameraFrameStatus.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
            CameraFrameStatus.BAD -> MaterialTheme.colorScheme.errorContainer
        },
        contentColor = when (status) {
            CameraFrameStatus.GOOD -> MaterialTheme.colorScheme.onSecondaryContainer
            CameraFrameStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
            CameraFrameStatus.BAD -> MaterialTheme.colorScheme.onErrorContainer
        },
        tonalElevation = 2.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(
                horizontal = AppSpacing.small,
                vertical = AppSpacing.extraSmall,
            ),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun exposureStatusText(adjustmentState: CameraAdjustmentState): String =
    if (adjustmentState.isExposureSupported) {
        stringResource(
            R.string.camera_exposure_status,
            adjustmentState.exposureIndex,
            adjustmentState.minExposureIndex,
            adjustmentState.maxExposureIndex,
        )
    } else {
        stringResource(R.string.camera_exposure_unsupported)
    }

@Composable
private fun FrozenPreviewImage(
    frozenFrame: FrozenCameraFrame,
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
            onClick = {},
            label = { Text(stringResource(R.string.frozen_camera_frame_badge)) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(AppSpacing.small),
        )
    }
}

@Composable
private fun FrozenFrameActions(
    isSavingFrozenFrame: Boolean,
    hasSavedFrame: Boolean,
    snapshotMessage: String?,
    onResumeRealtimePreview: () -> Unit,
    onSaveFrozenFrame: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
    ) {
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
        if (hasSavedFrame) {
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

private enum class CameraFrameStatus(val labelRes: Int) {
    GOOD(R.string.camera_frame_status_good),
    WARNING(R.string.camera_frame_status_warning),
    BAD(R.string.camera_frame_status_bad),
}

private fun RealtimeCameraAnalysis.toFrameStatus(
    coachResult: PhotoCoachResult?,
): CameraFrameStatus =
    coachResult?.toFrameStatus() ?: when (qualityResult.category) {
        ImageQualityCategory.NORMAL -> CameraFrameStatus.GOOD
        ImageQualityCategory.LOW_CONTRAST -> CameraFrameStatus.WARNING
        ImageQualityCategory.DARK,
        ImageQualityCategory.BRIGHT,
        -> CameraFrameStatus.BAD
    }

private fun PhotoCoachResult.toFrameStatus(): CameraFrameStatus = when (sceneStatus) {
    PhotoSceneStatus.NORMAL -> CameraFrameStatus.GOOD
    PhotoSceneStatus.SLIGHTLY_DARK,
    PhotoSceneStatus.SLIGHTLY_BRIGHT,
    PhotoSceneStatus.LOW_CONTRAST,
    -> CameraFrameStatus.WARNING

    PhotoSceneStatus.SEVERE_UNDEREXPOSED,
    PhotoSceneStatus.DARK,
    PhotoSceneStatus.SEVERE_OVEREXPOSED,
    PhotoSceneStatus.BRIGHT,
    -> CameraFrameStatus.BAD
}

private fun PhotoCoachResult.isGoodFrame(): Boolean =
    sceneStatus == PhotoSceneStatus.NORMAL && !hasActionableCameraSuggestion()

private fun PhotoCoachResult.hasActionableCameraSuggestion(): Boolean =
    exposureDelta != 0 || torchAction != TorchAction.KEEP

private fun PhotoCoachResult.hasSupportedCameraSuggestion(
    adjustmentState: CameraAdjustmentState,
): Boolean =
    (exposureDelta != 0 && adjustmentState.isExposureSupported) ||
        (torchAction != TorchAction.KEEP && adjustmentState.hasFlashUnit)

@Composable
private fun retakeActionText(coachResult: PhotoCoachResult?): String = when {
    coachResult?.isGoodFrame() == true -> stringResource(R.string.camera_retake_not_needed)
    coachResult != null && !coachResult.hasActionableCameraSuggestion() ->
        stringResource(R.string.camera_retake_review_advice)
    else -> stringResource(R.string.camera_retake_with_suggestion)
}

private fun Throwable.toCameraUserMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "相机启动或实时分析失败，请返回后重试"

internal const val CAMERA_SCREEN_LIST_TAG = "camera_screen_list"
private const val RETAKE_CAPTURE_DELAY_MS = 700L

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
