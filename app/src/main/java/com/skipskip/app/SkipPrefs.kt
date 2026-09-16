package com.skipskip.app

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SkipPrefs {
    private const val NAME = "skip_skip_prefs"
    private const val KEY_AUTO_CLICK = "auto_click_enabled"
    private const val KEY_CLICK_COUNT = "click_count"

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
