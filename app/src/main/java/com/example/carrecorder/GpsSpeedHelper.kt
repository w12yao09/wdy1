package com.example.carrecorder

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

class GpsSpeedHelper(private val ctx: Context) {
    private val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    var currentSpeedKmH: Float = 0f

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentSpeedKmH = location.speed * 3.6f
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    fun start() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, listener)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun stop() {
        locationManager.removeUpdates(listener)
    }
}
