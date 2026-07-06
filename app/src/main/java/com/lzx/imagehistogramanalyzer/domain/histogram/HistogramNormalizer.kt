package com.lzx.imagehistogramanalyzer.domain.histogram

import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult

/** 将 256 个灰度频次等比例映射到 0..100。 */
class HistogramNormalizer {
    fun normalize(counts: IntArray): IntArray {
        require(counts.size == HistogramResult.GRAY_LEVELS) { "灰度频次数组必须包含 256 项" }
        require(counts.all { it >= 0 }) { "灰度频次不能为负数" }

        val maxCount = counts.maxOrNull() ?: 0
        if (maxCount == 0) return IntArray(HistogramResult.GRAY_LEVELS)

        return IntArray(HistogramResult.GRAY_LEVELS) { index ->
            // 使用 Long 防止先乘 100 时发生 Int 溢出；加上 max/2 实现四舍五入。
            ((counts[index].toLong() * NORMALIZED_MAX + maxCount / 2L) / maxCount)
                .toInt()
                .coerceIn(0, NORMALIZED_MAX)
        }
    }

    companion object {
        const val NORMALIZED_MAX = 100
    }
}
