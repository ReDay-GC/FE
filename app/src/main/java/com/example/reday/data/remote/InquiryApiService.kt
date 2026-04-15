package com.example.reday.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class InquiryListDto(
    val inquiryId: Long,
    val title: String,
    val contentPreview: String,
    val status: String,
    val replyContent: String?,
    val createdAt: String
)

data class InquiryListResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: List<InquiryListDto>
)

data class InquiryDetailDto(
    val inquiryId: Long,
    val title: String,
    val content: String,
    val status: String,
    val replyContent: String?,
    val repliedAt: String?,
    val createdAt: String
)

data class InquiryDetailResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: InquiryDetailDto
)

data class InquiryCreateRequest(
    val title: String,
    val content: String
)

data class InquiryCreateData(val inquiryId: Long)

data class InquiryCreateResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: InquiryCreateData
)

interface InquiryApiService {

    @GET("api/inquiries")
    suspend fun getInquiries(): InquiryListResponse

    @POST("api/inquiries")
    suspend fun createInquiry(@Body request: InquiryCreateRequest): InquiryCreateResponse

    @GET("api/inquiries/{inquiryId}")
    suspend fun getInquiryDetail(@Path("inquiryId") inquiryId: Long): InquiryDetailResponse
}
