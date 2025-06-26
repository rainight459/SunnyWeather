package com.sunnyweather.android

import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.sunnyweather.android.databinding.ActivityWeatherBinding
import com.sunnyweather.android.databinding.ForecastBinding
import com.sunnyweather.android.databinding.LifeIndexBinding
import com.sunnyweather.android.databinding.NowBinding
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.logic.model.getSky
import com.sunnyweather.android.ui.weather.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class WeatherActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWeatherBinding
    private lateinit var bindingNow: NowBinding
    private lateinit var bindingForecast: ForecastBinding
    private lateinit var bindingLifeIndex: LifeIndexBinding

    val viewModel by lazy { ViewModelProvider(this).get(WeatherViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val decorView = window.decorView
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化各个子布局的绑定
        bindingNow = binding.nowLayout  // 根布局绑定
        bindingForecast = binding.forecastLayout

        bindingLifeIndex = binding.lifeIndexLayout

        // 其他初始化和数据赋值
        if (viewModel.locationLng.isEmpty()) {
            viewModel.locationLng = intent.getStringExtra("location_lng") ?: ""
        }
        if (viewModel.locationLat.isEmpty()) {
            viewModel.locationLat = intent.getStringExtra("location_lat") ?: ""
        }
        if (viewModel.placeName.isEmpty()) {
            viewModel.placeName = intent.getStringExtra("place_name") ?: ""
        }

        // 观察天气数据的变化
        viewModel.weatherLiveData.observe(this, Observer { result ->
            val weather = result.getOrNull()
            if (weather != null) {
                showWeatherInfo(weather)
            } else {
                Toast.makeText(this, "无法成功获取天气信息", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
        })
        viewModel.refreshWeather(viewModel.locationLng, viewModel.locationLat)
    }

    private fun showWeatherInfo(weather: Weather) {
        // 更新天气信息
       bindingNow.placeName.text = viewModel.placeName
        val realtime = weather.realtime
        val daily = weather.daily

        // 填充now.xml布局中的数据
        val currentTempText = "${realtime.temperature.toInt()} ℃"
        bindingNow.currentTemp.text = currentTempText
        bindingNow.currentSky.text = getSky(realtime.skycon).info
        val currentPM25Text = "空气指数 ${realtime.air_quality.aqi.chn.toInt()}"
        bindingNow.currentAQI.text = currentPM25Text
        bindingNow.nowLayout.setBackgroundResource(getSky(realtime.skycon).bg)

        // 填充forecast.xml布局中的数据
        bindingForecast.forecastLayout.removeAllViews()
        val days = daily.skycon.size
        for (i in 0 until days) {
            val skycon = daily.skycon[i]
            val temperature = daily.temperature[i]
            val view = LayoutInflater.from(this).inflate(R.layout.forecast_item,
                bindingForecast.forecastLayout, false)
            val dateInfo = view.findViewById<TextView>(R.id.dateInfo)
            val skyIcon = view.findViewById<ImageView>(R.id.skyIcon)
            val skyInfo = view.findViewById<TextView>(R.id.skyInfo)
            val temperatureInfo = view.findViewById<TextView>(R.id.temperatureInfo)

            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateInfo.text = skycon.date.take(10)
            val sky = getSky(skycon.value)
            skyIcon.setImageResource(sky.icon)
            skyInfo.text = sky.info
            val tempText = "${temperature.min.toInt()} ~ ${temperature.max.toInt()} ℃"
            temperatureInfo.text = tempText
            bindingForecast.forecastLayout.addView(view)
        }

        // 填充life_index.xml布局中的数据
        val lifeIndex = daily.lifeIndex
        bindingLifeIndex.coldRiskText.text = lifeIndex.coldRisk[0].desc
        bindingLifeIndex.dressingText.text = lifeIndex.dressing[0].desc
        bindingLifeIndex.ultravioletText.text = lifeIndex.ultraviolet[0].desc
        bindingLifeIndex.carWashingText.text = lifeIndex.carWashing[0].desc

        binding.weatherLayout.visibility = View.VISIBLE
    }
}
