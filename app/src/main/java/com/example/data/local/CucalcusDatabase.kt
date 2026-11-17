package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CalculationHistoryEntity::class,
        RecentConversionEntity::class,
        RecentToolEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CucalcusDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun converterDao(): ConverterDao
}
