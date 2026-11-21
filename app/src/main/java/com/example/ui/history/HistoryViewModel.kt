package com.example.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CalculationHistoryEntity
import com.example.data.repository.HistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistorySortOrder(val label: String) {
    RECENT("Recent First"),
    OLDEST("Oldest First")
}

data class HistoryUiState(
    val entries: List<CalculationHistoryEntity> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: HistorySortOrder = HistorySortOrder.RECENT,
    val favoritesOnly: Boolean = false,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val sortOrder = MutableStateFlow(HistorySortOrder.RECENT)
    private val favoritesOnly = MutableStateFlow(false)

    private val rawHistory = sortOrder.flatMapLatest { order ->
        historyRepository.observeHistory(newestFirst = order == HistorySortOrder.RECENT)
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        rawHistory,
        searchQuery,
        sortOrder,
        favoritesOnly
    ) { items, query, sort, favOnly ->
        val filtered = items.filter { item ->
            val matchesFav = !favOnly || item.isFavorite
            val q = query.trim()
            val matchesQuery = q.isEmpty() ||
                item.expression.contains(q, ignoreCase = true) ||
                item.result.contains(q, ignoreCase = true)
            matchesFav && matchesQuery
        }
        HistoryUiState(
            entries = filtered,
            searchQuery = query,
            sortOrder = sort,
            favoritesOnly = favOnly,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    fun updateSearch(query: String) {
        searchQuery.value = query
    }

    fun toggleSortOrder() {
        sortOrder.value = if (sortOrder.value == HistorySortOrder.RECENT) {
            HistorySortOrder.OLDEST
        } else {
            HistorySortOrder.RECENT
        }
    }

    fun toggleFavoritesFilter() {
        favoritesOnly.value = !favoritesOnly.value
    }

    fun toggleFavorite(item: CalculationHistoryEntity) {
        viewModelScope.launch {
            historyRepository.setFavorite(item.id, !item.isFavorite)
        }
    }

    fun togglePin(item: CalculationHistoryEntity) {
        viewModelScope.launch {
            historyRepository.setPinned(item.id, !item.isPinned)
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteById(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            historyRepository.clearAll()
        }
    }

    suspend fun generateCsvExport(): String = historyRepository.exportCsv()
    suspend fun generateTxtExport(): String = historyRepository.exportTxt()

    class Factory(private val historyRepository: HistoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(historyRepository) as T
        }
    }
}
