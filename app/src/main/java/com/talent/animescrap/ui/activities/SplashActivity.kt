package com.talent.animescrap.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.talent.animescrap.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set dynamic colors
        val settingsPreferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        settingsPreferenceManager.getBoolean("dynamic_colors", false).also {
            println(it)
            if (it) DynamicColors.applyToActivitiesIfAvailable(application)
        }

        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 400)

    }
}