package com.example.reday.data.repository

import com.example.reday.data.local.dao.MemoryDao
import com.example.reday.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val dao: MemoryDao) {

    suspend fun saveMemory(entity: MemoryEntity): Long {
        return dao.deleteAndInsert(entity)
    }

    fun getAllMemories(): Flow<List<MemoryEntity>> = dao.getAll()

    suspend fun getAllMemoryDates(): List<String> = dao.getAllDates()

    suspend fun getMemoryByDate(date: String): MemoryEntity? = dao.getByDate(date)

    suspend fun getMemoryDatesByMonth(year: Int, month: Int): Set<Int> {
        val yearMonth = "%04d-%02d".format(year, month)
        return dao.getDistinctDatesByMonth(yearMonth)
            .mapNotNull { it.split("-").getOrNull(2)?.toIntOrNull() }
            .toSet()
    }

    fun getMemoriesByMonth(year: Int, month: Int): Flow<List<MemoryEntity>> {
        val yearMonth = "%04d-%02d".format(year, month)
        return dao.getByMonth(yearMonth)
    }

    suspend fun getEmbeddingById(id: Long): String? = dao.getEmbeddingById(id)
}
