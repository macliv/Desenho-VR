package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.CameraLucidaViewModel
import com.example.ui.CameraScreen
import com.example.ui.HomeScreen
import com.example.ui.theme.CameraLucidaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            CameraLucidaTheme {
                val context = LocalContext.current
                val viewModel: CameraLucidaViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                val photoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia(),
                    onResult = { uri ->
                        if (uri != null) {
                            viewModel.onImageSelected(context, uri)
                        }
                    }
                )

                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { granted ->
                        if (granted) {
                            viewModel.openCameraScreen()
                        } else {
                            viewModel.showCameraPermissionDenied(context)
                        }
                    }
                )

                val launchPhotoPicker = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }

                val launchCameraWithPermissionCheck = {
                    val hasCamera = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasCamera) {
                        viewModel.openCameraScreen()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                Crossfade(
                    targetState = uiState.screen,
                    label = "screen_transition",
                    modifier = Modifier.fillMaxSize()
                ) { screen ->
                    when (screen) {
                        AppScreen.HOME -> {
                            HomeScreen(
                                uiState = uiState,
                                onChooseImageClick = launchPhotoPicker,
                                onOpenCameraClick = launchCameraWithPermissionCheck,
                                onOpenSettingsClick = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                }
                            )
                        }

                        AppScreen.CAMERA -> {
                            CameraScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onPickImageClick = launchPhotoPicker,
                                onBackToHome = { viewModel.navigateHome() }
                            )
                        }
                    }
                }
            }
        }
    }
}
