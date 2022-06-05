package com.talent.animescrap.ui.activities

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
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
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.talent.animescrap.R
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy


class PlayerActivity : AppCompatActivity() {

    private lateinit var simpleExoPlayer: SimpleExoPlayer
    private val mCookieManager = CookieManager()

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {

        mCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(mCookieManager)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

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
        val rotate = playerView.findViewById<ImageView>(R.id.rotate)

        // Set Video Name
        val videoNameTextView = playerView.findViewById<TextView>(R.id.videoName)
        videoNameTextView.text = linksNamesArray[0]
        val videoEpTextView = playerView.findViewById<TextView>(R.id.videoEpisode)
        videoEpTextView.text = linksNamesArray[1]

        // Back Button
        playerView.findViewById<ImageView>(R.id.back).apply {
            setOnClickListener {
                onBackPressed()
            }
        }
        val mediaItem: MediaItem = MediaItem.fromUri(linksArray[0])

        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (X11; Linux x86_64; rv:89.0) Gecko/20100101 Firefox/89.0")
            .setDefaultRequestProperties(hashMapOf("Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"))
        val mediaSource: HlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        simpleExoPlayer.setMediaSource(mediaSource)

//        simpleExoPlayer.setMediaItem(mediaItem)
        simpleExoPlayer.prepare()
        simpleExoPlayer.play()
        // For Screen Rotation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        var flag = false
        rotate.setOnClickListener {

            if (flag) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                flag = false
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                flag = true

            }
        }

        // Fullscreen controls
        var clickCount = 0
        btnScale.setOnClickListener {
            val centerTextTimer = object : CountDownTimer(500, 1000) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    centerText.visibility = View.GONE
                }
            }
            when (clickCount) {
                1 -> {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    centerText.visibility = View.VISIBLE
                    centerText.text = getString(R.string.stretched)
                    centerTextTimer.start()
                    btnScale.setImageResource(R.drawable.ic_baseline_fullscreen_24)
                    clickCount=2
                }
                2 -> {
                    if(requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                    }else{
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    }
                    centerText.visibility = View.VISIBLE
                    centerText.text = getString(R.string.height_fit)
                    centerTextTimer.start()
                    btnScale.setImageResource(R.drawable.ic_baseline_height_24)
                    clickCount=3
                }
                else -> {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    centerText.visibility = View.VISIBLE
                    centerText.text = getString(R.string.zoom)
                    centerTextTimer.start()
                    btnScale.setImageResource(R.drawable.ic_baseline_zoom_out_map_24)
                    clickCount=1
                }
            }
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