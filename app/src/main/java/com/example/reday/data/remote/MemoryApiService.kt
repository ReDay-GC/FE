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
    val records: List<FragmentInput>
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

interface MemoryApiService {
    @POST("generate-memory")
    suspend fun generateMemory(@Body request: GenerateMemoryRequest): GenerateMemoryResponse

    @Multipart
    @POST("transcribe")
    suspend fun transcribe(@Part file: MultipartBody.Part): TranscribeResponse
}
