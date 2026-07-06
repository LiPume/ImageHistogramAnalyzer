package com.lzx.imagehistogramanalyzer.data.image

import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageRepository(
    private val bitmapDecoder: BitmapDecoder,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun load(uri: Uri): DecodedImage = withContext(ioDispatcher) {
        bitmapDecoder.decode(uri)
    }
}
