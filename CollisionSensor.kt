package com.example.carrecorder

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class CollisionSensor(
    ctx: Context,
    private val onCollision: () -> Unit
) : SensorEventListener {
    private val sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastTime = 0L
    private val threshold = 12.0f // 碰撞灵敏度，可以后续在UI配置

    fun start() {
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME)
    }
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val now = System.currentTimeMillis()
        if (now - lastTime < 1500) return // 防抖，1.5s内只触发一次
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val g = Math.sqrt((x*x + y*y + z*z).toDouble()).toFloat()
        if (g > threshold) {
            lastTime = now
            onCollision()
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
