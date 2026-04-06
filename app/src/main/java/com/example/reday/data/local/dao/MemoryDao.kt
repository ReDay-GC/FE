package com.example.reday.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.reday.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MemoryEntity): Long

    @Transaction
    suspend fun deleteAndInsert(entity: MemoryEntity): Long {
        deleteByDate(entity.date)
        return insert(entity)
    }

    @Query("SELECT * FROM memories WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): MemoryEntity?

    @Query("SELECT * FROM memories ORDER BY date DESC")
    fun getAll(): Flow<List<MemoryEntity>>

    @Query("DELETE FROM memories WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("SELECT DISTINCT date FROM memories WHERE date LIKE :yearMonth || '%'")
    suspend fun getDistinctDatesByMonth(yearMonth: String): List<String>

    @Query("SELECT date FROM memories")
    suspend fun getAllDates(): List<String>

    @Query("SELECT * FROM memories WHERE date LIKE :yearMonth || '%' ORDER BY date DESC")
    fun getByMonth(yearMonth: String): Flow<List<MemoryEntity>>

    @Query("SELECT embedding FROM memories WHERE id = :id")
    suspend fun getEmbeddingById(id: Long): String?
}
