package com.lzx.imagehistogramanalyzer.ui.analyzer

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lzx.imagehistogramanalyzer.data.image.BitmapPixelReader
import com.lzx.imagehistogramanalyzer.data.image.ImageDecodeException
import com.lzx.imagehistogramanalyzer.data.image.ImageLoader
import com.lzx.imagehistogramanalyzer.data.image.ImageOpenException
import com.lzx.imagehistogramanalyzer.data.image.ImageTooLargeException
import com.lzx.imagehistogramanalyzer.data.image.NativeBitmapHistogramCalculator
import com.lzx.imagehistogramanalyzer.data.image.RoiBitmapCropper
import com.lzx.imagehistogramanalyzer.data.image.UnsupportedImageTypeException
import com.lzx.imagehistogramanalyzer.domain.color.RgbChannelStats
import com.lzx.imagehistogramanalyzer.domain.color.RgbHistogramAnalyzer
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculationStrategy
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculator
import com.lzx.imagehistogramanalyzer.domain.insight.ImageInsightAnalyzer
import com.lzx.imagehistogramanalyzer.domain.insight.ImageInsightResult
import com.lzx.imagehistogramanalyzer.domain.histogram.MonotonicNanoClock
import com.lzx.imagehistogramanalyzer.domain.histogram.NanoClock
import com.lzx.imagehistogramanalyzer.domain.model.HistogramPerformanceMetrics
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.model.ImageMetadata
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityResult
import com.lzx.imagehistogramanalyzer.domain.quality.ImageQualityAnalyzer
import com.lzx.imagehistogramanalyzer.domain.roi.AnalysisTargetInfo
import com.lzx.imagehistogramanalyzer.domain.roi.PreviewImageLayout
import com.lzx.imagehistogramanalyzer.domain.roi.PreviewRect
import com.lzx.imagehistogramanalyzer.domain.roi.PreviewRoiMapper
import com.lzx.imagehistogramanalyzer.domain.roi.RoiSelection
import com.lzx.imagehistogramanalyzer.domain.roi.fullImageTargetInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class AnalyzerViewModel(
    private val imageLoader: ImageLoader,
    private val pixelReader: BitmapPixelReader,
    histogramCalculators: List<HistogramCalculator>,
    private val nativeCalculator: NativeBitmapHistogramCalculator? = null,
    private val qualityAnalyzer: ImageQualityAnalyzer = ImageQualityAnalyzer(),
    private val rgbAnalyzer: RgbHistogramAnalyzer = RgbHistogramAnalyzer(),
    private val insightAnalyzer: ImageInsightAnalyzer = ImageInsightAnalyzer(),
    private val roiMapper: PreviewRoiMapper = PreviewRoiMapper(),
    private val roiBitmapCropper: RoiBitmapCropper = RoiBitmapCropper(),
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: NanoClock = MonotonicNanoClock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalyzerUiState())
    val uiState: StateFlow<AnalyzerUiState> = _uiState.asStateFlow()

    private var analysisJob: Job? = null
    private var requestId: Long = 0
    private var fullImage: Bitmap? = null
    private var fullMetadata: ImageMetadata? = null
    private var fullDecodeTimeNanos: Long? = null
    private val calculatorsByStrategy = histogramCalculators.associateBy { it.strategy }

    init {
        require(calculatorsByStrategy.keys.containsAll(HistogramCalculationStrategy.entries)) {
            "必须为课程要求的两种计算方案提供实现"
        }
    }

    fun selectImage(uri: Uri) {
        analysisJob?.cancel()
        val currentRequestId = ++requestId
        val previousState = _uiState.value
        _uiState.value = previousState.copy(
            isProcessing = true,
            isRoiSelectionMode = false,
            errorMessage = null,
        )

        analysisJob = viewModelScope.launch {
            try {
                val decodeStart = clock.nowNanos()
                val decodedImage = imageLoader.load(uri)
                val decodeTime = clock.nowNanos() - decodeStart

                if (currentRequestId == requestId) {
                    fullImage = decodedImage.bitmap
                    fullMetadata = decodedImage.metadata
                    fullDecodeTimeNanos = decodeTime
                    _uiState.value = AnalyzerUiState(
                        image = decodedImage.bitmap,
                        metadata = decodedImage.metadata,
                        analysisTargetInfo = decodedImage.metadata.toFullTargetInfo(),
                        decodeTimeNanos = decodeTime,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                if (currentRequestId == requestId) {
                    _uiState.value = previousState.copy(
                        isProcessing = false,
                        errorMessage = error.toUserMessage(),
                    )
                }
            }
        }
    }

    fun selectStrategy(strategy: HistogramCalculationStrategy) {
        _uiState.update { current ->
            if (current.image == null || current.isProcessing) {
                current
            } else {
                current.copy(
                    selectedStrategy = strategy,
                    isRoiSelectionMode = false,
                    histogram = null,
                    qualityResult = null,
                    rgbStats = null,
                    imageInsight = null,
                    performanceMetrics = null,
                    errorMessage = null,
                )
            }
        }
    }

    fun calculateHistogram() {
        val current = _uiState.value
        val bitmap = current.image ?: return
        val metadata = current.metadata ?: return
        val strategy = current.selectedStrategy ?: return
        launchCalculationForTarget(
            bitmap = bitmap,
            metadata = metadata,
            targetInfo = current.analysisTargetInfo ?: metadata.toFullTargetInfo(),
            decodeTimeNanos = current.decodeTimeNanos,
            canRestoreFullImage = current.canRestoreFullImage,
            strategy = strategy,
            currentRequestId = requestId,
        )
    }

    fun startRoiSelection() {
        _uiState.update { current ->
            if (current.image == null || current.isProcessing) {
                current
            } else {
                current.copy(
                    isRoiSelectionMode = true,
                    errorMessage = null,
                )
            }
        }
    }

    fun cancelRoiSelection() {
        _uiState.update { current ->
            current.copy(isRoiSelectionMode = false)
        }
    }

    fun confirmRoiSelection(
        previewRect: PreviewRect,
        previewImageLayout: PreviewImageLayout,
    ) {
        val current = _uiState.value
        val sourceBitmap = current.image ?: return
        val sourceMetadata = current.metadata ?: return
        val strategy = current.selectedStrategy ?: run {
            _uiState.update {
                it.copy(errorMessage = "请先选择计算方案，再确认局部区域")
            }
            return
        }
        val calculator = calculatorsByStrategy.getValue(strategy)
        val currentRequestId = ++requestId

        analysisJob?.cancel()
        _uiState.value = current.copy(
            isProcessing = true,
            isRoiSelectionMode = false,
            histogram = null,
            qualityResult = null,
            rgbStats = null,
            imageInsight = null,
            performanceMetrics = null,
            errorMessage = null,
        )

        analysisJob = viewModelScope.launch {
            try {
                val croppedTarget = withContext(computationDispatcher) {
                    coroutineContext.ensureActive()
                    val roi = roiMapper.mapToBitmap(previewRect, previewImageLayout)
                    val croppedBitmap = roiBitmapCropper.crop(sourceBitmap, roi)
                    CroppedTarget(
                        bitmap = croppedBitmap,
                        metadata = sourceMetadata.toRoiMetadata(roi),
                        targetInfo = roi.toTargetInfo(),
                    )
                }
                val computed = withContext(computationDispatcher) {
                    calculateForBitmap(croppedTarget.bitmap, strategy, calculator)
                }

                if (currentRequestId == requestId) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            image = croppedTarget.bitmap,
                            metadata = croppedTarget.metadata,
                            analysisTargetInfo = croppedTarget.targetInfo,
                            canRestoreFullImage = true,
                            decodeTimeNanos = 0L,
                            histogram = computed.histogram,
                            qualityResult = computed.qualityResult,
                            rgbStats = computed.rgbStats,
                            imageInsight = computed.imageInsight,
                            performanceMetrics = computed.performanceMetrics,
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                if (currentRequestId == requestId) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = error.toUserMessage(),
                        )
                    }
                }
            }
        }
    }

    fun restoreFullImageAnalysis() {
        val bitmap = fullImage ?: return
        val metadata = fullMetadata ?: return
        val current = _uiState.value
        val strategy = current.selectedStrategy

        if (strategy == null) {
            analysisJob?.cancel()
            requestId += 1
            _uiState.value = current.copy(
                isProcessing = false,
                image = bitmap,
                metadata = metadata,
                analysisTargetInfo = metadata.toFullTargetInfo(),
                isRoiSelectionMode = false,
                canRestoreFullImage = false,
                histogram = null,
                qualityResult = null,
                rgbStats = null,
                imageInsight = null,
                decodeTimeNanos = fullDecodeTimeNanos,
                performanceMetrics = null,
                errorMessage = null,
            )
            return
        }

        launchCalculationForTarget(
            bitmap = bitmap,
            metadata = metadata,
            targetInfo = metadata.toFullTargetInfo(),
            decodeTimeNanos = fullDecodeTimeNanos,
            canRestoreFullImage = false,
            strategy = strategy,
            currentRequestId = ++requestId,
        )
    }

    private fun launchCalculationForTarget(
        bitmap: Bitmap,
        metadata: ImageMetadata,
        targetInfo: AnalysisTargetInfo,
        decodeTimeNanos: Long?,
        canRestoreFullImage: Boolean,
        strategy: HistogramCalculationStrategy,
        currentRequestId: Long,
    ) {
        val calculator = calculatorsByStrategy.getValue(strategy)

        analysisJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            image = bitmap,
            metadata = metadata,
            analysisTargetInfo = targetInfo,
            isRoiSelectionMode = false,
            canRestoreFullImage = canRestoreFullImage,
            selectedStrategy = strategy,
            histogram = null,
            qualityResult = null,
            rgbStats = null,
            imageInsight = null,
            decodeTimeNanos = decodeTimeNanos,
            performanceMetrics = null,
            errorMessage = null,
        )

        analysisJob = viewModelScope.launch {
            try {
                val computed = withContext(computationDispatcher) {
                    calculateForBitmap(bitmap, strategy, calculator)
                }

                if (currentRequestId == requestId) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            histogram = computed.histogram,
                            qualityResult = computed.qualityResult,
                            rgbStats = computed.rgbStats,
                            imageInsight = computed.imageInsight,
                            performanceMetrics = computed.performanceMetrics,
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                if (currentRequestId == requestId) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = error.toUserMessage(),
                        )
                    }
                }
            }
        }
    }

    private suspend fun calculateForBitmap(
        bitmap: Bitmap,
        strategy: HistogramCalculationStrategy,
        calculator: HistogramCalculator,
    ): ComputedHistogram {
        coroutineContext.ensureActive()
        val native = nativeCalculator
        return if (native != null && native.isAvailable) {
            val result = native.calculate(bitmap, strategy)
            coroutineContext.ensureActive()
            val qualityResult = qualityAnalyzer.analyze(result.histogram)
            ComputedHistogram(
                histogram = result.histogram,
                performanceMetrics = result.metrics,
                qualityResult = qualityResult,
                rgbStats = result.rgbStats,
                imageInsight = insightAnalyzer.analyze(
                    histogram = result.histogram,
                    quality = qualityResult,
                    rgbStats = result.rgbStats,
                ),
            )
        } else {
            calculateWithKotlin(bitmap, calculator)
        }
    }

    private suspend fun calculateWithKotlin(
        bitmap: Bitmap,
        calculator: HistogramCalculator,
    ): ComputedHistogram {
        val calculationStart = clock.nowNanos()

        val pixelReadStart = clock.nowNanos()
        val pixels = pixelReader.read(bitmap)
        val pixelReadNanos = clock.nowNanos() - pixelReadStart

        val activeContext = coroutineContext
        val measured = calculator.calculateMeasured(pixels) { activeContext.ensureActive() }
        activeContext.ensureActive()
        val rgbStats = rgbAnalyzer.analyze(pixels)
        val qualityResult = qualityAnalyzer.analyze(measured.histogram)
        val coreTotalNanos = clock.nowNanos() - calculationStart
        return ComputedHistogram(
            histogram = measured.histogram,
            qualityResult = qualityResult,
            rgbStats = rgbStats,
            imageInsight = insightAnalyzer.analyze(
                histogram = measured.histogram,
                quality = qualityResult,
                rgbStats = rgbStats,
            ),
            performanceMetrics = HistogramPerformanceMetrics(
                pixelReadNanos = pixelReadNanos,
                grayscaleConversionNanos = measured.timings.grayscaleConversionNanos,
                countingNanos = measured.timings.countingNanos,
                normalizationNanos = measured.timings.normalizationNanos,
                mergingNanos = measured.timings.mergingNanos,
                coreTotalNanos = coreTotalNanos,
            ),
        )
    }

    private data class ComputedHistogram(
        val histogram: HistogramResult,
        val qualityResult: ImageQualityResult,
        val rgbStats: RgbChannelStats,
        val imageInsight: ImageInsightResult,
        val performanceMetrics: HistogramPerformanceMetrics,
    )

    private data class CroppedTarget(
        val bitmap: Bitmap,
        val metadata: ImageMetadata,
        val targetInfo: AnalysisTargetInfo,
    )
}

private fun ImageMetadata.toFullTargetInfo(): AnalysisTargetInfo = fullImageTargetInfo(
    displayName = displayName,
    width = width,
    height = height,
)

private fun ImageMetadata.toRoiMetadata(roi: RoiSelection): ImageMetadata = copy(
    displayName = "$displayName · 局部区域",
    width = roi.width,
    height = roi.height,
)

internal fun Exception.toUserMessage(): String = when (this) {
    is ImageTooLargeException -> {
        val megapixels = pixelCount / 1_000_000.0
        val maxMegapixels = maxPixelCount / 1_000_000
        "图片约 %.1f 百万像素，超过 MVP 的 %d 百万像素安全上限".format(
            megapixels,
            maxMegapixels,
        )
    }

    is ImageOpenException -> "无法打开所选图片，请重新选择"
    is ImageDecodeException -> "图片文件可能已损坏或格式不受支持，请重新选择"
    is UnsupportedImageTypeException -> "不支持的文件类型：$mimeType，请选择 JPG、PNG 或 WebP"
    is SecurityException -> "没有读取该图片的权限，请重新选择"
    is IllegalArgumentException -> message ?: "所选文件不是有效图片"
    else -> "图片处理失败，请重新选择"
}
