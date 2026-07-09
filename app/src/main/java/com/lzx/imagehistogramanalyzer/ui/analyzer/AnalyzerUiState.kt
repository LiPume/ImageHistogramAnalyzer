package com.lzx.imagehistogramanalyzer.ui.analyzer

import android.graphics.Bitmap
import com.lzx.imagehistogramanalyzer.domain.color.RgbChannelStats
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculationStrategy
import com.lzx.imagehistogramanalyzer.domain.insight.ImageInsightResult
import com.lzx.imagehistogramanalyzer.domain.model.HistogramPerformanceMetrics
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.model.ImageMetadata
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityResult
import com.lzx.imagehistogramanalyzer.domain.roi.AnalysisTargetInfo

data class AnalyzerUiState(
    val isProcessing: Boolean = false,
    val image: Bitmap? = null,
    val metadata: ImageMetadata? = null,
    val analysisTargetInfo: AnalysisTargetInfo? = null,
    val isRoiSelectionMode: Boolean = false,
    val canRestoreFullImage: Boolean = false,
    val selectedStrategy: HistogramCalculationStrategy? = null,
    val histogram: HistogramResult? = null,
    val qualityResult: ImageQualityResult? = null,
    val rgbStats: RgbChannelStats? = null,
    val imageInsight: ImageInsightResult? = null,
    val decodeTimeNanos: Long? = null,
    val performanceMetrics: HistogramPerformanceMetrics? = null,
    val errorMessage: String? = null,
)
