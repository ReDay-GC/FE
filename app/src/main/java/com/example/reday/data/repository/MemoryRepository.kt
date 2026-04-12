package com.example.reday.data.repository

import android.util.Log
import com.example.reday.data.local.dao.MemoryDao
import com.example.reday.data.local.entity.MemoryEntity
import com.example.reday.data.remote.CreateMemoryRequest
import com.example.reday.data.remote.MemoryDetailData
import com.example.reday.data.remote.RetrofitClient
import com.example.reday.data.remote.SpringMemoryApiService
import com.google.gson.Gson

class MemoryRepository(
    private val dao: MemoryDao,
    private val api: SpringMemoryApiService = RetrofitClient.springMemoryApi
) {

    // 기억 저장: 서버 POST 후 serverId를 로컬에도 캐싱
    suspend fun saveMemory(entity: MemoryEntity): Long {
        val gson = Gson()
        val tags = try { gson.fromJson(entity.tags, Array<String>::class.java).toList() } catch (e: Exception) { emptyList() }
        val people = try { gson.fromJson(entity.people, Array<String>::class.java).toList() } catch (e: Exception) { emptyList() }

        return try {
            val safeThumbUrl = entity.representativePhotoUrl
                ?.takeIf { it.startsWith("http") }
            val request = CreateMemoryRequest(
                title = entity.title.ifBlank { "기억" },
                summary = entity.summary.ifBlank { "" },
                description = null,
                memoryDate = entity.date,
                emotion = entity.emotion.toEmotionEnum(),
                thumbnailUrl = safeThumbUrl,
                location = entity.representativeLocationName,
                tags = tags,
                people = people
            )
            Log.d("MemoryRepo", "저장 요청: ${Gson().toJson(request)}")
            val response = api.createMemory(request)
            if (response.success && response.data != null) {
                dao.deleteAndInsert(entity.copy(serverId = response.data.memoryId))
            } else {
                dao.deleteAndInsert(entity)
            }
        } catch (e: Exception) {
            Log.e("MemoryRepo", "기억 서버 저장 실패: ${e.message}")
            dao.deleteAndInsert(entity)
        }
    }

    // 전체 기억 목록 (서버)
    suspend fun getAllMemories(): List<MemoryEntity> {
        return try {
            val response = api.getAllMemories()
            if (response.success) response.data.map { it.toEntity() }
            else emptyList()
        } catch (e: Exception) {
            Log.e("MemoryRepo", "기억 목록 서버 조회 실패: ${e.message}")
            emptyList()
        }
    }

    // 날짜별 기억 (서버 전체 조회 후 필터)
    suspend fun getMemoryByDate(date: String): MemoryEntity? {
        return try {
            getAllMemories().firstOrNull { it.date == date }
        } catch (e: Exception) {
            Log.e("MemoryRepo", "날짜별 기억 조회 실패: ${e.message}")
            null
        }
    }

    // 월별 기억 날짜 Set (캘린더 점 표시용)
    suspend fun getMemoryDatesByMonth(year: Int, month: Int): Set<Int> {
        val prefix = "%04d-%02d".format(year, month)
        return try {
            getAllMemories()
                .filter { it.date.startsWith(prefix) }
                .mapNotNull { it.date.split("-").getOrNull(2)?.toIntOrNull() }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    // 월별 기억 목록 (보관함용)
    suspend fun getMemoriesByMonth(year: Int, month: Int): List<MemoryEntity> {
        val prefix = "%04d-%02d".format(year, month)
        return try {
            getAllMemories().filter { it.date.startsWith(prefix) }
        } catch (e: Exception) {
            Log.e("MemoryRepo", "월별 기억 조회 실패: ${e.message}")
            emptyList()
        }
    }

    // 기억 상세 (서버)
    suspend fun getMemoryDetail(serverId: Long): MemoryDetailData? {
        return try {
            val response = api.getMemoryDetail(serverId)
            if (response.success) response.data else null
        } catch (e: Exception) {
            Log.e("MemoryRepo", "기억 상세 서버 조회 실패: ${e.message}")
            null
        }
    }

    // 기억 삭제 (서버 + 로컬)
    suspend fun deleteMemoryByDate(date: String) {
        val entity = getMemoryByDate(date)  // 서버에서 serverId 조회
        entity?.serverId?.let { serverId ->
            try {
                api.deleteMemory(serverId)
            } catch (e: Exception) {
                Log.e("MemoryRepo", "기억 서버 삭제 실패: ${e.message}")
            }
        }
        dao.deleteByDate(date)
    }

    // 임베딩 (AI 서버용, 로컬 캐시)
    suspend fun getEmbeddingById(id: Long): String? = dao.getEmbeddingById(id)

    // AI 감정 문자열 → 서버 enum 변환
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

    // MemoryItemData → MemoryEntity 변환
    private fun com.example.reday.data.remote.MemoryItemData.toEntity(): MemoryEntity = MemoryEntity(
        serverId = memoryId,
        date = memoryDate,
        title = title,
        summary = summary,
        tags = "[]",
        locations = Gson().toJson(listOfNotNull(location)),
        people = "[]",
        fragmentCount = 0,
        representativeFragmentId = null,
        representativePhotoUrl = thumbnailUrl,
        representativeLocationName = location,
        emotion = emotion,
        embedding = null,
        createdAt = memoryDate
    )
}
