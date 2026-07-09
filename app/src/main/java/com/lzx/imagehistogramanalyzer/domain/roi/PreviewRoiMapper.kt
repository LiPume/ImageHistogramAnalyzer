package com.lzx.imagehistogramanalyzer.domain.roi

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** 预览容器中的浮点矩形，支持用户从任意方向拖拽。 */
data class PreviewRect(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
) {
    val left: Float get() = min(startX, endX)
    val top: Float get() = min(startY, endY)
    val right: Float get() = max(startX, endX)
    val bottom: Float get() = max(startY, endY)
}

/** 图片以 ContentScale.Fit 方式放入预览容器后的几何信息。 */
data class PreviewImageLayout(
    val bitmapWidth: Int,
    val bitmapHeight: Int,
    val containerWidth: Float,
    val containerHeight: Float,
) {
    val scale: Float
    val displayedWidth: Float
    val displayedHeight: Float
    val offsetX: Float
    val offsetY: Float

    init {
        require(bitmapWidth > 0) { "Bitmap 宽度必须大于 0" }
        require(bitmapHeight > 0) { "Bitmap 高度必须大于 0" }
        require(containerWidth > 0f) { "预览容器宽度必须大于 0" }
        require(containerHeight > 0f) { "预览容器高度必须大于 0" }

        scale = min(containerWidth / bitmapWidth, containerHeight / bitmapHeight)
        displayedWidth = bitmapWidth * scale
        displayedHeight = bitmapHeight * scale
        offsetX = (containerWidth - displayedWidth) / 2f
        offsetY = (containerHeight - displayedHeight) / 2f
    }
}

/** 将预览容器坐标映射到 Bitmap 像素坐标。 */
class PreviewRoiMapper(
    private val minSidePixels: Int = DEFAULT_MIN_SIDE_PIXELS,
) {
    init {
        require(minSidePixels > 0) { "ROI 最小边长必须大于 0" }
    }

    fun mapToBitmap(
        previewRect: PreviewRect,
        layout: PreviewImageLayout,
    ): RoiSelection {
        val imageLeft = layout.offsetX
        val imageTop = layout.offsetY
        val imageRight = imageLeft + layout.displayedWidth
        val imageBottom = imageTop + layout.displayedHeight

        val clampedLeft = previewRect.left.coerceIn(imageLeft, imageRight)
        val clampedTop = previewRect.top.coerceIn(imageTop, imageBottom)
        val clampedRight = previewRect.right.coerceIn(imageLeft, imageRight)
        val clampedBottom = previewRect.bottom.coerceIn(imageTop, imageBottom)

        require(clampedRight > clampedLeft && clampedBottom > clampedTop) {
            "ROI 必须与图片显示区域相交"
        }

        val left = floor((clampedLeft - imageLeft) / layout.scale).toInt()
            .coerceIn(0, layout.bitmapWidth - 1)
        val top = floor((clampedTop - imageTop) / layout.scale).toInt()
            .coerceIn(0, layout.bitmapHeight - 1)
        val right = ceil((clampedRight - imageLeft) / layout.scale).toInt()
            .coerceIn(left + 1, layout.bitmapWidth)
        val bottom = ceil((clampedBottom - imageTop) / layout.scale).toInt()
            .coerceIn(top + 1, layout.bitmapHeight)

        val width = right - left
        val height = bottom - top
        require(width >= minSidePixels && height >= minSidePixels) {
            "ROI 区域过小，请框选更大的区域"
        }

        return RoiSelection(
            left = left,
            top = top,
            width = width,
            height = height,
            originalWidth = layout.bitmapWidth,
            originalHeight = layout.bitmapHeight,
        )
    }

    companion object {
        const val DEFAULT_MIN_SIDE_PIXELS = 2
    }
}
