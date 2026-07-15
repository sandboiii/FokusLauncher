package com.lu4p.fokuslauncher.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.PhotoWallpaperDrawerOverlayIntensity
import com.lu4p.fokuslauncher.ui.util.stateWhileSubscribedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class PhotoWallpaperDrawerOverlayUi(
        val usesPhotoWallpaper: Boolean,
        val intensityMultiplier: Float,
)

@HiltViewModel
class FokusNavGraphViewModel @Inject constructor(
        preferencesManager: PreferencesManager,
) : ViewModel() {

    /**
     * null = unknown (bootstrap mirror not yet written); the DataStore emission resolves it.
     * Seeding from the synchronous mirror lets onboarded users compose Home on the first frame.
     */
    val hasCompletedOnboarding: StateFlow<Boolean?> =
            preferencesManager.hasCompletedOnboardingFlow
                    .map<Boolean, Boolean?> { it }
                    .stateWhileSubscribedIn(
                            viewModelScope,
                            preferencesManager.hasCompletedOnboardingBootstrap(),
                    )

    val photoWallpaperDrawerOverlayUiState: StateFlow<PhotoWallpaperDrawerOverlayUi> =
            combine(
                    preferencesManager.launcherAppearanceFlow.map { it.usesPhotoWallpaper },
                    preferencesManager.photoWallpaperDrawerOverlayIntensityFlow,
            ) { usesPhoto, intensity ->
                PhotoWallpaperDrawerOverlayUi(usesPhoto, intensity)
            }
                    .stateWhileSubscribedIn(
                            viewModelScope,
                            PhotoWallpaperDrawerOverlayUi(
                                    usesPhotoWallpaper = false,
                                    intensityMultiplier =
                                            PhotoWallpaperDrawerOverlayIntensity.DEFAULT,
                            ),
                    )
}
