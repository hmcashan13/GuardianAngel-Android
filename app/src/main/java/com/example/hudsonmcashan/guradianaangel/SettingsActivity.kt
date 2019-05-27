package com.example.hudsonmcashan.guradianaangel

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ListView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_connection.*
import android.content.Intent
import android.util.Log

val TAG_SETTINGS = "SettingsActivity"
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupActionBar()
        setupListView()
    }

    private fun setupActionBar() {
        val actionBar = supportActionBar
        actionBar!!.title = "Settings"
        // Setup back button
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}