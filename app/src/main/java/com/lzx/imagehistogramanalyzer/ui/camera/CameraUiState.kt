package com.lzx.imagehistogramanalyzer.ui.camera

import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis
import com.lzx.imagehistogramanalyzer.domain.photo.PhotoCoachResult

data class CameraUiState(
    val hasCameraPermission: Boolean = false,
    val isBindingCamera: Boolean = false,
    val latestAnalysis: RealtimeCameraAnalysis? = null,
    val frozenFrame: FrozenCameraFrame? = null,
    val coachResult: PhotoCoachResult? = null,
    val isSavingFrozenFrame: Boolean = false,
    val snapshotMessage: String? = null,
    val errorMessage: String? = null,
)
