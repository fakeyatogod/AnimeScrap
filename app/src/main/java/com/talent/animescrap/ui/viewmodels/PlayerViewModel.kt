package com.talent.animescrap.ui.viewmodels

import android.app.Application
import android.support.v4.media.session.MediaSessionCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val app : Application,
    private val savedStateHandle: SavedStateHandle,
    val player: ExoPlayer
) : ViewModel() {

     var qualityTrackGroup: Tracks.Group? = null
    private var qualityMapUnsorted: MutableMap<String, Int> = mutableMapOf()
     var qualityMapSorted: MutableMap<String, Int> = mutableMapOf()
    private var mediaSession: MediaSessionCompat = MediaSessionCompat(app, "AnimeScrap Media Session")
    private var mediaSessionConnector: MediaSessionConnector = MediaSessionConnector(mediaSession)
    var simpleCache: SimpleCache? = null
    private val databaseProvider = StandaloneDatabaseProvider(app)
    private val savedUrl = savedStateHandle.getStateFlow("playerUrl", "")
    private val savedDone = savedStateHandle.getStateFlow("done",false)

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
//                            println(trackGroup.getTrackFormat(i))
//                            println(trackGroup.isTrackSupported(i))
//                            println(trackGroup.isTrackSelected(i))
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

    fun setUrl(url: String) {
        if (savedUrl.value == "") {
            savedStateHandle["playerUrl"] = url
            player.setMediaItem(MediaItem.fromUri(url))
        }

    }

    private fun releaseCache() {
        simpleCache?.release()
        simpleCache = null
    }

        fun setMediaSource(mediaSource: MediaSource) {
        if (!savedDone.value) {
            player.setMediaSource(mediaSource)
            savedStateHandle["done"] = true
        }
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
}