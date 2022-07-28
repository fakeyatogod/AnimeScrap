package com.talent.animescrap.ui.activities

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.talent.animescrap.R
import com.talent.animescrap.ui.viewmodels.AnimeDetailsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy


class PlayerActivity : AppCompatActivity() {

    private lateinit var simpleExoPlayer: ExoPlayer
    private lateinit var playerView: StyledPlayerView
    private lateinit var qualityBtn: Button
    private val mCookieManager = CookieManager()
    private val animeDetailsViewModel by viewModels<AnimeDetailsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        mCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(mCookieManager)

        // Intent Arguments
        val animeName = intent.getStringExtra("anime_name")
        val animeEpisode = intent.getStringExtra("anime_episode")
        val animeUrl = intent.getStringExtra("anime_url")

        /// Player Views
        playerView = findViewById(R.id.exoPlayerView)
        val btnScale = playerView.findViewById<ImageView>(R.id.btn_fullscreen)
        val centerText = playerView.findViewById<TextView>(R.id.centerText)
        val rotate = playerView.findViewById<ImageView>(R.id.rotate)
        qualityBtn = playerView.findViewById(R.id.quality_selection_btn)

        // Set Video Name
        val videoNameTextView = playerView.findViewById<TextView>(R.id.videoName)
        videoNameTextView.text = animeName
        val videoEpTextView = playerView.findViewById<TextView>(R.id.videoEpisode)
        videoEpTextView.text = animeEpisode

        simpleExoPlayer = ExoPlayer.Builder(this)
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .build()

        playerView.keepScreenOn = true
        playerView.player = simpleExoPlayer


        // Back Button
        playerView.findViewById<ImageView>(R.id.back).apply {
            setOnClickListener {
                onBackPressed()
            }
        }

        val mapOfQualities = mutableMapOf<String, Int>()
        simpleExoPlayer.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                // Update UI using current tracks.
                for (trackGroup in tracks.groups) {
                    // Group level information.
                    if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
                        for (i in 0 until trackGroup.length) {
                            val trackFormat = trackGroup.getTrackFormat(i).height
                            println(trackGroup.getTrackFormat(i))
                            mapOfQualities["${trackFormat}p"] = i
                            qualityBtn.setOnClickListener {
                                showQuality(mapOfQualities, trackGroup)
                            }
                        }
                    }

                }
            }
        })


        CoroutineScope(Dispatchers.IO).launch {
            if (animeUrl != null) {
                animeDetailsViewModel.getStreamLink(animeUrl)
                withContext(Dispatchers.Main) {
                    animeDetailsViewModel.animeStreamLink.observe(this@PlayerActivity) {
                        val mediaItem: MediaItem = MediaItem.fromUri(it)
                        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
                            .setUserAgent("Mozilla/5.0 (X11; Linux x86_64; rv:89.0) Gecko/20100101 Firefox/89.0")
                            .setDefaultRequestProperties(hashMapOf("Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"))
                        val mediaSource: HlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(mediaItem)
                        simpleExoPlayer.setMediaSource(mediaSource)
                        simpleExoPlayer.prepare()
                        simpleExoPlayer.play()
                    }
                }
            } else {
                Toast.makeText(this@PlayerActivity, "No Anime Website Url Found", Toast.LENGTH_LONG)
                    .show()
            }

        }

        // For Screen Rotation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        var flag = true
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
        btnScale.setImageResource(R.drawable.ic_baseline_height_24)
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
                    clickCount = 2
                }
                2 -> {
                    if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                    } else {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    }
                    centerText.visibility = View.VISIBLE
                    centerText.text = getString(R.string.height_fit)
                    centerTextTimer.start()
                    btnScale.setImageResource(R.drawable.ic_baseline_height_24)
                    clickCount = 3
                }
                else -> {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    centerText.visibility = View.VISIBLE
                    centerText.text = getString(R.string.zoom)
                    centerTextTimer.start()
                    btnScale.setImageResource(R.drawable.ic_baseline_zoom_out_map_24)
                    clickCount = 1
                }
            }
        }

    }

    override fun onPause() {
        super.onPause()
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

    override fun onResume() {
        super.onResume()
        hideSystemUi()
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, playerView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showQuality(qualities: MutableMap<String, Int>, trackGroup: Tracks.Group) {
        val bottomSheet = BottomSheetDialog(this@PlayerActivity)
        bottomSheet.setContentView(R.layout.bottom_sheet_layout)

        val list = bottomSheet.findViewById<ListView>(R.id.listView)

        val arr = ArrayAdapter(
            this,
            R.layout.support_simple_spinner_dropdown_item,
            qualities.keys.toList()
        )

        list?.adapter = arr
        bottomSheet.behavior.peekHeight = 600
        bottomSheet.show()

        list?.setOnItemClickListener { _, view, _, _ ->
            val quality = (view as TextView).text.toString()
            val trackIndex = qualities.getValue(quality)
            simpleExoPlayer.trackSelectionParameters = simpleExoPlayer.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(
                    TrackSelectionOverride(
                        trackGroup.mediaTrackGroup,  /* trackIndex= */
                        trackIndex
                    )
                )
                .build()
            qualityBtn.text = quality
            bottomSheet.hide()
        }

    }
}