package com.example.reday.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "record_fragments")
data class RecordFragmentEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: Long? = null,         // 서버 동기화 후 채워지는 fragment_id
    val fragmentType: String,           // "PHOTO", "TEXT", "VOICE"
    val contentText: String? = null,    // 텍스트 기록 내용
    val photoUrl: String? = null,       // 사진 파일 경로
    val voiceUrl: String? = null,       // 음성 파일 경로
    val durationSec: Int? = null,       // 음성 길이(초)
    val createdAt: String,              // ISO 8601 (예: "2026-03-23T10:30:00")
    val date: String,                   // "yyyy-MM-dd" (날짜별 조회용)
    val locationName: String? = null,   // 장소명
    val latitude: Double? = null,       // 위도
    val longitude: Double? = null       // 경도
)
