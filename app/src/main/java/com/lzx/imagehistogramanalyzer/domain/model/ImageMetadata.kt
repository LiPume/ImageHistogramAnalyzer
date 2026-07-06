package com.lzx.imagehistogramanalyzer.domain.model

/** 选中图片的只读基础信息，避免 UI 直接依赖 ContentResolver 查询细节。 */
data class ImageMetadata(
    val displayName: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
) {
    val pixelCount: Long = width.toLong() * height.toLong()
}
