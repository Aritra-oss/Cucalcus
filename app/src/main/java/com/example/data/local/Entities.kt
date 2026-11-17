package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_history")
data class CalculationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val expression: String,
    val result: String,
    val rawResult: Double,
    val angleMode: String = "DEG",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false
)

@Entity(tableName = "recent_conversions")
data class RecentConversionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val kind: String, // "UNIT" or "CURRENCY"
    val categoryId: String,
    val fromCode: String,
    val toCode: String,
    val inputValue: String,
    val outputValue: String,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_tools")
data class RecentToolEntity(
    @PrimaryKey val toolId: String,
    val lastUsedAt: Long = System.currentTimeMillis(),
    val useCount: Int = 1
)
