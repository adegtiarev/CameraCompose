package arg.adegtiarev.cameracompose.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraMode by viewModel.cameraMode.collectAsStateWithLifecycle()

    val buttonColor = if (cameraMode == CameraMode.VIDEO) {
        if (uiState.isRecording) Color.Red else Color.Red.copy(alpha = 0.5f)
    } else {
        Color.White
    }

    // Bind camera to lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        viewModel.onBindCamera(lifecycleOwner)
    }

    // Unbind camera when view is disposed
    DisposableEffect(lifecycleOwner) {
        onDispose {
            // Here we can call the stop recording method,
            // so as not to leave "hanging" processes
            viewModel.stopRecording()
        }
    }

    // Subscribe to events
    val context = LocalContext.current
    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is CameraEvent.Toast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // State for permissions (CAMERA, RECORD_AUDIO)
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    // if permissions are granted, show camera viewfinder
    if (permissionsState.allPermissionsGranted) {
        val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()

        surfaceRequest?.let { request ->
            Box(modifier = Modifier.fillMaxSize()) {
                CameraXViewfinder(
                    surfaceRequest = request,
                    modifier = Modifier.fillMaxSize()
                )

                if (uiState.isRecording) {
                    RecordTimer(
                        seconds = uiState.durationSeconds,
                        isPaused = uiState.isPaused,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 32.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Mode switcher
                    Row(
                        modifier = Modifier.padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CameraMode.entries.forEach { mode ->
                            val isSelected = cameraMode == mode
                            Text(
                                text = mode.name,
                                color = Color.White,
                                modifier = Modifier
                                    .alpha(if (isSelected) 1f else 0.5f) // Inactive modes are semi-transparent
                                    .clickable { viewModel.setCameraMode(mode) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Left ballast for symmetry (weight 1)
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            // Empty here, but this place "holds" the left edge
                        }

                        // 2. Center (no weight, fixed size)
                        CaptureButton(
                            onClick = { viewModel.onCaptureClick() },
                            color = buttonColor
                        )

                        // 3. Right part (weight 1)
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (uiState.isRecording) {
                                PauseButton(
                                    isPaused = uiState.isPaused,
                                    onClick = {
                                        if (uiState.isPaused) viewModel.resumeRecording()
                                        else viewModel.pauseRecording()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

    } else {
        // if permissions are not granted, show button to grant permissions
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val textToShow = if (permissionsState.shouldShowRationale) {
                "The camera is important for this app. Please grant the permission."
            } else {
                "Camera permission is required for this feature."
            }
            Text(textToShow)
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Grant permission")
            }
        }
    }
}

@Composable
fun CaptureButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1f,
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(BorderStroke(4.dp, color), CircleShape)
        )
        // Inner circle
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(scale)
                .background(color, CircleShape)
        )
    }
}

@Composable
fun PauseButton(
    isPaused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        label = "scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(56.dp)
            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isPaused) {
            // Play Icon (Resume) - Drawn manually
            Canvas(modifier = Modifier.size(20.dp)) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, size.height / 2)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, Color.White)
            }
        } else {
            // Pause Icon - Drawn manually
            Canvas(modifier = Modifier.size(20.dp)) {
                val barWidth = size.width / 3
                val cornerRadius = 2.dp.toPx()

                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(0f, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(cornerRadius)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(size.width - barWidth, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(cornerRadius)
                )
            }
        }
    }
}

@Composable
fun RecordTimer(
    seconds: Int,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    // In Compose it is very easy to make blinking via animation,
    // but for start we can just change transparency
    val alpha by animateFloatAsState(
        targetValue = if (isPaused) 0.5f else 1f,
        label = "timerAlpha"
    )

    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    // Format string as 00:00
    val timeText = "%02d:%02d".format(minutes, remainingSeconds)

    Text(
        text = timeText,
        color = Color.White,
        modifier = modifier
            .alpha(alpha)
            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun CaptureButtonPreview() {
    CaptureButton(color = Color.Red, onClick = {})
}

@Preview(showBackground = true, backgroundColor = 0xFF888888)
@Composable
fun PauseButtonPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PauseButton(isPaused = false, onClick = {})
        PauseButton(isPaused = true, onClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF888888)
@Composable
fun RecordTimerPreview() {
    RecordTimer(seconds = 120, isPaused = true)
}
