package arg.adegtiarev.cameracompose.ui.screens

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arg.adegtiarev.cameracompose.data.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed class CameraEvent {
    data class Toast(val message: String) : CameraEvent()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val cameraRepository: CameraRepository
) : ViewModel() {

    private val _events = Channel<CameraEvent>()
    val events = _events.receiveAsFlow()

    val surfaceRequest = cameraRepository.surfaceRequest

    fun onBindCamera(lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            cameraRepository.bindCamera(lifecycleOwner)
        }
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
