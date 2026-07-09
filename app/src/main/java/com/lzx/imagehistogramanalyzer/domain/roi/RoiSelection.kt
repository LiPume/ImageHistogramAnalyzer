package com.lzx.imagehistogramanalyzer.domain.roi

/** 当前分析对象类型：整张图片或用户框选的局部区域。 */
enum class AnalysisTargetType {
    FULL_IMAGE,
    ROI_REGION,
}

/** 展示给 UI 的分析对象信息；不持有 Bitmap。 */
data class AnalysisTargetInfo(
    val type: AnalysisTargetType,
    val displayName: String,
    val width: Int,
    val height: Int,
    val pixelCount: Long,
    val areaRatio: Double? = null,
) {
    init {
        require(width > 0) { "分析对象宽度必须大于 0" }
        require(height > 0) { "分析对象高度必须大于 0" }
        require(pixelCount == width.toLong() * height.toLong()) { "像素数量必须等于宽高乘积" }
        require(areaRatio == null || areaRatio in 0.0..1.0) { "区域占比必须在 0..1 之间" }
    }
}

/**
 * Bitmap 坐标系中的 ROI 区域。
 *
 * [left]、[top] 为左上角像素坐标；[width]、[height] 为区域尺寸，右下边界为开区间。
 */
data class RoiSelection(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
    val originalWidth: Int,
    val originalHeight: Int,
) {
    val right: Int get() = left + width
    val bottom: Int get() = top + height
    val pixelCount: Long get() = width.toLong() * height.toLong()
    val areaRatio: Double
        get() = pixelCount.toDouble() / (originalWidth.toLong() * originalHeight.toLong())

    init {
        require(originalWidth > 0) { "原图宽度必须大于 0" }
        require(originalHeight > 0) { "原图高度必须大于 0" }
        require(left >= 0) { "ROI left 不能为负数" }
        require(top >= 0) { "ROI top 不能为负数" }
        require(width > 0) { "ROI 宽度必须大于 0" }
        require(height > 0) { "ROI 高度必须大于 0" }
        require(right <= originalWidth) { "ROI 右边界不能超过原图宽度" }
        require(bottom <= originalHeight) { "ROI 下边界不能超过原图高度" }
    }

    fun toTargetInfo(displayName: String = "局部区域"): AnalysisTargetInfo = AnalysisTargetInfo(
        type = AnalysisTargetType.ROI_REGION,
        displayName = displayName,
        width = width,
        height = height,
        pixelCount = pixelCount,
        areaRatio = areaRatio,
    )
}

fun fullImageTargetInfo(
    displayName: String,
    width: Int,
    height: Int,
): AnalysisTargetInfo = AnalysisTargetInfo(
    type = AnalysisTargetType.FULL_IMAGE,
    displayName = displayName,
    width = width,
    height = height,
    pixelCount = width.toLong() * height.toLong(),
    areaRatio = null,
)
