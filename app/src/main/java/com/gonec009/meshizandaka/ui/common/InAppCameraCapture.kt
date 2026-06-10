package com.gonec009.meshizandaka.ui.common

import android.Manifest
import android.content.pm.PackageManager
import android.util.Rational
import android.view.Surface
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.gonec009.meshizandaka.R
import kotlinx.coroutines.launch

@Composable
fun InAppCameraCapture(
    folderName: String,
    filePrefix: String,
    onCaptured: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(previewView, hasPermission, lifecycleOwner) {
        val currentPreviewView = previewView
        if (!hasPermission || currentPreviewView == null) {
            onDispose { }
        } else {
            val future = ProcessCameraProvider.getInstance(context)
            val listener = Runnable {
                val provider = future.get()
                val preview = Preview.Builder().build().also { useCase ->
                    useCase.surfaceProvider = currentPreviewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                val viewport = currentPreviewView.viewPort
                    ?: ViewPort.Builder(Rational(16, 9), currentPreviewView.display?.rotation ?: Surface.ROTATION_0).build()
                val group = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(capture)
                    .setViewPort(viewport)
                    .build()
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, group)
                imageCapture = capture
            }
            future.addListener(listener, executor)
            onDispose {
                runCatching { future.get().unbindAll() }
                imageCapture = null
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.94f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                        Text(
                            text = stringResource(R.string.camera_capture_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                        )
                        Text(
                            text = stringResource(R.string.camera_capture_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(18.dp)),
                    ) {
                        AndroidView(
                            factory = { viewContext ->
                                PreviewView(viewContext).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                    )
                                }.also { previewView = it }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = { previewView = it },
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent),
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth(0.9f)
                                    .aspectRatio(16f / 9f)
                                    .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val capture = imageCapture ?: return@Button
                            val target = createManagedPhotoTarget(context, folderName, filePrefix)
                            capture.targetRotation = previewView?.display?.rotation ?: Surface.ROTATION_0
                            isCapturing = true
                            capture.takePicture(
                                ImageCapture.OutputFileOptions.Builder(target.file).build(),
                                executor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        scope.launch {
                                            val optimized = optimizeCapturedPhoto(context, target.uri)
                                            isCapturing = false
                                            onCaptured(optimized)
                                            onDismiss()
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        scope.launch {
                                            discardCapturedPhoto(context, target.uri)
                                            isCapturing = false
                                        }
                                    }
                                },
                            )
                        },
                        enabled = !isCapturing && imageCapture != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.camera_capture_button))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.camera_permission_message),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text(stringResource(R.string.camera_permission_action))
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}
