package com.example.reday.data.repository

import com.example.reday.data.local.dao.RecordFragmentDao
import com.example.reday.data.local.entity.RecordFragmentEntity
import com.example.reday.data.mapper.RecordFragmentMapper
import com.example.reday.data.model.RecordFragmentUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecordFragmentRepository(private val dao: RecordFragmentDao) {

    fun getFragmentsByDate(date: String): Flow<List<RecordFragmentUiModel>> =
        dao.getByDate(date).map { list -> list.map { RecordFragmentMapper.entityToUiModel(it) } }

    suspend fun saveTextFragment(
        contentText: String,
        createdAt: String,
        date: String,
        locationName: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Long {
        val entity = RecordFragmentEntity(
            fragmentType = "TEXT",
            contentText = contentText,
            createdAt = createdAt,
            date = date,
            locationName = locationName,
            latitude = latitude,
            longitude = longitude
        )
        return dao.insert(entity)
    }

    suspend fun savePhotoFragment(
        photoUrl: String,
        createdAt: String,
        date: String,
        contentText: String? = null,
        locationName: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Long {
        val entity = RecordFragmentEntity(
            fragmentType = "PHOTO",
            photoUrl = photoUrl,
            contentText = contentText,
            createdAt = createdAt,
            date = date,
            locationName = locationName,
            latitude = latitude,
            longitude = longitude
        )
        return dao.insert(entity)
    }

    suspend fun saveVoiceFragment(
        voiceUrl: String,
        durationSec: Int,
        date: String,
        locationName: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Long {
        val entity = RecordFragmentEntity(
            fragmentType = "VOICE",
            voiceUrl = voiceUrl,
            durationSec = durationSec,
            createdAt = java.time.LocalDateTime.now().toString(),
            date = date,
            locationName = locationName,
            latitude = latitude,
            longitude = longitude
        )
        return dao.insert(entity)
    }

    suspend fun deleteFragment(model: RecordFragmentUiModel) {
        dao.delete(RecordFragmentMapper.uiModelToEntity(model))
    }
}
