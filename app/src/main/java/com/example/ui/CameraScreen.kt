package com.example.ui

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.AmberOnTorch
import com.example.ui.theme.AmberTorch
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonBanner
import com.example.ui.theme.EmeraldLock
import com.example.ui.theme.EmeraldOnLock
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioGlassPanel
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun CameraScreen(
    uiState: CameraLucidaUiState,
    viewModel: CameraLucidaViewModel,
    onPickImageClick: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentView = LocalView.current

    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    BackHandler {
        boundCamera?.cameraControl?.enableTorch(false)
        onBackToHome()
    }

    // Keep screen awake while tracing
    DisposableEffect(currentView) {
        val previous = currentView.keepScreenOn
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = previous
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // COMPATIBLE (TextureView) allows seamless Compose graphicsLayer scaling/panning of camera + drawing together
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var cameraProvider: ProcessCameraProvider? = null

        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val resolutionSelector = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                    .build()

                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val selector = if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                provider.unbindAll()
                val camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview
                )
                boundCamera = camera

                val zoomState = camera.cameraInfo.zoomState.value
                viewModel.updateCameraCapabilities(
                    hasFlash = camera.cameraInfo.hasFlashUnit(),
                    minZoom = zoomState?.minZoomRatio ?: 1f,
                    maxZoom = zoomState?.maxZoomRatio ?: 1f,
                    currentZoom = zoomState?.zoomRatio ?: 1f
                )
            } catch (_: Exception) {
                viewModel.showCameraUnavailable(context)
            }
        }, mainExecutor)

        onDispose {
            boundCamera?.cameraControl?.enableTorch(false)
            cameraProvider?.unbindAll()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // STAGE: gesture area + #world container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .onSizeChanged { size ->
                    viewModel.updateStageSize(size.width.toFloat(), size.height.toFloat())
                }
                .pointerInput(uiState.isLocked) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)

                        var prevCentroid: Offset? = null
                        var prevDist: Float? = null
                        var prevAngle: Float? = null
                        var prevPointerCount = 0

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break

                            if (pressed.size == 1) {
                                val p = pressed[0]
                                if (prevPointerCount == 1 && p.positionChanged()) {
                                    val dx = p.position.x - p.previousPosition.x
                                    val dy = p.position.y - p.previousPosition.y
                                    viewModel.onSingleFingerDrag(dx, dy)
                                    p.consume()
                                }
                                prevPointerCount = 1
                                prevCentroid = null
                                prevDist = null
                                prevAngle = null
                            } else {
                                val p1 = pressed[0].position
                                val p2 = pressed[1].position
                                val cx = (p1.x + p2.x) / 2f
                                val cy = (p1.y + p2.y) / 2f
                                val dist = hypot(p2.x - p1.x, p2.y - p1.y)
                                val angle = atan2(p2.y - p1.y, p2.x - p1.x)

                                val lastC = prevCentroid
                                val lastD = prevDist
                                val lastA = prevAngle
                                if (prevPointerCount >= 2 && lastC != null && lastD != null && lastA != null && lastD > 5f) {
                                    val scaleFactor = dist / lastD
                                    var dAngle = angle - lastA
                                    if (dAngle > Math.PI) dAngle -= (2.0 * Math.PI).toFloat()
                                    if (dAngle < -Math.PI) dAngle += (2.0 * Math.PI).toFloat()

                                    viewModel.onTwoFingerGesture(
                                        prevCentroidX = lastC.x,
                                        prevCentroidY = lastC.y,
                                        curCentroidX = cx,
                                        curCentroidY = cy,
                                        scaleFactor = scaleFactor,
                                        dAngle = dAngle
                                    )
                                    pressed.forEach { it.consume() }
                                }
                                prevPointerCount = pressed.size
                                prevCentroid = Offset(cx, cy)
                                prevDist = dist
                                prevAngle = angle
                            }
                        }
                    }
                }
        ) {
            // WORLD: camera video + reference image transform together when locked
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = uiState.worldView.x
                        translationY = uiState.worldView.y
                        scaleX = uiState.worldView.scale
                        scaleY = uiState.worldView.scale
                    }
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Reference overlay image
                if (uiState.selectedImageUri != null) {
                    val ref = uiState.refTransform
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = uiState.selectedImageUri,
                            contentDescription = stringResource(R.string.cd_ref_image),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth(0.78f)
                                .graphicsLayer {
                                    translationX = ref.x
                                    translationY = ref.y
                                    scaleX = ref.scale
                                    scaleY = ref.scale
                                    rotationZ = uiState.refDegrees
                                    alpha = ref.opacity
                                }
                        )
                    }
                }
            }
        }

        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left badges + back button
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = StudioGlassPanel,
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    IconButton(
                        onClick = {
                            boundCamera?.cameraControl?.enableTorch(false)
                            onBackToHome()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("camera_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint = StudioTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val statusText = if (uiState.isLocked) {
                        stringResource(R.string.status_locked)
                    } else {
                        stringResource(R.string.status_free)
                    }
                    val statusBg = if (uiState.isLocked) Color(0x3810B981) else StudioGlassPanel
                    val statusBorder = if (uiState.isLocked) Color(0x9910B981) else StudioBorder
                    val statusFg = if (uiState.isLocked) Color(0xFFA7F3D0) else StudioTextPrimary

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = statusBg,
                        border = BorderStroke(1.dp, statusBorder)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusFg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = StudioGlassPanel,
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Text(
                            text = stringResource(R.string.badge_wake_lock),
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioTextSecondary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(6.dp))

            // Right top actions: LED, Imagem, Ocultar
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.hasFlashUnit) {
                    TopActionChip(
                        text = if (uiState.torchOn) stringResource(R.string.btn_led_on) else stringResource(R.string.btn_led),
                        icon = if (uiState.torchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                        containerColor = if (uiState.torchOn) AmberTorch else StudioGlassPanel,
                        contentColor = if (uiState.torchOn) AmberOnTorch else StudioTextPrimary,
                        borderColor = if (uiState.torchOn) Color.Transparent else StudioBorder,
                        testTag = "torch_button",
                        onClick = {
                            val camera = boundCamera ?: return@TopActionChip
                            val next = !uiState.torchOn
                            val future = camera.cameraControl.enableTorch(next)
                            future.addListener({
                                try {
                                    future.get()
                                    viewModel.setTorchState(next)
                                } catch (_: Exception) {
                                    viewModel.showTemporaryNotice(context.getString(R.string.warning_torch_failed))
                                }
                            }, ContextCompat.getMainExecutor(context))
                        }
                    )
                }

                TopActionChip(
                    text = stringResource(R.string.btn_image),
                    icon = Icons.Filled.Image,
                    containerColor = StudioGlassPanel,
                    contentColor = StudioTextPrimary,
                    borderColor = StudioBorder,
                    testTag = "change_image_top_button",
                    onClick = onPickImageClick
                )

                TopActionChip(
                    text = stringResource(R.string.btn_hide),
                    icon = Icons.Filled.VisibilityOff,
                    containerColor = StudioGlassPanel,
                    contentColor = StudioTextPrimary,
                    borderColor = StudioBorder,
                    testTag = "hide_panel_button",
                    onClick = { viewModel.togglePanelVisibility() }
                )
            }
        }

        // FLOATING NOTICE BELOW TOP BAR
        AnimatedVisibility(
            visible = uiState.temporaryNotice != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 68.dp, start = 14.dp, end = 14.dp)
                .align(Alignment.TopCenter)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CrimsonBanner,
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = uiState.temporaryNotice ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }

        // BOTTOM CONTROLS PANEL
        AnimatedVisibility(
            visible = uiState.isPanelVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = StudioGlassPanel,
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (uiState.isLocked) {
                            stringResource(R.string.mode_note_locked)
                        } else {
                            stringResource(R.string.mode_note_free)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Action row: Travar desenho / Centralizar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.toggleLock() },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                                .testTag("lock_button"),
                            shape = RoundedCornerShape(11.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isLocked) AmberTorch else EmeraldLock,
                                contentColor = if (uiState.isLocked) AmberOnTorch else EmeraldOnLock
                            )
                        ) {
                            Icon(
                                imageVector = if (uiState.isLocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isLocked) stringResource(R.string.btn_unlock) else stringResource(R.string.btn_lock),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = { viewModel.centerTransform() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("center_button"),
                            shape = RoundedCornerShape(11.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x1AFFFFFF),
                                contentColor = StudioTextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CenterFocusStrong,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.btn_center),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 2-column Sliders Grid
                    val zoomValue = if (uiState.isLocked) uiState.worldView.scale else uiState.refTransform.scale
                    val zoomRange = if (uiState.isLocked) 1.0f..5.0f else 0.2f..4.0f
                    val posXValue = if (uiState.isLocked) uiState.worldView.x else uiState.refTransform.x
                    val posYValue = if (uiState.isLocked) uiState.worldView.y else uiState.refTransform.y
                    val posLimitX = if (uiState.isLocked && uiState.stageWidth > 0f) {
                        uiState.stageWidth * 0.65f * uiState.worldView.scale + 100f
                    } else 500f
                    val posLimitY = if (uiState.isLocked && uiState.stageHeight > 0f) {
                        uiState.stageHeight * 0.65f * uiState.worldView.scale + 100f
                    } else 500f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StudioSlider(
                            label = stringResource(R.string.label_opacity),
                            valueLabel = "${(uiState.refTransform.opacity * 100f).roundToInt()}%",
                            value = uiState.refTransform.opacity,
                            valueRange = 0f..1f,
                            onValueChange = { viewModel.setOpacity(it) },
                            modifier = Modifier.weight(1f)
                        )
                        StudioSlider(
                            label = if (uiState.isLocked) stringResource(R.string.label_zoom_locked) else stringResource(R.string.label_zoom_drawing),
                            valueLabel = String.format(Locale.US, "%.2fx", zoomValue),
                            value = zoomValue.coerceIn(zoomRange.start, zoomRange.endInclusive),
                            valueRange = zoomRange,
                            onValueChange = { viewModel.setZoomSlider(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StudioSlider(
                            label = if (uiState.isLocked) stringResource(R.string.label_move_x) else stringResource(R.string.label_pos_x),
                            valueLabel = "${posXValue.roundToInt()} px",
                            value = posXValue.coerceIn(-posLimitX, posLimitX),
                            valueRange = -posLimitX..posLimitX,
                            onValueChange = { viewModel.setPosXSlider(it) },
                            modifier = Modifier.weight(1f)
                        )
                        StudioSlider(
                            label = if (uiState.isLocked) stringResource(R.string.label_move_y) else stringResource(R.string.label_pos_y),
                            valueLabel = "${posYValue.roundToInt()} px",
                            value = posYValue.coerceIn(-posLimitY, posLimitY),
                            valueRange = -posLimitY..posLimitY,
                            onValueChange = { viewModel.setPosYSlider(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (uiState.supportsCamZoom) {
                        StudioSlider(
                            label = stringResource(R.string.label_cam_zoom),
                            valueLabel = String.format(Locale.US, "%.1fx", uiState.camZoom),
                            value = uiState.camZoom,
                            valueRange = uiState.minCamZoom..uiState.maxCamZoom,
                            onValueChange = { z ->
                                viewModel.setCamZoom(z)
                                boundCamera?.cameraControl?.setZoomRatio(z)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // SHOW PANEL FLOATING BUTTON WHEN HIDDEN
        AnimatedVisibility(
            visible = !uiState.isPanelVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = 14.dp)
        ) {
            Button(
                onClick = { viewModel.togglePanelVisibility() },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("show_panel_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioGlassPanel,
                    contentColor = StudioTextPrimary
                ),
                border = BorderStroke(1.dp, StudioBorder)
            ) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.btn_show_panel),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun TopActionChip(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .height(40.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StudioSlider(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = StudioTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelSmall,
                color = StudioTextPrimary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
            colors = SliderDefaults.colors(
                thumbColor = CobaltPrimary,
                activeTrackColor = CobaltPrimary,
                inactiveTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}
