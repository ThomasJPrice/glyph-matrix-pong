package com.thomasjprice.pong.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlin.random.Random
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.Locale
import androidx.core.content.edit

class LeaderboardService(context: Context) {
    private val baseUrl = "https://pong-leaderboard-api.vercel.app/"
    private val prefs: SharedPreferences = context.getSharedPreferences("PongPrefs", Context.MODE_PRIVATE)
    private val USER_ID_KEY = "user_id"
    private val USERNAME_KEY = "username"

    private val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(
            OkHttpClient.Builder()
                // Removed API secret header interceptor
                .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                    level = okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS
                })
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LeaderboardApi::class.java)

    fun getUserId(): String {
        var userId = prefs.getString(USER_ID_KEY, null)
        if (userId == null) {
            userId = UUID.randomUUID().toString()
            prefs.edit { putString(USER_ID_KEY, userId) }
        }
        return userId
    }

    private fun generateRandomUsername(): String {
        return "Player" + String.format(Locale.US, "%04d", Random.nextInt(10000))
    }

    suspend fun ensureUsername(): String {
        var storedUsername = getStoredUsername()
        if (storedUsername == null) {
            repeat(10) {
                val newUsername = generateRandomUsername()
                if (checkUsernameAvailability(newUsername).getOrNull() == true) {
                    val result = setUsername(newUsername)
                    if (result.isSuccess) {
                        storedUsername = newUsername
                        return storedUsername
                    }
                }
            }
            // If we still don't have a username after 10 attempts, use user ID as fallback
            val fallbackUsername = "Player" + getUserId().take(4)
            val result = setUsername(fallbackUsername)
            if (result.isSuccess) {
                storedUsername = fallbackUsername
            }
        }
        return storedUsername ?: "Unknown"
    }

    fun getStoredUsername(): String? {
        return prefs.getString(USERNAME_KEY, null)
    }

    private fun storeUsername(username: String) {
        prefs.edit { putString(USERNAME_KEY, username) }
    }

    suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return try {
            val response = api.checkUsername(username)
            Result.success(response.available)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setUsername(username: String): Result<SuccessResponse> {
        return try {
            val request = SetUsernameRequest(getUserId(), username)
            val response = api.setUsername(request)
            if (response.success) {
                storeUsername(username)
            }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLeaderboard(): Result<LeaderboardResponse> {
        return try {
            Result.success(api.getLeaderboard(getUserId()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitScore(username: String, score: Int): Result<SuccessResponse> {
        return try {
            val request = ScoreRequest(getUserId(), username, score)
            Result.success(api.submitScore(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
