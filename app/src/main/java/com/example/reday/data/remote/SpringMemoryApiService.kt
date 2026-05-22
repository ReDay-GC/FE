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
    val location: String?,
    val recordCount: Int = 0,
    val tags: List<String> = emptyList(),
    val people: List<String> = emptyList(),
    val embedding: String? = null  // AI 생성 임베딩 벡터 JSON
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
    val people: List<String>,
    val recordIds: List<Long> = emptyList(),
    val embedding: String? = null
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

data class MemoryByDateResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: List<MemoryItemData>
)

data class MemoryCalendarResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: MemoryCalendarData
)

data class MemoryCalendarData(
    val datesWithMemory: List<String>
)

data class TagListData(val tags: List<String>)

data class TagListResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: TagListData
)

// ── 월간 분석 ──

data class MemoryTrendItem(
    val year: Int,
    val month: Int,
    val count: Int
)

data class RecordTypeStatItem(
    val recordType: String,
    val count: Int,
    val percentage: Double
)

data class ActivityStatItem(
    val activityType: String,
    val percentage: Int
)

data class PlaceStatItem(
    val rank: Int,
    val place: String,
    val count: Int
)

data class PeopleStatItem(
    val rank: Int,
    val name: String,
    val count: Int
)

data class MonthlyAnalysisData(
    val monthlyInsight: String?,
    val memoryTrend: List<MemoryTrendItem>,
    val recordTypeStats: List<RecordTypeStatItem>,
    val topActivities: List<ActivityStatItem>?,
    val topPlaces: List<PlaceStatItem>,
    val topPeople: List<PeopleStatItem>?
)

data class MonthlyAnalysisResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: MonthlyAnalysisData?
)

data class MapLocationData(
    val location: String,
    val memoryCount: Int,
    val latitude: Double?,
    val longitude: Double?
)

data class GetMapLocationsResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: List<MapLocationData>
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

    @GET("api/memories/date")
    suspend fun getMemoriesByDate(@Query("date") date: String): MemoryByDateResponse

    @GET("api/memories/calendar")
    suspend fun getMemoryCalendar(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): MemoryCalendarResponse

    @GET("api/memories/map")
    suspend fun getMapLocations(): GetMapLocationsResponse

    @GET("api/memories/map/location")
    suspend fun getMapLocationMemories(@Query("location") location: String): MemoryByDateResponse

    @GET("api/analysis/monthly")
    suspend fun getMonthlyAnalysis(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): MonthlyAnalysisResponse

    // ── 검색 ──

    @GET("api/memories/tags")
    suspend fun getAllTags(): TagListResponse

    @GET("api/memories/search")
    suspend fun searchMemoriesByKeyword(@Query("keyword") keyword: String): MemoryListResponse

    @GET("api/memories/search/tag")
    suspend fun getMemoriesByTag(@Query("tagName") tagName: String): MemoryListResponse

    @GET("api/memories/search/location")
    suspend fun getMemoriesByLocation(@Query("location") location: String): MemoryListResponse

    @GET("api/memories/search/emotion")
    suspend fun getMemoriesByEmotion(@Query("emotion") emotion: String): MemoryListResponse
}
