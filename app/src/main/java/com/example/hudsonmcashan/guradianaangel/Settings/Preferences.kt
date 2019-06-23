package com.example.hudsonmcashan.guradianaangel.Settings

import android.content.Context
import android.content.SharedPreferences

class Prefs (context: Context) {
    val PREFS_FILENAME = "com.example.hudsonmcashan.guradianaangel"
    val TEMP_SENSOR_ON_OFF = "tempSensorOnOff"
    val FARENHEIT_CELSIUS = "farenheitCelsius"
    val MAX_TEMP = "maxTemp"

    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0);

    var tempSensorOnOff: Boolean
        get() = prefs.getBoolean(TEMP_SENSOR_ON_OFF,true)
        set(value) = prefs.edit().putBoolean(TEMP_SENSOR_ON_OFF, value).apply()

    var farenheitCelsius: Boolean
        get() = prefs.getBoolean(FARENHEIT_CELSIUS,true)
        set(value) = prefs.edit().putBoolean(FARENHEIT_CELSIUS, value).apply()

    var maxTemp: Int
        get() = prefs.getInt(MAX_TEMP,90)
        set(value) = prefs.edit().putInt(MAX_TEMP, value).apply()
}