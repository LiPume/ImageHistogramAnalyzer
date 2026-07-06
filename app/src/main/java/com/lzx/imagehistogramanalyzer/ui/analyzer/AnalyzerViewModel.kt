package com.lzx.imagehistogramanalyzer.ui.analyzer

import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lzx.imagehistogramanalyzer.data.image.BitmapPixelReader
import com.lzx.imagehistogramanalyzer.data.image.ImageRepository
import com.lzx.imagehistogramanalyzer.data.image.ImageTooLargeException
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculationStrategy
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculator
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
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
    private val imageRepository: ImageRepository,
    private val pixelReader: BitmapPixelReader,
    histogramCalculators: List<HistogramCalculator>,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalyzerUiState())
    val uiState: StateFlow<AnalyzerUiState> = _uiState.asStateFlow()

    private var analysisJob: Job? = null
    private var requestId: Long = 0
    private val calculatorsByStrategy = histogramCalculators.associateBy { it.strategy }

    init {
        require(calculatorsByStrategy.keys.containsAll(HistogramCalculationStrategy.entries)) {
            "必须为课程要求的两种计算方案提供实现"
        }
    }

    fun selectImage(uri: Uri) {
        analysisJob?.cancel()
        val currentRequestId = ++requestId
        _uiState.value = AnalyzerUiState(isProcessing = true)

        analysisJob = viewModelScope.launch {
            try {
                val decodeStart = SystemClock.elapsedRealtimeNanos()
                val decodedImage = imageRepository.load(uri)
                val decodeTime = SystemClock.elapsedRealtimeNanos() - decodeStart

                if (currentRequestId == requestId) {
                    _uiState.value = AnalyzerUiState(
                        image = decodedImage.bitmap,
                        metadata = decodedImage.metadata,
                        decodeTimeNanos = decodeTime,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                if (currentRequestId == requestId) {
                    _uiState.value = AnalyzerUiState(errorMessage = error.toUserMessage())
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
                    histogram = null,
                    calculationTimeNanos = null,
                    errorMessage = null,
                )
            }
        }
    }

    fun calculateHistogram() {
        val current = _uiState.value
        val bitmap = current.image ?: return
        val strategy = current.selectedStrategy ?: return
        val calculator = calculatorsByStrategy.getValue(strategy)
        val currentRequestId = requestId

        analysisJob?.cancel()
        _uiState.value = current.copy(
            isProcessing = true,
            histogram = null,
            calculationTimeNanos = null,
            errorMessage = null,
        )

        analysisJob = viewModelScope.launch {
            try {
                val computed = withContext(computationDispatcher) {
                    val calculationStart = SystemClock.elapsedRealtimeNanos()
                    val pixels = pixelReader.read(bitmap)
                    val histogram = calculator.calculate(pixels) {
                        coroutineContext.ensureActive()
                    }
                    ComputedHistogram(
                        histogram = histogram,
                        durationNanos = SystemClock.elapsedRealtimeNanos() - calculationStart,
                    )
                }

                if (currentRequestId == requestId) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            histogram = computed.histogram,
                            calculationTimeNanos = computed.durationNanos,
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

    private fun Exception.toUserMessage(): String = when (this) {
        is ImageTooLargeException -> {
            val megapixels = pixelCount / 1_000_000.0
            val maxMegapixels = maxPixelCount / 1_000_000
            "图片约 %.1f 百万像素，超过 MVP 的 %d 百万像素安全上限".format(
                megapixels,
                maxMegapixels,
            )
        }

        is SecurityException -> "没有读取该图片的权限，请重新选择"
        is IllegalArgumentException -> message ?: "所选文件不是有效图片"
        else -> "图片处理失败，请重新选择"
    }

    private data class ComputedHistogram(
        val histogram: HistogramResult,
        val durationNanos: Long,
    )
}
