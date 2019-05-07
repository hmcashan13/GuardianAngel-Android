package com.example.hudsonmcashan.guradianaangel

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ListView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_connection.*

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Setup listView
        val context = this
        val listView = findViewById<ListView>(R.id.settings_listview)
        val settings = arrayListOf<String>(
                "Adjust Temperature Sensor","Change Email","Change Profile Image", "Change Password", "Delete Account"
        )
        listView.adapter = SettingsAdapter(this, settings)
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedSetting = settings[position]
            Toast.makeText(this, "${selectedSetting}", Toast.LENGTH_SHORT).show()
        }

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
}