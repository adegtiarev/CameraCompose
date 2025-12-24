package arg.adegtiarev.cameracompose.ui.screens

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arg.adegtiarev.cameracompose.data.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val cameraRepository: CameraRepository
) : ViewModel() {
    val surfaceRequest = cameraRepository.surfaceRequest

    fun onBindCamera(lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            cameraRepository.bindCamera(lifecycleOwner)
        }
    }

}
