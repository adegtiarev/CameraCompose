package arg.adegtiarev.cameracompose.data

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraManager @Inject constructor(private val context: Context) : CameraRepository {

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    override val surfaceRequest = _surfaceRequest.asStateFlow()

    override suspend fun bindCamera(lifecycleOwner: LifecycleOwner) {
        val provider = getCameraProvider()

        // 1. Setup Preview use case
        val preview = Preview.Builder().build()

        // 2. This is how we get the SurfaceRequest for Compose
        preview.setSurfaceProvider { request ->
            _surfaceRequest.value = request
        }

        // 3. Select back camera as default
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Unbind everything before binding again
            provider.unbindAll()

            // Bind to lifecycle
            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview
                // Later we will add imageCapture and videoCapture here
            )
        } catch (e: Exception) {
            Log.e("CameraManager", "Binding failed", e)
        }
    }

    // This is where the CameraX setup will live
    suspend fun getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(context).also { future ->
            future.addListener({
                continuation.resume(future.get())
            }, ContextCompat.getMainExecutor(context))
        }
    }

    override suspend fun takePhoto() {
        TODO("Not yet implemented")
    }

    override suspend fun recordVideo() {
        TODO("Not yet implemented")
    }
}