package com.example.data.repository

import com.example.data.local.ConverterDao
import com.example.data.local.RecentConversionEntity
import com.example.data.local.RecentToolEntity
import com.example.data.preferences.SettingsRepository
import com.example.data.remote.CurrencyApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONObject

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String
)

data class ExchangeRatesSnapshot(
    val baseCode: String,
    val rates: Map<String, Double>,
    val fetchedAtMs: Long,
    val apiUpdatedSec: Long,
    val isCached: Boolean,
    val isStale: Boolean
)

class CurrencyRepository(
    private val api: CurrencyApiService,
    private val settingsRepository: SettingsRepository,
    private val converterDao: ConverterDao
) {
    companion object {
        private const val STALE_THRESHOLD_MS = 6 * 60 * 60 * 1000L // 6 hours

        val supportedCurrencies: List<CurrencyInfo> = listOf(
            CurrencyInfo("INR", "Indian Rupee", "₹", "🇮🇳"),
            CurrencyInfo("USD", "US Dollar", "$", "🇺🇸"),
            CurrencyInfo("EUR", "Euro", "€", "🇪🇺"),
            CurrencyInfo("GBP", "British Pound", "£", "🇬🇧"),
            CurrencyInfo("AED", "UAE Dirham", "د.إ", "🇦🇪"),
            CurrencyInfo("JPY", "Japanese Yen", "¥", "🇯🇵"),
            CurrencyInfo("CNY", "Chinese Yuan", "¥", "🇨🇳"),
            CurrencyInfo("AUD", "Australian Dollar", "A$", "🇦🇺"),
            CurrencyInfo("CAD", "Canadian Dollar", "C$", "🇨🇦"),
            CurrencyInfo("SGD", "Singapore Dollar", "S$", "🇸🇬"),
            CurrencyInfo("CHF", "Swiss Franc", "CHF", "🇨🇭"),
            CurrencyInfo("SAR", "Saudi Riyal", "﷼", "🇸🇦"),
            CurrencyInfo("QAR", "Qatari Riyal", "QR", "🇶🇦"),
            CurrencyInfo("KWD", "Kuwaiti Dinar", "KD", "🇰🇼"),
            CurrencyInfo("BHD", "Bahraini Dinar", "BD", "🇧🇭"),
            CurrencyInfo("OMR", "Omani Rial", "RO", "🇴🇲"),
            CurrencyInfo("HKD", "Hong Kong Dollar", "HK$", "🇭🇰"),
            CurrencyInfo("KRW", "South Korean Won", "₩", "🇰🇷"),
            CurrencyInfo("NZD", "New Zealand Dollar", "NZ$", "🇳🇿"),
            CurrencyInfo("SEK", "Swedish Krona", "kr", "🇸🇪"),
            CurrencyInfo("NOK", "Norwegian Krone", "kr", "🇳🇴"),
            CurrencyInfo("DKK", "Danish Krone", "kr", "🇩🇰"),
            CurrencyInfo("ZAR", "South African Rand", "R", "🇿🇦"),
            CurrencyInfo("BRL", "Brazilian Real", "R$", "🇧🇷"),
            CurrencyInfo("MXN", "Mexican Peso", "$", "🇲🇽"),
            CurrencyInfo("TRY", "Turkish Lira", "₺", "🇹🇷"),
            CurrencyInfo("THB", "Thai Baht", "฿", "🇹🇭"),
            CurrencyInfo("MYR", "Malaysian Ringgit", "RM", "🇲🇾"),
            CurrencyInfo("IDR", "Indonesian Rupiah", "Rp", "🇮🇩"),
            CurrencyInfo("PHP", "Philippine Peso", "₱", "🇵🇭"),
            CurrencyInfo("VND", "Vietnamese Dong", "₫", "🇻🇳"),
            CurrencyInfo("BDT", "Bangladeshi Taka", "৳", "🇧🇩"),
            CurrencyInfo("PKR", "Pakistani Rupee", "₨", "🇵🇰"),
            CurrencyInfo("LKR", "Sri Lankan Rupee", "Rs", "🇱🇰"),
            CurrencyInfo("NPR", "Nepalese Rupee", "रू", "🇳🇵"),
            CurrencyInfo("PLN", "Polish Zloty", "zł", "🇵🇱"),
            CurrencyInfo("RUB", "Russian Ruble", "₽", "🇷🇺"),
            CurrencyInfo("ILS", "Israeli Shekel", "₪", "🇮🇱"),
            CurrencyInfo("EGP", "Egyptian Pound", "E£", "🇪🇬"),
            CurrencyInfo("NGN", "Nigerian Naira", "₦", "🇳🇬")
        )

        fun findCurrency(code: String): CurrencyInfo =
            supportedCurrencies.find { it.code == code }
                ?: CurrencyInfo(code, code, code, "🌐")
    }

    val recentUnitConversions: Flow<List<RecentConversionEntity>> = converterDao.observeConversions("UNIT")
    val recentCurrencyConversions: Flow<List<RecentConversionEntity>> = converterDao.observeConversions("CURRENCY")
    val recentTools: Flow<List<RecentToolEntity>> = converterDao.observeRecentTools()

    suspend fun getCachedSnapshot(): ExchangeRatesSnapshot? {
        val cached = settingsRepository.cachedCurrencyFlow.firstOrNull() ?: return null
        val map = parseRatesJson(cached.ratesJson)
        if (map.isEmpty()) return null
        val age = System.currentTimeMillis() - cached.fetchedAtMs
        return ExchangeRatesSnapshot(
            baseCode = cached.baseCode,
            rates = map,
            fetchedAtMs = cached.fetchedAtMs,
            apiUpdatedSec = cached.apiTimestampSec,
            isCached = true,
            isStale = age > STALE_THRESHOLD_MS
        )
    }

    suspend fun fetchFreshRates(baseCurrency: String = "USD"): Result<ExchangeRatesSnapshot> {
        return try {
            val response = api.getLatestRates(baseCurrency)
            val rates = response.rates
            if (response.result == "success" && !rates.isNullOrEmpty()) {
                val now = System.currentTimeMillis()
                val apiSec = response.timeLastUpdateUnix ?: (now / 1000L)
                val base = response.baseCode ?: baseCurrency
                val json = JSONObject(rates).toString()
                settingsRepository.saveCurrencyCache(base, json, now, apiSec)
                Result.success(
                    ExchangeRatesSnapshot(
                        baseCode = base,
                        rates = rates,
                        fetchedAtMs = now,
                        apiUpdatedSec = apiSec,
                        isCached = false,
                        isStale = false
                    )
                )
            } else {
                Result.failure(IllegalStateException("Exchange rate service returned invalid response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordConversion(
        kind: String,
        categoryId: String,
        fromCode: String,
        toCode: String,
        inputValue: String,
        outputValue: String
    ) {
        val existing = converterDao.findExisting(kind, categoryId, fromCode, toCode)
        converterDao.insert(
            RecentConversionEntity(
                id = existing?.id ?: 0L,
                kind = kind,
                categoryId = categoryId,
                fromCode = fromCode,
                toCode = toCode,
                inputValue = inputValue,
                outputValue = outputValue,
                isFavorite = existing?.isFavorite ?: false,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleConversionFavorite(id: Long, isFavorite: Boolean) {
        converterDao.setFavorite(id, isFavorite)
    }

    suspend fun deleteConversion(id: Long) {
        converterDao.deleteById(id)
    }

    suspend fun recordToolUse(toolId: String) {
        val existing = converterDao.getRecentTool(toolId)
        converterDao.upsertRecentTool(
            RecentToolEntity(
                toolId = toolId,
                lastUsedAt = System.currentTimeMillis(),
                useCount = (existing?.useCount ?: 0) + 1
            )
        )
    }

    private fun parseRatesJson(json: String): Map<String, Double> {
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, Double>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.getDouble(key)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
