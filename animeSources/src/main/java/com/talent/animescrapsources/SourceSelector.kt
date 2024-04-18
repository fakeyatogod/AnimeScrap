package com.talent.animescrapsources

import android.content.Context
import com.talent.animescrap_common.source.AnimeSource
import com.talent.animescrapsources.animesources.*

class SourceSelector(context: Context) {
    val sourceMap: Map<String, AnimeSource> = mapOf(
        "yugen" to YugenSource(),
        "allanime" to AllAnimeSource(),
        "animepahe" to AnimePaheSource(),
        "kawaiifu" to KawaiifuSource(context),
        "aniwave" to AniWaveSource(),
        "kiss_kh" to KissKhSource(),
        "asian_load" to AsianLoad(),
        "my_asian_tv" to MyAsianTvSource(),
    )

    fun getSelectedSource(selectedSource: String): AnimeSource {
        if (selectedSource in sourceMap.keys) {
            return sourceMap[selectedSource]!!
        }
        return sourceMap["yugen"]!!
    }
}