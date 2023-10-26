package com.talent.animescrap.animesources

import com.talent.animescrap_common.model.AnimeDetails
import com.talent.animescrap_common.model.AnimeStreamLink
import com.talent.animescrap_common.model.SimpleAnime

interface AnimeSource {
    suspend fun animeDetails(contentLink: String): AnimeDetails
    suspend fun searchAnime(searchedText: String): ArrayList<SimpleAnime>
    suspend fun latestAnime(): ArrayList<SimpleAnime>
    suspend fun trendingAnime(): ArrayList<SimpleAnime>
    suspend fun streamLink(
        animeUrl: String,
        animeEpCode: String,
        extras: List<String>? = null
    ): AnimeStreamLink
}

