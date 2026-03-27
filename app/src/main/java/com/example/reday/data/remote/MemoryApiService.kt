package com.example.reday.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

data class FragmentInput(
    val type: String,
    val content: String,
    val time: String,
    val location: String?
)

data class GenerateMemoryRequest(
    val date: String,
    val records: List<FragmentInput>
)

data class GenerateMemoryResponse(
    val title: String,
    val summary: String,
    val tags: List<String>,
    val people: List<String> = emptyList()
)

interface MemoryApiService {
    @POST("generate-memory")
    suspend fun generateMemory(@Body request: GenerateMemoryRequest): GenerateMemoryResponse
}
