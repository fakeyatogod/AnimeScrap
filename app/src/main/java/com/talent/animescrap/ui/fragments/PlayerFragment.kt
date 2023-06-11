package com.talent.animescrap.ui.fragments

import android.app.UiModeManager
import android.content.Context
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.talent.animescrap.R
import com.talent.animescrap.databinding.FragmentPlayerBinding
import com.talent.animescrap.model.AnimePlayingDetails
import com.talent.animescrap.ui.viewmodels.AnimeStreamViewModel
import com.talent.animescrap.ui.viewmodels.PlayerViewModel
import com.talent.animescrap.widgets.DoubleTapPlayerView
import dagger.hilt.android.AndroidEntryPoint
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

@AndroidEntryPoint
class PlayerFragment : Fragment() {
    private lateinit var animePlayingDetails: AnimePlayingDetails
    private var isInit: Boolean = false
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private val playerViewModel: PlayerViewModel by viewModels()

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var loadingLayout: LinearLayout
    private lateinit var playerView: DoubleTapPlayerView
    private lateinit var qualityBtn: Button
    private lateinit var subsToggleButton: ToggleButton
    private lateinit var rotateBtn: ImageView
    private lateinit var scaleBtn: ImageView
    private lateinit var prevEpBtn: ImageView
    private lateinit var nextEpBtn: ImageView
    private lateinit var centerText: TextView
    private lateinit var videoNameTextView: TextView
    private lateinit var videoSpeedTextView: TextView
    private lateinit var videoEpTextView: TextView
    private lateinit var mediaSource: MediaSource
    private lateinit var mediaItem: MediaItem
    private lateinit var bottomSheet: BottomSheetDialog

    private lateinit var settingsPreferenceManager: SharedPreferences

    private var animeSub: String? = null
    private var animeStreamUrl: String? = null
    private var extraHeaders: HashMap<String, String>? = null
    private var isHls: Boolean = true
    private var isTV: Boolean = false
    private var isVideoCacheEnabled: Boolean = true
    private var isAutoPlayEnabled: Boolean = true
    private val mCookieManager = CookieManager()
    private val animeStreamViewModelInPlayer: AnimeStreamViewModel by viewModels()
    private val args: PlayerFragmentArgs by navArgs()
    private var vidSpeed = 1.00f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)

        // Accept All Cookies
        mCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(mCookieManager)

        // Check TV
        val uiModeManager =
            requireActivity().getSystemService(AppCompatActivity.UI_MODE_SERVICE) as UiModeManager
        isTV = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        // Back Pressed
//        onBackPressedDispatcher.addCallback(activity, callback)

        // Settings
        settingsPreferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Video Cache
        isVideoCacheEnabled = settingsPreferenceManager.getBoolean("video_cache", true)

        // Autoplay pref
        isAutoPlayEnabled = settingsPreferenceManager.getBoolean("auto_play", true)
        // Prepare PiP
//        preparePip()

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(
            "LastWatchedPref",
            AppCompatActivity.MODE_PRIVATE
        )

        // Arguments
        animePlayingDetails = savedInstanceState?.getParcelable("animePlayingDetails") ?: args.animePlayingDetails!!
        isInit = savedInstanceState?.getBoolean("init") ?: false
        println("inininit = $isInit")
        /// Player Views
        playerView = binding.exoPlayerView
        playerView.doubleTapOverlay = binding.doubleTapOverlay
        loadingLayout = binding.loadingLayout

        // Set Video Name
        videoNameTextView = playerView.findViewById(R.id.videoName)
        videoEpTextView = playerView.findViewById(R.id.videoEpisode)
        videoSpeedTextView = playerView.findViewById(R.id.video_speed)
        videoNameTextView.isSelected = true
        videoNameTextView.text = animePlayingDetails.animeName
        updateEpisodeName()

        // Build ExoPlayer - now in Video Module

        // Setup Player View
        playerView.keepScreenOn = true
        playerView.player = playerViewModel.player
        playerView.subtitleView?.visibility = View.VISIBLE

        // 10 sec increment when seeking in TV - d-pad scenarios
        playerView.findViewById<DefaultTimeBar>(R.id.exo_progress).setKeyTimeIncrement(10000)

        // Prepare Custom Player View Buttons
        prepareButtons()

        // Add Listener for quality selection
//        playerViewModel.player.addListener(getPlayerListener())

        if (!isInit) {
            loadingLayout.visibility = View.VISIBLE
            playerView.visibility = View.GONE
            animeStreamViewModelInPlayer.setAnimeLink(
                animePlayingDetails.animeUrl,
                animePlayingDetails.animeEpisodeMap[animePlayingDetails.animeEpisodeIndex] as String,
                listOf(animePlayingDetails.epType)
            )
            prevEpBtn.setImageViewEnabled(animePlayingDetails.animeEpisodeIndex.toInt() >= 2)
            nextEpBtn.setImageViewEnabled(animePlayingDetails.animeEpisodeIndex.toInt() != animePlayingDetails.animeTotalEpisode.toInt())
        }

        animeStreamViewModelInPlayer.animeStreamLink.observe(viewLifecycleOwner) { animeStreamLink ->
            if (animeStreamLink.link.isNotBlank()) {
                animeStreamUrl = animeStreamLink.link
                if (animeStreamLink.subsLink.isNotBlank()) animeSub = animeStreamLink.subsLink
                if (!animeStreamLink.extraHeaders.isNullOrEmpty()) extraHeaders =
                    animeStreamLink.extraHeaders
                isHls = animeStreamLink.isHls
//                qualityMapUnsorted = mutableMapOf()
                loadingLayout.visibility = View.GONE
                playerView.visibility = View.VISIBLE
                prepareMediaSource()
            } else {
                Toast.makeText(requireContext(), "No streaming URL found", Toast.LENGTH_SHORT)
                    .show()
                backPressed()
            }
        }
        isInit = true
        return binding.root

    }

    private fun prepareMediaSource(force: Boolean = false) {
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

        if (isVideoCacheEnabled) {

            val cacheFactory = CacheDataSource.Factory().apply {
                setCache(playerViewModel.simpleCache!!)
                setUpstreamDataSourceFactory(dataSourceFactory)
            }
            mediaItem =
                MediaItem.fromUri(animeStreamUrl!!)
            mediaSource = if (isHls) {
                HlsMediaSource.Factory(cacheFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            } else {
                ProgressiveMediaSource.Factory(cacheFactory)
                    .createMediaSource(mediaItem)
            }
        } else {
            mediaItem =
                MediaItem.fromUri(animeStreamUrl!!)
            mediaSource = if (isHls) {
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            } else {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
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
                        if (animeSub!!.contains("srt")) setMimeType(MimeTypes.APPLICATION_SUBRIP) else setMimeType(
                            MimeTypes.TEXT_VTT
                        )
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
        playerViewModel.setMediaSource(mediaSource)

    }

    private fun getPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)

                // Keep screen on only when playing
                playerView.keepScreenOn = isPlaying

//                if (isPipEnabled && !isTV) {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                        setPictureInPictureParams(
//                            PictureInPictureParams.Builder()
//                                .setAutoEnterEnabled(isPlaying)
//                                .build()
//                        )
//                    }
//                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED && isAutoPlayEnabled) {
                    setNewEpisode()
                }
            }

        }
    }

    private fun updateEpisodeName() {
        videoEpTextView.text = resources.getString(R.string.episode, animePlayingDetails.animeEpisodeIndex)
    }

    private fun setNewEpisode(increment: Int = 1) {
        animePlayingDetails.animeEpisodeIndex = "${animePlayingDetails.animeEpisodeIndex.toInt() + increment}"
        println(animePlayingDetails.animeEpisodeIndex)
        if (animePlayingDetails.animeEpisodeIndex.toInt() > animePlayingDetails.animeTotalEpisode.toInt() || animePlayingDetails.animeEpisodeIndex.toInt() < 1)
            backPressed()
        else {
            animeStreamViewModelInPlayer.setAnimeLink(
                animePlayingDetails.animeUrl,
                animePlayingDetails.animeEpisodeMap[animePlayingDetails.animeEpisodeIndex] as String,
                listOf(animePlayingDetails.epType)
            )
            prevEpBtn.setImageViewEnabled(animePlayingDetails.animeEpisodeIndex.toInt() >= 2)
            nextEpBtn.setImageViewEnabled(animePlayingDetails.animeEpisodeIndex.toInt() != animePlayingDetails.animeTotalEpisode.toInt())
            playerViewModel.player.stop()
            loadingLayout.visibility = View.VISIBLE
            playerView.visibility = View.GONE
            updateEpisodeName()
//            qualityMapUnsorted = mutableMapOf()
            // Set Default Auto Text
            qualityBtn.text = resources.getString(R.string.quality_btn_txt)
            sharedPreferences.edit()
                .putString(animePlayingDetails.animeUrl, animePlayingDetails.animeEpisodeIndex).apply()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("init",isInit)
        outState.putParcelable("animePlayingDetails", animePlayingDetails)
        super.onSaveInstanceState(outState)
    }
    private fun prepareButtons() {

        // Custom player views
        scaleBtn = playerView.findViewById(R.id.btn_fullscreen)
        centerText = playerView.findViewById(R.id.centerText)
        rotateBtn = playerView.findViewById(R.id.rotate)
        qualityBtn = playerView.findViewById(R.id.quality_selection_btn)
        prevEpBtn = playerView.findViewById(R.id.prev_ep)
        nextEpBtn = playerView.findViewById(R.id.next_ep)
        subsToggleButton = playerView.findViewById(R.id.subs_toggle_btn)

        qualityBtn.setOnClickListener {
            showQuality()
        }
        subsToggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                playerView.subtitleView?.visibility = View.VISIBLE
            } else {
                playerView.subtitleView?.visibility = View.GONE
            }
        }

        videoSpeedTextView.setOnClickListener {
            changeVideoSpeed()
        }
        videoSpeedTextView.text = resources.getString(R.string.speed, "$vidSpeed")

        nextEpBtn.setOnClickListener {
            setNewEpisode(1)
        }
        prevEpBtn.setOnClickListener {
            setNewEpisode(-1)
        }

        // Back Button
        playerView.findViewById<ImageView>(R.id.back).apply {
            setOnClickListener {
                backPressed()
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
                    if (requireActivity().requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
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

        if (isTV) {
            rotateBtn.isVisible = false
            rotateBtn.isFocusable = false
            rotateBtn.isActivated = false
        }
    }

    private fun changeVideoSpeed() {
        vidSpeed += 0.25f
        if (vidSpeed > 2) vidSpeed = 0.25f
        videoSpeedTextView.text = resources.getString(R.string.speed, "$vidSpeed")
        playerViewModel.player.playbackParameters = PlaybackParameters(vidSpeed)
    }

    private fun showQuality() {

        bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.setContentView(R.layout.bottom_sheet_layout)

        val list = bottomSheet.findViewById<ListView>(R.id.listView)

        val arr = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            playerViewModel.qualityMapSorted.keys.toList()
        )

        list?.adapter = arr
        bottomSheet.behavior.peekHeight = 600
        bottomSheet.show()

        list?.setOnItemClickListener { _, view, _, _ ->
            val quality = (view as TextView).text.toString()
            val trackIndex = playerViewModel.qualityMapSorted.getValue(quality)
            val trackParams = playerViewModel.player.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(
                    TrackSelectionOverride(playerViewModel.qualityTrackGroup!!.mediaTrackGroup, trackIndex)
                )
                .build()

            playerViewModel.player.trackSelectionParameters = trackParams

            qualityBtn.text = quality
            bottomSheet.dismiss()
        }

    }


    private fun backPressed() {
        callback.handleOnBackPressed()
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        hideSystemUi()
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        WindowInsetsControllerCompat(requireActivity().window, requireActivity().window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    private fun hideSystemUi() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        WindowInsetsControllerCompat(
            requireActivity().window,
            requireActivity().window.decorView
        ).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }


    private fun ImageView.setImageViewEnabled(enabled: Boolean) = if (enabled) {
        drawable.clearColorFilter()
        isEnabled = true
        isFocusable = true
    } else {
        drawable.colorFilter = PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
        isEnabled = false
        isFocusable = false
    }


    // PIP
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

}
