package com.example.hudsonmcashan.guradianaangel.Settings

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ListView
import android.content.Intent
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
        val settings = arrayListOf("Adjust Temperature Sensor","Disconnect from Cushion")
        listView.adapter = SettingsAdapter(this, settings)
        listView.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                val intent = Intent(this, TemperatureAdjustActivity::class.java)
                startActivity(intent)
            } else {
                // TODO: disconnect everything
            }
        }
    }


}