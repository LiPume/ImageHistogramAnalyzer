package com.lzx.imagehistogramanalyzer.data.image

import android.graphics.Bitmap

/** 将 Bitmap 像素一次性读入连续数组，避免逐像素 JNI 调用。 */
class BitmapPixelReader {
    fun read(bitmap: Bitmap): IntArray {
        val pixelCount = bitmap.width.toLong() * bitmap.height.toLong()
        require(pixelCount in 1..Int.MAX_VALUE.toLong()) { "图片像素数量无效" }

        return IntArray(pixelCount.toInt()).also { pixels ->
            bitmap.getPixels(
                pixels,
                0,
                bitmap.width,
                0,
                0,
                bitmap.width,
                bitmap.height,
            )
        }
    }
}
