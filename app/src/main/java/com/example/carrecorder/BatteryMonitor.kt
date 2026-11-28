package com.example.carrecorder

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryMonitor(private val ctx: Context) {
    private val threshold = 30

    fun getBatteryPercent(): Int {
        val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return 100
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        return (level * 100f / scale).toInt()
    }

    fun canRecord(): Boolean {
        return getBatteryPercent() >= threshold
    }
}
