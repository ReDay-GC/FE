package com.example.reday.data.mapper

import com.example.reday.data.local.entity.RecordFragmentEntity
import com.example.reday.data.model.FragmentType
import com.example.reday.data.model.RecordFragmentUiModel

object RecordFragmentMapper {

    fun entityToUiModel(entity: RecordFragmentEntity): RecordFragmentUiModel {
        return RecordFragmentUiModel(
            localId = entity.localId,
            serverId = entity.serverId,
            fragmentType = when (entity.fragmentType) {
                "PHOTO" -> FragmentType.PHOTO
                "VOICE" -> FragmentType.VOICE
                else -> FragmentType.TEXT
            },
            contentText = entity.contentText,
            photoUrl = entity.photoUrl,
            voiceUrl = entity.voiceUrl,
            durationSec = entity.durationSec,
            createdAt = entity.createdAt,
            date = entity.date,
            locationName = entity.locationName,
            latitude = entity.latitude,
            longitude = entity.longitude
        )
    }

    fun uiModelToEntity(model: RecordFragmentUiModel): RecordFragmentEntity {
        return RecordFragmentEntity(
            localId = model.localId,
            serverId = model.serverId,
            fragmentType = model.fragmentType.name,
            contentText = model.contentText,
            photoUrl = model.photoUrl,
            voiceUrl = model.voiceUrl,
            durationSec = model.durationSec,
            createdAt = model.createdAt,
            date = model.date,
            locationName = model.locationName,
            latitude = model.latitude,
            longitude = model.longitude
        )
    }
}
