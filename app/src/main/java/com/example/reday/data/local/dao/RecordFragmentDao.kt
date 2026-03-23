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
}
