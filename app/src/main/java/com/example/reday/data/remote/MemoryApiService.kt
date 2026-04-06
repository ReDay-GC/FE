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
    val people: List<String> = emptyList()
)

data class TranscribeResponse(
    val text: String
)

data class GenerateInsightRequest(
    val year_month: String,             // "2026-03"
    val memories: List<MemorySummary>
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

data class ParseSearchRequest(
    val query: String
)

data class ParseSearchResponse(
    val people: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val locations: List<String> = emptyList(),
    val yearMonth: String? = null,
    val keywords: List<String> = emptyList()
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
}
