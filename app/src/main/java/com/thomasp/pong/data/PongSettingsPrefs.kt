package com.thomasjprice.pong.data

import android.content.Context
import android.content.SharedPreferences

class PongSettingsPrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("PongSettings", Context.MODE_PRIVATE)

    fun isSoundEnabled(): Boolean = prefs.getBoolean("sound_enabled", true)
    fun setSoundEnabled(enabled: Boolean) = prefs.edit().putBoolean("sound_enabled", enabled).apply()

    fun isHapticEnabled(): Boolean = prefs.getBoolean("haptic_enabled", true)
    fun setHapticEnabled(enabled: Boolean) = prefs.edit().putBoolean("haptic_enabled", enabled).apply()
}

