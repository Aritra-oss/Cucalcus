package com.example.ui.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.RecentConversionEntity
import com.example.data.preferences.CucalcusSettings
import com.example.data.preferences.SettingsRepository
import com.example.data.repository.CurrencyInfo
import com.example.data.repository.CurrencyRepository
import com.example.data.repository.ExchangeRatesSnapshot
import com.example.domain.converter.UnitCategory
import com.example.domain.converter.UnitConverterEngine
import com.example.domain.converter.UnitDefinition
import com.example.domain.math.NumberFormatMode
import com.example.domain.math.NumberFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ConverterTab(val label: String) {
    UNITS("Units"),
    CURRENCY("Currency")
}

data class ConverterUiState(
    val activeTab: ConverterTab = ConverterTab.UNITS,
    // Units state
    val selectedCategory: UnitCategory = UnitCategory.LENGTH,
    val fromUnit: UnitDefinition = UnitConverterEngine.unitsByCategory[UnitCategory.LENGTH]!![0],
    val toUnit: UnitDefinition = UnitConverterEngine.unitsByCategory[UnitCategory.LENGTH]!![1],
    val unitInputText: String = "1",
    val unitOutputText: String = "0.001",
    val recentUnitConversions: List<RecentConversionEntity> = emptyList(),
    // Currency state
    val fromCurrency: CurrencyInfo = CurrencyRepository.findCurrency("USD"),
    val toCurrency: CurrencyInfo = CurrencyRepository.findCurrency("INR"),
    val currencyInputText: String = "100",
    val currencyOutputText: String = "—",
    val exchangeRateDisplay: String = "",
    val ratesSnapshot: ExchangeRatesSnapshot? = null,
    val isLoadingRates: Boolean = false,
    val networkError: String? = null,
    val recentCurrencyConversions: List<RecentConversionEntity> = emptyList()
)

class ConverterViewModel(
    private val currencyRepository: CurrencyRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ConverterUiState())
    private var currentSettings = CucalcusSettings()

    val uiState: StateFlow<ConverterUiState> = combine(
        _state,
        settingsRepository.settingsFlow,
        currencyRepository.recentUnitConversions,
        currencyRepository.recentCurrencyConversions
    ) { st, cfg, recentUnits, recentCurrencies ->
        currentSettings = cfg
        val unitOut = computeUnitOutput(st.unitInputText, st.fromUnit, st.toUnit, cfg)
        val (currOut, rateStr) = computeCurrencyOutput(
            st.currencyInputText,
            st.fromCurrency,
            st.toCurrency,
            st.ratesSnapshot,
            cfg
        )
        st.copy(
            unitOutputText = unitOut,
            currencyOutputText = currOut,
            exchangeRateDisplay = rateStr,
            recentUnitConversions = recentUnits,
            recentCurrencyConversions = recentCurrencies
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConverterUiState())

    init {
        loadCachedAndRefreshCurrency()
    }

    fun selectTab(tab: ConverterTab) {
        _state.update { it.copy(activeTab = tab) }
    }

    // --- Unit Converter Actions ---
    fun selectCategory(category: UnitCategory) {
        val units = UnitConverterEngine.unitsByCategory[category] ?: return
        _state.update {
            it.copy(
                selectedCategory = category,
                fromUnit = units.first(),
                toUnit = units.getOrElse(1) { units.first() }
            )
        }
    }

    fun selectUnit(unit: UnitDefinition, isFrom: Boolean) {
        _state.update { current ->
            // If selecting from global unit search in another category, switch category too
            if (unit.category != current.selectedCategory) {
                val units = UnitConverterEngine.unitsByCategory[unit.category] ?: listOf(unit)
                val other = units.firstOrNull { it.id != unit.id } ?: unit
                current.copy(
                    selectedCategory = unit.category,
                    fromUnit = if (isFrom) unit else other,
                    toUnit = if (isFrom) other else unit
                )
            } else {
                current.copy(
                    fromUnit = if (isFrom) unit else current.fromUnit,
                    toUnit = if (isFrom) current.toUnit else unit
                )
            }
        }
    }

    fun updateUnitInput(text: String) {
        _state.update { it.copy(unitInputText = text) }
    }

    fun clearUnitInput() {
        _state.update { it.copy(unitInputText = "") }
    }

    fun swapUnits() {
        _state.update {
            it.copy(fromUnit = it.toUnit, toUnit = it.fromUnit)
        }
    }

    fun saveCurrentUnitConversion() {
        val st = uiState.value
        if (st.unitInputText.isBlank() || st.unitOutputText == "—") return
        viewModelScope.launch {
            currencyRepository.recordConversion(
                kind = "UNIT",
                categoryId = st.selectedCategory.id,
                fromCode = st.fromUnit.id,
                toCode = st.toUnit.id,
                inputValue = st.unitInputText,
                outputValue = st.unitOutputText
            )
        }
    }

    fun applyRecentUnitConversion(item: RecentConversionEntity) {
        val cat = UnitCategory.entries.firstOrNull { it.id == item.categoryId } ?: return
        val from = UnitConverterEngine.findUnitById(item.fromCode) ?: return
        val to = UnitConverterEngine.findUnitById(item.toCode) ?: return
        _state.update {
            it.copy(
                activeTab = ConverterTab.UNITS,
                selectedCategory = cat,
                fromUnit = from,
                toUnit = to,
                unitInputText = item.inputValue
            )
        }
    }

    // --- Currency Converter Actions ---
    fun loadCachedAndRefreshCurrency() {
        viewModelScope.launch {
            val cached = currencyRepository.getCachedSnapshot()
            if (cached != null) {
                _state.update { it.copy(ratesSnapshot = cached) }
            }
            refreshExchangeRates()
        }
    }

    fun refreshExchangeRates() {
        if (_state.value.isLoadingRates) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingRates = true, networkError = null) }
            val result = currencyRepository.fetchFreshRates("USD")
            result.onSuccess { snapshot ->
                _state.update {
                    it.copy(
                        ratesSnapshot = snapshot,
                        isLoadingRates = false,
                        networkError = null
                    )
                }
            }.onFailure { err ->
                val cached = currencyRepository.getCachedSnapshot()
                _state.update {
                    it.copy(
                        ratesSnapshot = cached ?: it.ratesSnapshot,
                        isLoadingRates = false,
                        networkError = err.localizedMessage ?: "Unable to fetch live exchange rates"
                    )
                }
            }
        }
    }

    fun selectCurrency(currency: CurrencyInfo, isFrom: Boolean) {
        _state.update {
            it.copy(
                fromCurrency = if (isFrom) currency else it.fromCurrency,
                toCurrency = if (isFrom) it.toCurrency else currency
            )
        }
    }

    fun updateCurrencyInput(text: String) {
        _state.update { it.copy(currencyInputText = text) }
    }

    fun clearCurrencyInput() {
        _state.update { it.copy(currencyInputText = "") }
    }

    fun swapCurrencies() {
        _state.update {
            it.copy(fromCurrency = it.toCurrency, toCurrency = it.fromCurrency)
        }
    }

    fun saveCurrentCurrencyConversion() {
        val st = uiState.value
        if (st.currencyInputText.isBlank() || st.currencyOutputText == "—") return
        viewModelScope.launch {
            currencyRepository.recordConversion(
                kind = "CURRENCY",
                categoryId = "currency",
                fromCode = st.fromCurrency.code,
                toCode = st.toCurrency.code,
                inputValue = st.currencyInputText,
                outputValue = st.currencyOutputText
            )
        }
    }

    fun applyRecentCurrencyConversion(item: RecentConversionEntity) {
        val from = CurrencyRepository.findCurrency(item.fromCode)
        val to = CurrencyRepository.findCurrency(item.toCode)
        _state.update {
            it.copy(
                activeTab = ConverterTab.CURRENCY,
                fromCurrency = from,
                toCurrency = to,
                currencyInputText = item.inputValue
            )
        }
    }

    fun toggleConversionFavorite(item: RecentConversionEntity) {
        viewModelScope.launch {
            currencyRepository.toggleConversionFavorite(item.id, !item.isFavorite)
        }
    }

    fun deleteRecentConversion(id: Long) {
        viewModelScope.launch {
            currencyRepository.deleteConversion(id)
        }
    }

    private fun computeUnitOutput(
        inputText: String,
        from: UnitDefinition,
        to: UnitDefinition,
        cfg: CucalcusSettings
    ): String {
        val cleaned = inputText.trim().replace(",", "").replace("−", "-")
        val valNum = cleaned.toDoubleOrNull() ?: return "—"
        val converted = UnitConverterEngine.convert(valNum, from, to)
        if (converted.isNaN() || converted.isInfinite()) return "—"
        return NumberFormatter.formatResult(converted, cfg.decimalPrecision, cfg.numberFormatMode)
    }

    private fun computeCurrencyOutput(
        inputText: String,
        from: CurrencyInfo,
        to: CurrencyInfo,
        snapshot: ExchangeRatesSnapshot?,
        cfg: CucalcusSettings
    ): Pair<String, String> {
        if (snapshot == null) return "—" to "Rates unavailable — tap Refresh"
        val fromRate = snapshot.rates[from.code] ?: if (from.code == snapshot.baseCode) 1.0 else null
        val toRate = snapshot.rates[to.code] ?: if (to.code == snapshot.baseCode) 1.0 else null
        if (fromRate == null || toRate == null || fromRate == 0.0) {
            return "—" to "Rate unavailable for ${from.code}/${to.code}"
        }
        val unitCrossRate = toRate / fromRate
        val rateDisplay = "1 ${from.code} = ${NumberFormatter.formatResult(unitCrossRate, 4, NumberFormatMode.INTERNATIONAL)} ${to.code}"

        val cleaned = inputText.trim().replace(",", "").replace("−", "-")
        val amount = cleaned.toDoubleOrNull() ?: return "—" to rateDisplay
        val converted = amount * unitCrossRate
        val formatted = NumberFormatter.formatResult(
            converted,
            cfg.decimalPrecision.coerceAtMost(4),
            cfg.numberFormatMode
        )
        return formatted to rateDisplay
    }

    class Factory(
        private val currencyRepository: CurrencyRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConverterViewModel(currencyRepository, settingsRepository) as T
        }
    }
}
