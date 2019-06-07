package com.example.hudsonmcashan.guradianaangel

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_temperature.*

class TemperatureAdjustActivity : AppCompatActivity() {
    var prefs: Prefs? = null
    var tempSensor: Boolean? = null
    var farenheitCelsius: Boolean? = null
    var maxTemp: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temperature)

        setupActionBar()
        setupPrefs()
        setupTemperatureSensorOnOffSwitch()
        setupFarenheitCelsiusSwitch()
        setupMaxTemperatureSlider()
    }

    private fun setupActionBar() {
        val actionBar = supportActionBar
        actionBar!!.title = "Adjust Temperature Sensor"
        // Setup back button
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
    }

    // Sets up the back button
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupPrefs() {
        prefs = Prefs(this)
    }

    private fun setupTemperatureSensorOnOffSwitch() {
        tempSensor = prefs?.tempSensorOnOff ?: true
        when(tempSensor) {
            true -> tempSensorOnOff_status.text = "On"
            false -> tempSensorOnOff_status.text = "Off"
        }
        tempSensorOnOff_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            val message = if (isChecked)  "On" else "Off"
            tempSensorOnOff_status.text = message
            Toast.makeText(this@TemperatureAdjustActivity, "Temperature Sensor is now $message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFarenheitCelsiusSwitch() {
        farenheitCelsius = prefs?.farenheitCelsius ?: true
        when(farenheitCelsius) {
            true -> farenheitCelsius_status.text = "°F"
            false -> farenheitCelsius_status.text = "°C"
        }
        farenheitCelsius_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            farenheitCelsius_status.text = if (isChecked)  "°F" else "°C"
            if (isChecked) {
                Toast.makeText(this@TemperatureAdjustActivity, "Murica!!!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@TemperatureAdjustActivity, "You are now using the metric system", Toast.LENGTH_SHORT).show()
            }


        }
    }

    private fun setupMaxTemperatureSlider() {
        maxTemp = prefs?.maxTemp ?: 90
        if (farenheitCelsius != null && farenheitCelsius as Boolean) {
            maxTemp_status.text = "${maxTemp}°F"
        } else {
            val celsiusTemp = (maxTemp!! - 32) * 5/9
            maxTemp_status.text = "${celsiusTemp}°C"
        }

        maxTemp_slider?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Write code to perform some action when progress is changed.
                val newMaxTemp = seekBar.progress + 80
                if (farenheitCelsius != null && farenheitCelsius as Boolean) {
                    maxTemp_status.text = "${newMaxTemp}°F"
                } else {
                    val celsiusTemp = (newMaxTemp - 32) * 5/9
                    maxTemp_status.text = "${celsiusTemp}°C"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Write code to perform some action when touch is started.
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Write code to perform some action when touch is stopped.
                val newMaxTemp = seekBar.progress + 80
                prefs?.maxTemp = newMaxTemp
                if (farenheitCelsius != null && farenheitCelsius as Boolean) {
                    Toast.makeText(this@TemperatureAdjustActivity, "Max Temperature is now $newMaxTemp°F", Toast.LENGTH_SHORT).show()
                } else {
                    val celsiusTemp = (newMaxTemp - 32) * 5/9
                    Toast.makeText(this@TemperatureAdjustActivity, "Max Temperature is now $celsiusTemp°C", Toast.LENGTH_SHORT).show()
                }

            }
        })

    }

}


