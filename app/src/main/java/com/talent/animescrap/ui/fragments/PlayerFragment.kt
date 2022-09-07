package com.talent.animescrap.ui.fragments

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.*
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
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.talent.animescrap.R
import com.talent.animescrap.databinding.FragmentPlayerBinding
import com.talent.animescrap.ui.viewmodels.AnimeStreamViewModel
import com.talent.animescrap.widgets.DoubleTapPlayerView
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

@AndroidEntryPoint
class PlayerFragment : Fragment() {
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private val args: PlayerFragmentArgs by navArgs()

    private lateinit var player: ExoPlayer
    private lateinit var playerView: DoubleTapPlayerView
    private lateinit var qualityBtn: Button
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)

        // Accept All Cookies
        mCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(mCookieManager)

        // Prepare PiP
        preparePip()

        // Arguments
        animeName = args.animeName
        animeEpisode = args.animeEpisode
        animeTotalEpisode = args.totalEpisodes
        animeUrl = args.animeUrl

        println("ANIME PLAYER $animeName $animeEpisode $animeUrl")

        if (animeUrl != null && animeEpisode != null) {
            animeStreamViewModelInPlayer.setAnimeLink(
                animeUrl!!,
                animeEpisode!!
            )
        }
        /// Player Views
        playerView = binding.exoPlayerView
        playerView.doubleTapOverlay = binding.doubleTapOverlay

        // Set Video Name
        videoNameTextView = playerView.findViewById(R.id.videoName)
        videoEpTextView = playerView.findViewById(R.id.videoEpisode)
        videoNameTextView.isSelected = true
        videoNameTextView.text = animeName
        videoEpTextView.text = animeEpisode

        // Build ExoPlayer
        player = ExoPlayer.Builder(requireContext())
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .build()

        // Setup Player View
        playerView.keepScreenOn = true
        playerView.player = player
        playerView.subtitleView?.visibility = View.VISIBLE

        // Build MediaSession
        mediaSession = MediaSession.Builder(requireContext(), player)
            .build()

        // Prepare Custom Player View Buttons
        prepareButtons()

        // Add Listener for quality selection
        player.addListener(getPlayerListener())

        animeStreamViewModelInPlayer.animeStreamLink.observe(viewLifecycleOwner) { animeStreamLink ->
            if (animeStreamLink.link.isNotBlank()) {
                animeStreamUrl = animeStreamLink.link
                if (animeStreamLink.subsLink.isNotBlank()) animeSub = animeStreamLink.subsLink
                if (!animeStreamLink.extraHeaders.isNullOrEmpty()) extraHeaders =
                    animeStreamLink.extraHeaders
                isHls = animeStreamLink.isHls
                prepareMediaSource()
            } else {
                Toast.makeText(requireContext(), "No Streaming Url Found", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        return binding.root
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

        val databaseProvider = StandaloneDatabaseProvider(requireContext())
        simpleCache?.release()
        simpleCache
        simpleCache = SimpleCache(
            File(
                activity?.cacheDir,
                "exoplayer"
            ).also { it.deleteOnExit() }, // Ensures always fresh file
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
            val subStyle = CaptionStyleCompat(
                Color.WHITE,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                Color.BLACK,
                null
            )
            playerView.subtitleView?.setStyle(subStyle)
            val subtitleMediaSource = SingleSampleMediaSource.Factory(dataSourceFactory)
                .createMediaSource(
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(animeSub))
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLanguage("en")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build(),
                    C.TIME_UNSET
                )
            mediaSource = MergingMediaSource(mediaSource, subtitleMediaSource)
        }
        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
    }

    private fun getPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                val qualityMapUnsorted = mutableMapOf<String, Int>()
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

                        // Set Default Auto Text
                        qualityBtn.text = resources.getString(R.string.quality_btn_txt)

                        qualityBtn.setOnClickListener {
                            showQuality(qualityMapSorted, trackGroup)
                        }
                    }

                }
            }
        }
    }

    private fun preparePip() {
        settingsPreferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
        isPipEnabled = settingsPreferenceManager.getBoolean("pip", true)

        println(isPipEnabled)
        if (isPipEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity?.setPictureInPictureParams(
                    PictureInPictureParams.Builder()
                        .setAutoEnterEnabled(true)
                        .build()
                )
            }
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
        // Back Button
        playerView.findViewById<ImageView>(R.id.back).apply {
            setOnClickListener {
                activity?.onBackPressed()
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
                    if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
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
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        var flag = true
        rotateBtn.setOnClickListener {
            clickCount = 3
            scaleBtn.setImageResource(R.drawable.ic_baseline_height_24)
            if (flag) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                flag = false
            } else {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                flag = true

            }
        }
    }


    private fun showQuality(qualities: MutableMap<String, Int>, trackGroup: Tracks.Group) {

        bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.setContentView(R.layout.bottom_sheet_layout)

        val list = bottomSheet.findViewById<ListView>(R.id.listView)

        val arr = ArrayAdapter(
            requireContext(),
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

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode)
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


    private fun releasePlayer() {
        releaseCache()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        showSystemUi()
        player.pause()
        player.stop()
        player.release()
        mediaSession.release()
    }

    private fun releaseCache() {
        simpleCache?.release()
        simpleCache = null
    }

    override fun onDetach() {
        super.onDetach()
        releasePlayer()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
        _binding = null
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        WindowInsetsControllerCompat(requireActivity().window, playerView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        WindowCompat.getInsetsController(requireActivity().window, playerView)
            .show(WindowInsetsCompat.Type.systemBars())
    }
}