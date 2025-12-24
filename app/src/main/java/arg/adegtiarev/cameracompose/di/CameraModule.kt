package arg.adegtiarev.cameracompose.di

import android.content.Context
import arg.adegtiarev.cameracompose.data.CameraManager
import arg.adegtiarev.cameracompose.data.CameraRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun getCameraRepository(
        @ApplicationContext context: Context,
    ): CameraRepository {
        return CameraManager(context)
    }
}
