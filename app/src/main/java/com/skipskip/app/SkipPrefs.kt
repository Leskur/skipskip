package com.skipskip.app

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SkipPrefs {
    private const val NAME = "skip_skip_prefs"
    private const val KEY_AUTO_CLICK = "auto_click_enabled"
    private const val KEY_CLICK_COUNT = "click_count"
    private const val KEY_EXCLUDED_PACKAGES = "excluded_packages"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isAutoClickEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_CLICK, true)

    fun setAutoClickEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_AUTO_CLICK, enabled) }
    }

    fun clickCount(context: Context): Long =
        prefs(context).getLong(KEY_CLICK_COUNT, 0L)

    fun incrementClickCount(context: Context) {
        prefs(context).edit {
            putLong(KEY_CLICK_COUNT, clickCount(context) + 1L)
        }
    }

    /** 在这些应用里不自动跳过。返回的是快照，可安全遍历。 */
    fun excludedPackages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_EXCLUDED_PACKAGES, emptySet())?.toSet() ?: emptySet()

    fun setExcluded(context: Context, packageName: String, excluded: Boolean) {
        val current = excludedPackages(context).toMutableSet()
        if (excluded) current.add(packageName) else current.remove(packageName)
        prefs(context).edit { putStringSet(KEY_EXCLUDED_PACKAGES, current) }
    }

    fun registerListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }
}
