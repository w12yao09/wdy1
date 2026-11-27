package com.example.carrecorder

import android.content.Context
import android.content.SharedPreferences

object AppPref {
    private const val NAME = "car_recorder_pref"
    private const val KEY_AUTO_REC_ON_CHARGE = "auto_rec_on_charge"

    private fun getPref(ctx: Context): SharedPreferences {
        return ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    fun setAutoRecOnCharge(ctx: Context, enable: Boolean) {
        getPref(ctx).edit().putBoolean(KEY_AUTO_REC_ON_CHARGE, enable).apply()
    }

    fun getAutoRecOnCharge(ctx: Context): Boolean {
        return getPref(ctx).getBoolean(KEY_AUTO_REC_ON_CHARGE, false)
    }
}
