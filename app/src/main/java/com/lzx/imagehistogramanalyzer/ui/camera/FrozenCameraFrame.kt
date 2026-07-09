package com.lzx.imagehistogramanalyzer.ui.camera

import android.graphics.Bitmap
import android.net.Uri
import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis

/** 用户从实时预览中定格下来的画面及其对应分析结果。 */
data class FrozenCameraFrame(
    val bitmap: Bitmap,
    val analysis: RealtimeCameraAnalysis,
    val savedUri: Uri? = null,
)
