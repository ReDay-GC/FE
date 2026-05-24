package com.example.reday.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

data class SaveRecordResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: SaveRecordData? = null
)

data class SaveRecordData(
    val recordId: Long,
    val createdAt: String,
    val fileUrl: String? = null
)

data class GetRecordsResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: List<RecordItemData>
)

data class RecordItemData(
    val recordId: Long,
    val recordType: String,
    val textContent: String?,
    val fileUrl: String?,
    val voiceDurationSeconds: Int?,
    val recordDate: String,
    val recordedAt: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: String
)

data class GetRecordDatesResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: RecordDatesData
)

data class RecordDatesData(
    val dates: List<String>
)

data class GetLocationRecordsResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: List<LocationRecordData>
)

data class LocationRecordData(
    val recordId: Long,
    val recordType: String,
    val fileUrl: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val recordDate: String,
    val recordedAt: String?
)

data class SaveTextRecordRequest(
    val recordDate: String,
    val textContent: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val recordedAt: String? = null
)

data class UpdateRecordRequest(
    val textContent: String?,
    val recordDate: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?
)

interface RecordApiService {

    @POST("api/records/text")
    suspend fun saveText(@Body request: SaveTextRecordRequest): SaveRecordResponse

    @Multipart
    @POST("api/records/photo")
    suspend fun savePhoto(
        @Part photo: MultipartBody.Part,
        @PartMap params: Map<String, @JvmSuppressWildcards RequestBody>
    ): SaveRecordResponse

    @Multipart
    @POST("api/records/voice")
    suspend fun saveVoice(
        @Part audio: MultipartBody.Part,
        @PartMap params: Map<String, @JvmSuppressWildcards RequestBody>
    ): SaveRecordResponse

    @GET("api/records")
    suspend fun getRecordsByDate(@Query("date") date: String): GetRecordsResponse

    @GET("api/records/dates")
    suspend fun getRecordDates(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): GetRecordDatesResponse

    @PUT("api/records/{recordId}")
    suspend fun updateRecord(
        @Path("recordId") recordId: Long,
        @Body request: UpdateRecordRequest
    ): SaveRecordResponse

    @DELETE("api/records/{recordId}")
    suspend fun deleteRecord(@Path("recordId") recordId: Long): SaveRecordResponse

    @GET("api/records/locations")
    suspend fun getLocationRecords(): GetLocationRecordsResponse

    @GET("api/records/summary")
    suspend fun getRecordSummary(): GetRecordSummaryResponse
}

data class RecordSummaryData(
    val photoCount: Int,
    val textCount: Int,
    val voiceCount: Int
)

data class GetRecordSummaryResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: RecordSummaryData
)
