package com.lzx.imagehistogramanalyzer.domain.roi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PreviewRoiMapperTest {
    private val mapper = PreviewRoiMapper()

    @Test
    fun fitWithoutLetterbox_mapsPreviewRectToBitmapCoordinates() {
        val layout = PreviewImageLayout(
            bitmapWidth = 400,
            bitmapHeight = 200,
            containerWidth = 200f,
            containerHeight = 100f,
        )

        val roi = mapper.mapToBitmap(
            previewRect = PreviewRect(50f, 25f, 150f, 75f),
            layout = layout,
        )

        assertEquals(100, roi.left)
        assertEquals(50, roi.top)
        assertEquals(200, roi.width)
        assertEquals(100, roi.height)
    }

    @Test
    fun horizontalLetterbox_clampsSelectionToDisplayedImage() {
        val layout = PreviewImageLayout(
            bitmapWidth = 100,
            bitmapHeight = 100,
            containerWidth = 200f,
            containerHeight = 100f,
        )

        val roi = mapper.mapToBitmap(
            previewRect = PreviewRect(0f, 20f, 100f, 80f),
            layout = layout,
        )

        assertEquals(0, roi.left)
        assertEquals(20, roi.top)
        assertEquals(50, roi.width)
        assertEquals(60, roi.height)
    }

    @Test
    fun verticalLetterbox_clampsSelectionToDisplayedImage() {
        val layout = PreviewImageLayout(
            bitmapWidth = 100,
            bitmapHeight = 200,
            containerWidth = 100f,
            containerHeight = 300f,
        )

        val roi = mapper.mapToBitmap(
            previewRect = PreviewRect(25f, 0f, 75f, 120f),
            layout = layout,
        )

        assertEquals(25, roi.left)
        assertEquals(0, roi.top)
        assertEquals(50, roi.width)
        assertEquals(70, roi.height)
    }

    @Test
    fun reversedDragDirection_isNormalized() {
        val layout = PreviewImageLayout(
            bitmapWidth = 100,
            bitmapHeight = 100,
            containerWidth = 100f,
            containerHeight = 100f,
        )

        val roi = mapper.mapToBitmap(
            previewRect = PreviewRect(90f, 80f, 10f, 20f),
            layout = layout,
        )

        assertEquals(10, roi.left)
        assertEquals(20, roi.top)
        assertEquals(80, roi.width)
        assertEquals(60, roi.height)
    }

    @Test
    fun selectionOnlyInLetterbox_isRejected() {
        val layout = PreviewImageLayout(
            bitmapWidth = 100,
            bitmapHeight = 100,
            containerWidth = 200f,
            containerHeight = 100f,
        )

        assertThrows(IllegalArgumentException::class.java) {
            mapper.mapToBitmap(
                previewRect = PreviewRect(0f, 10f, 40f, 90f),
                layout = layout,
            )
        }
    }

    @Test
    fun tooSmallSelection_isRejected() {
        val layout = PreviewImageLayout(
            bitmapWidth = 100,
            bitmapHeight = 100,
            containerWidth = 100f,
            containerHeight = 100f,
        )

        assertThrows(IllegalArgumentException::class.java) {
            mapper.mapToBitmap(
                previewRect = PreviewRect(10f, 10f, 10.8f, 20f),
                layout = layout,
            )
        }
    }

    @Test
    fun invalidLayout_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PreviewImageLayout(
                bitmapWidth = 0,
                bitmapHeight = 100,
                containerWidth = 100f,
                containerHeight = 100f,
            )
        }
    }
}
