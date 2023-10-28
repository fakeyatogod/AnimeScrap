package com.talent.animescrapsources

import android.content.Context
import com.talent.animescrap_common.source.AnimeSource
import com.talent.animescrapsources.animesources.*

class SourceSelector(context: Context) {
    val sourceMap: Map<String, AnimeSource> = mapOf(
        "yugen" to YugenSource(),
        "allanime" to AllAnimeSource(),
        "enime" to EnimeSource(),
        "kiss_kh" to KissKhSource(),
        "animepahe" to AnimePaheSource(context),
        "kawaiifu" to KawaiifuSource(context),
        "marin_moe" to MarinMoeSource(),
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