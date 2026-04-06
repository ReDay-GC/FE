package com.example.reday.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.reday.data.local.entity.MonthlyInsightEntity

@Dao
interface MonthlyInsightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MonthlyInsightEntity)

    @Query("SELECT * FROM monthly_insights WHERE yearMonth = :yearMonth LIMIT 1")
    suspend fun getByYearMonth(yearMonth: String): MonthlyInsightEntity?
}
