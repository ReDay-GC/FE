package com.example.reday.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_insights")
data class MonthlyInsightEntity(
    @PrimaryKey val yearMonth: String,   // "2026-03"
    val insightText: String,
    val createdAt: String
)
