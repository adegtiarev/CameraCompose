package arg.adegtiarev.cameracompose.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    // State for permissions (CAMERA, RECORD_AUDIO)
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    if (permissionsState.allPermissionsGranted) {
        Text("Camera is ready!")
    } else {
        Column {
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
