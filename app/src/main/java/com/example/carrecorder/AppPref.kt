package com.example.carrecorder

import android.content.Context
import android.content.SharedPreferences

object AppPref {
    private const val PREF_NAME = "car_recorder_pref"
    private const val KEY_AUTO_CHARGE = "auto_charge_record"

    fun getAutoChargeRecord(ctx: Context): Boolean {
        val sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_AUTO_CHARGE, false)
    }

    fun setAutoChargeRecord(ctx: Context, enable: Boolean) {
        val sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_AUTO_CHARGE, enable).apply()
    }
}
