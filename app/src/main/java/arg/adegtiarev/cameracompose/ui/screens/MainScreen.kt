package arg.adegtiarev.cameracompose.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    // Bind camera to lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        viewModel.onBindCamera(lifecycleOwner)
    }

    // Unbind camera when view is disposed
    DisposableEffect(lifecycleOwner) {
        onDispose {
            // Здесь мы можем вызвать метод остановки записи,
            // чтобы не оставить «повисших» процессов
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

                CaptureButton(
                    onClick = { viewModel.capturePhoto() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                )
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
    modifier: Modifier = Modifier
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
                .border(BorderStroke(4.dp, Color.White), CircleShape)
        )
        // Inner circle
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(scale)
                .background(Color.White, CircleShape)
        )
    }
}
