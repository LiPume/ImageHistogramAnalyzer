package com.lzx.imagehistogramanalyzer.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lzx.imagehistogramanalyzer.R
import com.lzx.imagehistogramanalyzer.domain.model.ImageMetadata
import com.lzx.imagehistogramanalyzer.domain.roi.AnalysisTargetInfo
import com.lzx.imagehistogramanalyzer.domain.roi.AnalysisTargetType
import com.lzx.imagehistogramanalyzer.domain.roi.PreviewImageLayout
import com.lzx.imagehistogramanalyzer.domain.roi.PreviewRect
import com.lzx.imagehistogramanalyzer.ui.theme.AppSpacing
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun ImagePreviewCard(
    bitmap: Bitmap,
    metadata: ImageMetadata,
    modifier: Modifier = Modifier,
    analysisTargetInfo: AnalysisTargetInfo? = null,
    isRoiSelectionMode: Boolean = false,
    isProcessing: Boolean = false,
    canRestoreFullImage: Boolean = false,
    canConfirmRoi: Boolean = false,
    onStartRoiSelection: () -> Unit = {},
    onCancelRoiSelection: () -> Unit = {},
    onConfirmRoiSelection: (PreviewRect, PreviewImageLayout) -> Unit = { _, _ -> },
    onRestoreFullImage: () -> Unit = {},
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val pixelCount = remember(metadata.pixelCount) {
        NumberFormat.getIntegerInstance().format(metadata.pixelCount)
    }
    var previewSize by remember(bitmap) { mutableStateOf(IntSize.Zero) }
    var dragStart by remember(bitmap, isRoiSelectionMode) { mutableStateOf<Offset?>(null) }
    var dragEnd by remember(bitmap, isRoiSelectionMode) { mutableStateOf<Offset?>(null) }
    val previewRect = remember(dragStart, dragEnd) {
        val start = dragStart
        val end = dragEnd
        if (start != null && end != null && abs(start.x - end.x) >= 2f && abs(start.y - end.y) >= 2f) {
            PreviewRect(start.x, start.y, end.x, end.y)
        } else {
            null
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
        ) {
            Text(
                text = stringResource(R.string.image_preview_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = stringResource(R.string.selected_image_description),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 320.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .onSizeChanged { previewSize = it },
                    contentScale = ContentScale.Fit,
                )
                if (isRoiSelectionMode) {
                    RoiSelectionOverlay(
                        previewSize = previewSize,
                        previewRect = previewRect,
                        onDragStart = { offset ->
                            val coerced = offset.coerceIn(previewSize)
                            dragStart = coerced
                            dragEnd = coerced
                        },
                        onDrag = { delta ->
                            val next = (dragEnd ?: dragStart ?: Offset.Zero) + delta
                            dragEnd = next.coerceIn(previewSize)
                        },
                        modifier = Modifier
                            .matchParentSize()
                            .clip(MaterialTheme.shapes.medium)
                            .testTag(ROI_PREVIEW_TEST_TAG),
                    )
                }
            }
            RoiActionArea(
                isRoiSelectionMode = isRoiSelectionMode,
                isProcessing = isProcessing,
                canRestoreFullImage = canRestoreFullImage,
                canConfirmRoi = canConfirmRoi,
                hasPreviewRect = previewRect != null && previewSize.width > 0 && previewSize.height > 0,
                onStartRoiSelection = onStartRoiSelection,
                onCancelRoiSelection = onCancelRoiSelection,
                onConfirmRoiSelection = confirm@{
                    val rect = previewRect ?: return@confirm
                    onConfirmRoiSelection(
                        rect,
                        PreviewImageLayout(
                            bitmapWidth = bitmap.width,
                            bitmapHeight = bitmap.height,
                            containerWidth = previewSize.width.toFloat(),
                            containerHeight = previewSize.height.toFloat(),
                        ),
                    )
                },
                onRestoreFullImage = onRestoreFullImage,
            )
            analysisTargetInfo?.let { info ->
                AnalysisTargetInfoPanel(info = info)
            }
            Text(metadata.displayName, style = MaterialTheme.typography.bodyLarge)
            MetadataRow(
                label = stringResource(R.string.image_resolution),
                value = "${metadata.width} × ${metadata.height}",
            )
            MetadataRow(
                label = stringResource(R.string.image_pixel_count),
                value = pixelCount,
            )
            MetadataRow(
                label = stringResource(R.string.image_mime_type),
                value = metadata.mimeType,
            )
        }
    }
}

@Composable
private fun RoiSelectionOverlay(
    previewSize: IntSize,
    previewRect: PreviewRect?,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val overlayColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.22f)
    val selectionFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val selectionStroke = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.roi_preview_accessibility)

    Canvas(
        modifier = modifier
            .semantics { contentDescription = description }
            .pointerInput(previewSize) {
                detectDragGestures(
                    onDragStart = onDragStart,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                )
            },
    ) {
        drawRect(overlayColor)
        previewRect?.let { rect ->
            val left = min(rect.startX, rect.endX).coerceIn(0f, size.width)
            val top = min(rect.startY, rect.endY).coerceIn(0f, size.height)
            val right = max(rect.startX, rect.endX).coerceIn(0f, size.width)
            val bottom = max(rect.startY, rect.endY).coerceIn(0f, size.height)
            val width = right - left
            val height = bottom - top
            if (width > 0f && height > 0f) {
                drawRect(
                    color = selectionFill,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                )
                drawRect(
                    color = selectionStroke,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun RoiActionArea(
    isRoiSelectionMode: Boolean,
    isProcessing: Boolean,
    canRestoreFullImage: Boolean,
    canConfirmRoi: Boolean,
    hasPreviewRect: Boolean,
    onStartRoiSelection: () -> Unit,
    onCancelRoiSelection: () -> Unit,
    onConfirmRoiSelection: () -> Unit,
    onRestoreFullImage: () -> Unit,
) {
    if (isRoiSelectionMode) {
        Text(
            text = stringResource(R.string.roi_selection_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing && canConfirmRoi && hasPreviewRect,
            onClick = onConfirmRoiSelection,
        ) {
            Text(stringResource(R.string.confirm_roi_analysis))
        }
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing,
            onClick = onCancelRoiSelection,
        ) {
            Text(stringResource(R.string.cancel_roi_selection))
        }
        if (!canConfirmRoi) {
            Text(
                text = stringResource(R.string.roi_requires_strategy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    } else {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing,
            onClick = onStartRoiSelection,
        ) {
            Text(stringResource(R.string.start_roi_selection))
        }
        if (canRestoreFullImage) {
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                onClick = onRestoreFullImage,
            ) {
                Text(stringResource(R.string.restore_full_image_analysis))
            }
        }
    }
}

@Composable
private fun AnalysisTargetInfoPanel(info: AnalysisTargetInfo) {
    val pixelCount = remember(info.pixelCount) {
        NumberFormat.getIntegerInstance().format(info.pixelCount)
    }
    val percent = remember(info.areaRatio) {
        info.areaRatio?.let {
            NumberFormat.getPercentInstance().apply {
                maximumFractionDigits = 1
                minimumFractionDigits = 0
            }.format(it)
        }
    }
    val containerColor = when (info.type) {
        AnalysisTargetType.FULL_IMAGE -> MaterialTheme.colorScheme.primaryContainer
        AnalysisTargetType.ROI_REGION -> MaterialTheme.colorScheme.secondaryContainer
    }
    val targetType = when (info.type) {
        AnalysisTargetType.FULL_IMAGE -> stringResource(R.string.analysis_target_full_image)
        AnalysisTargetType.ROI_REGION -> stringResource(R.string.analysis_target_roi)
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall),
        ) {
            MetadataRow(
                label = stringResource(R.string.analysis_target_label),
                value = targetType,
            )
            MetadataRow(
                label = stringResource(R.string.analysis_target_size),
                value = "${info.width} × ${info.height}",
            )
            MetadataRow(
                label = stringResource(R.string.analysis_target_pixels),
                value = pixelCount,
            )
            percent?.let {
                MetadataRow(
                    label = stringResource(R.string.analysis_target_ratio),
                    value = it,
                )
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

private fun Offset.coerceIn(size: IntSize): Offset {
    if (size.width <= 0 || size.height <= 0) return this
    return Offset(
        x = x.coerceIn(0f, size.width.toFloat()),
        y = y.coerceIn(0f, size.height.toFloat()),
    )
}

const val ROI_PREVIEW_TEST_TAG = "roi_preview_box"
