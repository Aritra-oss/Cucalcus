package com.example.data.repository

import com.example.data.local.CalculationHistoryEntity
import com.example.data.local.HistoryDao
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryRepository(private val historyDao: HistoryDao) {

    fun observeHistory(newestFirst: Boolean): Flow<List<CalculationHistoryEntity>> {
        return if (newestFirst) {
            historyDao.observeHistoryNewestFirst()
        } else {
            historyDao.observeHistoryOldestFirst()
        }
    }

    suspend fun saveCalculation(
        expression: String,
        formattedResult: String,
        rawResult: Double,
        angleMode: String
    ) {
        historyDao.insert(
            CalculationHistoryEntity(
                expression = expression,
                result = formattedResult,
                rawResult = rawResult,
                angleMode = angleMode,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun setFavorite(id: Long, isFavorite: Boolean) = historyDao.setFavorite(id, isFavorite)
    suspend fun setPinned(id: Long, isPinned: Boolean) = historyDao.setPinned(id, isPinned)
    suspend fun deleteById(id: Long) = historyDao.deleteById(id)
    suspend fun clearAll() = historyDao.deleteAll()

    suspend fun exportCsv(): String {
        val entries = historyDao.getAllSnapshot()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder()
        sb.appendLine("Timestamp,AngleMode,Expression,Result,Pinned,Favorite")
        for (item in entries) {
            val dt = sdf.format(Date(item.timestamp))
            val expr = item.expression.replace("\"", "\"\"")
            val res = item.result.replace("\"", "\"\"")
            sb.appendLine("\"$dt\",\"${item.angleMode}\",\"$expr\",\"$res\",${item.isPinned},${item.isFavorite}")
        }
        return sb.toString()
    }

    suspend fun exportTxt(): String {
        val entries = historyDao.getAllSnapshot()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder()
        sb.appendLine("Cucalcus — Calculation History")
        sb.appendLine("==============================")
        for (item in entries) {
            val dt = sdf.format(Date(item.timestamp))
            val tags = buildList {
                if (item.isPinned) add("PINNED")
                if (item.isFavorite) add("FAV")
            }.joinToString(", ")
            val tagStr = if (tags.isNotEmpty()) " [$tags]" else ""
            sb.appendLine("[$dt | ${item.angleMode}]$tagStr")
            sb.appendLine("  ${item.expression} = ${item.result}")
            sb.appendLine()
        }
        return sb.toString()
    }
}
