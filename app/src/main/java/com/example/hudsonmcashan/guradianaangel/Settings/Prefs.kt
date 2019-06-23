package com.example.hudsonmcashan.guradianaangel.Settings

import android.content.Context
import android.content.SharedPreferences

class Prefs (context: Context) {
    private val PREFS_FILENAME = "com.example.hudsonmcashan.guradianaangel"
    private val TEMP_SENSOR_ON_OFF = "tempSensorOnOff"
    private val FAHRENHEIT_CELSIUS = "fahrenheitCelsius"
    private val MAX_TEMP = "maxTemp"

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    var tempSensorOnOff: Boolean
        get() = prefs.getBoolean(TEMP_SENSOR_ON_OFF,true)
        set(value) = prefs.edit().putBoolean(TEMP_SENSOR_ON_OFF, value).apply()

    var fahrenheitCelsius: Boolean
        get() = prefs.getBoolean(FAHRENHEIT_CELSIUS,true)
        set(value) = prefs.edit().putBoolean(FAHRENHEIT_CELSIUS, value).apply()

    var maxTemp: Int
        get() = prefs.getInt(MAX_TEMP,90)
        set(value) = prefs.edit().putInt(MAX_TEMP, value).apply()
}