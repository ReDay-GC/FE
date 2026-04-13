package com.example.reday.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverId: Long? = null,
    val date: String,                        // "yyyy-MM-dd"
    val title: String,
    val summary: String,
    val tags: String,                        // JSON 배열 문자열
    val locations: String,                   // JSON 배열 문자열
    val people: String,                      // JSON 배열 문자열
    val fragmentCount: Int,
    val representativeFragmentId: Long?,     // 대표 조각 localId
    val representativePhotoUrl: String?,     // 대표 사진 경로
    val representativeLocationName: String?, // 대표 조각의 위치
    val emotion: String? = null,             // AI 감정 태그 (예: "😊 즐거운")
    val embedding: String? = null,           // JSON 배열 문자열 (임베딩 벡터)
    val createdAt: String
)
