package com.example.carrecorder

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryMonitor(private val ctx: Context) {
    fun getBatteryPercent(): Int {
        val intent: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        intent ?: return -1
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return (level * 100 / scale.toFloat()).toInt()
    }

    fun canRecord(): Boolean {
        val pct = getBatteryPercent()
        return pct >= 30
    }
}
