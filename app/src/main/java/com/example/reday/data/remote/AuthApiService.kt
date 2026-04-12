package com.example.reday.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val passwordConfirm: String,
    val termsAgreed: Boolean,
    val privacyAgreed: Boolean
)

data class SignupResponseData(
    val userId: Long,
    val email: String,
    val name: String
)

data class SignupResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: SignupResponseData
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponseData(
    val accessToken: String
)

data class LoginResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: LoginResponseData
)

interface AuthApiService {
    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): SignupResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}