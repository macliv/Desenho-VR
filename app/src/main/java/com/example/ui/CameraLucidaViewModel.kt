package com.example.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class AppScreen {
    HOME,
    CAMERA
}

data class RefTransform(
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotationRad: Float = 0f,
    val opacity: Float = 0.5f
)

data class WorldViewTransform(
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f
)

data class CameraLucidaUiState(
    val screen: AppScreen = AppScreen.HOME,
    val selectedImageUri: Uri? = null,
    val selectedImageName: String? = null,
    val homeErrorMessage: String? = null,
    val homeErrorIsPermissionDenied: Boolean = false,
    val stageWidth: Float = 0f,
    val stageHeight: Float = 0f,
    val refTransform: RefTransform = RefTransform(),
    val worldView: WorldViewTransform = WorldViewTransform(),
    val isLocked: Boolean = false,
    val isPanelVisible: Boolean = true,
    val hasFlashUnit: Boolean = false,
    val torchOn: Boolean = false,
    val supportsCamZoom: Boolean = false,
    val camZoom: Float = 1f,
    val minCamZoom: Float = 1f,
    val maxCamZoom: Float = 1f,
    val temporaryNotice: String? = null
) {
    val refDegrees: Float
        get() = refTransform.rotationRad * (180f / PI.toFloat())
}

class CameraLucidaViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CameraLucidaUiState())
    val uiState: StateFlow<CameraLucidaUiState> = _uiState.asStateFlow()

    private var noticeJob: Job? = null

    fun onImageSelected(context: Context, uri: Uri) {
        val displayName = resolveDisplayName(context, uri) ?: "Imagem selecionada"
        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                selectedImageName = displayName,
                homeErrorMessage = null,
                homeErrorIsPermissionDenied = false
            )
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        } catch (_: Exception) {
            null
        }
    }

    fun openCameraScreen() {
        _uiState.update {
            it.copy(
                screen = AppScreen.CAMERA,
                homeErrorMessage = null,
                homeErrorIsPermissionDenied = false
            )
        }
    }

    fun showCameraPermissionDenied(context: Context) {
        _uiState.update {
            it.copy(
                screen = AppScreen.HOME,
                homeErrorMessage = context.getString(R.string.error_camera_denied),
                homeErrorIsPermissionDenied = true
            )
        }
    }

    fun showCameraUnavailable(context: Context) {
        _uiState.update {
            it.copy(
                screen = AppScreen.HOME,
                homeErrorMessage = context.getString(R.string.error_camera_unavailable),
                homeErrorIsPermissionDenied = false
            )
        }
    }

    fun navigateHome() {
        _uiState.update {
            it.copy(
                screen = AppScreen.HOME,
                isLocked = false,
                torchOn = false,
                worldView = WorldViewTransform(),
                temporaryNotice = null
            )
        }
    }

    fun updateStageSize(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        _uiState.update { it.copy(stageWidth = width, stageHeight = height) }
    }

    fun updateCameraCapabilities(
        hasFlash: Boolean,
        minZoom: Float,
        maxZoom: Float,
        currentZoom: Float
    ) {
        _uiState.update {
            it.copy(
                hasFlashUnit = hasFlash,
                supportsCamZoom = maxZoom > minZoom + 0.05f,
                minCamZoom = minZoom,
                maxCamZoom = maxZoom,
                camZoom = currentZoom.coerceIn(minZoom, max(minZoom, maxZoom))
            )
        }
    }

    fun setTorchState(on: Boolean) {
        _uiState.update { it.copy(torchOn = on) }
    }

    fun setCamZoom(zoom: Float) {
        _uiState.update { state ->
            state.copy(camZoom = zoom.coerceIn(state.minCamZoom, max(state.minCamZoom, state.maxCamZoom)))
        }
    }

    fun togglePanelVisibility() {
        _uiState.update { it.copy(isPanelVisible = !it.isPanelVisible) }
    }

    fun showTemporaryNotice(message: String) {
        noticeJob?.cancel()
        _uiState.update { it.copy(temporaryNotice = message) }
        noticeJob = viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(temporaryNotice = null) }
        }
    }

    fun toggleLock() {
        _uiState.update { state ->
            if (state.isLocked) {
                state.copy(
                    isLocked = false,
                    worldView = WorldViewTransform(0f, 0f, 1f)
                )
            } else {
                state.copy(isLocked = true)
            }
        }
    }

    fun setOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0f, 1f)
        _uiState.update { state ->
            state.copy(refTransform = state.refTransform.copy(opacity = clamped))
        }
    }

    fun setZoomSlider(value: Float) {
        _uiState.update { state ->
            if (!state.isLocked) {
                state.copy(refTransform = state.refTransform.copy(scale = value.coerceIn(0.2f, 4.0f)))
            } else {
                val targetScale = value.coerceIn(1.0f, 5.0f)
                val factor = targetScale / state.worldView.scale
                val clamped = clampWorldView(
                    x = state.worldView.x * factor,
                    y = state.worldView.y * factor,
                    scale = targetScale,
                    stageW = state.stageWidth,
                    stageH = state.stageHeight
                )
                state.copy(worldView = clamped)
            }
        }
    }

    fun setPosXSlider(value: Float) {
        _uiState.update { state ->
            if (!state.isLocked) {
                state.copy(refTransform = state.refTransform.copy(x = value))
            } else {
                val clamped = clampWorldView(
                    x = value,
                    y = state.worldView.y,
                    scale = state.worldView.scale,
                    stageW = state.stageWidth,
                    stageH = state.stageHeight
                )
                state.copy(worldView = clamped)
            }
        }
    }

    fun setPosYSlider(value: Float) {
        _uiState.update { state ->
            if (!state.isLocked) {
                state.copy(refTransform = state.refTransform.copy(y = value))
            } else {
                val clamped = clampWorldView(
                    x = state.worldView.x,
                    y = value,
                    scale = state.worldView.scale,
                    stageW = state.stageWidth,
                    stageH = state.stageHeight
                )
                state.copy(worldView = clamped)
            }
        }
    }

    fun centerTransform() {
        _uiState.update { state ->
            if (!state.isLocked) {
                state.copy(
                    refTransform = state.refTransform.copy(x = 0f, y = 0f, scale = 1f, rotationRad = 0f)
                )
            } else {
                state.copy(worldView = WorldViewTransform(0f, 0f, 1f))
            }
        }
    }

    fun onSingleFingerDrag(dx: Float, dy: Float) {
        _uiState.update { state ->
            if (!state.isLocked) {
                state.copy(
                    refTransform = state.refTransform.copy(
                        x = state.refTransform.x + dx,
                        y = state.refTransform.y + dy
                    )
                )
            } else {
                val clamped = clampWorldView(
                    x = state.worldView.x + dx,
                    y = state.worldView.y + dy,
                    scale = state.worldView.scale,
                    stageW = state.stageWidth,
                    stageH = state.stageHeight
                )
                state.copy(worldView = clamped)
            }
        }
    }

    fun onTwoFingerGesture(
        prevCentroidX: Float,
        prevCentroidY: Float,
        curCentroidX: Float,
        curCentroidY: Float,
        scaleFactor: Float,
        dAngle: Float
    ) {
        _uiState.update { state ->
            if (state.stageWidth <= 0f || state.stageHeight <= 0f) {
                return@update state
            }
            val halfW = state.stageWidth / 2f
            val halfH = state.stageHeight / 2f
            val pmX = prevCentroidX - halfW
            val pmY = prevCentroidY - halfH
            val cmX = curCentroidX - halfW
            val cmY = curCentroidY - halfH

            if (!state.isLocked) {
                val vx = state.refTransform.x - pmX
                val vy = state.refTransform.y - pmY
                val cosA = cos(dAngle)
                val sinA = sin(dAngle)
                val newX = cmX + (vx * cosA - vy * sinA) * scaleFactor
                val newY = cmY + (vx * sinA + vy * cosA) * scaleFactor
                val newScale = (state.refTransform.scale * scaleFactor).coerceIn(0.2f, 4.0f)
                val newRot = state.refTransform.rotationRad + dAngle
                state.copy(
                    refTransform = state.refTransform.copy(
                        x = newX,
                        y = newY,
                        scale = newScale,
                        rotationRad = newRot
                    )
                )
            } else {
                val newScale = (state.worldView.scale * scaleFactor).coerceIn(1.0f, 5.0f)
                val actualFactor = newScale / state.worldView.scale
                val newX = cmX - (pmX - state.worldView.x) * actualFactor
                val newY = cmY - (pmY - state.worldView.y) * actualFactor
                val clamped = clampWorldView(
                    x = newX,
                    y = newY,
                    scale = newScale,
                    stageW = state.stageWidth,
                    stageH = state.stageHeight
                )
                state.copy(worldView = clamped)
            }
        }
    }

    private fun clampWorldView(
        x: Float,
        y: Float,
        scale: Float,
        stageW: Float,
        stageH: Float
    ): WorldViewTransform {
        val s = scale.coerceIn(1.0f, 5.0f)
        val maxX = if (stageW > 0f) stageW * 0.65f * s + 100f else 800f
        val maxY = if (stageH > 0f) stageH * 0.65f * s + 100f else 800f
        return WorldViewTransform(
            x = x.coerceIn(-maxX, maxX),
            y = y.coerceIn(-maxY, maxY),
            scale = s
        )
    }
}
