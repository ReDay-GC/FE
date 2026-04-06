package com.example.reday.data.mapper

import com.example.reday.data.local.entity.MemoryEntity
import com.example.reday.data.model.FragmentType
import com.example.reday.data.model.MapLocationGroup
import com.example.reday.data.model.MemoryUiModel
import com.example.reday.data.model.RecordFragmentUiModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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

    fun fromMemoryEntityList(entities: List<MemoryEntity>, limit: Int = Int.MAX_VALUE): List<MemoryUiModel> {
        return entities
            .sortedByDescending { it.date }
            .take(limit)
            .map { fromMemoryEntity(it) }
    }

    fun fromMemoryEntity(entity: MemoryEntity): MemoryUiModel {
        val locationName = entity.representativeLocationName
            ?: try {
                val type = object : TypeToken<List<String>>() {}.type
                Gson().fromJson<List<String>>(entity.locations, type).firstOrNull()
            } catch (e: Exception) { null }

        val tags = try {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson<List<String>>(entity.tags, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        val people = try {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson<List<String>>(entity.people, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        return MemoryUiModel(
            id = entity.id,
            date = entity.date,
            title = formatTitle(entity.date),
            thumbnailPath = entity.representativePhotoUrl,
            fragmentCount = entity.fragmentCount,
            locationName = locationName,
            previewText = entity.summary,
            tags = tags,
            people = people,
            emotion = entity.emotion
        )
    }

    fun groupByLocation(fragments: List<RecordFragmentUiModel>): List<MapLocationGroup> {
        return fragments
            .filter { it.locationName != null && it.latitude != null && it.longitude != null }
            .groupBy { it.locationName!! }
            .map { (locationName, group) ->
                val lat = group.first().latitude!!
                val lng = group.first().longitude!!
                val memories = fromFragmentList(group)
                MapLocationGroup(locationName, lat, lng, group.size, memories)
            }
    }

    private fun formatTitle(date: String): String {
        // "2026-03-08" → "2026년 3월 8일의 기억"
        return try {
            val parts = date.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            "${year}년 ${month}월 ${day}일의 기억"
        } catch (e: Exception) {
            date
        }
    }
}
