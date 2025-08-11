package com.thomasp.pong.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    private val USER_ID_KEY = "user_id"
    private val USERNAME_KEY = "username"

    fun getUserId(): String {
        var userId = prefs.getString(USER_ID_KEY, null)
        if (userId == null) {
            userId = UUID.randomUUID().toString()
            prefs.edit().putString(USER_ID_KEY, userId).apply()
        }
        return userId
    }

    fun getUsername(): String? = prefs.getString(USERNAME_KEY, null)

    fun setUsername(username: String) {
        prefs.edit().putString(USERNAME_KEY, username).apply()
    }

    fun hasUsername(): Boolean = getUsername() != null
}
