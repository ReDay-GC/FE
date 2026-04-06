package com.example.reday.data.repository

import com.example.reday.data.local.dao.MonthlyInsightDao
import com.example.reday.data.local.entity.MonthlyInsightEntity

class MonthlyInsightRepository(private val dao: MonthlyInsightDao) {

    suspend fun saveInsight(yearMonth: String, insightText: String) {
        dao.insert(
            MonthlyInsightEntity(
                yearMonth = yearMonth,
                insightText = insightText,
                createdAt = java.time.LocalDateTime.now().toString()
            )
        )
    }

    suspend fun getInsight(yearMonth: String): MonthlyInsightEntity? =
        dao.getByYearMonth(yearMonth)
}
