package com.example.reday.data.dto

// POST /api/fragments/text
data class TextFragmentRequestDto(
    val content_text: String,
    val created_at: String
)

// POST /api/fragments/photo
data class PhotoFragmentRequestDto(
    val photo_url: String,
    val created_at: String
)

// POST /api/fragments/voice
data class VoiceFragmentRequestDto(
    val voice_url: String,
    val duration_sec: Int
)

// POST 공통 응답
data class FragmentResponseDto(
    val result_code: Int,
    val data: FragmentDataDto?
)

data class FragmentDataDto(
    val fragment_id: Long
)

// GET /api/fragments/today, /api/fragments/{date} 응답
data class FragmentListResponseDto(
    val result_code: Int,
    val data: FragmentListDataDto?
)

data class FragmentListDataDto(
    val fragments: List<FragmentSummaryDto>
)

data class FragmentSummaryDto(
    val fragment_id: Long,
    val fragment_type: String   // "PHOTO", "TEXT", "VOICE"
)
