package com.example.reday.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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
    val name: String,
    val accessToken: String
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
    val accessToken: String,
    val userId: Long? = null
)

data class LoginResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: LoginResponseData
)

data class CheckIdResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: Boolean
)

interface AuthApiService {
    @GET("api/auth/check-id")
    suspend fun checkId(@Query("id") id: String): CheckIdResponse

    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): SignupResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    @DELETE("api/auth/withdraw")
    suspend fun withdraw(): Response<Unit>
}