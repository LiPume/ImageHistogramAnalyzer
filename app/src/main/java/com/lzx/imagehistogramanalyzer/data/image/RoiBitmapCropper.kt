package com.lzx.imagehistogramanalyzer.data.image

import android.graphics.Bitmap
import com.lzx.imagehistogramanalyzer.domain.roi.PreviewRoiMapper
import com.lzx.imagehistogramanalyzer.domain.roi.RoiSelection

/** 根据 Bitmap 坐标系中的 ROI 裁剪局部 Bitmap。 */
class RoiBitmapCropper(
    private val minSidePixels: Int = PreviewRoiMapper.DEFAULT_MIN_SIDE_PIXELS,
) {
    init {
        require(minSidePixels > 0) { "ROI 最小边长必须大于 0" }
    }

    fun crop(bitmap: Bitmap, roi: RoiSelection): Bitmap {
        require(bitmap.width == roi.originalWidth && bitmap.height == roi.originalHeight) {
            "ROI 原图尺寸与当前 Bitmap 不一致"
        }
        require(roi.width >= minSidePixels && roi.height >= minSidePixels) {
            "ROI 区域过小，请框选更大的区域"
        }

        return Bitmap.createBitmap(bitmap, roi.left, roi.top, roi.width, roi.height)
    }
}
