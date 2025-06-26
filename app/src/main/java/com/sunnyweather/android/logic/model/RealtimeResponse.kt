package com.sunnyweather.android.logic.model
//RealtimeResponse.kt
data class RealtimeResponse(val status: String,val result: Result) {
    data class Result(val realtime:Realtime)
    data class Realtime(val temperature: Float, val skycon:String, val air_quality:AirQuality)
    data class  AirQuality(val aqi:AQI)
    data class AQI(val chn: Float)
}