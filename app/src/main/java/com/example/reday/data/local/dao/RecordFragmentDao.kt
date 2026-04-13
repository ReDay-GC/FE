package com.example.reday.data.local.dao

import androidx.room.*
import com.example.reday.data.local.entity.RecordFragmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordFragmentDao {

    @Insert
    suspend fun insert(entity: RecordFragmentEntity): Long

    @Update
    suspend fun update(entity: RecordFragmentEntity)

    @Delete
    suspend fun delete(entity: RecordFragmentEntity)

    @Query("SELECT * FROM record_fragments WHERE date = :date ORDER BY createdAt ASC")
    fun getByDate(date: String): Flow<List<RecordFragmentEntity>>

    @Query("SELECT * FROM record_fragments WHERE localId = :localId")
    suspend fun getById(localId: Long): RecordFragmentEntity?

    @Query("SELECT * FROM record_fragments WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Long): RecordFragmentEntity?

    @Query("SELECT DISTINCT date FROM record_fragments WHERE date LIKE :yearMonth || '%'")
    suspend fun getDistinctDatesByMonth(yearMonth: String): List<String>

    @Query("SELECT * FROM record_fragments ORDER BY date DESC, createdAt ASC")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<RecordFragmentEntity>>

    @Query("SELECT * FROM record_fragments WHERE latitude IS NOT NULL AND longitude IS NOT NULL")
    suspend fun getAllWithLocation(): List<RecordFragmentEntity>

    @Query("SELECT * FROM record_fragments WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND date IN (:dates)")
    suspend fun getWithLocationByDates(dates: List<String>): List<RecordFragmentEntity>
}
