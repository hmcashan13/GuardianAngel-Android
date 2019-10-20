package com.example.hudsonmcashan.guradianaangel.Settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hudsonmcashan.guradianaangel.R

class InfoTempActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_temp)

        setupActionBar()
    }

    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.info_temp_toolbar))
        val actionBar = supportActionBar
        actionBar!!.title = "Info about Adjusting Temperature"
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