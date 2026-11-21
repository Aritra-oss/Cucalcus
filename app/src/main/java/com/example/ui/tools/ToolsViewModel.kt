package com.example.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.RecentToolEntity
import com.example.data.repository.CurrencyRepository
import com.example.domain.tools.ToolId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ToolsViewModel(
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

    private val _selectedTool = MutableStateFlow<ToolId?>(null)
    private val _isLoadingRecentTools = MutableStateFlow(true)
    val isLoadingRecentTools: StateFlow<Boolean> = _isLoadingRecentTools.asStateFlow()
    val selectedTool: StateFlow<ToolId?> = _selectedTool.asStateFlow()

    val recentTools: StateFlow<List<ToolId>> = currencyRepository.recentTools
        .map { entities: List<RecentToolEntity> ->
            entities.mapNotNull { e -> ToolId.entries.firstOrNull { it.name == e.toolId } }
        }
        .onEach { _isLoadingRecentTools.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun openTool(toolId: ToolId) {
        _selectedTool.value = toolId
        viewModelScope.launch {
            currencyRepository.recordToolUse(toolId.name)
        }
    }

    fun closeTool() {
        _selectedTool.value = null
    }

    class Factory(private val currencyRepository: CurrencyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ToolsViewModel(currencyRepository) as T
        }
    }
}
