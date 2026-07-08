package com.lzx.imagehistogramanalyzer.data.image

import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface ImageLoader {
    suspend fun load(uri: Uri): DecodedImage
}

class ImageRepository(
    private val bitmapDecoder: BitmapDecoder,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ImageLoader {
    override suspend fun load(uri: Uri): DecodedImage = withContext(ioDispatcher) {
        bitmapDecoder.decode(uri)
    }
}
