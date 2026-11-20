package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.AmberTorch
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonLost
import com.example.ui.theme.EmeraldLock
import com.example.ui.theme.StudioBgRadialTop
import com.example.ui.theme.StudioBgVoid
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun HomeScreen(
    uiState: CameraLucidaUiState,
    onChooseImageClick: () -> Unit,
    onOpenCameraClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(StudioBgRadialTop, StudioBgVoid),
                    radius = 1200f
                )
            )
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(22.dp),
            color = StudioCardSurface,
            border = BorderStroke(1.dp, StudioBorder),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CameraLucidaHeroDiagram(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp)
                        .clip(RoundedCornerShape(14.dp))
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = StudioTextPrimary
                    )
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioTextMuted
                    )
                }

                // 4-step studio instructions
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x0AFFFFFF),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StepRow(number = 1, text = stringResource(R.string.step_1))
                        StepRow(number = 2, text = stringResource(R.string.step_2))
                        StepRow(number = 3, text = stringResource(R.string.step_3))
                        StepRow(number = 4, text = stringResource(R.string.step_4))
                    }
                }

                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onChooseImageClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("choose_image_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StudioBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0x14FFFFFF),
                            contentColor = StudioTextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (uiState.selectedImageUri == null) {
                                stringResource(R.string.btn_choose_image)
                            } else {
                                stringResource(R.string.btn_change_image)
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = onOpenCameraClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("open_camera_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CobaltPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_open_camera),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Selected image preview card
                AnimatedVisibility(visible = uiState.selectedImageUri != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x0DFFFFFF),
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = uiState.selectedImageUri,
                                contentDescription = stringResource(R.string.cd_preview_thumb),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.preview_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldLock
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = uiState.selectedImageName ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioTextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Camera error banner
                AnimatedVisibility(visible = uiState.homeErrorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x29EF4444),
                        border = BorderStroke(1.dp, Color(0x80EF4444))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                    tint = CrimsonLost,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = uiState.homeErrorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFECACA)
                                )
                            }
                            if (uiState.homeErrorIsPermissionDenied) {
                                TextButton(
                                    onClick = onOpenSettingsClick,
                                    modifier = Modifier.testTag("open_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = AmberTorch
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.btn_open_settings),
                                        color = AmberTorch,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(
    number: Int,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(CobaltPrimary.copy(alpha = 0.2f))
                .border(1.dp, CobaltPrimary.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF93C5FD),
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextSecondary
        )
    }
}

@Composable
private fun CameraLucidaHeroDiagram(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "projection_pulse"
    )

    val diagramDesc = stringResource(R.string.cd_hero_diagram)

    Canvas(
        modifier = modifier
            .background(Color(0xFF0A0D14))
            .semantics { contentDescription = diagramDesc }
    ) {
        val w = size.width
        val h = size.height

        // Subtle studio grid lines
        val gridColor = Color(0x12FFFFFF)
        val step = 28f
        var gx = step
        while (gx < w) {
            drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
            gx += step
        }
        var gy = step
        while (gy < h) {
            drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
            gy += step
        }

        // Clean paper sheet slightly tilted
        val sheetW = w * 0.62f
        val sheetH = h * 0.72f
        val sheetLeft = (w - sheetW) / 2f
        val sheetTop = (h - sheetH) / 2f

        rotate(degrees = -3f, pivot = Offset(w / 2f, h / 2f)) {
            drawRoundRect(
                color = Color(0xFFF8FAFC),
                topLeft = Offset(sheetLeft, sheetTop),
                size = Size(sheetW, sheetH),
                cornerRadius = CornerRadius(10f, 10f)
            )

            // Translucent projected botanical / geometric outline
            val cx = w / 2f
            val cy = h / 2f
            val sketchPath = Path().apply {
                moveTo(cx, cy - sheetH * 0.28f)
                lineTo(cx + sheetW * 0.18f, cy)
                lineTo(cx, cy + sheetH * 0.28f)
                lineTo(cx - sheetW * 0.18f, cy)
                close()
            }
            drawPath(
                path = sketchPath,
                color = CobaltPrimary.copy(alpha = pulse),
                style = Stroke(
                    width = 2.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            )
            drawCircle(
                color = CobaltPrimary.copy(alpha = pulse * 0.65f),
                radius = sheetH * 0.16f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.2f)
            )
            drawCircle(
                color = EmeraldLock.copy(alpha = pulse),
                radius = 5f,
                center = Offset(cx, cy)
            )
        }

        // Optical viewfinder brackets
        val bracketLen = 16f
        val pad = 12f
        val bracketColor = Color(0x4DFFFFFF)
        drawLine(bracketColor, Offset(pad, pad), Offset(pad + bracketLen, pad), 2f, StrokeCap.Round)
        drawLine(bracketColor, Offset(pad, pad), Offset(pad, pad + bracketLen), 2f, StrokeCap.Round)
        drawLine(bracketColor, Offset(w - pad, h - pad), Offset(w - pad - bracketLen, h - pad), 2f, StrokeCap.Round)
        drawLine(bracketColor, Offset(w - pad, h - pad), Offset(w - pad, h - pad - bracketLen), 2f, StrokeCap.Round)
    }
}
