package arg.adegtiarev.cameracompose.ui.screens

import androidx.camera.video.VideoRecordEvent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arg.adegtiarev.cameracompose.data.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed class CameraEvent {
    data class Toast(val message: String) : CameraEvent()
}

data class VideoRecordState(
    val isRecording: Boolean = false,
    val durationSeconds: Int = 0,
    val isSaving: Boolean = false,
    val isPaused: Boolean = false,
)

enum class CameraMode { PHOTO, VIDEO }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val cameraRepository: CameraRepository
) : ViewModel() {
    private val _events = Channel<CameraEvent>()
    val events = _events.receiveAsFlow()

    private val _uiState = MutableStateFlow(VideoRecordState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cameraRepository.recordingEvents.collect { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        _uiState.update { it.copy(isRecording = true, durationSeconds = 0) }
                    }

                    is VideoRecordEvent.Status -> {
                        // Переводим наносекунды в секунды
                        val seconds = event.recordingStats.recordedDurationNanos / 1_000_000_000
                        _uiState.update { it.copy(durationSeconds = seconds.toInt()) }
                    }

                    is VideoRecordEvent.Finalize -> {
                        _uiState.update { it.copy(isRecording = false, isSaving = true) }
                        // После завершения можно отправить Toast через наш Channel
                        _events.send(CameraEvent.Toast("Video saved!"))
                        _uiState.update { it.copy(isSaving = false) }
                    }

                    is VideoRecordEvent.Pause -> {
                        _uiState.update { it.copy(isPaused = true) }
                    }

                    is VideoRecordEvent.Resume -> {
                        _uiState.update { it.copy(isPaused = false) }
                    }
                }
            }
        }
    }

    private val _cameraMode = MutableStateFlow(CameraMode.PHOTO)
    val cameraMode = _cameraMode.asStateFlow()

    fun setCameraMode(mode: CameraMode) {
        _cameraMode.value = mode
    }

    val surfaceRequest = cameraRepository.surfaceRequest

    fun onBindCamera(lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            cameraRepository.bindCamera(lifecycleOwner)
        }
    }

    fun onCaptureClick() {
        when (cameraMode.value) {
            CameraMode.PHOTO -> capturePhoto()
            CameraMode.VIDEO -> recordVideo()
        }
    }

    private fun recordVideo() {
        TODO("Not yet implemented")
    }

    fun capturePhoto() {
        viewModelScope.launch {
            try {
                cameraRepository.takePhoto()
                _events.send(CameraEvent.Toast("Photo saved successfully! ✅"))
            } catch (e: Exception) {
                _events.send(CameraEvent.Toast("Error: ${e.localizedMessage} ❌"))
            }

        }
    }

}
