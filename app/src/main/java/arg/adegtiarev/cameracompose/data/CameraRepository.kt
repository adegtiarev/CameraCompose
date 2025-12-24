package arg.adegtiarev.cameracompose.data

import androidx.camera.core.ImageCapture
import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow

interface CameraRepository {
    suspend fun takePhoto()

    suspend fun recordVideo()
    val surfaceRequest: StateFlow<SurfaceRequest?>

    // We will need a function to bind the camera to a lifecycle
    suspend fun bindCamera(lifecycleOwner: LifecycleOwner)
}