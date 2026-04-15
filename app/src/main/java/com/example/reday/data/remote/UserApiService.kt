package com.example.reday.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

data class UserProfileData(
    val userId: Long,
    val name: String,
    val email: String,
    val createdAt: String
)

data class UserProfileResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: UserProfileData
)

data class UpdateNameRequest(
    val name: String
)

interface UserApiService {
    @GET("api/users/me")
    suspend fun getMyProfile(): UserProfileResponse

    @PUT("api/users/me")
    suspend fun updateMyProfile(@Body request: UpdateNameRequest): UserProfileResponse
}
