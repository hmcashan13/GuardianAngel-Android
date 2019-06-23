package com.example.hudsonmcashan.guradianaangel.Settings

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import com.example.hudsonmcashan.guradianaangel.InfoDeviceActivity
import com.example.hudsonmcashan.guradianaangel.R
import kotlinx.android.synthetic.main.activity_temperature.*

class TemperatureAdjustActivity : AppCompatActivity() {
    private var prefs: Prefs? = null
    private var tempSensor: Boolean? = null
    private var fahrenheitCelsius: Boolean? = null
    private var maxTemp: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temperature)

        setupPrefs()
        setupActionBar()
        setupTemperatureSensorOnOffSwitch()
        setupFahrenheitCelsiusSwitch()
        setupMaxTemperatureSlider()
    }

    private fun setupPrefs() {
        prefs = Prefs(this)
    }

    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.temp_toolbar))
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

    // Sets up the action bar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.device_action_bar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // Handles action bar events
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_info -> {
            // launch the settings activity
            val intent = Intent(this, InfoTempActivity::class.java)
            startActivity(intent)
            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    private fun setupTemperatureSensorOnOffSwitch() {
        // Grab saved tempSensorOnOff setting
        tempSensor
        when(tempSensor) {
            true -> tempSensorOnOff_status.text = getString(R.string.on)
            false -> tempSensorOnOff_status.text = getString(R.string.off)
        }
        tempSensorOnOff_switch.setOnCheckedChangeListener { _, isChecked ->
            val message = if (isChecked)  getString(R.string.on) else getString(R.string.off)
            // Persist tempSensorOnOff setting
            prefs!!.tempSensorOnOff = isChecked
            tempSensorOnOff_status.text = message
            Toast.makeText(this@TemperatureAdjustActivity, "Temperature Sensor is now $message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFahrenheitCelsiusSwitch() {
        // Grab saved fahrenheitCelsius setting
        fahrenheitCelsius = prefs!!.fahrenheitCelsius
        when(fahrenheitCelsius) {
            true -> fahrenheitCelsius_status.text = "°F"
            false -> fahrenheitCelsius_status.text = "°C"
        }
        fahrenheitCelsius_switch.setOnCheckedChangeListener { _, isChecked ->
            fahrenheitCelsius_status.text = if (isChecked)  "°F" else "°C"
            // Persist fahrenheitCelsius setting
            prefs!!.fahrenheitCelsius = isChecked
            if (isChecked) {
                Toast.makeText(this@TemperatureAdjustActivity, "Murica!!!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@TemperatureAdjustActivity, "You are now using the metric system", Toast.LENGTH_SHORT).show()
            }


        }
    }

    private fun setupMaxTemperatureSlider() {
        // Grab saved max temp setting
        maxTemp = prefs!!.maxTemp
        if (fahrenheitCelsius != null && fahrenheitCelsius as Boolean) {
            maxTemp_status.text = "${maxTemp}°F"
        } else {
            val celsiusTemp = (maxTemp!! - 32) * 5/9
            maxTemp_status.text = "${celsiusTemp}°C"
        }

        maxTemp_slider?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val newMaxTemp = seekBar.progress + 80
                if (fahrenheitCelsius != null && fahrenheitCelsius as Boolean) {
                    maxTemp_status.text = "${newMaxTemp}°F"
                } else {
                    val celsiusTemp = (newMaxTemp - 32) * 5/9
                    maxTemp_status.text = "${celsiusTemp}°C"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val newMaxTemp = seekBar.progress + 80
                // Persist max temp setting
                prefs!!.maxTemp = newMaxTemp
                if (fahrenheitCelsius != null && fahrenheitCelsius as Boolean) {
                    Toast.makeText(this@TemperatureAdjustActivity, "Max Temperature is now $newMaxTemp°F", Toast.LENGTH_SHORT).show()
                } else {
                    val celsiusTemp = (newMaxTemp - 32) * 5/9
                    Toast.makeText(this@TemperatureAdjustActivity, "Max Temperature is now $celsiusTemp°C", Toast.LENGTH_SHORT).show()
                }

            }
        })

    }

}


