package com.example.reday.data.model

data class RecordFragmentUiModel(
    val localId: Long,
    val serverId: Long?,
    val fragmentType: FragmentType,
    val contentText: String?,
    val photoUrl: String?,
    val voiceUrl: String?,
    val durationSec: Int?,
    val createdAt: String,
    val date: String,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?
)

enum class FragmentType { PHOTO, TEXT, VOICE }
