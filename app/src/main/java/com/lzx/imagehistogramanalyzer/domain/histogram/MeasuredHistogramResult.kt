package com.lzx.imagehistogramanalyzer.domain.histogram

import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult

/** 算法内部阶段耗时；统计时灰度化方案的转换与计数融合，因此转换耗时为空。 */
data class HistogramStageTimings(
    val grayscaleConversionNanos: Long?,
    val countingNanos: Long,
    val normalizationNanos: Long,
    val mergingNanos: Long? = null,
) {
    init {
        require(grayscaleConversionNanos == null || grayscaleConversionNanos >= 0)
        require(countingNanos >= 0)
        require(normalizationNanos >= 0)
        require(mergingNanos == null || mergingNanos >= 0)
    }
}

/** 将直方图结果与阶段计时绑定，避免 UI 直接参与算法测量。 */
data class MeasuredHistogramResult(
    val histogram: HistogramResult,
    val timings: HistogramStageTimings,
)
