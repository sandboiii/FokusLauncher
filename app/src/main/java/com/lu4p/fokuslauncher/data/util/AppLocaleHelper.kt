package com.lu4p.fokuslauncher.data.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.lu4p.fokuslauncher.data.local.BootstrapPrefs

object AppLocaleHelper {

    fun applyLocaleTag(tag: String) {
        val locales =
                if (tag.isBlank()) LocaleListCompat.getEmptyLocaleList()
                else LocaleListCompat.forLanguageTags(tag.trim())
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /**
     * Apply the persisted tag before the first activity attaches, without touching DataStore:
     * blocking on its first disk read here kept the main thread waiting for the whole file parse.
     * Reads the [BootstrapPrefs] mirror instead; DataStore stays the source of truth and
     * [com.lu4p.fokuslauncher.FokusLauncherApp] backfills the mirror asynchronously.
     */
    fun applyStoredLocaleFromBootstrap(context: Context) {
        val tag = try {
            BootstrapPrefs.readLocaleTag(context)
        } catch (_: Exception) {
            ""
        }
        // Blank tag = follow system: skip AppCompat entirely on the common path.
        if (tag.isBlank()) return
        try {
            applyLocaleTag(tag)
        } catch (_: Exception) {
            // Avoid taking down the process if AppCompat locale APIs fail on a specific device.
        }
    }
}
