package com.sunnyweather.android

import WeatherCheckWorker
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.core.app.NotificationCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
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
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class WeatherActivity : AppCompatActivity() {


    private lateinit var binding: ActivityWeatherBinding
    val drawerLayout get() = binding.drawerLayout
    private lateinit var bindingNow: NowBinding
    private lateinit var bindingForecast: ForecastBinding
    private lateinit var bindingLifeIndex: LifeIndexBinding

    val viewModel by lazy { ViewModelProvider(this).get(WeatherViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//透明状态栏
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

        bindingNow.navBtn.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE)
                        as InputMethodManager
                manager.hideSoftInputFromWindow(drawerView.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS)
            }
        })
        Log.d("intent", intent.toString())
        Log.d("IntentCheck", "extra lng = ${intent.getStringExtra("location_lng")}")
        Log.d("IntentCheck", "extra lat = ${intent.getStringExtra("location_lat")}")



        // 其他初始化和数据赋值
        // 1. 无条件覆盖 —— 再也不怕旧值残留
        viewModel.locationLng = intent.getStringExtra("location_lng") ?: ""
        viewModel.locationLat = intent.getStringExtra("location_lat") ?: ""
        viewModel.placeName   = intent.getStringExtra("place_name")  ?: ""

// 2. 如果两项为空，再尝试从 SharedPreferences 兜底
        if (viewModel.locationLng.isEmpty() || viewModel.locationLat.isEmpty()) {
            getSharedPreferences("weather_prefs", MODE_PRIVATE).apply {
                viewModel.locationLng = getString("lng", "") ?: ""
                viewModel.locationLat = getString("lat", "") ?: ""
            }
        }

// 3. 把最新坐标写回 prefs（经度→lng，纬度→lat）
        getSharedPreferences("weather_prefs", MODE_PRIVATE).edit {
            putString("lng", viewModel.locationLng)
            putString("lat", viewModel.locationLat)
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
            binding.swipeRefresh.isRefreshing = false
        })
        binding.swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        refreshWeather()
        binding.swipeRefresh.setOnRefreshListener {
            refreshWeather()
        }
        viewModel.refreshWeather(viewModel.locationLng, viewModel.locationLat)

        val instant = OneTimeWorkRequestBuilder<WeatherCheckWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .addTag("WeatherCheckNow")
            .build()
        WorkManager.getInstance(this)
            .enqueueUniqueWork("WeatherCheckNow", ExistingWorkPolicy.REPLACE, instant)



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
    fun refreshWeather() {
        viewModel.refreshWeather(viewModel.locationLng, viewModel.locationLat)
        binding.swipeRefresh.isRefreshing = true
    }
    private fun debugNotify() {
        val channelId = "debug_channel"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            println("clicked")
            nm.createNotificationChannel(
                NotificationChannel(channelId, "调试通知", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val n = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)   // *必须*有小图标
            .setContentTitle("调试通知")
            .setContentText("如果你能看到这条，说明通知没问题")
            .build()
        nm.notify(2025, n)
    }

}
