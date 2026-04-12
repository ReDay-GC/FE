package com.example.reday.data.remote

import retrofit2.http.*

data class MemoryListResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: List<MemoryItemData>
)

data class MemoryItemData(
    val memoryId: Long,
    val title: String,
    val summary: String,
    val memoryDate: String,
    val emotion: String?,
    val thumbnailUrl: String?,
    val location: String?
)

data class MemoryDetailResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: MemoryDetailData?
)

data class MemoryDetailData(
    val memoryId: Long,
    val title: String,
    val summary: String,
    val description: String?,
    val memoryDate: String,
    val emotion: String?,
    val thumbnailUrl: String?,
    val location: String?,
    val archived: Boolean,
    val tags: List<TagData>,
    val records: List<MemoryRecordData>,
    val analysis: MemoryAnalysisData?
)

data class TagData(val tagName: String)

data class MemoryRecordData(
    val recordId: Long,
    val type: String,
    val fileUrl: String?
)

data class MemoryAnalysisData(
    val analysisId: Long,
    val emotionResult: String?,
    val keywords: List<String>,
    val placeSummary: String?,
    val activitySummary: String?,
    val overallSummary: String?,
    val analyzedAt: String?
)

data class CreateMemoryRequest(
    val title: String,
    val summary: String,
    val description: String?,
    val memoryDate: String?,
    val emotion: String?,
    val thumbnailUrl: String?,
    val location: String?,
    val tags: List<String>,
    val people: List<String>
)

data class CreateMemoryResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: MemoryItemData?
)

data class DeleteMemoryResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: Any?
)

interface SpringMemoryApiService {

    @GET("api/memories")
    suspend fun getAllMemories(): MemoryListResponse

    @POST("api/memories")
    suspend fun createMemory(@Body request: CreateMemoryRequest): CreateMemoryResponse

    @GET("api/memories/{memoryId}")
    suspend fun getMemoryDetail(@Path("memoryId") memoryId: Long): MemoryDetailResponse

    @DELETE("api/memories/{memoryId}")
    suspend fun deleteMemory(@Path("memoryId") memoryId: Long): DeleteMemoryResponse
}
