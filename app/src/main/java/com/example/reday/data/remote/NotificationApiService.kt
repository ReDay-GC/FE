package com.example.reday.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class NotificationSettingsData(
    val pushEnabled: Boolean,
    val dailyRecordEnabled: Boolean,
    val aiGenerationEnabled: Boolean
)

data class NotificationSettingsResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: NotificationSettingsData
)

data class NotificationSettingsRequest(
    val pushEnabled: Boolean,
    val dailyRecordEnabled: Boolean,
    val aiGenerationEnabled: Boolean
)

data class NotificationDto(
    val notificationId: Long,
    val type: String,
    val title: String,
    val content: String,
    val isRead: Boolean,
    val createdAt: String,
    val relatedId: Long? = null
)

data class NotificationListResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: List<NotificationDto>
)

data class ReadNotificationResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: Any?
)

interface NotificationApiService {

    @GET("api/notifications/settings")
    suspend fun getSettings(): NotificationSettingsResponse

    @PUT("api/notifications/settings")
    suspend fun updateSettings(@Body request: NotificationSettingsRequest): NotificationSettingsResponse

    @GET("api/notifications")
    suspend fun getNotifications(@Query("category") category: String = "ALL"): NotificationListResponse

    @PATCH("api/notifications/{notificationId}/read")
    suspend fun markAsRead(@Path("notificationId") notificationId: Long): Response<ReadNotificationResponse>
}
