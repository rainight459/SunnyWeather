import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sunnyweather.android.R
import com.sunnyweather.android.logic.Repository

class WeatherCheckWorker(appContext: Context, params: WorkerParameters)
    : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        val lng = prefs.getString("lng", "") ?: return Result.failure()
        val lat = prefs.getString("lat", "") ?: return Result.failure()

        val result = Repository.fetchWeatherNow(lng, lat)   // ✅ 新函数
        val weather = result.getOrNull() ?: return Result.retry()  // 网络异常再重试一次

        val sky = weather.daily.skycon.firstOrNull()?.value ?: return Result.success()
        if (sky.contains("RAIN")) {
            sendRainNotification(sky)
        }
        return Result.success()
    }

    private fun sendRainNotification(sky: String) {
        val channelId = "rain_alert"
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "雨天提醒", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val n = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_rain)    // 请确保这张图存在
            .setContentTitle("今天可能下雨 ☔")
            .setContentText("天气：$sky，记得带伞！")
            .setAutoCancel(true)
            .build()
        nm.notify(1001, n)
    }
}
