package com.talent.animescrap.animesources

import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.SimpleAnime

interface AnimeSource {
    suspend fun animeDetails(contentLink: String): AnimeDetails
    suspend fun searchAnime(searchUrl: String): ArrayList<SimpleAnime>
    suspend fun latestAnime(): ArrayList<SimpleAnime>
    suspend fun trendingAnime(): ArrayList<SimpleAnime>
    suspend fun streamLink(animeEpUrl: String): String
}

