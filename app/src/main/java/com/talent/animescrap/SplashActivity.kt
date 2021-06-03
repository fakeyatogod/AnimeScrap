package com.talent.animescrap

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.jsoup.Jsoup.connect

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        var isConnected = false
        Thread {
            try {
                val google = connect("https://www.google.com").get()
                isConnected = true
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "No Internet Connection, Connect to the Internet and Restart App",
                        Toast.LENGTH_LONG
                    ).show()
                }
                finish()
            }
            runOnUiThread {
                if (isConnected) {
                    val intent = Intent(this, MainBottomNav::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }.start()


    }
}