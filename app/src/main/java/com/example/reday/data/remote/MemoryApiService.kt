package com.example.reday.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class FragmentInput(
    val type: String,
    val content: String,
    val time: String,
    val location: String?
)

data class GenerateMemoryRequest(
    val date: String,
    val records: List<FragmentInput>,
    val photo_data: List<String> = emptyList()  // base64 인코딩된 사진 목록
)

data class GenerateMemoryResponse(
    val title: String,
    val summary: String,
    val tags: List<String>,
    val people: List<String> = emptyList(),
    val emotion: String = "😐 평범한",
    val embedding: List<Float> = emptyList()
)

data class SaveEmbeddingRequest(
    val memory_id: Long,
    val embedding: String  // List<Float> JSON
)

data class MemoryTextItem(
    val memory_id: Long,
    val text: String
)

data class SearchSemanticRequest(
    val query: String,
    val memories: List<MemoryTextItem>
)

data class SearchSemanticResponse(
    val ranked_ids: List<Long>
)

data class TranscribeResponse(
    val text: String
)

data class GenerateInsightRequest(
    val year_month: String,             // "2026-03"
    val memories: List<MemorySummary>,
    val user_id: Long? = null
)

data class MemorySummary(
    val date: String,
    val title: String,
    val summary: String,
    val tags: List<String>,
    val locations: List<String>,
    val people: List<String>
)

data class GenerateInsightResponse(
    val insight: String
)

data class MemoryForComment(
    val title: String,
    val summary: String,
    val tags: List<String> = emptyList()
)

data class DailyCommentRequest(
    val memories: List<MemoryForComment>
)

data class DailyCommentResponse(
    val comment: String
)

data class ParseSearchRequest(
    val query: String
)

data class ParseSearchResponse(
    val people: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val locations: List<String> = emptyList(),
    val yearMonth: String? = null,
    val keywords: List<String> = emptyList(),
    val sentiment: String? = null  // "긍정" | "부정" | null
)

interface MemoryApiService {
    @POST("generate-memory")
    suspend fun generateMemory(@Body request: GenerateMemoryRequest): GenerateMemoryResponse

    @Multipart
    @POST("transcribe")
    suspend fun transcribe(@Part file: MultipartBody.Part): TranscribeResponse

    @POST("generate-insight")
    suspend fun generateInsight(@Body request: GenerateInsightRequest): GenerateInsightResponse

    @POST("parse-search")
    suspend fun parseSearch(@Body request: ParseSearchRequest): ParseSearchResponse

    @POST("daily-comment")
    suspend fun dailyComment(@Body request: DailyCommentRequest): DailyCommentResponse

    @POST("save-embedding")
    suspend fun saveEmbedding(@Body request: SaveEmbeddingRequest)

    @POST("search-semantic")
    suspend fun searchSemantic(@Body request: SearchSemanticRequest): SearchSemanticResponse
}
