package com.thomasp.pong.toys.pong.game

import android.content.Context
import android.content.SharedPreferences
import com.thomasp.pong.api.LeaderboardService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LevelManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("PongGame", Context.MODE_PRIVATE)
    private val leaderboardService = LeaderboardService(context)

    var currentLevel: Int
        get() = prefs.getInt("current_level", 0)
        set(value) = prefs.edit().putInt("current_level", value).apply()

    var highestLevel: Int
        get() = prefs.getInt("highest_level", 0)
        private set(value) {
            prefs.edit().putInt("highest_level", value).apply()
            // Submit score using existing LeaderboardService
            CoroutineScope(Dispatchers.IO).launch {
                val username = leaderboardService.ensureUsername()
                leaderboardService.submitScore(username, value)
            }
        }

    fun getBotErrorPercentage(): Float {
        // Start at 40% for level 1, decrease to 25% by level 5, then decrease by 1% per level
        return when {
            currentLevel <= 5 -> 0.40f - (currentLevel - 1) * 0.03f  // 40% -> 25% over first 5 levels
            else -> maxOf(0.01f, 0.25f - (currentLevel - 5) * 0.01f)
        }
    }

    fun getBallSpeedMultiplier(): Float {
        // Increase speed by 5% per level
        return 1f + (currentLevel * 0.05f)
    }

    fun levelUp() {
        currentLevel++
        if (currentLevel > highestLevel) {
            highestLevel = currentLevel
        }
    }

    fun resetToStart() {
        currentLevel = 0
    }
}
