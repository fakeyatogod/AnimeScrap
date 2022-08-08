package com.talent.animescrap.ui.activities

import android.app.PictureInPictureParams
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.*
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.talent.animescrap.R
import com.talent.animescrap.widgets.DoubleTapOverlay
import com.talent.animescrap.widgets.DoubleTapPlayerView
import dagger.hilt.android.AndroidEntryPoint
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: DoubleTapPlayerView
    private lateinit var qualityBtn: Button
    private lateinit var mediaSource: HlsMediaSource
    private lateinit var bottomSheet: BottomSheetDialog
    private lateinit var settingsPreferenceManager: SharedPreferences
    private var isPipEnabled: Boolean = true
    private val mCookieManager = CookieManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        bottomSheet = BottomSheetDialog(this@PlayerActivity)
        bottomSheet.setContentView(R.layout.bottom_sheet_layout)

        settingsPreferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        isPipEnabled = settingsPreferenceManager.getBoolean("pip", true)

        println(isPipEnabled)
        if (isPipEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setPictureInPictureParams(
                    PictureInPictureParams.Builder()
                        .setAutoEnterEnabled(true)
                        .build()
                )
            }
        }

        mCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(mCookieManager)

        // Intent Arguments
        val animeName = intent.getStringExtra("anime_name")
        val animeEpisode = intent.getStringExtra("anime_episode")
        val animeUrl = intent.getStringExtra("anime_url")

        /// Player Views
        playerView = findViewById(R.id.exoPlayerView)
        val doubleTapOverlay = findViewById<DoubleTapOverlay>(R.id.double_tap_overlay)
        playerView.doubleTapOverlay = doubleTapOverlay

        val btnScale = playerView.findViewById<ImageView>(R.id.btn_fullscreen)
        val centerText = playerView.findViewById<TextView>(R.id.centerText)
        val rotate = playerView.findViewById<ImageView>(R.id.rotate)
        qualityBtn = playerView.findViewById(R.id.quality_selection_btn)

        // Set Video Name
        val videoNameTextView = playerView.findViewById<TextView>(R.id.videoName)
        videoNameTextView.isSelected = true
        videoNameTextView.text = animeName
        val videoEpTextView = playerView.findViewById<TextView>(R.id.videoEpisode)
        videoEpTextView.text = animeEpisode

        player = ExoPlayer.Builder(this)
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .build()

        playerView.keepScreenOn = true
        playerView.player = player


        // Back Button
        playerView.findViewById<ImageView>(R.id.back).apply {
            setOnClickListener {
                onBackPressed()
            }
        }

        val qualityMapUnsorted = mutableMapOf<String, Int>()
        player.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                // Update UI using current tracks.
                for (trackGroup in tracks.groups) {
                    // Group level information.
                    if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
                        for (i in 0 until trackGroup.length) {
                            val trackFormat = trackGroup.getTrackFormat(i).height
                            println(trackGroup.getTrackFormat(i))
                            println(trackGroup.isTrackSupported(i))
                            println(trackGroup.isTrackSelected(i))
                            if (trackGroup.isTrackSupported(i) && trackGroup.isTrackSelected(i)) {
                                qualityMapUnsorted["${trackFormat}p"] = i
                            }
                        }
                        val qualityMapSorted = mutableMapOf<String, Int>()
                        qualityMapUnsorted.entries.sortedBy { it.key.replace("p", "").toInt() }
                            .reversed().forEach { qualityMapSorted[it.key] = it.value }
                        qualityBtn.setOnClickListener {
                            showQuality(qualityMapSorted, trackGroup)
                        }
                    }

                }
            }
        })

        if (animeUrl != null) {

            val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (X11; Linux x86_64; rv:89.0) Gecko/20100101 Firefox/89.0")
                .setDefaultRequestProperties(hashMapOf("Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"))
            mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(animeUrl))
            player.setMediaSource(mediaSource)
            player.prepare()
            player.play()


        } else {
            Toast.makeText(this@PlayerActivity, "No Anime Website Url Found", Toast.LENGTH_LONG)
                .show()
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

    override fun onBackPressed() {
        super.onBackPressed()
        player.stop()
        player.release()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.stop()
        player.release()
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
            val trackParams = player.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(
                    TrackSelectionOverride(trackGroup.mediaTrackGroup, trackIndex)
                )
                .build()

            player.trackSelectionParameters = trackParams

            qualityBtn.text = quality
            bottomSheet.dismiss()
        }

    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        }
        val totalLayout = playerView.findViewById<RelativeLayout>(R.id.totalLayout)
        if (isInPictureInPictureMode) {
            // Hide the full-screen UI (controls, etc.) while in picture-in-picture mode.
            totalLayout.visibility = View.GONE
        } else {
            // Restore the full-screen UI.
            totalLayout.visibility = View.VISIBLE
        }
    }

    override fun onUserLeaveHint() {

        if (isPipEnabled) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    enterPictureInPictureMode(
                        PictureInPictureParams.Builder()
                            .build()
                    )
                }
            }
        }
    }
}