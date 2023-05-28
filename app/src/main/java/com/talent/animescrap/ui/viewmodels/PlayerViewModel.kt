package com.talent.animescrap.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    val player: ExoPlayer
) : ViewModel() {
    private val savedMediaSource: StateFlow<MediaSource?> =
        savedStateHandle.getStateFlow("mediaSource", null)
    private val savedUrl = savedStateHandle.getStateFlow("playerUrl", "")
    private var done = false

    init {
        player.prepare()
        player.playWhenReady = true
    }

    fun setUrl(url: String) {
        if (savedUrl.value == "") {
            savedStateHandle["playerUrl"] = url
            player.setMediaItem(MediaItem.fromUri(url))
        }

    }

    fun setMediaSource(mediaSource: MediaSource) {
        if (savedMediaSource.value == null && !done) {
            player.setMediaSource(mediaSource)
            done = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }

    fun releasePlayer() {
        player.release()
    }
}