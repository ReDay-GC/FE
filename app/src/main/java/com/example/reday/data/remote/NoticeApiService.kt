package com.example.reday.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

data class NoticeListDto(
    val noticeId: Long,
    val title: String,
    val contentPreview: String,
    val isNew: Boolean,
    val createdAt: String
)

data class NoticeListResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: List<NoticeListDto>
)

data class NoticeDetailDto(
    val noticeId: Long,
    val title: String,
    val content: String,
    val createdAt: String
)

data class NoticeDetailResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: NoticeDetailDto
)

interface NoticeApiService {

    @GET("api/notices")
    suspend fun getNotices(): NoticeListResponse

    @GET("api/notices/{noticeId}")
    suspend fun getNoticeDetail(@Path("noticeId") noticeId: Long): NoticeDetailResponse
}
