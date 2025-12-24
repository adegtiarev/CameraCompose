package arg.adegtiarev.cameracompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import arg.adegtiarev.cameracompose.ui.screens.MainScreen
import arg.adegtiarev.cameracompose.ui.theme.CameraComposeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CameraComposeTheme {
                Scaffold(Modifier.safeDrawingPadding()) { innerPadding ->
                    MainScreen()
                }
            }
        }
    }
}
