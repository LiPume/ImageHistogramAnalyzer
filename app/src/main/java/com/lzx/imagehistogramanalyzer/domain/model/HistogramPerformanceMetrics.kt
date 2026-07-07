package com.lzx.imagehistogramanalyzer.domain.model

/**
 * 一次核心计算的完整分段耗时。
 *
 * [coreTotalNanos] 从批量取像素开始，到归一化结果完成为止；Canvas 绘制与图片解码不计入。
 */
data class HistogramPerformanceMetrics(
    val pixelReadNanos: Long,
    val grayscaleConversionNanos: Long?,
    val countingNanos: Long,
    val normalizationNanos: Long,
    val mergingNanos: Long? = null,
    val coreTotalNanos: Long,
    val executionEngine: HistogramExecutionEngine = HistogramExecutionEngine.KOTLIN_V2,
    val workerCount: Int = 1,
) {
    init {
        require(pixelReadNanos >= 0)
        require(grayscaleConversionNanos == null || grayscaleConversionNanos >= 0)
        require(countingNanos >= 0)
        require(normalizationNanos >= 0)
        require(mergingNanos == null || mergingNanos >= 0)
        require(coreTotalNanos >= 0)
        require(workerCount > 0)
    }

    /** 核心总耗时中未被显式阶段覆盖的数组分配、结果封装和调度时间。 */
    val overheadNanos: Long
        get() {
            val measuredStages = pixelReadNanos +
                (grayscaleConversionNanos ?: 0L) +
                countingNanos +
                normalizationNanos +
                (mergingNanos ?: 0L)
            return (coreTotalNanos - measuredStages).coerceAtLeast(0L)
        }
}
