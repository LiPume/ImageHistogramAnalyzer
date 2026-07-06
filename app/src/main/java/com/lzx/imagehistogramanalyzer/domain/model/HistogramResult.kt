package com.lzx.imagehistogramanalyzer.domain.model

/**
 * 与 UI 解耦的直方图结果。
 *
 * 对传入数组做副本，保证计算完成后数据不会被外部修改。
 */
class HistogramResult(
    counts: IntArray,
    normalizedHeights: IntArray,
    val pixelCount: Long,
    val maxCount: Int,
) {
    val counts: IntArray = counts.copyOf()
    val normalizedHeights: IntArray = normalizedHeights.copyOf()

    init {
        require(this.counts.size == GRAY_LEVELS) { "灰度频次数组必须包含 256 项" }
        require(this.normalizedHeights.size == GRAY_LEVELS) { "归一化数组必须包含 256 项" }
        require(pixelCount > 0) { "像素数量必须大于 0" }
        require(maxCount > 0) { "最大频次必须大于 0" }
    }

    companion object {
        const val GRAY_LEVELS = 256
    }
}
