package com.lzx.imagehistogramanalyzer.ui.camera

import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis
import com.lzx.imagehistogramanalyzer.domain.photo.PhotoCoachResult

data class CameraUiState(
    val hasCameraPermission: Boolean = false,
    val isBindingCamera: Boolean = false,
    val latestAnalysis: RealtimeCameraAnalysis? = null,
    val coachResult: PhotoCoachResult? = null,
    val errorMessage: String? = null,
)
