package com.talent.animescrap

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView


class LinksActivity : AppCompatActivity() {

    private lateinit var simpleExoPlayer : SimpleExoPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_links)

        if (intent != null) {

            val linksNamesArray = intent.getStringArrayListExtra("nameOfLinks")
            val linksArray = intent.getStringArrayListExtra("theLinks")
            println(linksNamesArray)
            println(linksArray?.get(0))

            simpleExoPlayer = SimpleExoPlayer.Builder(this).build()

            val playerView = findViewById<PlayerView>(R.id.exoPlayerView)
            playerView.player = simpleExoPlayer

            val mediaItem: MediaItem = MediaItem.fromUri(linksArray?.get(0)?.toString().toString())
            simpleExoPlayer.setMediaItem(mediaItem)
            simpleExoPlayer.prepare()
            simpleExoPlayer.play()

        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        simpleExoPlayer.stop()
        simpleExoPlayer.release()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        simpleExoPlayer.stop()
        simpleExoPlayer.release()
        finish()
    }

    @Suppress("DEPRECATION")
    override fun onStart() {
        super.onStart()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController?.hide(WindowInsets.Type.navigationBars())
        }
        else{
            val decorView: View = window.decorView

            val uiOptions: Int = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            decorView.systemUiVisibility = uiOptions
        }

    }
}