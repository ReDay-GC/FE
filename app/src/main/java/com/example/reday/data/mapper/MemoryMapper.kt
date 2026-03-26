package com.example.reday.data.mapper

import com.example.reday.data.model.FragmentType
import com.example.reday.data.model.MemoryUiModel
import com.example.reday.data.model.RecordFragmentUiModel

object MemoryMapper {

    fun fromFragmentList(fragments: List<RecordFragmentUiModel>, limit: Int = Int.MAX_VALUE): List<MemoryUiModel> {
        return fragments
            .groupBy { it.date }
            .entries
            .sortedByDescending { it.key }
            .take(limit)
            .map { (date, group) -> toMemoryUiModel(date, group) }
    }

    private fun toMemoryUiModel(date: String, fragments: List<RecordFragmentUiModel>): MemoryUiModel {
        val sorted = fragments.sortedBy { it.createdAt }

        // 썸네일: 가장 먼저 추가된 비-VOICE 기록 기준
        // PHOTO → 사진 경로 / TEXT → null(기본 이미지) / VOICE → 제외
        val thumbnailFragment = sorted.firstOrNull { it.fragmentType != FragmentType.VOICE }
        val thumbnailPath = if (thumbnailFragment?.fragmentType == FragmentType.PHOTO) {
            thumbnailFragment.photoUrl
        } else {
            null
        }

        val locationName = sorted.firstOrNull { it.locationName != null }?.locationName

        val previewText = sorted.firstOrNull {
            it.fragmentType == FragmentType.TEXT && it.contentText != null
        }?.contentText ?: sorted.firstOrNull {
            it.fragmentType == FragmentType.PHOTO && it.contentText != null
        }?.contentText

        val title = formatTitle(date)

        return MemoryUiModel(
            date = date,
            title = title,
            thumbnailPath = thumbnailPath,
            fragmentCount = fragments.size,
            locationName = locationName,
            previewText = previewText
        )
    }

    private fun formatTitle(date: String): String {
        // "2026-03-08" → "3월 8일의 기억"
        return try {
            val parts = date.split("-")
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            "${month}월 ${day}일의 기억"
        } catch (e: Exception) {
            date
        }
    }
}
