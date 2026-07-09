package com.lzx.imagehistogramanalyzer.domain.roi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class RoiSelectionTest {
    @Test
    fun validSelection_exposesBoundsPixelCountAndRatio() {
        val roi = RoiSelection(
            left = 10,
            top = 20,
            width = 30,
            height = 40,
            originalWidth = 100,
            originalHeight = 200,
        )

        assertEquals(40, roi.right)
        assertEquals(60, roi.bottom)
        assertEquals(1_200L, roi.pixelCount)
        assertEquals(0.06, roi.areaRatio, TOLERANCE)
    }

    @Test
    fun roiTargetInfo_containsRegionMetadata() {
        val roi = RoiSelection(
            left = 0,
            top = 0,
            width = 50,
            height = 20,
            originalWidth = 100,
            originalHeight = 100,
        )

        val info = roi.toTargetInfo()

        assertEquals(AnalysisTargetType.ROI_REGION, info.type)
        assertEquals("局部区域", info.displayName)
        assertEquals(50, info.width)
        assertEquals(20, info.height)
        assertEquals(1_000L, info.pixelCount)
        assertEquals(0.10, info.areaRatio ?: error("缺少 ROI 占比"), TOLERANCE)
    }

    @Test
    fun fullImageTargetInfo_hasNoAreaRatio() {
        val info = fullImageTargetInfo(
            displayName = "全图",
            width = 4,
            height = 3,
        )

        assertEquals(AnalysisTargetType.FULL_IMAGE, info.type)
        assertEquals(12L, info.pixelCount)
        assertNull(info.areaRatio)
    }

    @Test
    fun invalidSelectionOutsideOriginal_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            RoiSelection(
                left = 90,
                top = 0,
                width = 20,
                height = 10,
                originalWidth = 100,
                originalHeight = 100,
            )
        }
    }

    @Test
    fun invalidTargetPixelCount_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AnalysisTargetInfo(
                type = AnalysisTargetType.ROI_REGION,
                displayName = "错误区域",
                width = 10,
                height = 10,
                pixelCount = 99,
                areaRatio = 0.5,
            )
        }
    }

    companion object {
        private const val TOLERANCE = 0.000_001
    }
}
