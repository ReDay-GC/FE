package com.example.reday.data.model

data class MemoryUiModel(
    val date: String,           // "2026-03-08"
    val title: String,          // AI 연동 전: "3월 8일의 기억"
    val thumbnailPath: String?, // PHOTO 기록이 썸네일이면 파일 경로, 없으면 null (기본 이미지)
    val fragmentCount: Int,
    val locationName: String?,
    val previewText: String?,
    val tags: List<String> = emptyList(),
    val people: List<String> = emptyList(),
    val emotion: String? = null
)
