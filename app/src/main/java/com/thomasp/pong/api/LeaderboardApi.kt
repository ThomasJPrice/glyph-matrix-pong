package com.thomasjprice.pong.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Query

data class LeaderboardEntry(
    val user_id: String,
    val username: String,
    val score: Int
)

data class LeaderboardResponse(
    val top: List<LeaderboardEntry>,
    val score: Int,
    val inTop: Boolean,
    val position: Int?
)

data class ScoreRequest(
    val user_id: String,
    val username: String,
    val score: Int
)

data class SuccessResponse(
    val success: Boolean,
    val error: String? = null
)

data class UsernameAvailabilityResponse(
    val available: Boolean
)

data class SetUsernameRequest(
    val user_id: String,
    val username: String
)

interface LeaderboardApi {
    @GET("api/leaderboard")
    suspend fun getLeaderboard(@Query("user_id") userId: String? = null): LeaderboardResponse

    @POST("api/leaderboard")
    suspend fun submitScore(@Body request: ScoreRequest): SuccessResponse

    @GET("api/username/check")
    suspend fun checkUsername(@Query("username") username: String): UsernameAvailabilityResponse

    @POST("api/username/set")
    suspend fun setUsername(@Body request: SetUsernameRequest): SuccessResponse
}
