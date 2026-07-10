package com.lzx.imagehistogramanalyzer.data.camera

import androidx.camera.core.Camera
import androidx.camera.core.TorchState
import com.google.common.util.concurrent.ListenableFuture
import com.lzx.imagehistogramanalyzer.domain.camera.CameraAdjustmentController
import com.lzx.imagehistogramanalyzer.domain.camera.CameraAdjustmentState
import com.lzx.imagehistogramanalyzer.domain.photo.PhotoCoachResult
import com.lzx.imagehistogramanalyzer.domain.photo.TorchAction
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** CameraX 曝光补偿与补光灯控制器；所有动作都由用户点击后才触发。 */
class CameraXAdjustmentController(
    private val camera: Camera,
) : CameraAdjustmentController {
    override fun currentState(): CameraAdjustmentState {
        val exposureState = camera.cameraInfo.exposureState
        val range = exposureState.exposureCompensationRange
        val exposureSupported = exposureState.isExposureCompensationSupported
        val hasFlashUnit = camera.cameraInfo.hasFlashUnit()
        return CameraAdjustmentState(
            isExposureSupported = exposureSupported,
            exposureIndex = exposureState.exposureCompensationIndex,
            minExposureIndex = range.lower,
            maxExposureIndex = range.upper,
            hasFlashUnit = hasFlashUnit,
            isTorchOn = hasFlashUnit && camera.cameraInfo.torchState.value == TorchState.ON,
        )
    }

    override suspend fun adjustExposureBy(delta: Int): CameraAdjustmentState {
        val state = currentState()
        if (!state.isExposureSupported) {
            return state.copy(message = "当前设备不支持曝光补偿。")
        }
        if (delta == 0) {
            return state.copy(message = "曝光补偿保持不变。")
        }

        val target = (state.exposureIndex + delta)
            .coerceIn(state.minExposureIndex, state.maxExposureIndex)
        if (target == state.exposureIndex) {
            val edge = if (delta > 0) "上限" else "下限"
            return state.copy(message = "曝光补偿已到$edge。")
        }

        val appliedIndex = camera.cameraControl
            .setExposureCompensationIndex(target)
            .awaitFuture()
        return currentState().copy(message = "曝光补偿已调整为 $appliedIndex。")
    }

    override suspend fun toggleTorch(): CameraAdjustmentState {
        val state = currentState()
        if (!state.hasFlashUnit) {
            return state.copy(message = "当前设备不支持补光灯。")
        }
        val target = !state.isTorchOn
        camera.cameraControl.enableTorch(target).awaitFuture()
        return currentState().copy(
            message = if (target) "补光灯已开启。" else "补光灯已关闭。",
        )
    }

    override suspend fun applySuggestion(coachResult: PhotoCoachResult): CameraAdjustmentState {
        val messages = mutableListOf<String>()
        var state = currentState()

        if (coachResult.exposureDelta != 0) {
            state = adjustExposureBy(coachResult.exposureDelta)
            state.message?.let(messages::add)
        } else {
            messages += "曝光补偿保持不变。"
        }

        when (coachResult.torchAction) {
            TorchAction.TURN_ON -> {
                state = setTorchEnabled(enabled = true)
                state.message?.let(messages::add)
            }

            TorchAction.TURN_OFF -> {
                state = setTorchEnabled(enabled = false)
                state.message?.let(messages::add)
            }

            TorchAction.KEEP -> {
                messages += "补光灯保持不变。"
            }
        }

        return currentState().copy(message = messages.joinToString(" "))
    }

    private suspend fun setTorchEnabled(enabled: Boolean): CameraAdjustmentState {
        val state = currentState()
        if (!state.hasFlashUnit) {
            return state.copy(message = "当前设备不支持补光灯。")
        }
        if (state.isTorchOn == enabled) {
            return state.copy(message = if (enabled) "补光灯已处于开启状态。" else "补光灯已处于关闭状态。")
        }
        camera.cameraControl.enableTorch(enabled).awaitFuture()
        return currentState().copy(
            message = if (enabled) "补光灯已开启。" else "补光灯已关闭。",
        )
    }
}

private suspend fun <T> ListenableFuture<T>.awaitFuture(): T =
    suspendCancellableCoroutine { continuation ->
        addListener(
            {
                try {
                    continuation.resume(get())
                } catch (error: ExecutionException) {
                    continuation.resumeWithException(error.cause ?: error)
                } catch (error: Throwable) {
                    continuation.resumeWithException(error)
                }
            },
            DirectExecutor,
        )
        continuation.invokeOnCancellation { cancel(true) }
    }

private object DirectExecutor : Executor {
    override fun execute(command: Runnable) = command.run()
}
