package com.lzx.imagehistogramanalyzer.domain.camera

/** 相机曝光补偿和补光灯的可调状态；不直接持有 CameraX 对象，便于 UI 展示和测试。 */
data class CameraAdjustmentState(
    val isExposureSupported: Boolean = false,
    val exposureIndex: Int = 0,
    val minExposureIndex: Int = 0,
    val maxExposureIndex: Int = 0,
    val hasFlashUnit: Boolean = false,
    val isTorchOn: Boolean = false,
    val isAdjusting: Boolean = false,
    val message: String? = null,
) {
    val canDecreaseExposure: Boolean
        get() = isExposureSupported && !isAdjusting && exposureIndex > minExposureIndex

    val canIncreaseExposure: Boolean
        get() = isExposureSupported && !isAdjusting && exposureIndex < maxExposureIndex

    val canToggleTorch: Boolean
        get() = hasFlashUnit && !isAdjusting
}
