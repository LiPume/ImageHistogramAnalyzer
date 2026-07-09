package com.lzx.imagehistogramanalyzer.data.image

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lzx.imagehistogramanalyzer.domain.roi.RoiSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoiBitmapCropperTest {
    private val cropper = RoiBitmapCropper()

    @Test
    fun crop_returnsExpectedBitmapPixels() {
        val bitmap = bitmapOf(
            width = 4,
            height = 3,
            pixels = intArrayOf(
                color(0), color(1), color(2), color(3),
                color(4), color(5), color(6), color(7),
                color(8), color(9), color(10), color(11),
            ),
        )
        val roi = RoiSelection(
            left = 1,
            top = 1,
            width = 2,
            height = 2,
            originalWidth = 4,
            originalHeight = 3,
        )

        val cropped = cropper.crop(bitmap, roi)

        assertEquals(2, cropped.width)
        assertEquals(2, cropped.height)
        assertEquals(color(5), cropped.getPixel(0, 0))
        assertEquals(color(6), cropped.getPixel(1, 0))
        assertEquals(color(9), cropped.getPixel(0, 1))
        assertEquals(color(10), cropped.getPixel(1, 1))

        cropped.recycle()
        bitmap.recycle()
    }

    @Test
    fun cropWithMismatchedOriginalSize_isRejected() {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val roi = RoiSelection(
            left = 0,
            top = 0,
            width = 2,
            height = 2,
            originalWidth = 5,
            originalHeight = 4,
        )

        assertThrows(IllegalArgumentException::class.java) {
            cropper.crop(bitmap, roi)
        }

        bitmap.recycle()
    }

    @Test
    fun tooSmallRoi_isRejected() {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val roi = RoiSelection(
            left = 0,
            top = 0,
            width = 1,
            height = 4,
            originalWidth = 4,
            originalHeight = 4,
        )

        assertThrows(IllegalArgumentException::class.java) {
            cropper.crop(bitmap, roi)
        }

        bitmap.recycle()
    }

    private fun bitmapOf(width: Int, height: Int, pixels: IntArray): Bitmap =
        Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

    private fun color(value: Int): Int =
        (0xFF shl 24) or (value shl 16) or (value shl 8) or value
}
