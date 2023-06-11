package com.talent.animescrap.ui.viewmodels

import android.app.Application
import android.graphics.Color
import android.net.Uri
import android.support.v4.media.session.MediaSessionCompat
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.MimeTypes
import com.talent.animescrap.model.AnimeStreamLink
import com.talent.animescrap.repo.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val app: Application,
    private val animeRepository: AnimeRepository,
    private val savedStateHandle: SavedStateHandle,
    val player: ExoPlayer
) : ViewModel() {
    private val settingsPreferenceManager = PreferenceManager.getDefaultSharedPreferences(app)

    // Video Cache
    private val isVideoCacheEnabled = settingsPreferenceManager.getBoolean("video_cache", true)

    private val _animeStreamLink: MutableLiveData<AnimeStreamLink> = MutableLiveData()
    val animeStreamLink: LiveData<AnimeStreamLink> = _animeStreamLink
    fun setAnimeLink(animeUrl: String, animeEpCode: String, extras: List<String>, getNextEp: Boolean = false) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                println("STREAM GET LINK")
                animeRepository.getStreamLink(animeUrl, animeEpCode, extras).apply {
                    _animeStreamLink.postValue(this@apply)
                    withContext(Dispatchers.Main) {
                        if(!savedDone.value || getNextEp) {
                            println("Prrpate")
                            prepareMediaSource()
                            savedStateHandle["done"] = true
                        }
                    }
                }
            }
        }

    }

    var qualityTrackGroup: Tracks.Group? = null
    private var qualityMapUnsorted: MutableMap<String, Int> = mutableMapOf()
    var qualityMapSorted: MutableMap<String, Int> = mutableMapOf()

    private var mediaSession: MediaSessionCompat =
        MediaSessionCompat(app, "AnimeScrap Media Session")
    private var mediaSessionConnector: MediaSessionConnector = MediaSessionConnector(mediaSession)

    var simpleCache: SimpleCache? = null
    private val databaseProvider = StandaloneDatabaseProvider(app)

    private val savedDone = savedStateHandle.getStateFlow("done", false)

    init {
        player.prepare()
        player.playWhenReady = true
        mediaSessionConnector.setPlayer(player)
        mediaSession.isActive = true
        player.addListener(getQualitiesListener())

        // Cache
        simpleCache?.release()
        simpleCache = SimpleCache(
            File(
                app.cacheDir,
                "exoplayer"
            ).also { it.deleteOnExit() }, // Ensures always fresh file
            LeastRecentlyUsedCacheEvictor(300L * 1024L * 1024L),
            databaseProvider
        )
    }

    private fun getQualitiesListener(): Player.Listener {
        return object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                // Update UI using current tracks.
                for (trackGroup in tracks.groups) {
                    // Group level information.
                    if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
                        for (i in 0 until trackGroup.length) {
                            val trackFormat = trackGroup.getTrackFormat(i).height
                            if (trackGroup.isTrackSupported(i) && trackGroup.isTrackSelected(i)) {
                                qualityMapUnsorted["${trackFormat}p"] = i
                            }
                        }
                        qualityMapUnsorted.entries.sortedBy { it.key.replace("p", "").toInt() }
                            .reversed().forEach { qualityMapSorted[it.key] = it.value }

                        qualityTrackGroup = trackGroup
                    }

                }
            }
        }

    }

    private fun releaseCache() {
        simpleCache?.release()
        simpleCache = null
    }

    private fun setMediaSource(mediaSource: MediaSource) {
        println("Set media Source")
        player.stop()
        player.prepare()
        player.setMediaSource(mediaSource)
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
        releaseCache()
    }

    private fun releasePlayer() {
        player.release()
        mediaSession.release()
    }

    private fun prepareMediaSource() {
        if (animeStreamLink.value == null) return
        var mediaSource: MediaSource
        val mediaItem: MediaItem
        val headerMap = mutableMapOf(
            "Accept" to "*/*",
            "Connection" to "keep-alive",
            "Upgrade-Insecure-Requests" to "1"
        )
        animeStreamLink.value!!.extraHeaders?.forEach { header ->
            headerMap[header.key] = header.value
        }

        println(headerMap)
        val dataSourceFactory
                : DataSource.Factory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(headerMap)
            .setReadTimeoutMs(20000)
            .setConnectTimeoutMs(20000)

        if (isVideoCacheEnabled) {

            val cacheFactory = CacheDataSource.Factory().apply {
                setCache(simpleCache!!)
                setUpstreamDataSourceFactory(dataSourceFactory)
            }
            mediaItem =
                MediaItem.fromUri(animeStreamLink.value!!.link)
            mediaSource = if (animeStreamLink.value!!.isHls) {
                HlsMediaSource.Factory(cacheFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            } else {
                ProgressiveMediaSource.Factory(cacheFactory)
                    .createMediaSource(mediaItem)
            }
        } else {
            mediaItem =
                MediaItem.fromUri(animeStreamLink.value!!.link)
            mediaSource = if (animeStreamLink.value!!.isHls) {
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            } else {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }

        if (animeStreamLink.value!!.subsLink.isNotBlank()) {
//            subsToggleButton.isChecked = true
//            subsToggleButton.visibility = View.VISIBLE
            val subStyle = CaptionStyleCompat(
                Color.WHITE,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                Color.BLACK,
                null
            )
//            playerView.subtitleView?.setStyle(subStyle)
            val subtitleMediaSource = SingleSampleMediaSource.Factory(dataSourceFactory)
                .createMediaSource(
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(animeStreamLink.value!!.subsLink))
                        .apply {
                            if (animeStreamLink.value!!.subsLink.contains("srt"))
                                setMimeType(MimeTypes.APPLICATION_SUBRIP)
                            else
                                setMimeType(MimeTypes.TEXT_VTT)
                            setLanguage("en")
                            setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        }.build(),
                    C.TIME_UNSET
                )
            mediaSource = MergingMediaSource(mediaSource, subtitleMediaSource)
        } else {
//            subsToggleButton.isChecked = false
//            subsToggleButton.visibility = View.GONE
        }
        setMediaSource(mediaSource)
    }

}