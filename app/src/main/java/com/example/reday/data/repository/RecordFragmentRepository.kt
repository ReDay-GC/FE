package com.example.reday.data.repository

import android.util.Log
import com.example.reday.data.local.dao.RecordFragmentDao
import com.example.reday.data.local.entity.RecordFragmentEntity
import com.example.reday.data.mapper.RecordFragmentMapper
import com.example.reday.data.model.RecordFragmentUiModel
import com.example.reday.data.remote.RecordApiService
import com.example.reday.data.remote.RetrofitClient
import com.example.reday.data.remote.SaveTextRecordRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RecordFragmentRepository(
    private val dao: RecordFragmentDao,
    private val api: RecordApiService = RetrofitClient.recordApi
) {

    private val apiDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    private fun String.toPlainRequestBody(): RequestBody =
        toRequestBody("text/plain".toMediaTypeOrNull())

    private fun String.formatForApi(): String = try {
        val dt = LocalDateTime.parse(this.take(19))
        dt.format(apiDateFormatter)
    } catch (e: Exception) {
        this
    }

    fun getFragmentsByDate(date: String): Flow<List<RecordFragmentUiModel>> =
        dao.getByDate(date).map { list -> list.map { RecordFragmentMapper.entityToUiModel(it) } }

    fun getAllFragments(): Flow<List<RecordFragmentUiModel>> =
        dao.getAll().map { list -> list.map { RecordFragmentMapper.entityToUiModel(it) } }

    suspend fun getFragmentsWithLocation(): List<RecordFragmentUiModel> =
        dao.getAllWithLocation().map { RecordFragmentMapper.entityToUiModel(it) }

    suspend fun getFragmentsWithLocationByDates(dates: List<String>): List<RecordFragmentUiModel> =
        dao.getWithLocationByDates(dates).map { RecordFragmentMapper.entityToUiModel(it) }

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
        val localId = dao.insert(entity)

        try {
            val response = api.saveText(
                SaveTextRecordRequest(
                    recordDate = date,
                    textContent = contentText,
                    address = locationName,
                    latitude = latitude,
                    longitude = longitude,
                    recordedAt = createdAt.formatForApi()
                )
            )
            if (response.success && response.data != null) {
                dao.update(entity.copy(localId = localId, serverId = response.data.recordId))
            }
        } catch (e: Exception) {
            Log.e("RecordRepo", "텍스트 서버 저장 실패: ${e.message}")
        }

        return localId
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
        val localId = dao.insert(entity)

        try {
            val file = File(photoUrl)
            val ext = file.extension.lowercase()
            val mimeType = when (ext) {
                "png" -> "image/png"
                "gif" -> "image/gif"
                else -> "image/jpeg"
            }
            val photoPart = MultipartBody.Part.createFormData(
                "photo", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull())
            )
            val params = mutableMapOf<String, RequestBody>(
                "recordDate" to date.toPlainRequestBody()
            )
            contentText?.let { params["textContent"] = it.toPlainRequestBody() }
            locationName?.let { params["address"] = it.toPlainRequestBody() }
            latitude?.let { params["latitude"] = it.toString().toPlainRequestBody() }
            longitude?.let { params["longitude"] = it.toString().toPlainRequestBody() }
            params["recordedAt"] = createdAt.formatForApi().toPlainRequestBody()

            val response = api.savePhoto(photoPart, params)
            if (response.success && response.data != null) {
                dao.update(
                    entity.copy(
                        localId = localId,
                        serverId = response.data.recordId,
                        photoUrl = response.data.fileUrl ?: photoUrl
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("RecordRepo", "사진 서버 저장 실패: ${e.message}")
        }

        return localId
    }

    suspend fun saveVoiceFragment(
        voiceUrl: String,
        durationSec: Int,
        date: String,
        contentText: String? = null,
        locationName: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Long {
        val createdAt = LocalDateTime.now().format(apiDateFormatter)
        val entity = RecordFragmentEntity(
            fragmentType = "VOICE",
            voiceUrl = voiceUrl,
            durationSec = durationSec,
            contentText = contentText,
            createdAt = createdAt,
            date = date,
            locationName = locationName,
            latitude = latitude,
            longitude = longitude
        )
        val localId = dao.insert(entity)

        try {
            val file = File(voiceUrl)
            val ext = file.extension.lowercase()
            val mimeType = when (ext) {
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                else -> "audio/m4a"
            }
            val audioPart = MultipartBody.Part.createFormData(
                "audio", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull())
            )
            val params = mutableMapOf<String, RequestBody>(
                "recordDate" to date.toPlainRequestBody(),
                "recordedAt" to createdAt.toPlainRequestBody(),
                "voiceDurationSeconds" to durationSec.toString().toPlainRequestBody()
            )
            contentText?.let { params["textContent"] = it.toPlainRequestBody() }
            locationName?.let { params["address"] = it.toPlainRequestBody() }
            latitude?.let { params["latitude"] = it.toString().toPlainRequestBody() }
            longitude?.let { params["longitude"] = it.toString().toPlainRequestBody() }

            val response = api.saveVoice(audioPart, params)
            if (response.success && response.data != null) {
                dao.update(
                    entity.copy(
                        localId = localId,
                        serverId = response.data.recordId,
                        voiceUrl = response.data.fileUrl ?: voiceUrl
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("RecordRepo", "음성 서버 저장 실패: ${e.message}")
        }

        return localId
    }

    // 서버 데이터를 로컬 DB에 반영 (날짜별)
    // serverId 기준으로 upsert, serverId 없는 로컬 전용 기록은 유지
    suspend fun syncFragmentsByDate(date: String) {
        try {
            val response = api.getRecordsByDate(date)
            if (!response.success) return
            response.data.forEach { item ->
                val existing = dao.getByServerId(item.recordId)
                val entity = RecordFragmentEntity(
                    localId = existing?.localId ?: 0,
                    serverId = item.recordId,
                    fragmentType = item.recordType,
                    contentText = item.textContent,
                    photoUrl = if (item.recordType == "PHOTO") item.fileUrl else null,
                    voiceUrl = if (item.recordType == "VOICE") item.fileUrl else null,
                    durationSec = item.voiceDurationSeconds,
                    createdAt = item.recordedAt ?: item.createdAt,
                    date = item.recordDate,
                    locationName = item.address,
                    latitude = item.latitude,
                    longitude = item.longitude
                )
                if (existing != null) {
                    dao.update(entity)
                } else {
                    dao.insert(entity)
                }
            }
        } catch (e: Exception) {
            Log.e("RecordRepo", "날짜별 sync 실패: ${e.message}")
        }
    }

    suspend fun getRecordDatesByMonth(year: Int, month: Int): Set<Int> {
        val yearMonth = "%04d-%02d".format(year, month)
        return dao.getDistinctDatesByMonth(yearMonth)
            .mapNotNull { dateStr ->
                dateStr.split("-").getOrNull(2)?.toIntOrNull()
            }.toSet()
    }

    suspend fun deleteFragment(model: RecordFragmentUiModel) {
        model.serverId?.let { serverId ->
            try {
                api.deleteRecord(serverId)
            } catch (e: Exception) {
                Log.e("RecordRepo", "서버 기록 삭제 실패: ${e.message}")
            }
        }
        dao.delete(RecordFragmentMapper.uiModelToEntity(model))
    }
}
