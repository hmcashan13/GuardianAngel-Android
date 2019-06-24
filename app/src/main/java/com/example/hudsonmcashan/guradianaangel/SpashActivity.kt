package com.example.hudsonmcashan.guradianaangel

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import com.example.hudsonmcashan.guradianaangel.Settings.SettingsActivity

class SplashActivity : AppCompatActivity() {
    private val splashTimeout = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)
        Handler().postDelayed({ launchApp() }, splashTimeout)
    }

    private fun launchApp() {
        val intent = Intent(this, DeviceActivity::class.java)
        startActivity(intent)
        finish()
    }
}