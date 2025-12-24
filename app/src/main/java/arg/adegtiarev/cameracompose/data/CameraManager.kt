package arg.adegtiarev.cameracompose.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraManager @Inject constructor(private val context: Context) : CameraRepository {

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    override val surfaceRequest = _surfaceRequest.asStateFlow()

    private val _recordingEvents = MutableSharedFlow<VideoRecordEvent>(extraBufferCapacity = 1)
    override val recordingEvents = _recordingEvents.asSharedFlow()

    override suspend fun bindCamera(lifecycleOwner: LifecycleOwner) {
        // Get the camera provider
        val provider = getCameraProvider()

        // Setup Preview use case
        val preview = Preview.Builder().build()

        // Setup ImageCapture use case
        imageCapture =
            ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

        // Setup VideoCapture use case
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        // This is how we get the SurfaceRequest for Compose
        preview.setSurfaceProvider { request ->
            _surfaceRequest.value = request
        }

        // Select back camera as default
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Unbind everything before binding again
            provider.unbindAll()

            // Bind to lifecycle
            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview, imageCapture, videoCapture
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
        val imageCapture = imageCapture ?: return

        // 1. Prepare file metadata
        val name = "CameraCompose_${System.currentTimeMillis()}"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraCompose-Images")
        }

        // 2. Create output options
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

        // 3. Take picture
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // We'll think about how to notify the UI in a moment! 📩
                    Log.d("CameraManager", "Photo saved: ${outputFileResults.savedUri}")
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraManager", "Photo capture failed", exception)
                }
            }
        )
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun recordVideo() {
        // 1. Если запись уже идет — останавливаем её
        val recording = currentRecording
        if (recording != null) {
            recording.stop()
            currentRecording = null
            return
        }

        // 2. Если записи нет — подготавливаем всё для старта
        val videoCapture = videoCapture ?: return

        val name = "Video_${System.currentTimeMillis()}"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        }

        val outputOptions = MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        // 3. Запускаем и сохраняем ссылку в currentRecording
        currentRecording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                _recordingEvents.tryEmit(event)
            }
    }

    override fun stopRecording() {
        currentRecording?.stop()
        currentRecording = null
    }

    override fun pauseRecording() {
        currentRecording?.pause()
    }

    override fun resumeRecording() {
        currentRecording?.resume()
    }
}
