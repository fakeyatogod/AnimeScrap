package com.talent.animescrap.ui.activities

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.talent.animescrap.R
import com.talent.animescrap.databinding.ActivityPlayerBinding
import com.talent.animescrap.ui.viewmodels.AnimeStreamViewModel
import com.talent.animescrap.widgets.DoubleTapPlayerView
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy


@UnstableApi @AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var loadingLayout: LinearLayout
    private lateinit var player: ExoPlayer
    private lateinit var playerView: DoubleTapPlayerView
    private lateinit var qualityBtn: Button
    private lateinit var subsToggleButton: ToggleButton
    private lateinit var rotateBtn: ImageView
    private lateinit var scaleBtn: ImageView
    private lateinit var prevEpBtn: ImageView
    private lateinit var nextEpBtn: ImageView
    private lateinit var centerText: TextView
    private lateinit var videoNameTextView: TextView
    private lateinit var videoEpTextView: TextView
    private lateinit var mediaSource: MediaSource
    private lateinit var mediaItem: MediaItem
    private lateinit var bottomSheet: BottomSheetDialog
    private lateinit var mediaSession: MediaSession
    private lateinit var animeEpisodeMap: HashMap<*, *>
    private lateinit var qualityMapUnsorted: MutableMap<String, Int>
    private lateinit var settingsPreferenceManager: SharedPreferences
    private var isPipEnabled: Boolean = true
    private var animeUrl: String? = null
    private var animeSub: String? = null
    private var animeEpisode: String? = null
    private var animeTotalEpisode: String? = null
    private var animeName: String? = null
    private var animeStreamUrl: String? = null
    private var extraHeaders: HashMap<String, String>? = null
    private var isHls: Boolean = true
    private var simpleCache: SimpleCache? = null
    private val mCookieManager = CookieManager()
    private val animeStreamViewModelInPlayer: AnimeStreamViewModel by viewModels()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Accept All Cookies
        mCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(mCookieManager)

        // Prepare PiP
        preparePip()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("LastWatchedPref", MODE_PRIVATE)

        // Arguments
        animeName = intent.getStringExtra("animeName")
        animeEpisode = intent.getStringExtra("animeEpisodeIndex")
        animeTotalEpisode = intent.getStringExtra("animeTotalEpisode")
        animeUrl = intent.getStringExtra("animeUrl")
        animeEpisodeMap = intent.getSerializableExtra("animeEpisodeMap") as HashMap<*, *>

        /// Player Views
        playerView = binding.exoPlayerView
        playerView.doubleTapOverlay = binding.doubleTapOverlay
        loadingLayout = binding.loadingLayout

        // Set Video Name
        videoNameTextView = playerView.findViewById(R.id.videoName)
        videoEpTextView = playerView.findViewById(R.id.videoEpisode)
        videoNameTextView.isSelected = true
        videoNameTextView.text = animeName
        updateEpisodeName()

        // Build ExoPlayer
        player = ExoPlayer.Builder(this)
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .build()

        // Setup Player View
        playerView.keepScreenOn = true
        playerView.player = player
        playerView.subtitleView?.visibility = View.VISIBLE

        // Build MediaSession
        mediaSession = MediaSession.Builder(this, player)
            .build()

        // Prepare Custom Player View Buttons
        prepareButtons()

        // Add Listener for quality selection
        player.addListener(getPlayerListener())

        if (animeUrl != null && animeEpisode != null) {
            loadingLayout.visibility = View.VISIBLE
            playerView.visibility = View.GONE
            animeStreamViewModelInPlayer.setAnimeLink(
                animeUrl!!,
                animeEpisodeMap[animeEpisode!!] as String
            )
            prevEpBtn.setImageViewEnabled(animeEpisode!!.toInt() >= 2)
            nextEpBtn.setImageViewEnabled(animeEpisode!!.toInt() != animeTotalEpisode!!.toInt())
        }

        animeStreamViewModelInPlayer.animeStreamLink.observe(this) { animeStreamLink ->
            if (animeStreamLink.link.isNotBlank()) {
                animeStreamUrl = animeStreamLink.link
                if (animeStreamLink.subsLink.isNotBlank()) animeSub = animeStreamLink.subsLink
                if (!animeStreamLink.extraHeaders.isNullOrEmpty()) extraHeaders =
                    animeStreamLink.extraHeaders
                isHls = animeStreamLink.isHls
                qualityMapUnsorted = mutableMapOf()
                loadingLayout.visibility = View.GONE
                playerView.visibility = View.VISIBLE
                prepareMediaSource()
            } else {
                Toast.makeText(this, "No Streaming Url Found", Toast.LENGTH_SHORT)
                    .show()
            }
        }


    }

    private fun prepareMediaSource() {
        val headerMap = mutableMapOf(
            "Accept" to "*/*",
            "Connection" to "keep-alive",
            "Upgrade-Insecure-Requests" to "1"
        )
        extraHeaders?.forEach { header ->
            headerMap[header.key] = header.value
        }

        println(headerMap)

        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(headerMap)
            .setReadTimeoutMs(20000)
            .setConnectTimeoutMs(20000)

        val databaseProvider = StandaloneDatabaseProvider(this)
        simpleCache?.release()
        simpleCache
        simpleCache = SimpleCache(
            File(cacheDir, "exoplayer").also { it.deleteOnExit() }, // Ensures always fresh file
            LeastRecentlyUsedCacheEvictor(300L * 1024L * 1024L),
            databaseProvider
        )
        val cacheFactory = CacheDataSource.Factory().apply {
            setCache(simpleCache!!)
            setUpstreamDataSourceFactory(dataSourceFactory)
        }
        mediaItem =
            MediaItem.fromUri(animeStreamUrl!!)
        mediaSource = if (isHls) {
            HlsMediaSource.Factory(cacheFactory)
                .setAllowChunklessPreparation(true)
                .setLoadErrorHandlingPolicy(getMyErrorHandlingPolicy())
                .createMediaSource(mediaItem)
        } else {
            ProgressiveMediaSource.Factory(cacheFactory)
                .createMediaSource(mediaItem)
        }

        if (animeSub != null) {
            subsToggleButton.isChecked = true
            subsToggleButton.visibility = View.VISIBLE
            val subStyle = CaptionStyleCompat(
                Color.WHITE,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                Color.BLACK,
                null
            )
            println(animeSub)
            playerView.subtitleView?.setStyle(subStyle)
            val subtitleMediaSource = SingleSampleMediaSource.Factory(dataSourceFactory)
                .createMediaSource(
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(animeSub)).apply {
                        if (animeSub!!.contains("srt")) setMimeType(MimeTypes.APPLICATION_SUBRIP) else setMimeType(MimeTypes.TEXT_VTT)
                        setLanguage("en")
                        setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    }.build(),
                    C.TIME_UNSET
                )
            mediaSource = MergingMediaSource(mediaSource, subtitleMediaSource)
        } else {
            subsToggleButton.isChecked = false
            subsToggleButton.visibility = View.GONE
        }
        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
    }

    private fun getPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) {
                    setNewEpisode()
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                // Update UI using current tracks.
                for (trackGroup in tracks.groups) {
                    // Group level information.
                    if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
                        for (i in 0 until trackGroup.length) {
                            val trackFormat = trackGroup.getTrackFormat(i).height
//                            println(trackGroup.getTrackFormat(i))
//                            println(trackGroup.isTrackSupported(i))
//                            println(trackGroup.isTrackSelected(i))
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
        }
    }

    private fun preparePip() {
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
    }

    @SuppressLint("SetTextI18n")
    fun updateEpisodeName() {
        videoEpTextView.text = "${resources.getString(R.string.episode)} $animeEpisode"
    }

    private fun setNewEpisode(increment: Int = 1) {
        animeEpisode = "${animeEpisode!!.toInt() + increment}"
        println(animeEpisode)
        if (animeEpisode!!.toInt() > animeTotalEpisode!!.toInt() || animeEpisode!!.toInt() < 1)
            onBackPressed()
        else {
            animeStreamViewModelInPlayer.setAnimeLink(
                animeUrl!!,
                animeEpisodeMap[animeEpisode!!] as String
            )
            prevEpBtn.setImageViewEnabled(animeEpisode!!.toInt() >= 2)
            nextEpBtn.setImageViewEnabled(animeEpisode!!.toInt() != animeTotalEpisode!!.toInt())
            player.stop()
            loadingLayout.visibility = View.VISIBLE
            playerView.visibility = View.GONE
            updateEpisodeName()
            qualityMapUnsorted = mutableMapOf()
            // Set Default Auto Text
            qualityBtn.text = resources.getString(R.string.quality_btn_txt)
            sharedPreferences.edit()
                .putString(animeUrl!!, animeEpisode!!).apply()
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    fun prepareButtons() {

        // Custom player views
        scaleBtn = playerView.findViewById(R.id.btn_fullscreen)
        centerText = playerView.findViewById(R.id.centerText)
        rotateBtn = playerView.findViewById(R.id.rotate)
        qualityBtn = playerView.findViewById(R.id.quality_selection_btn)
        prevEpBtn = playerView.findViewById(R.id.prev_ep)
        nextEpBtn = playerView.findViewById(R.id.next_ep)
        subsToggleButton = playerView.findViewById(R.id.subs_toggle_btn)
        
        subsToggleButton.setOnCheckedChangeListener { _,isChecked ->
            if (isChecked) {
                playerView.subtitleView?.visibility = View.VISIBLE
            } else {
                playerView.subtitleView?.visibility = View.GONE
            }
        }

        nextEpBtn.setOnClickListener {
            setNewEpisode(1)
        }
        prevEpBtn.setOnClickListener {
            setNewEpisode(-1)
        }

        // Back Button
        playerView.findViewById<ImageView>(R.id.back).apply {
            setOnClickListener {
                onBackPressed()
            }
        }
        // Fullscreen controls
        var clickCount = 0
        scaleBtn.setImageResource(R.drawable.ic_baseline_height_24)
        scaleBtn.setOnClickListener {
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
                    scaleBtn.setImageResource(R.drawable.ic_baseline_fullscreen_24)
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
                    scaleBtn.setImageResource(R.drawable.ic_baseline_height_24)
                    clickCount = 3
                }
                else -> {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    centerText.visibility = View.VISIBLE
                    centerText.text = getString(R.string.zoom)
                    centerTextTimer.start()
                    scaleBtn.setImageResource(R.drawable.ic_baseline_zoom_out_map_24)
                    clickCount = 1
                }
            }
        }

        // For Screen Rotation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        var flag = true
        rotateBtn.setOnClickListener {
            clickCount = 3
            scaleBtn.setImageResource(R.drawable.ic_baseline_height_24)
            if (flag) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                flag = false
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                flag = true

            }
        }
    }


    private fun showQuality(qualities: MutableMap<String, Int>, trackGroup: Tracks.Group) {

        bottomSheet = BottomSheetDialog(this)
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

    private fun getMyErrorHandlingPolicy(): DefaultLoadErrorHandlingPolicy {
        return object : DefaultLoadErrorHandlingPolicy() {
            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                return 10000
            }
        }
    }

    private fun releasePlayer() {
        releaseCache()
        player.pause()
        player.stop()
        player.release()
        mediaSession.release()
    }

    private fun releaseCache() {
        simpleCache?.release()
        simpleCache = null
    }

    override fun onBackPressed() {
        super.onBackPressed()
        releasePlayer()
        finish()
        startActivity(
            Intent(this@PlayerActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
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

    private fun ImageView.setImageViewEnabled(enabled: Boolean) = if(enabled) {
        drawable.clearColorFilter()
        isEnabled = true
    } else {
        drawable.colorFilter = PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
        isEnabled = false
    }
}
