package com.sunnyweather.android

import WeatherCheckWorker
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.*
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class SunnyWeatherApplication : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        const val TOKEN = "90DzF4909TW4AkDG"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        val now = LocalDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
        val delayMillis = Duration.between(now, nextMidnight).toMillis()

        val workRequest = PeriodicWorkRequestBuilder<WeatherCheckWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyWeatherCheck",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
