package com.example.hudsonmcashan.guradianaangel

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_temperature.*
import kotlinx.android.synthetic.main.activity_temperature.view.*

class TemperatureAdjustActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temperature)

        setupActionBar()
        setupSwitches()
    }

    private fun setupActionBar() {
        val actionBar = supportActionBar
        actionBar!!.title = "Adjust Temperature Sensor"
        // Setup back button
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupSwitches() {
        celsiusOrFarenheit_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            celsiusOrFarenheit_status.textView.text = if (isChecked)  "°F" else "°C"
        }
        // TODO: setup switch to turn on/off temperature sensor
    }
}