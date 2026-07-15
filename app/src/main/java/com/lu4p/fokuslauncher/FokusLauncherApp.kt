package com.lu4p.fokuslauncher

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.datastore.preferences.core.Preferences
import com.lu4p.fokuslauncher.data.local.APP_LOCALE_TAG_KEY
import com.lu4p.fokuslauncher.data.local.BootstrapPrefs
import com.lu4p.fokuslauncher.data.local.HAS_COMPLETED_ONBOARDING_KEY
import com.lu4p.fokuslauncher.data.local.fokusLauncherPreferencesDataStore
import com.lu4p.fokuslauncher.data.util.AppLocaleHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class FokusLauncherApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // After super: Hilt / Application init is ready; still before any activity is created.
        AppLocaleHelper.applyStoredLocaleFromBootstrap(this)
        // Warm the DataStore file off-main so its first parse overlaps activity startup instead
        // of gating the first home frame; then reconcile the synchronous bootstrap mirror.
        appScope.launch {
            try {
                backfillBootstrapMirror(fokusLauncherPreferencesDataStore.data.first())
            } catch (_: Exception) {
                // Warm-up is best-effort; readers fall back to their own DataStore reads.
            }
        }
    }

    private fun backfillBootstrapMirror(prefs: Preferences) {
        val tag = prefs[APP_LOCALE_TAG_KEY] ?: ""
        if (BootstrapPrefs.readLocaleTag(this) != tag) {
            BootstrapPrefs.writeLocaleTag(this, tag)
            // Migration/restore launch only: DataStore has a locale the mirror missed. Applying
            // when the mirror already matches would recreate activities on every start (pre-33).
            if (tag.isNotBlank()) {
                Handler(Looper.getMainLooper()).post { AppLocaleHelper.applyLocaleTag(tag) }
            }
        }
        val onboarded = prefs[HAS_COMPLETED_ONBOARDING_KEY] ?: false
        if (BootstrapPrefs.readHasCompletedOnboarding(this) != onboarded) {
            BootstrapPrefs.writeHasCompletedOnboarding(this, onboarded)
        }
    }
}
