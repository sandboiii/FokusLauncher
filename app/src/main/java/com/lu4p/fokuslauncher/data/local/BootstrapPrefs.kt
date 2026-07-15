package com.lu4p.fokuslauncher.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Synchronous mirror of the few DataStore values needed before the first frame.
 *
 * DataStore (`fokus_launcher_prefs`) stays the source of truth; every writer of a mirrored value
 * must write through here as well. [FokusLauncherApp][com.lu4p.fokuslauncher.FokusLauncherApp]
 * backfills the mirror from DataStore on each launch, so drift (e.g. after a backup restore)
 * self-heals on the next start.
 */
object BootstrapPrefs {
    private const val FILE = "fokus_bootstrap"
    private const val KEY_LOCALE_TAG = "app_locale_tag"
    private const val KEY_ONBOARDED = "has_completed_onboarding"

    private fun prefs(context: Context): SharedPreferences =
            context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun readLocaleTag(context: Context): String =
            prefs(context).getString(KEY_LOCALE_TAG, "") ?: ""

    fun writeLocaleTag(context: Context, tag: String) {
        prefs(context).edit().putString(KEY_LOCALE_TAG, tag.trim()).apply()
    }

    /** null = never written: fresh install, or first launch after updating from a version without the mirror. */
    fun readHasCompletedOnboarding(context: Context): Boolean? {
        val p = prefs(context)
        return if (p.contains(KEY_ONBOARDED)) p.getBoolean(KEY_ONBOARDED, false) else null
    }

    fun writeHasCompletedOnboarding(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ONBOARDED, value).apply()
    }
}
