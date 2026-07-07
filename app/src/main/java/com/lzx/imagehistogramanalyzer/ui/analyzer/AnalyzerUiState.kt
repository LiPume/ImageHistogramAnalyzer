package com.lzx.imagehistogramanalyzer.ui.analyzer

import android.graphics.Bitmap
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculationStrategy
import com.lzx.imagehistogramanalyzer.domain.model.HistogramPerformanceMetrics
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.model.ImageMetadata
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityResult

data class AnalyzerUiState(
    val isProcessing: Boolean = false,
    val image: Bitmap? = null,
    val metadata: ImageMetadata? = null,
    val selectedStrategy: HistogramCalculationStrategy? = null,
    val histogram: HistogramResult? = null,
    val qualityResult: ImageQualityResult? = null,
    val decodeTimeNanos: Long? = null,
    val performanceMetrics: HistogramPerformanceMetrics? = null,
    val errorMessage: String? = null,
)
