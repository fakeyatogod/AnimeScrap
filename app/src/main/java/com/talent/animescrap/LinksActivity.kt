package com.talent.animescrap

import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView


class LinksActivity : AppCompatActivity() {

    private lateinit var simpleExoPlayer: SimpleExoPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_links)

        val linksNamesArray = intent.getStringArrayListExtra("nameOfLinks") as ArrayList<String>
        val linksArray = intent.getStringArrayListExtra("theLinks") as ArrayList<String>
        println(linksNamesArray)
        println(linksArray[0])

        simpleExoPlayer = SimpleExoPlayer.Builder(this).build()

        val playerView = findViewById<PlayerView>(R.id.exoPlayerView)
        playerView.keepScreenOn = true
        playerView.player = simpleExoPlayer

        val btnScale = playerView.findViewById<ImageView>(R.id.btn_fullscreen)
        val centerText = playerView.findViewById<TextView>(R.id.centerText)

        val mediaItem: MediaItem = MediaItem.fromUri(linksArray[0])
        simpleExoPlayer.setMediaItem(mediaItem)
        simpleExoPlayer.prepare()
        simpleExoPlayer.play()


        var clickCount = 0
        btnScale.setOnClickListener {
            when (clickCount) {
                0 -> {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    centerText.visibility = View.VISIBLE
                    centerText.text = getString(R.string.zoom)
                    object : CountDownTimer(500, 1000) {
                        override fun onTick(millisUntilFinished: Long) {//running functionality for now its no use
                        }

                        override fun onFinish() {
                            centerText.visibility = View.GONE
                        }
                    }.start()
                    btnScale.setImageResource(R.drawable.ic_baseline_zoom_out_map_24)


                }
                1 -> {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    centerText.visibility = View.VISIBLE
                    centerText.text = getString(R.string.fillToScreen)
                    object : CountDownTimer(500, 1000) {
                        override fun onTick(millisUntilFinished: Long) {//running functionality for now its no use
                        }

                        override fun onFinish() {
                            centerText.visibility = View.GONE
                        }
                    }.start()
                    btnScale.setImageResource(R.drawable.ic_baseline_fullscreen_24)
                }
                2 -> {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    centerText.visibility = View.VISIBLE
                    centerText.text = getString(R.string.stretched)
                    object : CountDownTimer(500, 1000) {
                        override fun onTick(millisUntilFinished: Long) {//running functionality for now its no use
                        }

                        override fun onFinish() {
                            centerText.visibility = View.GONE
                        }
                    }.start()
                    btnScale.setImageResource(R.drawable.ic_baseline_open_with_24)

                }
                3 -> {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                    centerText.visibility = View.VISIBLE
                    centerText.text = getString(R.string.hundred)
                    object : CountDownTimer(500, 1000) {
                        override fun onTick(millisUntilFinished: Long) {//running functionality for now its no use
                        }

                        override fun onFinish() {
                            centerText.visibility = View.GONE
                        }
                    }.start()
                    btnScale.setImageResource(R.drawable.ic_baseline_height_24)

                }
                4 -> {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    centerText.visibility = View.VISIBLE
                    centerText.text = getString(R.string.width)
                    object : CountDownTimer(500, 1000) {
                        override fun onTick(millisUntilFinished: Long) {//running functionality for now its no use
                        }

                        override fun onFinish() {
                            centerText.visibility = View.GONE
                        }
                    }.start()
                    btnScale.setImageResource(R.drawable.ic_baseline_switch_video_24)
                }
                else -> {
                    clickCount = -1
                }
            }
            clickCount++
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
        } else {
            val decorView: View = window.decorView

            val uiOptions: Int = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            decorView.systemUiVisibility = uiOptions
        }

    }
}