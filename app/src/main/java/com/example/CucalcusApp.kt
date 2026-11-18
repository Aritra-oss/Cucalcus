package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.local.CucalcusDatabase
import com.example.data.preferences.SettingsRepository
import com.example.data.remote.CurrencyApiService
import com.example.data.repository.CurrencyRepository
import com.example.data.repository.HistoryRepository
import com.example.platform.BackgroundImageManager
import com.example.platform.CustomIconManager
import com.example.platform.HapticManager
import com.example.platform.LauncherIconManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class CucalcusApp : Application() {

    lateinit var database: CucalcusDatabase
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var historyRepository: HistoryRepository
        private set
    lateinit var currencyRepository: CurrencyRepository
        private set
    lateinit var hapticManager: HapticManager
        private set
    lateinit var launcherIconManager: LauncherIconManager
        private set
    lateinit var backgroundImageManager: BackgroundImageManager
        private set
    lateinit var customIconManager: CustomIconManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            CucalcusDatabase::class.java,
            "cucalcus_db"
        ).build()

        settingsRepository = SettingsRepository(applicationContext)
        historyRepository = HistoryRepository(database.historyDao())

        val okHttp = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://open.er-api.com/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val currencyApi = retrofit.create(CurrencyApiService::class.java)
        currencyRepository = CurrencyRepository(
            api = currencyApi,
            settingsRepository = settingsRepository,
            converterDao = database.converterDao()
        )

        hapticManager = HapticManager(applicationContext)
        launcherIconManager = LauncherIconManager(applicationContext)
        backgroundImageManager = BackgroundImageManager(applicationContext)
        customIconManager = CustomIconManager(applicationContext)
    }
}
