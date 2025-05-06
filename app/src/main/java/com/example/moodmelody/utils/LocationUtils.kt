package com.example.moodmelody.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationUtils {

    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? = suspendCancellableCoroutine { continuation ->
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 没有权限
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        // 尝试获取最后已知位置（作为快速方案）
        val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location != null) {
            continuation.resume(Pair(location.latitude, location.longitude))
        } else {
            // 如果没有最后已知位置，可以返回默认值或null
            // 这里我们简单地返回北京的坐标作为示例
            continuation.resume(Pair(39.9042, 116.4074))
        }
    }
}