package com.example.reday.data.repository

import android.util.Log
import com.example.reday.data.local.entity.MemoryEntity
import com.example.reday.data.mapper.MemoryMapper
import com.example.reday.data.model.MemoryUiModel
import com.example.reday.data.remote.CreateMemoryRequest
import com.example.reday.data.remote.MapLocationData
import com.example.reday.data.remote.MemoryDetailData
import com.example.reday.data.remote.MemoryItemData
import com.example.reday.data.remote.MonthlyAnalysisData
import com.example.reday.data.remote.RetrofitClient
import com.example.reday.data.remote.SaveEmbeddingRequest
import com.example.reday.data.remote.SpringMemoryApiService
import com.google.gson.Gson

class MemoryRepository(
    private val api: SpringMemoryApiService = RetrofitClient.springMemoryApi
) {

    suspend fun saveMemory(entity: MemoryEntity, recordIds: List<Long> = emptyList()): Long {
        val gson = Gson()
        val tags = try { gson.fromJson(entity.tags, Array<String>::class.java).toList() } catch (e: Exception) { emptyList() }
        val people = try { gson.fromJson(entity.people, Array<String>::class.java).toList() } catch (e: Exception) { emptyList() }

        val safeThumbUrl = entity.representativePhotoUrl?.takeIf { it.startsWith("http") }
        val request = CreateMemoryRequest(
            title = entity.title.ifBlank { "기억" },
            summary = entity.summary.ifBlank { "" },
            description = null,
            memoryDate = entity.date,
            emotion = entity.emotion.toEmotionEnum(),
            thumbnailUrl = safeThumbUrl,
            location = entity.representativeLocationName,
            tags = tags,
            people = people,
            recordIds = recordIds
        )
        Log.d("MemoryRepo", "저장 요청: ${Gson().toJson(request)}")
        val response = api.createMemory(request)
        if (response.success && response.data != null) {
            val serverId = response.data.memoryId
            entity.embedding?.let { embedding ->
                try {
                    RetrofitClient.memoryApi.saveEmbedding(
                        SaveEmbeddingRequest(memory_id = serverId, embedding = embedding)
                    )
                } catch (e: Exception) {
                    Log.e("MemoryRepo", "임베딩 AI 저장 실패: ${e.message}")
                }
            }
            return serverId
        }
        throw Exception("기억 서버 저장 실패: ${response.message}")
    }

    suspend fun getAllMemories(): List<MemoryEntity> {
        return try {
            val response = api.getAllMemories()
            if (response.success) response.data.map { it.toEntity() } else emptyList()
        } catch (e: Exception) {
            Log.e("MemoryRepo", "기억 목록 서버 조회 실패: ${e.message}")
            emptyList()
        }
    }

    suspend fun getMemoryByDate(date: String): MemoryEntity? {
        return try {
            val response = api.getMemoriesByDate(date)
            if (response.success) response.data.firstOrNull()?.toEntity() else null
        } catch (e: Exception) {
            Log.e("MemoryRepo", "날짜별 기억 조회 실패: ${e.message}")
            null
        }
    }

    suspend fun getMemoryDatesByMonth(year: Int, month: Int): Set<Int> {
        return try {
            val response = api.getMemoryCalendar(year, month)
            if (response.success) {
                response.data.datesWithMemory
                    .mapNotNull { it.split("-").getOrNull(2)?.toIntOrNull() }
                    .toSet()
            } else emptySet()
        } catch (e: Exception) {
            Log.e("MemoryRepo", "월별 기억 날짜 조회 실패: ${e.message}")
            emptySet()
        }
    }

    suspend fun getMemoriesByMonth(year: Int, month: Int): List<MemoryEntity> {
        val prefix = "%04d-%02d".format(year, month)
        return try {
            getAllMemories().filter { it.date.startsWith(prefix) }
        } catch (e: Exception) {
            Log.e("MemoryRepo", "월별 기억 조회 실패: ${e.message}")
            emptyList()
        }
    }

    suspend fun getMemoryDetail(serverId: Long): MemoryDetailData? {
        return try {
            val response = api.getMemoryDetail(serverId)
            if (response.success) response.data else null
        } catch (e: Exception) {
            Log.e("MemoryRepo", "기억 상세 서버 조회 실패: ${e.message}")
            null
        }
    }

    suspend fun deleteMemoryByDate(date: String) {
        val entity = getMemoryByDate(date)
        entity?.serverId?.let { serverId ->
            try {
                api.deleteMemory(serverId)
            } catch (e: Exception) {
                Log.e("MemoryRepo", "기억 서버 삭제 실패: ${e.message}")
            }
        }
    }

    suspend fun deleteMemoryById(serverId: Long) {
        try {
            api.deleteMemory(serverId)
        } catch (e: Exception) {
            Log.e("MemoryRepo", "기억 서버 삭제 실패: ${e.message}")
        }
    }

    suspend fun getMapLocations(): List<MapLocationData> {
        return try {
            val response = api.getMapLocations()
            if (response.success) response.data else emptyList()
        } catch (e: Exception) {
            Log.e("MemoryRepo", "지도 위치 목록 조회 실패: ${e.message}")
            emptyList()
        }
    }

    suspend fun getMapLocationMemories(location: String): List<MemoryUiModel> {
        return try {
            val response = api.getMapLocationMemories(location)
            if (response.success) response.data.map { MemoryMapper.fromMemoryEntity(it.toEntity()) }
            else emptyList()
        } catch (e: Exception) {
            Log.e("MemoryRepo", "장소별 기억 목록 조회 실패: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAllTags(): List<String> {
        return try {
            val response = api.getAllTags()
            if (response.success) response.data.tags else emptyList()
        } catch (e: Exception) {
            Log.e("MemoryRepo", "태그 목록 조회 실패: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchByKeyword(keyword: String): List<MemoryEntity> {
        return try {
            val response = api.searchMemoriesByKeyword(keyword)
            if (response.success) response.data.map { it.toEntity() } else emptyList()
        } catch (e: Exception) {
            Log.e("MemoryRepo", "키워드 검색 실패: ${e.message}")
            emptyList()
        }
    }

    suspend fun getMemoriesByTag(tagName: String): List<MemoryEntity> {
        return try {
            val response = api.getMemoriesByTag(tagName)
            if (response.success) response.data.map { it.toEntity() } else emptyList()
        } catch (e: Exception) {
            Log.e("MemoryRepo", "태그별 기억 조회 실패: ${e.message}")
            emptyList()
        }
    }

    suspend fun getMemoriesByEmotion(emotion: String): List<MemoryEntity> {
        return try {
            val response = api.getMemoriesByEmotion(emotion)
            if (response.success) response.data.map { it.toEntity() } else emptyList()
        } catch (e: Exception) {
            Log.e("MemoryRepo", "감정별 기억 조회 실패: ${e.message}")
            emptyList()
        }
    }

    suspend fun getMemoriesByLocation(location: String): List<MemoryEntity> {
        return try {
            val response = api.getMemoriesByLocation(location)
            if (response.success) response.data.map { it.toEntity() } else emptyList()
        } catch (e: Exception) {
            Log.e("MemoryRepo", "장소별 기억 조회 실패: ${e.message}")
            emptyList()
        }
    }

    suspend fun getMonthlyAnalysis(year: Int, month: Int): MonthlyAnalysisData? {
        return try {
            val response = api.getMonthlyAnalysis(year, month)
            if (response.success) response.data else null
        } catch (e: Exception) {
            Log.e("MemoryRepo", "월간 분석 조회 실패: ${e.message}")
            null
        }
    }

    private fun String?.toEmotionEnum(): String? = when {
        this == null -> null
        contains("즐거") -> "HAPPY"
        contains("설레") -> "EXCITED"
        contains("평온") -> "CONTENT"
        contains("신나") -> "EXCITED"
        contains("지친") -> "SAD"
        contains("힘든") -> "SAD"
        contains("평범") -> "NEUTRAL"
        else -> null
    }

    private fun MemoryItemData.toEntity(): MemoryEntity = MemoryEntity(
        serverId = memoryId,
        date = memoryDate,
        title = title,
        summary = summary,
        tags = Gson().toJson(tags),
        locations = Gson().toJson(listOfNotNull(location)),
        people = Gson().toJson(people),
        fragmentCount = recordCount,
        representativeFragmentId = null,
        representativePhotoUrl = thumbnailUrl,
        representativeLocationName = location,
        emotion = emotion,
        embedding = null,
        createdAt = memoryDate
    )
}
