package com.example.hudsonmcashan.guradianaangel

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class InfoDeviceActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_device)

        setupActionBar()
    }

    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.info_device_toolbar))
        val actionBar = supportActionBar
        actionBar!!.title = "Info about Connecting to Cushion"
        // Setup back button
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(R.mipmap.ic_action_close)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}