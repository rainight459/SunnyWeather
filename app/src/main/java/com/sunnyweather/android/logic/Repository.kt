package com.sunnyweather.android.logic
//Repository.kt
import android.util.Log
import android.util.Log.e
import androidx.lifecycle.liveData
import com.sunnyweather.android.SunnyWeatherApplication.Companion.TOKEN
import com.sunnyweather.android.logic.dao.PlaceDao
import com.sunnyweather.android.logic.model.Place
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.logic.network.SunnyWeatherNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

object Repository {
    fun searchPlaces(query: String) = fire(Dispatchers.IO) {

        val placeResponse = SunnyWeatherNetwork.searchPlaces(query)
        if (placeResponse.status == "ok") {
            val places = placeResponse.places
            Result.success(places)
        } else {
            Result.failure(RuntimeException("response status is${placeResponse.status}"))
        }
    }



fun refreshWeather(lng: String, lat: String) = fire(Dispatchers.IO) {

    coroutineScope {
        val deferredRealtime = async { SunnyWeatherNetwork.getRealtimeWeather(lng, lat) }
        val deferredDaily = async { SunnyWeatherNetwork.getDailyWeather(lng, lat) }
        val realtimeResponse = deferredRealtime.await()
        val dailyResponse = deferredDaily.await()
        if (realtimeResponse.status == "ok" && dailyResponse.status == "ok") {
            val weather = Weather(realtimeResponse.result.realtime, dailyResponse.result.daily)
            Result.success(weather)


        } else {
            Result.failure(
                RuntimeException(
                    "realtime response status is ${realtimeResponse.status}" +
                            "daily response status is ${dailyResponse.status}"
                )
            )
        }
    }


}
    fun savePlace(place: Place) = PlaceDao.savePlace(place)
    fun getSavedPlace() = PlaceDao.getSavedPlace()
    fun isPlaceSaved() = PlaceDao.isPlaceSaved()

private fun <T> fire(context: CoroutineContext, block: suspend () -> Result<T>) =
    liveData<Result<T>>(context) {
        val result = try {
            block()
        } catch (e: Exception) {
            Result.failure<T>(e)
        }
        emit(result)
    }
    // Repository.kt
    suspend fun fetchWeatherNow(lng: String, lat: String): Result<Weather> = try {
        val realtime = SunnyWeatherNetwork.getRealtimeWeather(lng, lat)
        val daily    = SunnyWeatherNetwork.getDailyWeather(lng, lat)
        if (realtime.status == "ok" && daily.status == "ok") {
            Result.success(Weather(realtime.result.realtime, daily.result.daily))
        } else {
            Result.failure(
                RuntimeException("realtime=${realtime.status}, daily=${daily.status}")
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

}
