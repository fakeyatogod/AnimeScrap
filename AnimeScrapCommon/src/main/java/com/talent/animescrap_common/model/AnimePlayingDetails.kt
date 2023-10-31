package com.talent.animescrap_common.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class AnimePlayingDetails(
    val animeName: String,
    val animeUrl: String,
    var animeEpisodeIndex: String,
    val animeEpisodeMap: HashMap<String, String>,
    val animeTotalEpisode: String,
    val epType: String
) : Parcelable