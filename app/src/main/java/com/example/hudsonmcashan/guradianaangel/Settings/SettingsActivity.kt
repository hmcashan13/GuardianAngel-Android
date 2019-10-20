package com.example.hudsonmcashan.guradianaangel.Settings

import android.os.Bundle
import android.widget.ListView
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.hudsonmcashan.guradianaangel.R

val TAG_SETTINGS = "SettingsActivity"
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupActionBar()
        setupListView()
    }

    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.settings_toolbar))
        val actionBar = supportActionBar
        actionBar!!.title = "Settings"
        // Setup back button
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupListView() {
        val listView = findViewById<ListView>(R.id.settings_listview)
        val settings = arrayListOf("Adjust Temperature Sensor","Adjust GPS", "Reconnect to Cushion","Disconnect from Cushion")
        listView.adapter = SettingsAdapter(this, settings)
        listView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> {
                    val intent = Intent(this, TemperatureAdjustActivity::class.java)
                    startActivity(intent)
                }
                1 -> {
                    val intent = Intent(this, GPSAdjustActivity::class.java)
                    startActivity(intent)
                }
                2 -> {
                    // TODO: reconnect
                }
                3 -> {
                    // TODO: disconnect everything
                }
            }
        }
    }


}