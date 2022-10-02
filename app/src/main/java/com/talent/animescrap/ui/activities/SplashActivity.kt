package com.talent.animescrap.ui.activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.talent.animescrap.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Set dynamic colors
        val settingsPreferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        settingsPreferenceManager.getBoolean("dynamic_colors", true).also {
            if (it) DynamicColors.applyToActivitiesIfAvailable(application)
        }
        settingsPreferenceManager.getString("dark_mode", "follow_system").also {
            when (it.toString()) {
                "on" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                "off" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                else -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        }
        val intent = Intent(this, MainActivity::class.java)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            startActivity(intent)
            finish()
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(intent)
                finish()
            }, 400)
        }

    }
}