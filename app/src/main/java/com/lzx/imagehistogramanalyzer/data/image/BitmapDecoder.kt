package com.lzx.imagehistogramanalyzer.data.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import com.lzx.imagehistogramanalyzer.domain.model.ImageMetadata
import java.io.FileNotFoundException
import java.io.InputStream

data class DecodedImage(
    val bitmap: Bitmap,
    val metadata: ImageMetadata,
)

class ImageTooLargeException(
    val pixelCount: Long,
    val maxPixelCount: Long,
) : IllegalArgumentException("图片像素数量超过 MVP 安全上限")

class ImageOpenException(cause: Throwable? = null) :
    IllegalArgumentException("无法打开所选图片", cause)

class ImageDecodeException(cause: Throwable? = null) :
    IllegalArgumentException("图片文件可能已损坏或格式不受支持", cause)

class UnsupportedImageTypeException(val mimeType: String) :
    IllegalArgumentException("不支持的文件类型：$mimeType")

/** 只通过 ContentResolver 解码 content URI，不依赖真实文件路径。 */
class BitmapDecoder(
    private val contentResolver: ContentResolver,
) {
    fun decode(uri: Uri): DecodedImage {
        val mimeType = contentResolver.getType(uri) ?: UNKNOWN_MIME_TYPE
        if (mimeType != UNKNOWN_MIME_TYPE && !mimeType.startsWith(IMAGE_MIME_PREFIX)) {
            throw UnsupportedImageTypeException(mimeType)
        }

        val bitmap = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                decodeWithImageDecoder(uri)
            } else {
                decodeWithBitmapFactory(uri)
            }
        } catch (error: ImageTooLargeException) {
            throw error
        } catch (error: ImageOpenException) {
            throw error
        } catch (error: ImageDecodeException) {
            throw error
        } catch (error: SecurityException) {
            throw error
        } catch (error: FileNotFoundException) {
            throw ImageOpenException(error)
        } catch (error: Exception) {
            throw ImageDecodeException(error)
        }

        return DecodedImage(
            bitmap = bitmap,
            metadata = ImageMetadata(
                displayName = queryDisplayName(uri),
                mimeType = mimeType,
                width = bitmap.width,
                height = bitmap.height,
            ),
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            validateDimensions(info.size.width, info.size.height)
            // 软件 Bitmap 才能稳定执行 getPixels；硬件 Bitmap 不支持直接读取像素。
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    private fun decodeWithBitmapFactory(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }

        validateDimensions(bounds.outWidth, bounds.outHeight)

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw ImageDecodeException()
    }

    private fun openInputStream(uri: Uri): InputStream {
        return try {
            contentResolver.openInputStream(uri) ?: throw ImageOpenException()
        } catch (error: ImageOpenException) {
            throw error
        } catch (error: SecurityException) {
            throw error
        } catch (error: Exception) {
            throw ImageOpenException(error)
        }
    }

    private fun validateDimensions(width: Int, height: Int) {
        if (width <= 0 || height <= 0) throw ImageDecodeException()
        val pixelCount = width.toLong() * height.toLong()
        if (pixelCount > MAX_IMAGE_PIXELS) {
            throw ImageTooLargeException(pixelCount, MAX_IMAGE_PIXELS)
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                }
        }.getOrNull().orEmpty().ifBlank {
            uri.lastPathSegment ?: DEFAULT_FILE_NAME
        }
    }

    companion object {
        const val MAX_IMAGE_PIXELS = 16_000_000L
        private const val DEFAULT_FILE_NAME = "未命名图片"
        private const val UNKNOWN_MIME_TYPE = "未知类型"
        private const val IMAGE_MIME_PREFIX = "image/"
    }
}
