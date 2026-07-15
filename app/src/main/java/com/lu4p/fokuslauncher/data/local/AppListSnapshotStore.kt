package com.lu4p.fokuslauncher.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persisted snapshot of the merged installed-app list (including archived rows), so the first
 * drawer render after process start can show the last known list instantly instead of waiting
 * for the full [android.content.pm.LauncherApps] scan (label resolution across all profiles takes
 * hundreds of ms on large devices). The snapshot is never authoritative: callers serve it once,
 * then re-scan in the background and reconcile.
 *
 * Icons are not persisted — [com.lu4p.fokuslauncher.data.model.AppInfo.icon] is loaded lazily by
 * the UI anyway. UserHandles are persisted as profile keys and re-resolved against live profiles
 * on read; rows whose profile no longer exists are dropped by the repository.
 */
@Singleton
class AppListSnapshotStore @Inject constructor(@param:ApplicationContext context: Context) {

    data class Entry(
            val packageName: String,
            val label: String,
            val category: String,
            /** `"0"` = owner profile; otherwise [com.lu4p.fokuslauncher.data.model.appProfileKey]. */
            val profileKey: String,
            /** [android.content.ComponentName.flattenToString] or null. */
            val componentName: String?,
            val launcherShortcutId: String?,
            val isArchived: Boolean,
    )

    private val file = File(context.filesDir, FILE_NAME)
    private val writeLock = Any()

    /** Last known app list, or null when absent/corrupt/incompatible. Call on a background thread. */
    fun read(): List<Entry>? {
        return try {
            if (!file.isFile) return null
            val root = JSONObject(file.readText())
            if (root.optInt(KEY_VERSION) != VERSION) return null
            val rows = root.optJSONArray(KEY_APPS) ?: return null
            val entries = ArrayList<Entry>(rows.length())
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val packageName = row.optString(KEY_PACKAGE)
                val label = row.optString(KEY_LABEL)
                if (packageName.isBlank() || label.isBlank()) continue
                entries.add(
                        Entry(
                                packageName = packageName,
                                label = label,
                                category = row.optString(KEY_CATEGORY),
                                profileKey = row.optString(KEY_PROFILE, "0").ifBlank { "0" },
                                componentName =
                                        row.optString(KEY_COMPONENT).takeIf { it.isNotBlank() },
                                launcherShortcutId =
                                        row.optString(KEY_SHORTCUT_ID).takeIf { it.isNotBlank() },
                                isArchived = row.optBoolean(KEY_ARCHIVED, false),
                        )
                )
            }
            entries.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    /** Atomically replaces the snapshot (temp file + rename). Call on a background thread. */
    fun write(entries: List<Entry>) {
        if (entries.isEmpty()) return
        try {
            val rows = JSONArray()
            entries.forEach { entry ->
                rows.put(
                        JSONObject().apply {
                            put(KEY_PACKAGE, entry.packageName)
                            put(KEY_LABEL, entry.label)
                            if (entry.category.isNotBlank()) put(KEY_CATEGORY, entry.category)
                            if (entry.profileKey != "0") put(KEY_PROFILE, entry.profileKey)
                            entry.componentName?.let { put(KEY_COMPONENT, it) }
                            entry.launcherShortcutId?.let { put(KEY_SHORTCUT_ID, it) }
                            if (entry.isArchived) put(KEY_ARCHIVED, true)
                        }
                )
            }
            val payload =
                    JSONObject().apply {
                        put(KEY_VERSION, VERSION)
                        put(KEY_APPS, rows)
                    }
            synchronized(writeLock) {
                val tmp = File(file.parentFile, "$FILE_NAME.tmp")
                tmp.writeText(payload.toString())
                if (!tmp.renameTo(file)) {
                    // Cross-filesystem or racing rename: fall back to a direct write.
                    file.writeText(payload.toString())
                    tmp.delete()
                }
            }
        } catch (_: Exception) {
            // Snapshot is an optimization; never propagate persistence failures.
        }
    }

    private companion object {
        const val FILE_NAME = "app_list_snapshot.json"
        const val VERSION = 1
        const val KEY_VERSION = "version"
        const val KEY_APPS = "apps"
        const val KEY_PACKAGE = "pkg"
        const val KEY_LABEL = "label"
        const val KEY_CATEGORY = "category"
        const val KEY_PROFILE = "profile"
        const val KEY_COMPONENT = "component"
        const val KEY_SHORTCUT_ID = "shortcutId"
        const val KEY_ARCHIVED = "archived"
    }
}
