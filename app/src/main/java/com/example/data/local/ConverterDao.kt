package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConverterDao {
    @Query("SELECT * FROM recent_conversions WHERE kind = :kind ORDER BY isFavorite DESC, timestamp DESC LIMIT 40")
    fun observeConversions(kind: String): Flow<List<RecentConversionEntity>>

    @Query("SELECT * FROM recent_conversions WHERE kind = :kind AND categoryId = :categoryId AND fromCode = :fromCode AND toCode = :toCode LIMIT 1")
    suspend fun findExisting(kind: String, categoryId: String, fromCode: String, toCode: String): RecentConversionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecentConversionEntity): Long

    @Query("UPDATE recent_conversions SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM recent_conversions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM recent_tools ORDER BY lastUsedAt DESC LIMIT 12")
    fun observeRecentTools(): Flow<List<RecentToolEntity>>

    @Query("SELECT * FROM recent_tools WHERE toolId = :toolId LIMIT 1")
    suspend fun getRecentTool(toolId: String): RecentToolEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentTool(entity: RecentToolEntity)
}
