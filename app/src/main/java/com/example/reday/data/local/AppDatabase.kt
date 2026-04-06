package com.example.reday.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.reday.data.local.dao.MemoryDao
import com.example.reday.data.local.dao.MonthlyInsightDao
import com.example.reday.data.local.dao.RecordFragmentDao
import com.example.reday.data.local.entity.MemoryEntity
import com.example.reday.data.local.entity.MonthlyInsightEntity
import com.example.reday.data.local.entity.RecordFragmentEntity

@Database(entities = [RecordFragmentEntity::class, MemoryEntity::class, MonthlyInsightEntity::class], version = 6)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordFragmentDao(): RecordFragmentDao
    abstract fun memoryDao(): MemoryDao
    abstract fun monthlyInsightDao(): MonthlyInsightDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reday_db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
