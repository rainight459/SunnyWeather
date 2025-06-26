package com.sunnyweather.android.ui.weather

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.sunnyweather.android.logic.Repository
import com.sunnyweather.android.logic.model.Location
import com.sunnyweather.android.logic.model.Weather

class WeatherViewModel : ViewModel() {

    private val locationLiveData = MutableLiveData<Location>()

    var locationLng = ""
    var locationLat = ""
    var placeName  = ""

    /** 当 locationLiveData 更新时，自动触发 Repository.refreshWeather 并把结果转接给观察者 */
    val weatherLiveData: LiveData<Result<Weather>> = locationLiveData.switchMap { location ->
        Repository.refreshWeather(location.lng, location.lat)

    }

    /** 外部调用以刷新天气；只需更新经纬度即可驱动整条链路 */
    fun refreshWeather(lng: String, lat: String) {
        locationLiveData.value = Location(lng, lat)
    }
}
